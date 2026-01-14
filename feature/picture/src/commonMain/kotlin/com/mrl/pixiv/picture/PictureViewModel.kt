package com.mrl.pixiv.picture

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import co.touchlab.kermit.Logger
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.ugoira.UgoiraMetadata
import com.mrl.pixiv.common.datasource.local.entity.DownloadStatus
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.DownloadManager
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.paging.RelatedIllustPaging
import com.mrl.pixiv.common.repository.requireUserPreferenceValue
import com.mrl.pixiv.common.repository.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.TAG
import com.mrl.pixiv.common.util.ToastUtil
import com.mrl.pixiv.common.util.ZipUtil
import com.mrl.pixiv.common.util.isDesktop
import com.mrl.pixiv.common.util.platform
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.state
import com.mrl.pixiv.strings.copy_to_clipboard
import com.mrl.pixiv.strings.download_add_to_queue
import com.mrl.pixiv.strings.download_failed
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.write
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.utils.io.asSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

@Stable
data class PictureState(
    val illust: Illust? = null,
    val userIllusts: ImmutableList<Illust> = persistentListOf(),
    val bottomSheetState: BottomSheetState? = null,
    val loading: Boolean = false,
    val ugoiraState: UgoiraState = UgoiraState(),
)

@Stable
data class BottomSheetState(
    val index: Int = 0,
    val downloadUrl: String = "",
    val downloadSize: Long = 0L,
)

@Stable
data class UgoiraState(
    val ugoiraImages: ImmutableList<Pair<ImageBitmap, Long>> = persistentListOf(),
    val loading: Boolean = false,
    val isPlaying: Boolean = false,
)

sealed class PictureAction : ViewIntent {
    data class GetIllustDetail(val illustId: Long) : PictureAction()
    data class AddSearchHistory(val keyword: String) : PictureAction()
    data class GetUserIllustsIntent(
        val userId: Long,
    ) : PictureAction()

    data class BookmarkIllust(val illustId: Long) : PictureAction()

    data class UnBookmarkIllust(val illustId: Long) : PictureAction()

    data class DownloadUgoira(val illustId: Long) : PictureAction()

    data class DownloadIllust(
        val illustId: Long,
        val index: Int,
        val originalUrl: String,
    ) : PictureAction()

    data class GetPictureInfo(val index: Int) : PictureAction()
}

@KoinViewModel
class PictureViewModel(
    illust: Illust?,
    illustId: Long?,
    private val zipUtil: ZipUtil,
) : BaseMviViewModel<PictureState, PictureAction>(
    initialState = PictureState(illust = illust),
), KoinComponent {
    private val imageOkHttpClient: HttpClient by inject(named<ImageClient>())
    private val downloadManager: DownloadManager by inject()
    val relatedIllusts = Pager(PagingConfig(pageSize = 20)) {
        RelatedIllustPaging(illust?.id ?: illustId!!)
    }.flow.cachedIn(viewModelScope)

    private val cachedDownloadSize = mutableMapOf<Int, Long>()
    private val ugoiraDir = FileKit.cacheDir / "ugoira"
    private var cachedUgoiraMetadata: UgoiraMetadata? = null

    override suspend fun handleIntent(intent: PictureAction) {
        when (intent) {
            is PictureAction.GetIllustDetail -> getIllustDetail(intent.illustId)
            is PictureAction.AddSearchHistory -> addSearchHistory(intent.keyword)
            is PictureAction.GetUserIllustsIntent -> getUserIllusts(intent.userId)
            is PictureAction.BookmarkIllust -> bookmark(intent.illustId)

            is PictureAction.UnBookmarkIllust -> unBookmark(intent.illustId)
            is PictureAction.DownloadIllust -> downloadIllust(
                intent.illustId,
                intent.index,
                intent.originalUrl,
            )

            is PictureAction.DownloadUgoira -> downloadUgoira(intent.illustId)
            is PictureAction.GetPictureInfo -> getPictureInfo(intent.index)
        }
    }

    init {
        when {
            illust != null -> {
                dispatch(PictureAction.GetUserIllustsIntent(illust.user.id))
            }

            illustId != null -> {
                dispatch(PictureAction.GetIllustDetail(illustId))
            }
        }
    }

    private fun downloadUgoira(illustId: Long) {
        launchIO {
            updateState { copy(ugoiraState = ugoiraState.copy(loading = true)) }
            val resp = PixivRepository.getUgoiraMetadata(illustId)
            cachedUgoiraMetadata = resp.ugoiraMetadata
            if (!ugoiraDir.exists()) ugoiraDir.createDirectories()
            val file = ugoiraDir / "$illustId.zip"
            if (file.exists() && file.size() > 0) {
                val imageFiles = unzipUgoira(file, illustId).mapIndexed { index, img ->
                    img.readBytes().decodeToImageBitmap() to resp.ugoiraMetadata.frames[index].delay
                }
                Logger.e(TAG) { "downloadUgoira: $imageFiles" }
                updateState {
                    copy(
                        ugoiraState = ugoiraState.copy(
                            ugoiraImages = imageFiles.toImmutableList(),
                            loading = false,
                            isPlaying = true
                        )
                    )
                }
            } else {
                val zipUrl = resp.ugoiraMetadata.zipUrls.medium
                val response = imageOkHttpClient.request {
                    url.takeFrom(zipUrl)
                }
                if (response.status.isSuccess()) {
                    response.bodyAsChannel().asSource().buffered().use { source ->
                        file.write(source.readByteArray())
                    }
                    // 解压
                    val imageFiles = unzipUgoira(file, illustId).mapIndexed { index, img ->
                        img.readBytes()
                            .decodeToImageBitmap() to resp.ugoiraMetadata.frames[index].delay
                    }
                    Logger.e(TAG) { "downloadUgoira: $imageFiles" }
                    updateState {
                        copy(
                            ugoiraState = ugoiraState.copy(
                                ugoiraImages = imageFiles.toImmutableList(),
                                loading = false,
                                isPlaying = true
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun unzipUgoira(
        zipFile: PlatformFile,
        illustId: Long
    ): MutableList<PlatformFile> {
        val unzipDir = ugoiraDir / "$illustId"
        val list = mutableListOf<PlatformFile>()
        unzipDir.createDirectories()
        val entries = zipUtil.getZipEntryList(zipFile.absolutePath())
        entries.forEach { (zipEntryName, isDirectory) ->
            val newFile = PlatformFile(unzipDir, zipEntryName)
            if (isDirectory) {
                newFile.createDirectories()
            } else {
                if (!newFile.exists()) {
                    zipUtil.getZipEntryContent(zipFile.absolutePath(), zipEntryName)?.let { bytes ->
                        newFile.write(bytes)
                    }
                }
                list.add(newFile)
            }
        }
        return list
    }

    private fun getIllustDetail(illustId: Long) {
        launchIO {
            val resp = PixivRepository.getIllustDetail(
                illustId = illustId,
                filter = Filter.ANDROID.value
            )
            updateState {
                copy(illust = resp.illust)
            }
            getUserIllusts(resp.illust.user.id)
        }
    }

    private fun addSearchHistory(keyword: String) {
        SearchRepository.addSearchHistory(keyword)
    }

    fun downloadIllust(
        illustId: Long,
        index: Int,
        originalUrl: String,
    ) {
        launchIO {
            val illust = state.illust ?: return@launchIO
            val title = illust.title
            val userId = illust.user.id
            val userName = illust.user.name
            val thumbnailUrl = illust.imageUrls.squareMedium

            val subFolder = if (requireUserPreferenceValue.downloadSubFolderByUser) {
                illust.user.id.toString()
            } else {
                null
            }

            downloadManager.enqueueDownload(
                illustId = illustId,
                index = index,
                title = title,
                userId = userId,
                userName = userName,
                thumbnailUrl = thumbnailUrl,
                originalUrl = originalUrl,
                subFolder = subFolder
            )

            closeBottomSheet()
            ToastUtil.safeShortToast(RStrings.download_add_to_queue)
        }
    }

    private fun unBookmark(illustId: Long) {
        BookmarkState.deleteBookmarkIllust(illustId)
    }

    private fun bookmark(illustId: Long) {
        BookmarkState.bookmarkIllust(illustId)
    }

    private fun getUserIllusts(userId: Long) {
        launchIO {
            val resp = PixivRepository.getUserIllusts(
                userId = userId,
                type = Type.Illust.value
            )
            updateState {
                copy(userIllusts = resp.illusts.toImmutableList())
            }
        }
    }

    private fun getPictureInfo(index: Int) {
        val illust = state.illust ?: return
        val url = if (illust.pageCount > 1) {
            illust.metaPages?.get(index)?.imageUrls?.original
        } else {
            illust.metaSinglePage.originalImageURL
        } ?: return
        val cachedSize = cachedDownloadSize[index]
        updateState {
            copy(bottomSheetState = BottomSheetState(index, url, cachedSize ?: 0))
        }
        if (cachedSize == null) {
            launchIO {
                val downloadSize = calculateImageSize(url)
                updateState {
                    cachedDownloadSize[index] = downloadSize
                    copy(bottomSheetState = bottomSheetState?.copy(downloadSize = downloadSize))
                }
            }
        }
    }

    fun getUgoiraInfo() {
        launchIO {
            val illustId = state.illust?.id ?: return@launchIO
            val metadata = cachedUgoiraMetadata
                ?: PixivRepository.getUgoiraMetadata(illustId).ugoiraMetadata.also {
                    cachedUgoiraMetadata = it
                }
            val url = metadata.zipUrls.medium
            val cachedSize = cachedDownloadSize[0]
            updateState {
                copy(bottomSheetState = BottomSheetState(0, url, cachedSize ?: 0))
            }
            if (cachedSize == null) {
                launchIO {
                    val downloadSize = calculateImageSize(url)
                    updateState {
                        cachedDownloadSize[0] = downloadSize
                        copy(bottomSheetState = bottomSheetState?.copy(downloadSize = downloadSize))
                    }
                }
            }
        }
    }

    private suspend fun calculateImageSize(url: String): Long {
        return try {
            val response = imageOkHttpClient.request {
                method = HttpMethod.Head
                url(url)
            }
            val contentLength = response.contentLength() ?: 0
            return contentLength
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun closeBottomSheet() {
        updateState {
            copy(bottomSheetState = null)
        }
    }

    fun showLoading(show: Boolean) {
        updateState {
            copy(loading = show)
        }
    }

    fun shareImage(
        index: Int,
        downloadUrl: String,
        illust: Illust,
    ) {
        launchIO {
            showLoading(true)
            closeBottomSheet()
            val subFolder = if (requireUserPreferenceValue.downloadSubFolderByUser) {
                illust.user.id.toString()
            } else {
                null
            }
            downloadManager.enqueueDownload(
                illustId = illust.id,
                index = index,
                title = illust.title,
                userId = illust.user.id,
                userName = illust.user.name,
                thumbnailUrl = illust.imageUrls.squareMedium,
                originalUrl = downloadUrl,
                subFolder = subFolder,
            ) { entity ->
                if (entity != null && entity.status == DownloadStatus.SUCCESS.value) {
                    ShareUtil.shareImage(entity.fileUri)
                    if (platform.isDesktop()) {
                        ToastUtil.safeShortToast(RStrings.copy_to_clipboard)
                    }
                } else {
                    ToastUtil.safeShortToast(RStrings.download_failed)
                }
                showLoading(false)
            }
        }
    }

    fun blockIllust() {
        BlockingRepositoryV2.blockIllust(state.illust?.id ?: return)
    }

    fun removeBlockIllust() {
        BlockingRepositoryV2.removeBlockIllust(state.illust?.id ?: return)
    }

    fun addHistory() {
        launchProcess(Dispatchers.IO) {
            PixivRepository.addIllustBrowsingHistory(state.illust?.id ?: return@launchProcess)
        }
    }

    fun removeBlockUser() {
        val userId = state.illust?.user?.id ?: return
        launchIO {
            PixivRepository.postMuteSetting(deleteUserIds = listOf(userId))
        }
        BlockingRepositoryV2.removeBlockUser(userId)
    }

    fun downloadUgoiraAsGIF() {
        launchIO {
            showLoading(true)
            val illust = state.illust ?: return@launchIO
            val illustId = illust.id
            val title = illust.title
            val userId = illust.user.id
            val userName = illust.user.name
            val thumbnailUrl = illust.imageUrls.squareMedium

            val subFolder = if (requireUserPreferenceValue.downloadSubFolderByUser) {
                state.illust?.user?.id?.toString()
            } else {
                null
            }

            val metadata = cachedUgoiraMetadata
                ?: PixivRepository.getUgoiraMetadata(illustId).ugoiraMetadata.also {
                    cachedUgoiraMetadata = it
                }
            val zipUrl = metadata.zipUrls.medium

            downloadManager.enqueueDownload(
                illustId = illustId,
                index = 0,
                title = title,
                userId = userId,
                userName = userName,
                thumbnailUrl = thumbnailUrl,
                originalUrl = zipUrl,
                subFolder = subFolder
            )

            closeBottomSheet()
            showLoading(false)
            ToastUtil.safeShortToast(RStrings.download_add_to_queue)
        }
    }

    fun toggleUgoiraPlayState() {
        updateState {
            copy(
                ugoiraState = ugoiraState.copy(
                    isPlaying = !ugoiraState.isPlaying
                )
            )
        }
    }

    override fun onCleared() {
        cachedDownloadSize.clear()
    }
}

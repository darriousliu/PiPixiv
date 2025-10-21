package com.mrl.pixiv.picture

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.mrl.pixiv.common.data.Filter
import com.mrl.pixiv.common.data.Illust
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.network.ImageClient
import com.mrl.pixiv.common.repository.PixivRepository
import com.mrl.pixiv.common.repository.SearchRepository
import com.mrl.pixiv.common.repository.paging.RelatedIllustPaging
import com.mrl.pixiv.common.util.AppUtil
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.ShareUtil
import com.mrl.pixiv.common.util.TAG
import com.mrl.pixiv.common.util.saveToAlbum
import com.mrl.pixiv.common.util.toBitmap
import com.mrl.pixiv.common.viewmodel.BaseMviViewModel
import com.mrl.pixiv.common.viewmodel.ViewIntent
import com.mrl.pixiv.common.viewmodel.bookmark.BookmarkState
import com.mrl.pixiv.common.viewmodel.state
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.time.Duration.Companion.seconds

@Stable
data class PictureState(
    val illust: Illust? = null,
    val userIllusts: ImmutableList<Illust> = persistentListOf(),
    val ugoiraImages: ImmutableList<Pair<Bitmap, Long>> = persistentListOf(),
    val bottomSheetState: BottomSheetState? = null,
    val loading: Boolean = false,
)

@Stable
data class BottomSheetState(
    val index: Int = 0,
    val downloadUrl: String = "",
    val downloadSize: Float = 0f,
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
) : BaseMviViewModel<PictureState, PictureAction>(
    initialState = PictureState(),
), KoinComponent {
    private val imageOkHttpClient: HttpClient by inject(named<ImageClient>())
    val relatedIllusts = Pager(PagingConfig(pageSize = 20)) {
        RelatedIllustPaging(illust?.id ?: illustId!!)
    }.flow.cachedIn(viewModelScope)

    private val cachedDownloadSize = mutableMapOf<Int, Float>()

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
                if (illust.type == Type.Ugoira) {
                    dispatch(PictureAction.DownloadUgoira(illust.id))
                }
                updateState { copy(illust = illust) }
            }

            illustId != null -> {
                dispatch(PictureAction.GetIllustDetail(illustId))
            }
        }
    }

    private fun downloadUgoira(illustId: Long) {
        launchIO {
            val resp = PixivRepository.getUgoiraMetadata(illustId)
            val file = AppUtil.appContext.cacheDir.resolve("$illustId.zip")
            if (file.exists() && file.length() > 0) {
                val imageFiles =
                    unzipUgoira(ZipFile(file), illustId).mapIndexed { index, img ->
                        img.toBitmap()!! to resp.ugoiraMetadata.frames[index].delay
                    }
                Log.e(TAG, "downloadUgoira: $imageFiles")
                updateState {
                    copy(ugoiraImages = imageFiles.toImmutableList())
                }
            } else {
                val zipUrl = resp.ugoiraMetadata.zipUrls.medium
                val response = imageOkHttpClient.request {
                    url.takeFrom(zipUrl)
                }
                if (response.status.isSuccess()) {
                    response.bodyAsChannel().toInputStream().use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    // 解压
                    val imageFiles =
                        unzipUgoira(ZipFile(file), illustId).mapIndexed { index, img ->
                            img.toBitmap()!! to resp.ugoiraMetadata.frames[index].delay
                        }
                    Log.e(TAG, "downloadUgoira: $imageFiles")
                    updateState {
                        copy(ugoiraImages = imageFiles.toImmutableList())
                    }
                }
            }
        }
    }

    private fun unzipUgoira(zipFile: ZipFile, illustId: Long): MutableList<File> {
        val unzipDir = AppUtil.appContext.cacheDir.resolve("$illustId")
        val list = mutableListOf<File>()
        unzipDir.mkdirs()
        zipFile.entries().asSequence().forEach { zipEntry ->
            val newFile = File(unzipDir, zipEntry.name)
            if (zipEntry.isDirectory) {
                newFile.mkdirs()
            } else {
                if (!newFile.exists()) {
                    FileOutputStream(newFile).use { fileOutputStream ->
                        zipFile.getInputStream(zipEntry).use { inputStream ->
                            inputStream.copyTo(fileOutputStream)
                        }
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

    private fun downloadIllust(
        illustId: Long,
        index: Int,
        originalUrl: String,
    ) {
        launchIO {
            showLoading(true)
            // 使用coil下载图片
            val imageLoader = SingletonImageLoader.get(AppUtil.appContext)
            val request = ImageRequest.Builder(AppUtil.appContext)
                .data(originalUrl)
                .build()
            val result = withTimeoutOrNull(10.seconds) {
                imageLoader.execute(request)
            }
            result ?: run {
                closeBottomSheet()
                return@launchIO
            }
            result.image?.toBitmap()?.saveToAlbum("${illustId}_$index") {
                with(AppUtil.appContext) {
                    closeBottomSheet()
                    handleError(Exception(getString(if (it) RString.download_success else RString.download_failed)))
                }
            }
            closeBottomSheet()
            showLoading(false)
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
            copy(bottomSheetState = BottomSheetState(index, url, cachedSize ?: 0f))
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

    private suspend fun calculateImageSize(url: String): Float {
        return try {
            val response = imageOkHttpClient.request {
                method = HttpMethod.Head
                url(url)
            }
            val contentLength = response.contentLength()?.toFloat() ?: 0f
            return contentLength / 1024 / 1024
        } catch (e: Exception) {
            e.printStackTrace()
            0f
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
        shareLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        launchIO {
            showLoading(true)
            ShareUtil.createShareImage(index, downloadUrl, illust, shareLauncher)
            showLoading(false)
            closeBottomSheet()
        }
    }

    override fun onCleared() {
        cachedDownloadSize.clear()
    }
}
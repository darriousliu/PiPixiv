package com.mrl.pixiv.common.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrl.pixiv.common.coroutine.launchCatch
import com.mrl.pixiv.common.data.comment.Comment
import com.mrl.pixiv.common.datasource.local.dao.BlockContentDao
import com.mrl.pixiv.common.datasource.local.entity.BlockCommentEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockIllustEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockNovelEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockTagEntity
import com.mrl.pixiv.common.datasource.local.entity.BlockUserEntity
import com.mrl.pixiv.common.mmkv.MMKVOwner
import com.mrl.pixiv.common.mmkv.mmkvSerializable
import com.mrl.pixiv.common.mmkv.mmkvStringSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object BlockingRepositoryV2 : KoinComponent {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val contentDao by inject<BlockContentDao>()
    private var initialized = false
    private var legacyMigrationTriggered = false

    private val blockIllustIds = MutableStateFlow<Set<Long>>(emptySet())
    private val blockNovelIds = MutableStateFlow<Set<Long>>(emptySet())
    private val blockUserIds = MutableStateFlow<Set<Long>>(emptySet())
    private val blockCommentIds = MutableStateFlow<Set<Long>>(emptySet())
    private val blockTagExactValues = MutableStateFlow<Set<String>>(emptySet())
    private val blockTagRegexValues = MutableStateFlow<List<Regex>>(emptyList())

    val blockCommentsFlow = contentDao.observeComments().mapLatest { list ->
        list.map { json.decodeFromString<Comment>(it.commentJson) }
    }

    val blockIllustItemsFlow = contentDao.observeIllusts().map { entities ->
        entities.map { BlockIllustEntity(illustId = it.illustId, title = it.title) }
    }

    val blockNovelItemsFlow = contentDao.observeNovels().map { entities ->
        entities.map { BlockNovelEntity(novelId = it.novelId, title = it.title) }
    }

    val blockUserItemsFlow = contentDao.observeUsers().map { entities ->
        entities.map { BlockUserEntity(userId = it.userId, name = it.name) }
    }

    val blockTagItemsFlow = contentDao.observeTags().map { entities ->
        entities.map { BlockTagEntity(tag = it.tag, isRegex = it.isRegex) }
    }

    fun initAfterKoin() {
        if (initialized) return
        val dao = runCatching { contentDao }.getOrNull() ?: return
        initialized = true
        observeDatabase(dao)
        migrateLegacyDataIfNeeded(dao)
    }

    fun blockIllust(illustId: Long, title: String) {
        write {
            upsertIllust(
                BlockIllustEntity(
                    illustId = illustId,
                    title = title
                )
            )
        }
    }

    fun removeBlockIllust(illustId: Long) {
        write { deleteIllust(illustId) }
    }

    fun blockNovel(novelId: Long, title: String) {
        write {
            upsertNovel(
                BlockNovelEntity(
                    novelId = novelId,
                    title = title
                )
            )
        }
    }

    fun removeBlockNovel(novelId: Long) {
        write { deleteNovel(novelId) }
    }

    fun blockUser(userId: Long, name: String) {
        write {
            upsertUser(
                BlockUserEntity(
                    userId = userId,
                    name = name
                )
            )
        }
    }

    fun removeBlockUser(userId: Long) {
        write { deleteUser(userId) }
    }

    fun blockTag(tag: String, isRegex: Boolean = false) {
        val normalizedTag = tag.trim()
        if (normalizedTag.isEmpty()) return
        write {
            upsertTag(
                BlockTagEntity(
                    tag = normalizedTag,
                    isRegex = isRegex
                )
            )
        }
    }

    fun removeBlockTag(tag: String) {
        val normalizedTag = tag.trim()
        if (normalizedTag.isEmpty()) return
        write { deleteTag(normalizedTag) }
    }

    fun blockComment(comment: Comment) {
        write {
            upsertComment(
                BlockCommentEntity(
                    commentId = comment.id,
                    commentJson = json.encodeToString(comment)
                )
            )
        }
    }

    fun removeBlockComment(commentId: Long) {
        write { deleteComment(commentId) }
    }

    @Suppress("DEPRECATION")
    fun migrate() {
        initAfterKoin()
    }

    suspend fun restore(
        illusts: List<BlockIllustEntity>,
        users: List<BlockUserEntity>,
        comments: List<Comment>,
        novels: List<BlockNovelEntity> = emptyList(),
        tags: List<BlockTagEntity> = emptyList(),
    ) {
        val dao = contentDao
        withContext(Dispatchers.IO) {
            val illustEntities = illusts
                .distinctBy { it.illustId }
            if (illustEntities.isNotEmpty()) {
                dao.upsertIllusts(illustEntities)
            }

            val novelEntities = novels
                .distinctBy { it.novelId }
            if (novelEntities.isNotEmpty()) {
                dao.upsertNovels(novelEntities)
            }

            val userEntities = users
                .distinctBy { it.userId }
            if (userEntities.isNotEmpty()) {
                dao.upsertUsers(userEntities)
            }

            val tagEntities = tags
                .map { it.copy(tag = it.tag.trim()) }
                .filter { it.tag.isNotEmpty() }
                .distinctBy { it.tag }
            if (tagEntities.isNotEmpty()) {
                dao.upsertTags(tagEntities)
            }

            val commentEntities = comments
                .distinctBy { it.id }
                .map {
                    BlockCommentEntity(
                        commentId = it.id,
                        commentJson = json.encodeToString(it),
                    )
                }
            if (commentEntities.isNotEmpty()) {
                dao.upsertComments(commentEntities)
            }
        }
    }

    suspend fun restore(
        illusts: Set<String>,
        users: Set<String>,
        comments: List<Comment>,
        novels: Set<String> = emptySet(),
        tags: Set<String> = emptySet(),
    ) {
        restore(
            illusts = illusts
                .mapNotNull { it.toLongOrNull() }
                .distinct()
                .map { BlockIllustEntity(illustId = it) },
            users = users
                .mapNotNull { it.toLongOrNull() }
                .distinct()
                .map { BlockUserEntity(userId = it) },
            comments = comments,
            novels = novels
                .mapNotNull { it.toLongOrNull() }
                .distinct()
                .map { BlockNovelEntity(novelId = it) },
            tags = tags
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .map { BlockTagEntity(tag = it, isRegex = false) },
        )
    }

    fun isCommentBlocked(commentId: Long): Boolean = commentId in blockCommentIds.value

    fun isTagBlocked(tag: String): Boolean {
        val value = tag.trim()
        if (value.isEmpty()) return false
        if (value in blockTagExactValues.value) return true
        return blockTagRegexValues.value.any { regex ->
            runCatching { regex.matches(value) }.getOrDefault(false)
        }
    }

    @Composable
    fun collectIllustBlockAsState(illustId: Long): Boolean {
        val isBlocked by remember(illustId) {
            blockIllustIds
                .map { illustId in it }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = false)
        return isBlocked
    }

    @Composable
    fun collectUserBlockAsState(userId: Long): Boolean {
        val isBlocked by remember(userId) {
            blockUserIds
                .map { userId in it }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = false)
        return isBlocked
    }

    @Composable
    fun collectNovelBlockAsState(novelId: Long): Boolean {
        val isBlocked by remember(novelId) {
            blockNovelIds
                .map { novelId in it }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = false)
        return isBlocked
    }

    @Composable
    fun collectCommentBlockAsState(commentId: Long): Boolean {
        val isBlocked by remember(commentId) {
            blockCommentIds
                .map { commentId in it }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = false)
        return isBlocked
    }

    private fun write(block: suspend BlockContentDao.() -> Unit) {
        scope.launchCatch {
            contentDao.block()
        }
    }

    private fun observeDatabase(dao: BlockContentDao) {
        scope.launchCatch {
            dao.observeIllusts().collect { entities ->
                val idSet = entities.mapTo(mutableSetOf()) { it.illustId }
                blockIllustIds.value = idSet
            }
        }
        scope.launchCatch {
            dao.observeNovels().collect { entities ->
                val idSet = entities.mapTo(mutableSetOf()) { it.novelId }
                blockNovelIds.value = idSet
            }
        }
        scope.launchCatch {
            dao.observeUsers().collect { entities ->
                val idSet = entities.mapTo(mutableSetOf()) { it.userId }
                blockUserIds.value = idSet
            }
        }
        scope.launchCatch {
            dao.observeTags().collect { entities ->
                updateTagMatchIndexes(entities)
            }
        }
        scope.launchCatch {
            dao.observeComments().collect { entities ->
                blockCommentIds.value = entities.mapTo(mutableSetOf()) { it.commentId }
            }
        }
    }

    private fun updateTagMatchIndexes(tags: List<BlockTagEntity>) {
        val exactTags = mutableSetOf<String>()
        val regexList = mutableListOf<Regex>()
        tags.forEach { item ->
            if (item.isRegex) {
                runCatching { Regex(item.tag) }.getOrNull()?.let(regexList::add)
            } else {
                exactTags += item.tag
            }
        }
        blockTagExactValues.value = exactTags
        blockTagRegexValues.value = regexList
    }

    @Suppress("DEPRECATION")
    private fun migrateLegacyDataIfNeeded(dao: BlockContentDao) {
        if (legacyMigrationTriggered) return
        legacyMigrationTriggered = true
        scope.launchCatch {
            val legacyIllusts = LegacyBlockingStore.blockIllusts.orEmpty() +
                    BlockingRepository.blockIllustsFlow.value.orEmpty()
            val legacyUsers = LegacyBlockingStore.blockUsers.orEmpty() +
                    BlockingRepository.blockUsersFlow.value.orEmpty()
            val legacyNovels = LegacyBlockingStore.blockNovels.orEmpty()
            val legacyTags = LegacyBlockingStore.blockTags.orEmpty()
            val legacyComments = LegacyBlockingStore.blockComments.distinctBy { it.id }

            val hasLegacyData = legacyIllusts.isNotEmpty() ||
                    legacyUsers.isNotEmpty() ||
                    legacyNovels.isNotEmpty() ||
                    legacyTags.isNotEmpty() ||
                    legacyComments.isNotEmpty()

            if (!hasLegacyData) return@launchCatch

            runCatching {
                val illustEntities = legacyIllusts.mapNotNull { it.toLongOrNull() }
                    .distinct()
                    .map { BlockIllustEntity(illustId = it) }
                if (illustEntities.isNotEmpty()) {
                    dao.upsertIllusts(illustEntities)
                }

                val novelEntities = legacyNovels.mapNotNull { it.toLongOrNull() }
                    .distinct()
                    .map { BlockNovelEntity(novelId = it) }
                if (novelEntities.isNotEmpty()) {
                    dao.upsertNovels(novelEntities)
                }

                val userEntities = legacyUsers.mapNotNull { it.toLongOrNull() }
                    .distinct()
                    .map { BlockUserEntity(userId = it) }
                if (userEntities.isNotEmpty()) {
                    dao.upsertUsers(userEntities)
                }

                val tagEntities = legacyTags
                    .distinct()
                    .map { BlockTagEntity(tag = it, isRegex = false) }
                if (tagEntities.isNotEmpty()) {
                    dao.upsertTags(tagEntities)
                }

                val commentEntities = legacyComments
                    .map {
                        BlockCommentEntity(
                            commentId = it.id,
                            commentJson = json.encodeToString(it),
                        )
                    }
                if (commentEntities.isNotEmpty()) {
                    dao.upsertComments(commentEntities)
                }

                LegacyBlockingStore.clear()
                BlockingRepository.restore(emptySet(), emptySet())
            }
        }
    }
}

private object LegacyBlockingStore : MMKVOwner {
    override val id: String = "block_content"

    var blockIllusts by mmkvStringSet(emptySet())
    var blockNovels by mmkvStringSet(emptySet())
    var blockUsers by mmkvStringSet(emptySet())
    var blockTags by mmkvStringSet(emptySet())
    var blockComments by mmkvSerializable<List<Comment>>(emptyList())

    fun clear() {
        blockIllusts = emptySet()
        blockNovels = emptySet()
        blockUsers = emptySet()
        blockTags = emptySet()
        blockComments = emptyList()
    }
}

package com.mrl.pixiv.common.repository.recovery

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MigrationItem {
    Session, Settings, SearchAndTags, SeriesProgress, GlobalDatabase,
    BrowsingHistory, ReadingProgress, ReadLater, TranslationCache, LegacyBlocking,
}

@Serializable
enum class MigrationReason {
    StorageSpace, Corrupted, Io, Unknown,
}

@Serializable
sealed interface ItemStatus {
    @Serializable
    @SerialName("Pending")
    data class Pending(val reason: MigrationReason? = null) : ItemStatus

    @Serializable
    @SerialName("Done")
    data object Done : ItemStatus

    @Serializable
    @SerialName("Skipped")
    data object Skipped : ItemStatus

    @Serializable
    @SerialName("Failed")
    data class Failed(val attempts: Int, val reason: MigrationReason) : ItemStatus

    @Serializable
    @SerialName("Abandoned")
    data object Abandoned : ItemStatus
}

val ItemStatus.isResolved: Boolean
    get() = this == ItemStatus.Done || this == ItemStatus.Skipped || this == ItemStatus.Abandoned

interface DataRecoveryRepository {
    val items: StateFlow<Map<MigrationItem, ItemStatus>>
    val hasPending: StateFlow<Boolean>
    val recoverAfterLogin: StateFlow<Boolean>

    suspend fun retry(item: MigrationItem, targetUserId: Long? = null): ItemStatus
    suspend fun exportLegacyData(target: PlatformFile)
    suspend fun importLegacyPackage(source: PlatformFile, targetUserId: Long)
    suspend fun abandonAll()
    fun clearRecoverAfterLogin()
}

package com.mrl.pixiv.setting.block

import com.mrl.pixiv.common.data.Tag
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.data.mute.ForText
import com.mrl.pixiv.common.data.mute.MutedResp
import com.mrl.pixiv.common.data.mute.MutedTag
import com.mrl.pixiv.common.data.mute.MutedUser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudMuteInputTest {
    private val data = MutedResp(
        ForText(1, 500), 500, 2,
        listOf(MutedTag(false, Tag("existing"))), 1,
        listOf(MutedUser(false, User(id = 42))), 1,
    )

    @Test
    fun addingUsesServerQuotaAndRejectsDuplicateOrInvalidIds() {
        assertTrue(canAddCloudMute(data, CloudMuteInputKind.User, " 43 "))
        listOf("42", "0", "-1", "invalid", "9223372036854775808").forEach {
            assertFalse(canAddCloudMute(data, CloudMuteInputKind.User, it))
        }
        assertFalse(canAddCloudMute(data.copy(muteLimitCount = 2), CloudMuteInputKind.Tag, "new"))
        assertFalse(canAddCloudMute(null, CloudMuteInputKind.Tag, "new"))
    }

    @Test
    fun tagInputTrimsWhitespaceAndRejectsExistingTags() {
        assertTrue(canAddCloudMute(data, CloudMuteInputKind.Tag, " new "))
        assertFalse(canAddCloudMute(data, CloudMuteInputKind.Tag, " existing "))
        assertFalse(canAddCloudMute(data, CloudMuteInputKind.Tag, " \n "))
    }
}

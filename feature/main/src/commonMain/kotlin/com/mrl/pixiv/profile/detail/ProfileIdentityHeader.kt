package com.mrl.pixiv.profile.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.util.RDrawables
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.community_related_users
import com.mrl.pixiv.strings.copy_to_clipboard
import com.mrl.pixiv.strings.followed
import com.mrl.pixiv.strings.ic_profile_premium
import com.mrl.pixiv.strings.reading_manga_user_series
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun ProfileIdentityHeader(
    userInfo: UserDetailResp,
    onFollowing: () -> Unit,
    onCopyId: () -> Unit,
    onRelatedUsers: () -> Unit,
    onMangaSeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SelectionContainer(Modifier.weight(1f, fill = false)) {
                Text(
                    userInfo.user.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (userInfo.profile.isPremium) {
                Image(vectorResource(RDrawables.ic_profile_premium), null, Modifier.size(20.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${userInfo.profile.totalFollowUsers} ${stringResource(RStrings.followed)}",
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onFollowing).padding(vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer {
                Text(
                    "ID: ${userInfo.user.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCopyId) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    stringResource(RStrings.copy_to_clipboard),
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (userInfo.user.comment.isNotBlank()) {
            val comment = remember(userInfo.user.comment) {
                htmlToAnnotatedString(userInfo.user.comment, compactMode = true)
            }
            SelectionContainer {
                Text(
                    comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ProfileDiscoveryActions(
            enabled = userInfo.user.id > 0,
            onRelatedUsers = onRelatedUsers,
            onMangaSeries = onMangaSeries,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun ProfileDiscoveryActions(
    enabled: Boolean,
    onRelatedUsers: () -> Unit,
    onMangaSeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            ProfileDiscoveryRow(
                Icons.AutoMirrored.Rounded.MenuBook,
                stringResource(RStrings.reading_manga_user_series),
                enabled,
                onMangaSeries
            )
            HorizontalDivider(
                Modifier.padding(start = 56.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)
            )
            ProfileDiscoveryRow(
                Icons.Rounded.PeopleOutline,
                stringResource(RStrings.community_related_users),
                enabled,
                onRelatedUsers
            )
        }
    }
}

@Composable
private fun ProfileDiscoveryRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color =
            if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                alpha = .38f
            )
        Icon(
            icon,
            null,
            Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else color
        )
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = color)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

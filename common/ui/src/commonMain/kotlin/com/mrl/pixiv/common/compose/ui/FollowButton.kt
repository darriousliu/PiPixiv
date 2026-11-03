package com.mrl.pixiv.common.compose.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.strings.follow
import com.mrl.pixiv.strings.followed
import org.jetbrains.compose.resources.stringResource

@Composable
fun FollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUpdating: Boolean = false,
    enabled: Boolean = true,
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !isUpdating,
            shapes = ButtonDefaults.shapes(),
        ) {
            FollowButtonContent(isFollowing = true, isUpdating = isUpdating)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !isUpdating,
            shapes = ButtonDefaults.shapes(),
        ) {
            FollowButtonContent(isFollowing = false, isUpdating = isUpdating)
        }
    }
}

@Composable
private fun FollowButtonContent(isFollowing: Boolean, isUpdating: Boolean) {
    if (isUpdating) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        Icon(
            imageVector = if (isFollowing) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
    Text(text = stringResource(if (isFollowing) RStrings.followed else RStrings.follow))
}

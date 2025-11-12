package com.mrl.pixiv.profile.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowRgb565
import com.mrl.pixiv.common.compose.ui.BlockSurface
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepository
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RString
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.common.viewmodel.follow.isFollowing
import com.mrl.pixiv.feature.R
import com.mrl.pixiv.profile.detail.components.IllustWidget
import com.mrl.pixiv.profile.detail.components.NovelBookmarkWidget
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.math.pow

private const val KEY_USER_INFO = "user_info"
private const val KEY_USER_ILLUSTS = "user_illusts"
private const val KEY_USER_BOOKMARKS_ILLUSTS = "user_bookmarks_illusts"
private const val KEY_USER_BOOKMARKS_NOVELS = "user_bookmarks_novels"
private const val KEY_SPACE = "space"

@Composable
fun ProfileDetailScreen(
    uid: Long,
    modifier: Modifier = Modifier,
    viewModel: ProfileDetailViewModel = koinViewModel { parametersOf(uid) },
    navigationManager: NavigationManager = koinInject(),
) {
    val state = viewModel.asState()
    val userInfo = state.userInfo
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val isBlocked = BlockingRepository.collectUserBlockAsState(uid)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isBlocked) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = { navigationManager.popBackStack() },
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                                contentDescription = null
                            )
                        }
                    }
                )
            } else {
                ProfileDetailAppBar(
                    userInfo = userInfo,
                    scrollBehavior = scrollBehavior,
                    isBlocked = isBlocked,
                    onBack = navigationManager::popBackStack,
                    onPrivateFollow = { userId ->
                        viewModel.followUser(userId, Restrict.PRIVATE)
                    },
                    onBlockUser = { userId ->
                        viewModel.blockUser(userId)
                    }
                )
            }
        },
    ) {
        if (isBlocked) {
            BlockSurface(
                modifier = Modifier.fillMaxSize(),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(RString.user_blocked),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                button = {
                    Button(
                        onClick = {
                            viewModel.removeBlockUser(uid)
                        }
                    ) {
                        Text(
                            text = stringResource(RString.cancel_user_blocked)
                        )
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(horizontal = 15.dp)
            ) {
                item(key = KEY_USER_INFO) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = CenterVertically
                        ) {
                            Text(
                                text = userInfo.user.name,
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            if (userInfo.profile.isPremium) {
                                Image(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_profile_premium),
                                    modifier = Modifier
                                        .padding(start = 5.dp)
                                        .size(20.dp),
                                    contentDescription = null
                                )
                            }
                        }
                        Text(
                            text = buildString {
                                append(userInfo.profile.totalFollowUsers.toString())
                                append(" ")
                                append(stringResource(RString.followed))
                            },
                            modifier = Modifier.throttleClick {
                                navigationManager.navigateToFollowingScreen(userInfo.user.id)
                            },
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        )
                        //id点击可复制
                        Row(
                            horizontalArrangement = 5f.spaceBy,
                            verticalAlignment = CenterVertically,
                        ) {
                            Text(
                                text = "ID: ${userInfo.user.id}",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                                    .throttleClick(indication = ripple(radius = 15.dp)) {
                                        copyToClipboard(userInfo.user.id.toString())
                                    }
                                    .padding(5.dp)
                            )
                        }
                        // 个人简介
                        if (userInfo.user.comment.isNotEmpty()) {
                            Text(
                                text = userInfo.user.comment,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
                if (state.userIllusts.isNotEmpty()) {
                    item(key = KEY_USER_ILLUSTS) {
                        // 插画、漫画网格组件
                        IllustWidget(
                            title = stringResource(RString.illustration_works),
                            endText = stringResource(
                                RString.illustration_count,
                                userInfo.profile.totalIllusts
                            ),
                            navToPictureScreen = navigationManager::navigateToPictureScreen,
                            illusts = state.userIllusts,
                            modifier = Modifier.fillMaxWidth(),
                            onAllClick = {
                                navigationManager.navigateToUserIllustScreen(uid)
                            }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
                if (state.userBookmarksIllusts.isNotEmpty()) {
                    item(key = KEY_USER_BOOKMARKS_ILLUSTS) {
                        // 插画、漫画收藏网格组件
                        IllustWidget(
                            title = stringResource(RString.illust_and_manga_liked),
                            endText = stringResource(RString.view_all),
                            navToPictureScreen = navigationManager::navigateToPictureScreen,
                            illusts = state.userBookmarksIllusts,
                            modifier = Modifier.fillMaxWidth(),
                            onAllClick = {
                                navigationManager.navigateToCollectionScreen(uid)
                            }
                        )
                    }
                }
                item(key = KEY_USER_BOOKMARKS_NOVELS) {
                    if (state.userBookmarksNovels.isNotEmpty()) {
                        // 小说收藏网格组件
                        NovelBookmarkWidget(
                            novels = state.userBookmarksNovels,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                        )
                    }
                }
                item(key = KEY_SPACE) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailAppBar(
    userInfo: UserDetailResp,
    scrollBehavior: TopAppBarScrollBehavior,
    isBlocked: Boolean,
    onBack: () -> Unit,
    onPrivateFollow: (Long) -> Unit,
    onBlockUser: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarSize = 50.dp
    val expandedHeight = TopAppBarDefaults.MediumAppBarExpandedHeight + avatarSize
    val backgroundHeight = TopAppBarDefaults.MediumAppBarExpandedHeight +
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            avatarSize * scrollBehavior.state.collapsedFraction.pow(2) +
            with(LocalDensity.current) {
                scrollBehavior.state.heightOffset.toDp()
            }
    val backgroundUrl = userInfo.profile.backgroundImageURL.ifEmpty {
        userInfo.user.profileImageUrls.medium
    }
    var showMenu by remember { mutableStateOf(false) }

    if (backgroundUrl.isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(backgroundUrl)
                .allowRgb565(true)
                .build(),
            contentScale = ContentScale.FillWidth,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(backgroundHeight)
                .blur(10.dp)
                .drawWithCache {
                    val color = Color.Black.copy(alpha = 0.5f)
                    onDrawWithContent {
                        drawContent()
                        drawRect(color)
                    }
                }
        )
    }
    MediumTopAppBar(
        title = {
            Row(
                verticalAlignment = CenterVertically
            ) {
                UserAvatar(
                    url = userInfo.user.profileImageUrls.medium,
                    modifier = Modifier.size(avatarSize * (2 - scrollBehavior.state.collapsedFraction)),
                )
                if (scrollBehavior.state.collapsedFraction == 1f) {
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = userInfo.user.name,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        },
        modifier = modifier.statusBarsPadding(),
        navigationIcon = {
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.padding(vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = null
                )
            }
        },
        actions = {
            if (!isBlocked) {
                IconButton(
                    onClick = { showMenu = true },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = null)
                }
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!userInfo.user.isFollowing) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(RString.private_follow),
                            )
                        },
                        onClick = {
                            onPrivateFollow(userInfo.user.id)
                            showMenu = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(RString.block_user),
                        )
                    },
                    onClick = {
                        onBlockUser(userInfo.user.id)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(RString.report_user),
                        )
                    },
                    onClick = {
                        // todo report
                        showMenu = false
                    }
                )
            }
        },
        expandedHeight = expandedHeight,
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        scrollBehavior = scrollBehavior
    )
}

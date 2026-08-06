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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.mrl.pixiv.common.compose.ui.BlockSurface
import com.mrl.pixiv.common.compose.ui.image.UserAvatar
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.Type
import com.mrl.pixiv.common.data.user.UserDetailResp
import com.mrl.pixiv.common.kts.spaceBy
import com.mrl.pixiv.common.repository.BlockingRepositoryV2
import com.mrl.pixiv.common.repository.isSelf
import com.mrl.pixiv.common.repository.viewmodel.follow.isFollowing
import com.mrl.pixiv.common.router.NavigationManager
import com.mrl.pixiv.common.util.RDrawables
import com.mrl.pixiv.common.util.RStrings
import com.mrl.pixiv.common.util.allowRgb565
import com.mrl.pixiv.common.util.copyToClipboard
import com.mrl.pixiv.common.util.throttleClick
import com.mrl.pixiv.common.viewmodel.asState
import com.mrl.pixiv.profile.detail.components.IllustWidget
import com.mrl.pixiv.profile.detail.components.NovelBookmarkWidget
import com.mrl.pixiv.strings.block_user
import com.mrl.pixiv.strings.cancel_user_blocked
import com.mrl.pixiv.strings.followed
import com.mrl.pixiv.strings.ic_profile_premium
import com.mrl.pixiv.strings.illust_and_manga_liked
import com.mrl.pixiv.strings.illustration_count
import com.mrl.pixiv.strings.illustration_works
import com.mrl.pixiv.strings.manga
import com.mrl.pixiv.strings.private_follow
import com.mrl.pixiv.strings.profile_account
import com.mrl.pixiv.strings.profile_birthday
import com.mrl.pixiv.strings.profile_chair
import com.mrl.pixiv.strings.profile_desk
import com.mrl.pixiv.strings.profile_desktop
import com.mrl.pixiv.strings.profile_details
import com.mrl.pixiv.strings.profile_job
import com.mrl.pixiv.strings.profile_monitor
import com.mrl.pixiv.strings.profile_mouse
import com.mrl.pixiv.strings.profile_music
import com.mrl.pixiv.strings.profile_pawoo
import com.mrl.pixiv.strings.profile_pc
import com.mrl.pixiv.strings.profile_printer
import com.mrl.pixiv.strings.profile_region
import com.mrl.pixiv.strings.profile_scanner
import com.mrl.pixiv.strings.profile_tablet
import com.mrl.pixiv.strings.profile_tool
import com.mrl.pixiv.strings.profile_twitter
import com.mrl.pixiv.strings.profile_twitter_url
import com.mrl.pixiv.strings.profile_webpage
import com.mrl.pixiv.strings.profile_workspace
import com.mrl.pixiv.strings.profile_workspace_comment
import com.mrl.pixiv.strings.report_user
import com.mrl.pixiv.strings.user_blocked
import com.mrl.pixiv.strings.view_all
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.pow

private const val KEY_USER_INFO = "user_info"
private const val KEY_USER_DETAILS = "user_details"
private const val KEY_USER_ILLUSTS = "user_illusts"
private const val KEY_USER_MANGAS = "user_mangas"
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
    val isBlocked = BlockingRepositoryV2.collectUserBlockAsState(uid)

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
                        text = stringResource(RStrings.user_blocked),
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
                            text = stringResource(RStrings.cancel_user_blocked)
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
                            SelectionContainer {
                                Text(
                                    text = userInfo.user.name,
                                    style = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                            if (userInfo.profile.isPremium) {
                                Image(
                                    imageVector = vectorResource(RDrawables.ic_profile_premium),
                                    modifier = Modifier
                                        .padding(start = 5.dp)
                                        .size(20.dp),
                                    contentDescription = null
                                )
                            }
                        }
                        SelectionContainer {
                            Text(
                                text = buildString {
                                    append(userInfo.profile.totalFollowUsers.toString())
                                    append(" ")
                                    append(stringResource(RStrings.followed))
                                },
                                modifier = Modifier.throttleClick {
                                    navigationManager.navigateToFollowingScreen(userInfo.user.id)
                                },
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            )
                        }
                        //id点击可复制
                        Row(
                            horizontalArrangement = 5f.spaceBy,
                            verticalAlignment = CenterVertically,
                        ) {
                            SelectionContainer {
                                Text(
                                    text = "ID: ${userInfo.user.id}",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
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
                            val comment = remember(userInfo.user.comment) {
                                htmlToAnnotatedString(
                                    html = userInfo.user.comment,
                                    compactMode = true,
                                )
                            }
                            SelectionContainer {
                                Text(
                                    text = comment,
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                        }
                    }
                }
                item(key = KEY_USER_DETAILS) {
                    ProfileDetails(userInfo = userInfo)
                }
                if (state.userIllusts.isNotEmpty()) {
                    item(key = KEY_USER_ILLUSTS) {
                        // 插画、漫画网格组件
                        IllustWidget(
                            title = stringResource(RStrings.illustration_works),
                            endText = stringResource(
                                RStrings.illustration_count,
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
                if (state.userMangas.isNotEmpty()) {
                    item(key = KEY_USER_MANGAS) {
                        IllustWidget(
                            title = stringResource(RStrings.manga),
                            endText = stringResource(RStrings.view_all),
                            navToPictureScreen = navigationManager::navigateToPictureScreen,
                            illusts = state.userMangas,
                            modifier = Modifier.fillMaxWidth(),
                            onAllClick = {
                                navigationManager.navigateToUserIllustScreen(uid, Type.Manga)
                            }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
                if (state.userBookmarksIllusts.isNotEmpty()) {
                    item(key = KEY_USER_BOOKMARKS_ILLUSTS) {
                        // 插画、漫画收藏网格组件
                        IllustWidget(
                            title = stringResource(RStrings.illust_and_manga_liked),
                            endText = stringResource(RStrings.view_all),
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
                            onAllClick = {
                                navigationManager.navigateToCollectionScreen(uid, true)
                            },
                            onSeriesClick = navigationManager::navigateToNovelSeriesScreen,
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
private fun ProfileDetails(
    userInfo: UserDetailResp,
    modifier: Modifier = Modifier,
) {
    val profile = userInfo.profile
    val hasProfileDetails = userInfo.user.account.isNotEmpty() ||
            profile.webpage.isNotEmpty() ||
            profile.birth.isNotEmpty() ||
            profile.region.isNotEmpty() ||
            profile.job.isNotEmpty() ||
            profile.twitterAccount.isNotEmpty() ||
            profile.twitterURL.isNotEmpty() ||
            profile.pawooURL.isNotEmpty()
    val workspace = userInfo.workspace?.takeIf {
        it.workspaceImageURL.isNotEmpty() ||
                it.pc.isNotEmpty() ||
                it.monitor.isNotEmpty() ||
                it.tool.isNotEmpty() ||
                it.scanner.isNotEmpty() ||
                it.tablet.isNotEmpty() ||
                it.mouse.isNotEmpty() ||
                it.printer.isNotEmpty() ||
                it.desktop.isNotEmpty() ||
                it.music.isNotEmpty() ||
                it.desk.isNotEmpty() ||
                it.chair.isNotEmpty() ||
                it.comment.isNotEmpty()
    }
    val hasWorkspaceDetails = workspace != null

    if (!hasProfileDetails && !hasWorkspaceDetails) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasProfileDetails) {
            SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(RStrings.profile_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (userInfo.user.account.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_account),
                            value = userInfo.user.account,
                        )
                    }
                    if (profile.webpage.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_webpage),
                            value = profile.webpage,
                        )
                    }
                    if (profile.birth.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_birthday),
                            value = profile.birth,
                        )
                    }
                    if (profile.region.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_region),
                            value = profile.region,
                        )
                    }
                    if (profile.job.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_job),
                            value = profile.job,
                        )
                    }
                    if (profile.twitterAccount.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_twitter),
                            value = profile.twitterAccount,
                        )
                    }
                    if (profile.twitterURL.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_twitter_url),
                            value = profile.twitterURL,
                        )
                    }
                    if (profile.pawooURL.isNotEmpty()) {
                        ProfileDetailRow(
                            label = stringResource(RStrings.profile_pawoo),
                            value = profile.pawooURL,
                        )
                    }
                }
            }
        }

        if (workspace != null) {
            if (hasProfileDetails) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            Text(
                text = stringResource(RStrings.profile_workspace),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            if (workspace.workspaceImageURL.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(workspace.workspaceImageURL)
                        .allowRgb565(true)
                        .build(),
                    contentDescription = stringResource(RStrings.profile_workspace),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                )
            }
            SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_pc), workspace.pc)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_monitor), workspace.monitor)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_tool), workspace.tool)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_scanner), workspace.scanner)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_tablet), workspace.tablet)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_mouse), workspace.mouse)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_printer), workspace.printer)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_desktop), workspace.desktop)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_music), workspace.music)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_desk), workspace.desk)
                    ProfileDetailRowIfNotEmpty(stringResource(RStrings.profile_chair), workspace.chair)
                    ProfileDetailRowIfNotEmpty(
                        stringResource(RStrings.profile_workspace_comment),
                        workspace.comment,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRowIfNotEmpty(label: String, value: String) {
    if (value.isNotEmpty()) {
        ProfileDetailRow(label = label, value = value)
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
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
    var showMenu by rememberSaveable { mutableStateOf(false) }

    if (backgroundUrl.isNotEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
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
                val isSelf = userInfo.user.isSelf
                if (!userInfo.user.isFollowing && !isSelf) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(RStrings.private_follow),
                            )
                        },
                        onClick = {
                            onPrivateFollow(userInfo.user.id)
                            showMenu = false
                        }
                    )
                }
                if (!isSelf) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(RStrings.block_user),
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
                                text = stringResource(RStrings.report_user),
                            )
                        },
                        onClick = {
                            // todo report
                            showMenu = false
                        }
                    )
                }
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

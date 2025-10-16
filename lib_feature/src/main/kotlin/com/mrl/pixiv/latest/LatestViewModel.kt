package com.mrl.pixiv.latest

import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.ViewModel
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LatestViewModel: ViewModel() {
    val pagerState = PagerState { LatestPage.entries.size }
}
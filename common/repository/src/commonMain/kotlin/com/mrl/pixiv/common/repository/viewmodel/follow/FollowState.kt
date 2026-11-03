package com.mrl.pixiv.common.repository.viewmodel.follow

import androidx.compose.runtime.mutableStateMapOf
import com.mrl.pixiv.common.coroutine.launchProcess
import com.mrl.pixiv.common.data.Restrict
import com.mrl.pixiv.common.data.User
import com.mrl.pixiv.common.repository.PixivRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

val User.isFollowing: Boolean
    get() = FollowState.state[id] ?: isFollowed

object FollowState {
    private val requests = FollowRequestState { request ->
        launchProcess(Dispatchers.Main.immediate, start = CoroutineStart.LAZY, errorToast = true) {
            request()
        }
    }

    internal val state get() = requests.state

    fun isUpdating(userId: Long): Boolean = requests.isUpdating(userId)

    fun followUser(userId: Long, restrict: Restrict = Restrict.PUBLIC): Job =
        requests.update(userId, isFollowing = true) {
            withContext(Dispatchers.IO) {
                PixivRepository.followUser(userId, restrict)
            }
        }

    fun unFollowUser(userId: Long): Job =
        requests.update(userId, isFollowing = false) {
            withContext(Dispatchers.IO) {
                PixivRepository.unFollowUser(userId)
            }
        }
}

// UI entry points and request completions run on Main; only the API call switches to IO.
internal class FollowRequestState(
    private val launchLazyRequest: (suspend () -> Unit) -> Job,
) {
    val state = mutableStateMapOf<Long, Boolean>()
    private val jobs = mutableStateMapOf<Long, Job>()

    fun isUpdating(userId: Long): Boolean = jobs.containsKey(userId)

    fun update(userId: Long, isFollowing: Boolean, request: suspend () -> Unit): Job {
        jobs[userId]?.let { return it }
        val job = launchLazyRequest {
            request()
            state[userId] = isFollowing
        }
        jobs[userId] = job
        job.invokeOnCompletion {
            if (jobs[userId] === job) {
                jobs.remove(userId)
            }
        }
        job.start()
        return job
    }
}

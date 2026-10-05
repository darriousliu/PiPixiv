package com.mrl.pixiv.novel

import com.mrl.pixiv.common.data.novel.NovelPollChoice
import com.mrl.pixiv.common.data.novel.NovelPollData
import com.mrl.pixiv.common.data.novel.PublicNovelPoll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelPollStateTest {
    private val poll = PublicNovelPoll("Question", 3, listOf(NovelPollChoice(7, "Choice", 3)))

    @Test
    fun initialSelectedAnswerIsRestoredAndPreventsAnotherVote() {
        val state = NovelPollState(loading = true).loaded(poll.copy(selectedValue = 7))
        assertEquals(7, state.choiceId)
        assertTrue(state.submitted)
        assertFalse(state.loading)
        assertEquals(state, state.select(7))
        val unanswered = state.loaded(poll.copy(selectedValue = null))
        assertNull(unanswered.choiceId)
        assertFalse(unanswered.submitted)
        assertFalse(state.loaded(poll.copy(selectedValue = 99)).submitted)
    }

    @Test
    fun onlyServerSuppliedChoicesCanBeSelected() {
        val state = NovelPollState(loaded = true, poll = poll)
        assertEquals(state, state.select(99))
        assertEquals(7, state.select(7).choiceId)
    }

    @Test
    fun submittingAndSuccessfulVotesCannotBeChangedOrSubmittedTwice() {
        val submitting = NovelPollState(poll = poll, choiceId = 7, submitting = true)
        assertEquals(submitting, submitting.select(99))
        val answered = submitting.answered(NovelPollData("Question", 4, listOf(NovelPollChoice(7, "Choice", 4)), 7))
        assertTrue(answered.submitted)
        assertEquals(4, answered.poll!!.total)
        assertEquals(7, answered.poll!!.selectedValue)
        assertEquals(answered, answered.select(7))
    }
}

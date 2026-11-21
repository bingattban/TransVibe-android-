package com.transvibe.app

import com.transvibe.app.presentation.screens.previewMediaUriForJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPreviewMediaRoutingTest {
    @Test fun selectedJobLoadsItsPersistedVideo() {
        assertEquals("content://media/old-video", previewMediaUriForJob("job-old", "job-old", "content://media/old-video"))
    }

    @Test fun previewMustNotShowRecentlyInspectedButUnrelatedVideo() {
        assertNull(previewMediaUriForJob("job-old", "job-recent", "content://media/recent-video"))
    }

    @Test fun previewWaitsForAsynchronousJobLoad() {
        assertNull(previewMediaUriForJob("job-old", null, null))
        assertEquals("content://media/old-video", previewMediaUriForJob("job-old", "job-old", "content://media/old-video"))
    }

    @Test fun blankOrUnknownUriDoesNotPreparePlayer() {
        assertNull(previewMediaUriForJob("job-old", "job-old", ""))
        assertNull(previewMediaUriForJob("job-old", "job-old", " ".trim()))
        assertNull(previewMediaUriForJob("", "", "content://media/invalid"))
    }
}

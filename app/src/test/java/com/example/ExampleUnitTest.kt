package com.example

import com.example.core.runtime.MediaPipeEmbeddingService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun cosineSimilarityOfIdenticalVectorsIsOne() {
        val svc = MediaPipeEmbeddingService(RuntimeEnvironment.getApplication())
        val v = floatArrayOf(1f, 0f, 0f)
        assertEquals(1f, svc.computeCosineSimilarity(v, v), 1e-5f)
    }
}

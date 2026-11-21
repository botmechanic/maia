package com.example.core.database

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VectorCodecTest {

    @Test
    fun encodeAndDecodeRoundTripPreservesExactFloats() {
        val original = FloatArray(512) { i -> (i - 256) / 512.0f }
        val bytes = VectorCodec.encode(original)
        assertEquals(512 * 4, bytes.size)

        val decoded = VectorCodec.decode(bytes)
        assertArrayEquals(original, decoded, 0.0f)
    }

    @Test
    fun decodeWithFallbackPrefersBlobOverCsv() {
        val expected = floatArrayOf(0.25f, -0.5f, 0.75f)
        val blob = VectorCodec.encode(expected)
        val decoded = VectorCodec.decodeWithFallback(blob, "9.9,9.9,9.9")
        assertArrayEquals(expected, decoded, 0.0f)
    }

    @Test
    fun decodeWithFallbackReadsLegacyCsvWhenBlobIsNull() {
        val decoded = VectorCodec.decodeWithFallback(null, "0.1, -0.2, 0.3")
        assertArrayEquals(floatArrayOf(0.1f, -0.2f, 0.3f), decoded, 1e-6f)
    }

    @Test
    fun decodeHandlesEmptyOrCorruptedByteArraysSafely() {
        assertEquals(0, VectorCodec.decode(ByteArray(0)).size)
        assertEquals(0, VectorCodec.decode(ByteArray(3)).size)
        assertEquals(0, VectorCodec.decodeWithFallback(null, "").size)
    }
}


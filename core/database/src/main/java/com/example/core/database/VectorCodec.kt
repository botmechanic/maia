package com.example.core.database

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Compact little-endian IEEE-754 binary codec for embedding vectors.
 * Stores 512-dim float vectors in 2,048 bytes (vs ~5,500 bytes as ASCII CSV)
 * and eliminates String.split / toFloatOrNull GC overhead during semantic search.
 */
object VectorCodec {

    fun encode(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
        for (value in vector) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }

    fun decode(bytes: ByteArray): FloatArray {
        if (bytes.isEmpty() || bytes.size % Float.SIZE_BYTES != 0) return FloatArray(0)
        val count = bytes.size / Float.SIZE_BYTES
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(count) { buffer.getFloat() }
    }

    /**
     * Decodes binary [blob] when present, transparently falling back to legacy v5
     * comma-separated [csv] rows so existing encrypted vaults migrate seamlessly.
     */
    fun decodeWithFallback(blob: ByteArray?, csv: String): FloatArray {
        if (blob != null && blob.isNotEmpty()) {
            return decode(blob)
        }
        if (csv.isBlank()) return FloatArray(0)
        return csv.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
    }
}


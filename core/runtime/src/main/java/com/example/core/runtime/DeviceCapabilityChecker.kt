package com.example.core.runtime

import android.app.ActivityManager
import android.content.Context
import java.util.Locale

data class DeviceMemoryReport(
    val totalRamBytes: Long,
    val totalRamGb: Double,
    val isLowMemoryDevice: Boolean,
    val recommendedModel: String,
    val compatibilityNotice: String?
)

/**
 * Inspects device RAM so 4 GB budget devices receive a calm, honest compatibility notice
 * before attempting to load a 555 MB+ quantized on-device LLM, while flagship 12 GB+
 * devices can comfortably run larger 4B tiers.
 */
object DeviceCapabilityChecker {
    private const val GB_BYTES = 1024.0 * 1024.0 * 1024.0
    private const val MIN_RECOMMENDED_RAM_GB = 5.5
    private const val FLAGSHIP_4B_RAM_GB = 10.5

    fun evaluate(totalRamBytes: Long): DeviceMemoryReport {
        val totalGb = totalRamBytes / GB_BYTES
        val isLow = totalGb < MIN_RECOMMENDED_RAM_GB
        val recommended = if (totalGb >= FLAGSHIP_4B_RAM_GB) "Gemma 4B" else "Gemma 1B"
        val notice = if (isLow) {
            String.format(
                Locale.US,
                "This phone has %.1f GB RAM. On-device AI works best with 6 GB+ RAM and may run slowly here.",
                totalGb
            )
        } else {
            null
        }
        return DeviceMemoryReport(
            totalRamBytes = totalRamBytes,
            totalRamGb = totalGb,
            isLowMemoryDevice = isLow,
            recommendedModel = recommended,
            compatibilityNotice = notice
        )
    }

    fun inspect(context: Context): DeviceMemoryReport {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val total = if (memInfo.totalMem > 0L) memInfo.totalMem else (8L * 1024L * 1024L * 1024L)
        return evaluate(total)
    }
}


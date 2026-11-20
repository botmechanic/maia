package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Permission audit: every permission in any merged manifest (debug and release) must be on the
 * core allowlist (WorkManager) or declared by a registered MaiaModule.
 * v1 modules declare none, so only the allowlist is expected. INTERNET and ACCESS_NETWORK_STATE must stay absent.
 */
class PermissionAuditTest {

    private val coreAllowlist = setOf(
        "android.permission.WAKE_LOCK",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.USE_BIOMETRIC",
        "android.permission.USE_FINGERPRINT",
        "app.heymaia.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    )

    @Test
    fun mergedManifestPermissionsAreAllowlistedAndInternetAbsent() {
        val manifests = findMergedManifests()
        if (manifests.isEmpty()) {
            fail("No merged AndroidManifest.xml found! Ensure :app:processDebugMainManifest or :app:processReleaseMainManifest has executed before this test.")
        }

        for (manifest in manifests) {
            val text = manifest.readText()
            val permissions = Regex("""android:name="([^"]+)"""")
                .findAll(text)
                .map { it.groupValues[1] }
                .filter { it.startsWith("android.permission.") || it.endsWith("_PERMISSION") }
                .toSet()

            assertFalse(
                "INTERNET must remain stripped in ${manifest.path}",
                permissions.contains("android.permission.INTERNET")
            )
            assertFalse(
                "ACCESS_NETWORK_STATE must remain stripped in ${manifest.path}",
                permissions.contains("android.permission.ACCESS_NETWORK_STATE")
            )
            assertFalse(
                "READ_GSERVICES must remain stripped in ${manifest.path}",
                permissions.contains("com.google.android.providers.gsf.permission.READ_GSERVICES")
            )

            // Module-declared permissions (v1: empty). Future modules add here via registry.
            val moduleDeclared = emptySet<String>()
            val allowed = coreAllowlist + moduleDeclared

            permissions.forEach { perm ->
                assertTrue(
                    "Unexpected permission in merged manifest (${manifest.path}): $perm",
                    perm in allowed || perm.endsWith("DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
                )
            }
        }
    }

    private fun findMergedManifests(): List<File> {
        val candidates = listOf(
            File("build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml"),
            File("app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml"),
            File("build/intermediates/merged_manifests/debug/AndroidManifest.xml"),
            File("app/build/intermediates/merged_manifests/debug/AndroidManifest.xml"),
            File("build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml"),
            File("app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml"),
            File("build/intermediates/merged_manifests/release/AndroidManifest.xml"),
            File("app/build/intermediates/merged_manifests/release/AndroidManifest.xml")
        ).filter { it.exists() }

        if (candidates.isNotEmpty()) return candidates

        return File(".").walkTopDown()
            .maxDepth(12)
            .filter { it.name == "AndroidManifest.xml" && it.path.contains("merged_manifest") }
            .toList()
    }
}

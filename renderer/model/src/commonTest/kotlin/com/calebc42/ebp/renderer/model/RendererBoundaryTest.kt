// SPDX-License-Identifier: GPL-3.0-or-later
package com.calebc42.ebp.renderer.model

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level dependency guards for the renderer-neutral layers. */
class RendererBoundaryTest {
    @Test
    fun protocolModelAndCoreComposeDoNotImportMaterialOrStyles() {
        val root = findRendererRoot()
        val roots = listOf(
            root.resolve("../ebp-kmp/ebp-kmp/src"),
            root.resolve("../ebp-kmp/wire/src"),
        ) + listOf("renderer/model", "renderer/compose", "renderer/compose-testing")
            .flatMap { mainSourceSets(root.resolve(it)) }
        val forbidden = listOf(
            "androidx.compose.material",
            "androidx.compose.foundation.style",
        )
        val offenders = offendingLines(roots, root, forbidden)
        assertTrue("neutral renderer boundary violations:\n${offenders.joinToString("\n")}",
            offenders.isEmpty())
    }

    @Test
    fun commonRendererSourcesStayPlatformNeutral() {
        // commonMain compiles for Android and for the desktop JVM (jetpacs-poc
        // docs/PLAN-desktop-companion.md). A platform import there is a leak
        // that only the second host would notice.
        val root = findRendererRoot()
        // A module that has not moved to commonMain yet is not scanned; at
        // least one must have, or the guard would check nothing.
        val roots = listOf("renderer/model", "renderer/compose")
            .map { root.resolve("$it/src/commonMain") }
            .filter(File::isDirectory)
        require(roots.isNotEmpty()) { "no commonMain source set under $root/renderer" }
        val forbidden = listOf(
            "import android.",
            "import org.jetbrains.skia",
            "import java.awt",
            "import javax.swing",
        )
        val offenders = offendingLines(roots, root, forbidden)
        assertTrue("platform imports in commonMain:\n${offenders.joinToString("\n")}",
            offenders.isEmpty())
    }

    /** Every production source set of MODULE: `src/main` or any source set whose name ends in `Main`. */
    private fun mainSourceSets(module: File): List<File> {
        val sets = module.resolve("src").listFiles().orEmpty()
            .filter { it.isDirectory && (it.name == "main" || it.name.endsWith("Main")) }
        require(sets.isNotEmpty()) { "no production source set under $module/src" }
        return sets
    }

    private fun offendingLines(
        roots: List<File>,
        relativeTo: File,
        forbidden: List<String>,
    ): List<String> = roots.flatMap { directory ->
        // A root that scans nothing would let the guard pass by accident.
        require(directory.isDirectory) { "missing source root $directory" }
        val files = directory.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "kts") }
            .toList()
        require(files.isNotEmpty()) { "no Kotlin sources under $directory" }
        files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                line.takeIf { text -> forbidden.any(text::contains) }
                    ?.let { "${file.relativeTo(relativeTo)}:${index + 1}: $it" }
            }
        }
    }

    private fun findRendererRoot(): File {
        var directory: File? = File(
            requireNotNull(System.getProperty("user.dir")),
        ).absoluteFile
        while (directory != null) {
            if (File(directory, "settings.gradle.kts").isFile &&
                File(directory, "renderer").isDirectory) {
                return directory
            }
            directory = directory.parentFile
        }
        error("renderer root not found")
    }
}

package com.saveourtool.save.plugins.fixandwarn

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.createFile
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.utils.isCurrentOsWindows
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPluginConfig

import okio.FileSystem

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixAndWarnPluginTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "${FixAndWarnPluginTest::class.simpleName!!}-${Random.nextInt()}")
    private val defaultExtraConfigPattern = Regex("(.+):(\\d+):(\\d+): (.+)")

    @BeforeTest
    fun setUp() {
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
        fs.createDirectory(tmpDir)
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `base test`() {
        val config = fs.createFile(tmpDir / "save.toml")

        val textBeforeFixAndWarn =
                """
                    package com.saveourtool.save.example
                    
                    class example {
                        int foo = 42;
                    }
                """.trimIndent()

        val testFile = fs.createFile(tmpDir / "Test1Test.java")
        fs.write(testFile) {
            write(textBeforeFixAndWarn.encodeToByteArray())
        }

        val textAfterFixAndWarn =
                """
                    package com.saveourtool.save.example
                    
                    // ;warn:4:6: Some Warning
                    class Example {
                        int foo = 42;
                    }
                """.trimIndent()

        val expectedFile = fs.createFile(tmpDir / "Test1Expected.java")
        fs.write(expectedFile) {
            write(textAfterFixAndWarn.encodeToByteArray())
        }

        val diskWithTmpDir = if (isCurrentOsWindows()) "${tmpDir.toString().substringBefore("\\").lowercase()} && " else ""
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
        val fixExecutionCmd = "${diskWithTmpDir}cd $tmpDir && $catCmd $expectedFile >"
        val warnExecutionCmd = "echo Test1Expected.java:4:6: Some Warning && set stub="

        val fixAndWarnPlugin = FixAndWarnPlugin(
            TestConfig(
                config,
                null,
                mutableListOf(
                    FixAndWarnPluginConfig(
                        FixPluginConfig(fixExecutionCmd, batchSize = 1),
                        WarnPluginConfig(warnExecutionCmd,
                            Regex("(.+):(\\d+):(\\d+): (.+)"),
                            true, true, 1, ", ", 1, 2, 3, 1, 1, 1, 2, 3, 4
                        )
                    ),
                    GeneralConfig("", listOf(""), "", "", expectedWarningsPattern = Regex("// ;warn:(\\d+):(\\d+): (.*)"), runConfigPattern = defaultExtraConfigPattern)
                ),
                fs,
            ),
            testFiles = emptyList(),
            fs,
            useInternalRedirections = false
        )
        val results = fixAndWarnPlugin.execute().toList()

        println("Results $results")
        assertEquals(1, results.count(), "Size of results should equal number of pairs")
        // FixMe: Check FixPlugin results (assert that temporary file with formatted code has been created)
        // Check WarnPlugin results
        assertTrue(results.single().status is Pass)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}

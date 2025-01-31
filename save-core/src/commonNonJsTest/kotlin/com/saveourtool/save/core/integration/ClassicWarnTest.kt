package com.saveourtool.save.core.integration

import com.saveourtool.save.core.test.utils.runTestsWithDiktat

import kotlin.test.Ignore
import kotlin.test.Test

/**
 * testing that save can successfully work with:
 * 1) separate files
 * 2) save.toml config
 * 3) test directory
 * 4) root path directory
 */
class ClassicWarnTest {
    @Test
    @Ignore  // fixme: change directory in fix-plugin too?
    fun `execute warn plugin with default testFiles`() {
        runTestsWithDiktat(
            null, 9
        )
    }

    @Test
    fun `execute warn plugin on separate files`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/EnumValueSnakeCaseTest.kt",
                "warn/chapter1/TestMatchOnlyByLineColumn/EnumValueSnakeCaseTest.kt",
                "warn/chapter1/GenericFunctionTest.kt"
            ), 3
        )
    }

    @Test
    @Ignore  // https://github.com/saveourtool/save/issues/333
    fun `execute warn plugin with timeout`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter2/EnumValueSnakeCaseTest.kt",
                "warn/chapter2/GenericFunctionTest.kt"
            ), 2
        )
    }

    @Test
    fun `executing warn plugin on directory`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1"
            ), 8
        )
    }

    @Test
    fun `executing warn plugin on directory when files are set with regex`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/TestsWithRegex"
            ), 1
        )
    }

    @Test
    fun `lines that match ignoreLines should be ignored`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/IgnoreLinesTest"
            ), 1
        )
    }

    @Test
    fun `test output file set`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/TestResultsFileTest"
            ), 1
        )
    }

    @Test
    @Ignore
    // FixMe: this test should be investigated, as resource discovery looks to be buggy
    // org.opentest4j.AssertionFailedError: expected: <3> but was: <8>
    fun `executing warn plugin on parental directory`() {
        runTestsWithDiktat(
            listOf(
                "warn"
            ), 3
        )
    }

    @Test
    @Ignore  // https://github.com/saveourtool/save/issues/333
    fun `executing warn plugin on save-toml file in directory`() {
        runTestsWithDiktat(
            listOf(
                "warn/save.toml"
            ), 10
        )
    }

    @Test
    fun `executing warn plugin on parental save-toml file`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/save.toml"
            ), 8
        )
    }

    @Test
    fun `executing warn plugin so that no resources will be found`() {
        runTestsWithDiktat(
            listOf(
                "warn/chapter1/TestsWithRegex/NoMatchingTests/",
            ), 0
        )
    }

    @Test
    @Ignore
    fun `execute warn plugin with included and excluded suites`() {
        runTestsWithDiktat(
            emptyList(), 1
        ) {
            it.copy(
                includeSuites = "Autofix and Warn",
                excludeSuites = "Directory: Chapter1"
            )
        }
    }

    @Test
    fun `tests should have a relative path`() {
        runTestsWithDiktat(
            listOf(
                "EnumValueSnakeCaseTest.kt",
                "GenericFunctionTest.kt"
            ), 0
        )
    }

    @Test
    fun `execute warn-plugin with expected warnings from SARIF`() {
        runTestsWithDiktat(
            listOf("sarif-expected"),
            1
        )
    }

    @Test
    fun `execute warn-plugin with actual warnings from SARIF`() {
        runTestsWithDiktat(
            listOf("sarif-actual"),
            1
        )
    }
}

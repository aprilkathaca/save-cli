package com.saveourtool.save.plugins.fixandwarn

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestResult
import com.saveourtool.save.plugin.warn.WarnPlugin
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPlugin
import com.saveourtool.save.plugins.fix.FixPluginConfig

import okio.FileSystem
import okio.Path

private typealias WarningsList = MutableList<Pair<Int, String>>

/**
 * A plugin that runs an executable on a file, and combines two actions: fix and warn
 * Plugin fixes test file, warns if something couldn't be auto-corrected after fix
 * and compares output with expected output during one execution.
 */
class FixAndWarnPlugin(
    testConfig: TestConfig,
    testFiles: List<String>,
    fileSystem: FileSystem,
    useInternalRedirections: Boolean = true,
    redirectTo: Path? = null,
) : Plugin(
    testConfig,
    testFiles,
    fileSystem,
    useInternalRedirections,
    redirectTo,
) {
    private val fixPluginConfig: FixPluginConfig =
            testConfig.pluginConfigs
                .filterIsInstance<FixAndWarnPluginConfig>()
                .single()
                .fix
    private val warnPluginConfig: WarnPluginConfig =
            testConfig.pluginConfigs
                .filterIsInstance<FixAndWarnPluginConfig>()
                .single()
                .warn
    private val generalConfig: GeneralConfig =
            testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()

    @Suppress("MISSING_KDOC_CLASS_ELEMENTS")
    internal lateinit var fixPlugin: FixPlugin
    private lateinit var warnPlugin: WarnPlugin

    private fun initOrUpdateConfigs() {
        fixPlugin = FixPlugin(createTestConfigForPlugins(fixPluginConfig), testFiles, fs)
        warnPlugin = WarnPlugin(createTestConfigForPlugins(warnPluginConfig), testFiles, fs)
    }

    /**
     * Create TestConfig same as current, but with corresponding plugin configs list for nested "fix" and "warn" sections
     *
     * @param pluginConfig [fix] or [warn] config of nested section
     * @return TestConfig for corresponding section
     */
    private fun createTestConfigForPlugins(pluginConfig: PluginConfig) = TestConfig(
        testConfig.location,
        testConfig.parentConfig,
        mutableListOf(
            generalConfig,
            pluginConfig
        ),
        fs,
    )

    override fun handleFiles(files: Sequence<TestFiles>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()
        // Need to update private fields after validation
        initOrUpdateConfigs()
        val expectedFiles = files.map { it as FixPlugin.FixTestFiles }.map { it.expected }

        // Remove (in place) warnings from test files before fix plugin execution
        // fixme: should be performed on copies of files
        val filesAndTheirWarningsMap = removeWarningsFromExpectedFiles(expectedFiles)

        val fixTestResults = fixPlugin.handleFiles(files).toList()

        val (fixTestResultsPassed, fixTestResultsFailed) = fixTestResults.partition { it.status is Pass }

        val expectedFilesWithPass = expectedFiles.filter { expectedFile ->
            fixTestResultsPassed.map { (it.resources as FixPlugin.FixTestFiles).expected }.contains(expectedFile)
        }

        // Fill back original data with warnings
        filesAndTheirWarningsMap.forEach { (filePath, warningsList) ->
            val fileData = fs.readLines(filePath) as MutableList
            // Append warnings into appropriate place
            warningsList.forEach { (line, warningMsg) ->
                fileData.add(line, warningMsg)
            }
            fs.write(filePath) {
                fileData.forEach {
                    write((it + "\n").encodeToByteArray())
                }
            }
        }

        // TODO: If we receive just one command for execution, and want to avoid extra executions
        // TODO: then warn plugin should look at the fix plugin output for actual warnings, and not execute command one more time.
        // TODO: However it's required changes in warn plugin logic (it's should be able to compare expected and actual warnings from different places),
        // TODO: this probably could be obtained after https://github.com/saveourtool/save/issues/164,
        val warnTestResults = warnPlugin.handleFiles(expectedFilesWithPass.map { Test(it) })

        val fixAndWarnTestResults = fixTestResultsFailed.asSequence() + warnTestResults.map { testResult ->
            files.map { it as FixPlugin.FixTestFiles }
                // find these results among all files and replace the paths to the *Expected with *Test
                .filter { fixTestFiles -> fixTestFiles.expected == testResult.resources.test }
                .map { testFile ->
                    TestResult(
                        testFile,
                        testResult.status,
                        testResult.debugInfo
                    )
                }.single()
        }

        return fixAndWarnTestResults
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<TestFiles> {
        initOrUpdateConfigs()
        // Test files for fix and warn plugin should be the same, so this will be enough
        return fixPlugin.rawDiscoverTestFiles(resourceDirectories)
    }

    /**
     * Remove warnings from the given files, which satisfy pattern from <warn> plugin and save data about warnings, which were deleted
     *
     * @files files to be modified
     *
     * @return map of files and theirs list of warnings
     */
    private fun removeWarningsFromExpectedFiles(files: Sequence<Path>): MutableMap<Path, WarningsList> {
        val filesAndTheirWarningsMap: MutableMap<Path, WarningsList> = mutableMapOf()
        files.forEach { file ->
            val fileData = fs.readLines(file)
            filesAndTheirWarningsMap[file] = mutableListOf()
            val fileDataWithoutWarnings = fileData.filterIndexed { index, line ->
                val isLineWithWarning = (generalConfig.expectedWarningsPattern!!.find(line)?.groups != null)
                if (isLineWithWarning) {
                    filesAndTheirWarningsMap[file]!!.add(index to line)
                }
                !isLineWithWarning
            }
            writeDataWithoutWarnings(file, filesAndTheirWarningsMap, fileDataWithoutWarnings)
        }
        return filesAndTheirWarningsMap
    }

    private fun writeDataWithoutWarnings(
        file: Path,
        filesAndTheirWarningsMap: MutableMap<Path, WarningsList>,
        fileDataWithoutWarnings: List<String>
    ) {
        if (filesAndTheirWarningsMap[file]!!.isEmpty()) {
            filesAndTheirWarningsMap.remove(file)
        } else {
            fs.write(file) {
                fileDataWithoutWarnings.forEach {
                    write((it + "\n").encodeToByteArray())
                }
            }
        }
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixAndWarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }
}

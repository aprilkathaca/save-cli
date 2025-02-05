/**
 * This file contain utils for toml files processing
 */

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.config.TestConfigSections
import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.plugin.PluginException
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPluginConfig
import com.saveourtool.save.plugins.fixandwarn.FixAndWarnPluginConfig

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.TomlDecodingException
import com.akuleshov7.ktoml.file.TomlFileReader
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.TomlTable
import okio.FileSystem
import okio.Path

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

private fun Path.testConfigFactory(table: TomlTable) =
        when (table.fullTableName.uppercase().replace("\"", "")) {
            TestConfigSections.FIX.name -> this.createPluginConfig<FixPluginConfig>(
                table.fullTableName
            )
            TestConfigSections.`FIX AND WARN`.name -> this.createPluginConfig<FixAndWarnPluginConfig>(
                table.fullTableName
            )
            TestConfigSections.WARN.name -> this.createPluginConfig<WarnPluginConfig>(
                table.fullTableName
            )
            TestConfigSections.GENERAL.name -> this.createPluginConfig<GeneralConfig>(
                table.fullTableName
            )
            else -> throw PluginException(
                "Received unknown plugin section name in the input: [${table.fullTableName}]." +
                        " Please check your <$this> config"
            )
        }

/**
 * Create the plugin config according section name
 *
 * @param pluginSectionName name of plugin section from toml file
 */
@OptIn(ExperimentalSerializationApi::class)
private inline fun <reified T : PluginConfig> Path.createPluginConfig(
    pluginSectionName: String
) = try {
    TomlFileReader.partiallyDecodeFromFile<T>(serializer(), this.toString(), pluginSectionName)
        .apply {
            configLocation = this@createPluginConfig
        }
} catch (e: TomlDecodingException) {
    logError(
        "Plugin extraction failed for $this and [$pluginSectionName] section." +
                " This file has incorrect toml format or missing section [$pluginSectionName]." +
                " Valid sections are: ${TestConfigSections.values().map { it.name.lowercase() }}."
    )
    throw e
}

/**
 * Create the list of plugins from toml file with plugin sections
 *
 * @param testConfigPath path to the toml file
 * @param fs FileSystem for file reading
 * @return list of plugin configs from toml file
 * @throws PluginException in case of unknown plugin
 */
fun createPluginConfigListFromToml(testConfigPath: Path, fs: FileSystem): List<PluginConfig> =
        // We need to extract only top level sections, since plugins could have own subtables
        getTopLevelTomlTables(testConfigPath, fs)
            .map { testConfigPath.testConfigFactory(it) }

/**
 * @param testConfigPath path to the test config
 * @param fs FileSystem for file reading
 * @return all top level table nodes
 */
fun getTopLevelTomlTables(testConfigPath: Path, fs: FileSystem): List<TomlTable> = TomlParser(TomlInputConfig())
    .parseStringsToTomlTree(fs.readLines(testConfigPath), TomlInputConfig())
    .children
    .filterIsInstance<TomlTable>()
    .filter { !it.isSynthetic }

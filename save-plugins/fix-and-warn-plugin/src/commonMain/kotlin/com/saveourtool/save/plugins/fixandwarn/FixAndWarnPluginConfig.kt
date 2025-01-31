package com.saveourtool.save.plugins.fixandwarn

import com.saveourtool.save.core.config.TestConfigSections
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPluginConfig

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @property fix config for nested [fix] section
 * @property warn config for nested [warn] section
 */
@Serializable
data class FixAndWarnPluginConfig(
    val fix: FixPluginConfig,
    val warn: WarnPluginConfig
) : PluginConfig {
    override val type = TestConfigSections.`FIX AND WARN`

    @Transient
    override var configLocation: Path = "undefined_toml_location".toPath()
    override val resourceNamePatternStr: String = "(${fix.resourceNamePatternStr})|(${warn.resourceNamePatternStr})"

    @Transient
    override val ignoreLinesPatterns: MutableList<Regex> = mutableListOf()

    override fun mergeWith(otherConfig: PluginConfig): PluginConfig {
        val other = otherConfig as FixAndWarnPluginConfig
        val mergedFixPluginConfig = fix.mergeWith(other.fix)
        val mergedWarnPluginConfig = warn.mergeWith(other.warn)
        return FixAndWarnPluginConfig(
            mergedFixPluginConfig as FixPluginConfig,
            mergedWarnPluginConfig as WarnPluginConfig
        ).also {
            it.configLocation = this.configLocation
        }
    }

    override fun validateAndSetDefaults(): PluginConfig {
        require(warn.resourceNamePattern.matches(fix.resourceNameTest) &&
                fix.batchSize == warn.batchSize
        ) {
            """
               Test files batch sizes should be identical for [fix] and [warn] plugins and [fix] files should match [warn] regex .
               But found [fix]: {${fix.resourceNameTest}, ${fix.batchSize}},
                         [warn]: {${warn.resourceNamePatternStr}, ${warn.batchSize}}
           """
        }
        return FixAndWarnPluginConfig(
            fix.validateAndSetDefaults(),
            warn.validateAndSetDefaults()
        ).also {
            it.configLocation = this.configLocation
        }
    }
}

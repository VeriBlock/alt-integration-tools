package altchain.explorer.verifier.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import io.github.config4k.extract
import java.lang.ClassLoader.getSystemResourceAsStream
import java.nio.file.Paths

private const val CONFIG_FILE_ENV_VAR = "EXPLORER_VERIFIER_CONFIG_FILE"
private const val DEFAULT_CONFIG_FILE = "application.conf"

private val logger = createLogger {}

class Configuration(
    configFilePath: String = "./$DEFAULT_CONFIG_FILE"
) {
    val path = System.getenv(CONFIG_FILE_ENV_VAR) ?: configFilePath

    private var config: Config = loadConfig(path)

    fun <T> getOrNull(path: String, extractor: Config.(String) -> T): T? {
        return if (config.hasPath(path)) {
            config.extractor(path)
        } else {
            null
        }
    }

    fun getBoolean(path: String) = getOrNull(path) { getBoolean(it) }

    fun getInt(path: String) = getOrNull(path) { getInt(it) }

    fun getLong(path: String) = getOrNull(path) { getLong(it) }

    fun getDouble(path: String) = getOrNull(path) { getDouble(it) }

    fun getString(path: String) = getOrNull(path) { getString(it) }

    inline fun <reified T> extract(path: String): T? = getOrNull(path) { extract<T>(it) }

    fun list(): Map<String, String> {
        val sysProperties = System.getProperties()
        return config.entrySet().filter { entry ->
            !sysProperties.containsKey(entry.key)
        }.associate {
            it.key to it.value.render(ConfigRenderOptions.concise()).replace("\"", "")
        }.toSortedMap()
    }
}

private fun loadConfig(configFilePath: String): Config {
    val createDefault = System.getenv("CREATE_DEFAULT_CONFIG_FILE")?.toBoolean() ?: true
    // Attempt to load config file
    val configFile = Paths.get(configFilePath).toFile()
    val appConfig = if (configFile.exists()) {
        // Parse it if it exists
        logger.debug { "Loading config file $configFile" }
        try {
            ConfigFactory.parseFile(configFile)
        } catch (e: Throwable) {
            logger.debugWarn(e) { "Unable to load the configuration: ${e.message}" }
            logger.info { "Loading the default configuration file..." }
            ConfigFactory.load()
        }
    } else {
        logger.debug { "Config file $configFile does not exist! Loading defaults..." }
        if (createDefault) {
            logger.debug { "Writing to config file with default contents..." }
            // Otherwise, write the default config resource file (in non-docker envs)
            getSystemResourceAsStream(configFilePath.getDefaultConfigResourceFile())?.let {
                // Write its contents as the config file
                configFile.writeBytes(it.readBytes())
            }
        }
        // And return the default config
        ConfigFactory.load()
    }
    val resourceConfig = ConfigFactory.load(DEFAULT_CONFIG_FILE)
    return appConfig.withFallback(resourceConfig).resolve()
}

private fun String.getDefaultConfigResourceFile(): String {
    val file = substringAfterLast('/')
    if (!contains('.')) {
        return "$file-default"
    }
    val name = file.substringBeforeLast('.')
    val extension = file.substringAfterLast('.')
    return "$name-default.$extension"
}

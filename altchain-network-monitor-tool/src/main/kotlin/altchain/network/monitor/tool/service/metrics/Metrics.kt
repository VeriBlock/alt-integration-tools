package altchain.network.monitor.tool.service.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

object Metrics {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val meterBinders: List<MeterBinder> = listOf(
        UptimeMetrics(),
        ProcessorMetrics(),
        Log4j2Metrics(),
        JvmThreadMetrics(),
        JvmMemoryMetrics(),
        JvmGcMetrics(),
        ClassLoaderMetrics()
    )

    private val values: MutableMap<String, Double> = HashMap()

    fun updateGauge(systemName: String, networkName: String, instanceName: String, extraTags: List<Pair<String, String>>, name: String, value: Number) {
        val gaugeKey = "$systemName.$name-$instanceName-$networkName-${extraTags.joinToString(separator = "-") { "${it.first}=${it.second}" }}"

        if (!values.containsKey(gaugeKey)) {
            Gauge.builder("$systemName.$name") { values[gaugeKey] ?: 0.0 }
                .tags("network", networkName)
                .tags("instance", instanceName)
                .register(registry)
        }

        values[gaugeKey] = value.toDouble()
    }

    fun updateGauge(systemName: String, networkName: String, instanceName: String, name: String, value: Number) =
        updateGauge(systemName, networkName, instanceName, emptyList(), name, value)
}
package altchain.network.monitor.tool

val IMPORTANT_METRICS = mapOf(
    MetricType.SYSTEM_CPU_USAGE to "system_cpu_usage",
    MetricType.PROCESS_CPU_USAGE to "process_cpu_usage",
    MetricType.RUNNABLE_THREADS to "jvm_threads_states_threads{state=\"runnable\",}",
    MetricType.BLOCKED_THREADS to "jvm_threads_states_threads{state=\"blocked\",}",
    MetricType.WAITING_THREADS to "jvm_threads_states_threads{state=\"waiting\",}",
    MetricType.TIMED_WAITING_THREADS to "jvm_threads_states_threads{state=\"timed-waiting\",}",
    MetricType.NEW_THREADS to "jvm_threads_states_threads{state=\"new\",}",
    MetricType.TERMINATED_THREADS to "jvm_threads_states_threads{state=\"terminated\",}",
    MetricType.LIVE_THREADS to "jvm_threads_live_threads",
    MetricType.PEAK_THREADS to "jvm_threads_peak_threads",
    MetricType.DAEMON_THREADS to "jvm_threads_daemon_threads"
)

val MINER_METRICS = mapOf(
    MetricType.UPTIME_SECONDS to "process_uptime_seconds",
    MetricType.COMPLETED_OPERATIONS to "pop_miner_operations_total{action=\"completed\",}",
    MetricType.FAILED_OPERATIONS to "pop_miner_operations_total{action=\"failed\",}",
    MetricType.STARTED_OPERATIONS to "pop_miner_operations_total{action=\"started\",}"
)

enum class MetricType {
    // General
    UPTIME_SECONDS,
    SYSTEM_CPU_USAGE,
    PROCESS_CPU_USAGE,
    RUNNABLE_THREADS,
    BLOCKED_THREADS,
    WAITING_THREADS,
    TIMED_WAITING_THREADS,
    NEW_THREADS,
    TERMINATED_THREADS,
    LIVE_THREADS,
    PEAK_THREADS,
    DAEMON_THREADS,

    // Miners
    COMPLETED_OPERATIONS,
    FAILED_OPERATIONS,
    STARTED_OPERATIONS
}
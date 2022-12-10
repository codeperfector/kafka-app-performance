import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;


fun prometheusStart() {
    try {
        DefaultExports.initialize()
        HTTPServer(8080)
        logger.info("Started prometheus on port 8080")
    } catch (e: Exception) {
        logger.info("Failed to start prometheus: $e")
    }
}
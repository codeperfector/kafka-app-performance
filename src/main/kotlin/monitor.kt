import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;


fun prometheusStart() {
    try {
        DefaultExports.initialize()
        HTTPServer(8080)
        println("Started prometheus on port 8080")
    } catch (e: Exception) {
        println("Failed to start prometheus: $e")
    }
}
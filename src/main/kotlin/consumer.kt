import io.prometheus.client.Counter
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

val consumedMessages = Counter.build().apply {
    name("consumed_messages")
    help("Counts all messages consumed from the topic")
}.register()

fun createConsumer(): Consumer<String, String> {
    val props = Properties()
    props.setProperty("bootstrap.servers", "kafka-0.kafka-headless.default.svc.cluster.local:9092")
    props.setProperty("group.id", "test")
    props.setProperty("enable.auto.commit", "true")
    props.setProperty("auto.commit.interval.ms", "1000")
    props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    return KafkaConsumer(props)
}

fun Consumer<String, String>.consumeMessages(topic: String) {
    subscribe(listOf(topic))
    while (true) {
        val messages: ConsumerRecords<String, String> = poll(Duration.ofMillis(5000))
        if (!messages.isEmpty) {
            for (message: ConsumerRecord<String, String> in messages) {
                println("Consumer reading message: ${message.value()}")
                consumedMessages.inc()
            }
            commitSync()
        } else {
            println("No messages to read and poll timeout reached")
        }
    }
}
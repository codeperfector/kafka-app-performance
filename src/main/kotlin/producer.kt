import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.fixedRateTimer

fun createProducer(): Producer<String, String> {
    val props = Properties()
    props["bootstrap.servers"] = "kafka-0.kafka-headless.default.svc.cluster.local:9092"
//    props["bootstrap.servers"] = "192.168.64.5:30100"
    props["acks"] = "all"
    props["retries"] = 0
    props["linger.ms"] = 1
    props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
    props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"

    return KafkaProducer(props)
}

fun Producer<String, String>.produceMessages(topic: String) {
    while (true) {
        Thread.sleep(1000)
        val time = LocalDateTime.now()
        val message = ProducerRecord(
            topic, // topic
            time.toString(), // key
            "Message sent at ${LocalDateTime.now()}" // value
        )
        println("Producer sending message: $message")
        this@produceMessages.send(message)
        this@produceMessages.flush()
    }
}

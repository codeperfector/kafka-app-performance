import io.prometheus.client.Counter
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.time.Instant
import java.util.*
import java.util.concurrent.Future

val producedMessages = Counter.build()
    .name("produced_messages_per_partition")
    .labelNames("partition")
    .help("Counts all messages produced to the topic per partition")
    .register()


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

var currentKey = 0
val maxKeys = 5
fun getLowGranularityKey(): String {
    currentKey = (currentKey++) % maxKeys
    return currentKey.toString()
}

fun produceMessages(producer: Producer<String, String>, topic: String) {
    val recordMetadataFutures = mutableListOf<Future<RecordMetadata>>();
    while (true) {
        // Current time will be used as timestamp
        val time = Instant.now()

        // We need to produce a granaular key that has good distribution among our partitions.
        // If the key has a small set of values then it can produce hot partitions that have too much data.
        // Kafka consumer uses Murmur2Hash to calculate the partition according to this pseudocode:
        // murmur2_hash(keyBytes) % numPartitions
        // So if the key is the same it produces to the same partition. Ordering is guaranteed
        // by Kafka for messages sent to the same partition.
        // Note that if the number of partitions changes, then messages with the same key can be sent to
        // different partitions. You must be cognizant of this issue if you choose to increase the
        // partition count in the broker to achieve more parallelism. Unless necessary, you must almost never change
        // the partition count in the broker.
        val key = when(selectedScenario.producerKeyType){
            ProducerKeyType.NotKeyed -> null
            ProducerKeyType.LowGranularity -> getLowGranularityKey()
            ProducerKeyType.HighGranularity -> time.epochSecond.toString()
        }

        val message = ProducerRecord(
            topic, // topic
            null, // partition, if null selected by producer default partitioner
            time.epochSecond, //timestamp, if null then System.currentTimeMillis()
            key, // key
            "Message sent at ${time.epochSecond}", // value
            null
        )
        recordMetadataFutures.add(producer.send(message))

        // Let's flush and record a batch of messages at a time
        if (recordMetadataFutures.count() >= selectedScenario.producerBatchSize) {

            // This guarantees that the producer actually sent the messages up to this point.
            // However the producer is sending messages in the background and not waiting for a call to flush().
            producer.flush()

            recordMetadataFutures
                .mapNotNull { it.get() }
                .groupBy { it.partition() }
                .forEach{ (partition, records) ->
                    producedMessages.labels(partition.toString()).inc(records.count().toDouble())
                    logger.debug("Produced messages partition=$partition, count=${records.count()}")
                }
            // Clear so that we don't record the same future again
            recordMetadataFutures.clear()

            Thread.sleep(selectedScenario.producerDelayMillis)
        }
    }
}

import io.prometheus.client.Counter
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.time.Instant
import java.util.*
import java.util.concurrent.Future

/**
 * This is a simple producer that produces string messages with message keys.
 * Based on the selected scenario, the producer will use different types of keys.
 * Imagine that we are running a business that sells product and we have multiple store locations.
 * For demonstration purposes, we can produce two types of message keys:
 * "store-product" (high granularity) or just "store" (low granularity)
 * Assume we have 100 stores and 1000 products. To make things simple we will just use numbers to represent them.
 * Some stores are larger and have more products while some stores have less.
 * Different stores will have different number of products. We will simply randomize the number per store.
 */

val producedMessages = Counter.build()
    .name("produced_messages_per_partition")
    .labelNames("partition")
    .help("Counts all messages produced to the topic per partition")
    .register()

// Kotlin allows the lazy initialization pattern without the cumbersome boilerplate in Java
val productsPerStore by lazy {initState()}

fun initState(): MutableMap<Int, Int> {
    val productsPerStore = mutableMapOf<Int, Int>()
    val random = Random()
    // 80% of stores will have less than 100 products
    for (i in 1..80) {
        productsPerStore[i] = random.nextInt(100) + 1
    }
    // 20% will have less than 1000 products
    for (i in 81..100) {
        productsPerStore[i] = random.nextInt(1000) + 1
    }
    return productsPerStore
}


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

val recordMetadataFutures = mutableListOf<Future<RecordMetadata>>();

fun produceMessages(producer: Producer<String, String>, topic: String) {
    while (true) {
        for (store in 1..100) {
            for (product in 1..(productsPerStore[store]?:100)) {
                produceMessageWithDelay(producer, topic, store, product)
            }
        }
    }
}

fun produceMessageWithDelay(producer: Producer<String, String>, topic: String, store:Int, product:Int) {
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
        ProducerKeyType.StoreKeyed -> "$store"
        ProducerKeyType.StoreProductKeyed -> "$store-$product"
    }

    val message = ProducerRecord(
        topic, // topic
        null, // partition, if null selected by producer default partitioner
        time.epochSecond, //timestamp, if null then System.currentTimeMillis()
        key, // key
        "Store $store, product $product sent at ${time.epochSecond}", // value
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
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant
import java.util.*


val consumedMessages = Counter
    .build()
    .name("consumed_messages_per_partition")
    .labelNames("partition")
    .help("Counts all messages consumed from the topic per partition")
    .register()

val delayCounter = Counter
    .build()
    .name("delay_counter")
    .help("Counts delays inserted intentionally into the consumer")
    .register()

val rebalances = Counter
    .build()
    .name("rebalances")
    .help("Counts rebalances")
    .register()

val lagPerPartition = Gauge
    .build()
    .name("lag_per_partition")
    .labelNames("partition")
    .help("Lag from the Kafka Client per partition")
    .register()

// Using a summary since we want to know what poll size distribution actually looks like
val pollSize = Summary
    .build()
    .name("poll_size")
    .help("Number of messages in the poll, excluding zero polls")
    .quantile(0.0, 0.0) // min
    .quantile(0.5, 0.01) // median
    .quantile(0.95, 0.005) // 95th quantile
    .quantile(1.0, 0.0) // max
    .register()

// Using a summary because latency is unbounded so histogram buckets can be wildly inaccurate.
// We can use partition as a label since a partition can be assigned to only one pod at a time.
val messageLatency = Summary
    .build()
    .name("message_latency")
    .labelNames("partition")
    .help("Summary that measures latency from production to consumption for each message, measured at consumer.")
    .quantile(0.0, 0.0) // min
    .quantile(0.5, 0.01) // median
    .quantile(0.95, 0.005) // 95th quantile
    .quantile(1.0, 0.0) // max
    .register()

val pollInterval = Summary
    .build()
    .name("poll_interval")
    .help("Summary that measures interval between consumer polls")
    .quantile(0.0, 0.0) // min
    .quantile(0.5, 0.01) // median
    .quantile(0.95, 0.005) // 95th quantile
    .quantile(1.0, 0.0) // max
    .register()

val startTime = Instant.now().epochSecond

private object rebalanceListener : ConsumerRebalanceListener {
    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        // do nothing
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        logger.info("partitions assigned: $partitions")
        partitions?.let { c ->
            val p0 = c.firstOrNull { it.partition() == 0 }
            // Look for when partition 0 gets assigned to this consumer and then increment
            // We will only track partition 0 reassignment across the cluster so that we
            // can count each assignment as a single rebalance event.
            p0?.let { rebalances.inc() }
        }
    }

}

fun createConsumer(): Consumer<String, String> {
    val props = mapOf(
        "bootstrap.servers" to "kafka-0.kafka-headless.default.svc.cluster.local:9092",
        "group.id" to "test",
        "enable.auto.commit" to selectedScenario.consumerAutoCommit.toString(),
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        // The consumer should consume all messages in the poll in this period of time.
        // Otherwise the broker will remove it from the group and cause a rebalance.
        "max.poll.interval.ms" to selectedScenario.consumerMaxPollIntervalMillis.toString(),
        "max.poll.records" to selectedScenario.consumerPollSize.toString(),
        "partition.assignment.strategy" to selectedScenario.consumerPartitionAssignment
    )

    val consumer = KafkaConsumer<String, String>(
        Properties()
            .apply { props.forEach { (key, value) -> setProperty(key, value) } }
            .also { logger.info("properties: $it") }
    )

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            // If consumer is stuck in a poll during shutdown, release it.
            consumer.wakeup()
        }
    })

    return consumer
}

fun consumeMessages(consumer: Consumer<String, String>, topic: String) {
    consumer.subscribe(listOf(topic), rebalanceListener)
    while (true) {

        if (selectedScenario.consumerAdditionalDelayType != ConsumerAdditionalDelayType.None) {
            val now = Instant.now().epochSecond
            val elapsedTime = now - startTime
            logger.debug("elapsed time = $elapsedTime")
            if (elapsedTime > selectedScenario.consumerAdditionalDelayQuietPeriod &&
                if (selectedScenario.consumerAdditionalDelayActivePeriod < Long.MAX_VALUE) {
                    elapsedTime < (selectedScenario.consumerAdditionalDelayQuietPeriod
                            + selectedScenario.consumerAdditionalDelayActivePeriod)
                } else true
            ) {
                logger.debug("elapsed time within active period")
                if (Math.random() < selectedScenario.consumerAdditionalDelayPercentage/100.0) {
                    logger.debug("adding additional delay")
                    delayCounter.inc()
                    val delaySign = when (selectedScenario.consumerAdditionalDelayType) {
                        ConsumerAdditionalDelayType.Small -> -1
                        ConsumerAdditionalDelayType.Large -> +1
                        else -> 0
                    }
                    val delay = selectedScenario.consumerMaxPollIntervalMillis + delaySign * 1000
                    logger.debug("delay = $delay")
                    Thread.sleep(delay)
                }
            }
        }

        val messages: ConsumerRecords<String, String> = consumer.poll(Duration.ofMillis(5000))

        pollInterval.time {

            updateLagMetrics(consumer)

            if (messages.isEmpty) {
                // This may happen for various reasons internal to the consumer as part of the fetch protocol especially during rebalances.
                // The correct thing to do here is to return back to the poll immediately.
                logger.debug("poll size: 0")
            } else {
                pollSize.observe(messages.count().toDouble())
                logger.debug("poll size: ${messages.count()}")
                messages
                    .groupBy { it.partition() }
                    .forEach { (partition, records) ->
                        records.forEach {
                            val msgTimestamp = it.timestamp()
                            messageLatency.labels(partition.toString())
                                .observe((Instant.now().epochSecond - msgTimestamp).toDouble())
                        }
                        consumedMessages.labels(partition.toString()).inc(records.count().toDouble())
                        logger.debug("Consumed messages partition=$partition, count=${records.count()}")
                    }
                consumer.commitSync()
                Thread.sleep(selectedScenario.consumerConstantDelayMillis)
            }
        }
    }
}

private fun updateLagMetrics(consumer: Consumer<String, String>) {
    // This is a hack to measure lag for our project. In real production systems you will want some sort of
    // external lag monitoring system such as Burrow or you can write one using the Kafka admin client.
    consumer
        .assignment()
        .mapNotNull { topicPartition -> topicPartition.partition() to consumer.currentLag(topicPartition) }
        .forEach { (partition, lag) ->
            lag.ifPresent {
                //logger.debug("partition=$partition, lag=${lag.asLong}")
                lagPerPartition
                    .labels(partition.toString())
                    .set(it.toDouble())
            }
        }
}
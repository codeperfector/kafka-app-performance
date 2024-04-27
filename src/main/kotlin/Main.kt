import mu.KotlinLogging


val logger = KotlinLogging.logger {}

val selectedScenario = Scenarios.Normal

enum class ProducerKeyType {
    NotKeyed, StoreKeyed, StoreProductKeyed
}

enum class ConsumerAdditionalDelayType {
    None, Small, Large
}


const val CONSUMER_PARTITION_ASSIGNMENT_DEFAULT = "org.apache.kafka.clients.consumer.RangeAssignor"
const val CONSUMER_PARTITION_ASSIGNMENT_COOPERATIVE = "org.apache.kafka.clients.consumer.CooperativeStickyAssignor"

private const val PRODUCER_CONSTANT_DELAY_MILLIS: Long = 100
private const val CONSUMER_CONSTANT_DELAY_MILLIS: Long = 10

// consumerConstantDelayMillis is a constant delay inserted into the consumer poll loop.
// consumerAdditionalDelayType specifies how much additional delay is inserted per poll. Large additional delay triggers rebalances.
// consumerAdditionalDelayQuietPeriod is the initial period where no additional delays are inserted into the consumer poll loop.
// consumerAdditionalDelayActivePeriod is the period of time following the quiet period where additional delays are inserted into the consumer poll loop at the
// rate dictated by consumerAdditionalDelayPercentage. Not every poll will experience an additional delay but all poll experience the constant delay.
enum class Scenarios(
    val description: String,
    // Producer sends messages in batches
    val producerBatchSize: Int,
    // Key for producer
    val producerKeyType: ProducerKeyType,
    // Partition assignment strategy
    val consumerPartitionAssignment: String,
    val consumerPollSize: Int,
    val producerDelayMillis: Long,
    val consumerMaxPollIntervalMillis: Long,
    // Adds a delay between consumer polls
    val consumerConstantDelayMillis: Long,
    val consumerAdditionalDelayQuietPeriod: Long,
    val consumerAdditionalDelayActivePeriod: Long,
    val consumerAdditionalDelayPercentage: Double,
    val consumerAdditionalDelayType: ConsumerAdditionalDelayType,
    val consumerAutoCommit: Boolean
) {
    Normal(
        "Consumption rate matches producer rate, no rebalances expected.",
        500,
        ProducerKeyType.StoreKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        0,
        Long.MAX_VALUE,
        0.0,
        ConsumerAdditionalDelayType.None,
        false
    ),
    Rebalancing(
        "Consumers have additional delay greater than max poll interval inserted after the first 15 minutes causing them to rebalance frequently.",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        60,
        Long.MAX_VALUE,
        1.0,
        ConsumerAdditionalDelayType.Large,
        false
    ),
    RebalancingCooperative(
        "Same as Rebalancing scenario but with Cooperative Sticky Assignment.",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_COOPERATIVE,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        60,
        Long.MAX_VALUE,
        1.0,
        ConsumerAdditionalDelayType.Large,
        false
    ),
    DelayedNoRebalancing(
        "Consumers have a delay smaller than max poll interval inserted after the first 15 minutes causing them to slow down but not rebalance.",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        60,
        Long.MAX_VALUE,
        1.0,
        ConsumerAdditionalDelayType.Small,
        false
    ),
    NotKeyed(
        "Consumption rate matches producer rate, no rebalances expected, no producer keys.",
        500,
        ProducerKeyType.NotKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        0,
        Long.MAX_VALUE,
        0.0,
        ConsumerAdditionalDelayType.None,
        false
    ),
    HighGranularityKeys(
        "Consumption rate matches producer rate, no rebalances expected, high granularity producer keys.",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        0,
        Long.MAX_VALUE,
        0.0,
        ConsumerAdditionalDelayType.None,
        false
    ),
    ConsumerAutoCommitWithLargeRandomDelay(
        "Consumption rate matches producer rate, no rebalances expected, high granularity producer keys.",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        0,
        Long.MAX_VALUE,
        0.0,
        ConsumerAdditionalDelayType.None,
        false
    ),
    ProducerFlushesEachMessage(
        "Consumption rate matches producer rate, no rebalances expected, high granularity producer keys.",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        0,
        Long.MAX_VALUE,
        0.0,
        ConsumerAdditionalDelayType.None,
        false
    ),
    LowConsumptionThenHigh(
        "Consumers run slow for the first 15 minutes and then back to normal",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        10000,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        0,
        300,
        100.0,
        ConsumerAdditionalDelayType.Small,
        false
    ),
    LowConsumptionThenHighWithTunedFetchRate(
        "Consumption rate matches producer rate, no rebalances expected, high granularity producer keys.",
        500,
        ProducerKeyType.StoreProductKeyed,
        CONSUMER_PARTITION_ASSIGNMENT_DEFAULT,
        500,
        PRODUCER_CONSTANT_DELAY_MILLIS,
        10000,
        CONSUMER_CONSTANT_DELAY_MILLIS,
        0,
        Long.MAX_VALUE,
        0.0,
        ConsumerAdditionalDelayType.None,
        false
    ),
}

fun main(args: Array<String>) {

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    logger.info("Program arguments: ${args.joinToString()}")

    logger.info("Test scenario: ${selectedScenario.name} - ${selectedScenario.description}")

    printEnvVars()

    try {
        prometheusStart()

        if (System.getenv("MYAPP_PRODUCER")?.toBoolean() == true) {
            logger.info("Running as producer")
            produceMessages(createProducer(), "test")
        } else {
            logger.info("Running as consumer")
            consumeMessages(createConsumer(), "test")
        }
    } catch (e: Exception) {
        logger.info("Exception: $e")
        e.printStackTrace()
        System.exit(1)
    }
}

private fun printEnvVars() {
    val allEnvs = System.getenv()
    allEnvs.forEach { (k, v) -> println("$k => $v") }
}
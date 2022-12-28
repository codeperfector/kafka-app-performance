import mu.KotlinLogging

val logger = KotlinLogging.logger {}
val chosenTestCase = TestCases.Normal

enum class ConsumerAdditionalDelayType {
    None, Small, Large
}

enum class TestCases(val description: String,
                     val producerBatchSize: Int,
                     val consumerPollSize: Int,
                     val producerDelayMillis: Long,
                     val consumerMaxPollIntervalMillis: Long,
                     val consumerConstantDelayMillis: Long,
                     val consumerAdditionalDelayQuietPeriod: Long,
                     val consumerAdditionalDelayActivePeriod: Long,
                     val consumerAdditionalDelayPercentage: Double,
                     val consumerAdditionalDelayType: ConsumerAdditionalDelayType) {
    Normal("Consumption rate matches producer rate, no rebalances expected.",
        500, 500, 10, 10000, 10, 0, Long.MAX_VALUE, 0.0, ConsumerAdditionalDelayType.None),
    Rebalancing("Consumers have additional delay greater than max poll interval inserted after the first 15 minutes causing them to rebalance frequently",
        500, 500, 10, 10000, 10, 60, Long.MAX_VALUE, 1.0, ConsumerAdditionalDelayType.Large),
    DelayedNoRebalancing("Consumers have a delay smaller than max poll interval inserted after the first 15 minutes causing them to slow down but not rebalance",
        500, 500, 10, 10000, 10, 60, Long.MAX_VALUE, 1.0, ConsumerAdditionalDelayType.Small),
    LowConsumptionThenHigh("Consumers run slow for the first 15 minutes and then back to normal",
        500, 10000, 0, 10000, 10, 0, 300, 100.0, ConsumerAdditionalDelayType.Small),
}

fun main(args: Array<String>) {

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    logger.info("Program arguments: ${args.joinToString()}")

    logger.info("Test scenario: ${chosenTestCase.name} - ${chosenTestCase.description}")

    val allEnvs = System.getenv()
    allEnvs.forEach { (k, v) -> println("$k => $v") }

    try {
        prometheusStart()

        if (System.getenv("MYAPP_PRODUCER")?.toBoolean() == true) {
            logger.info("Starting producer")
            produceMessages(createProducer(), "test")
        } else {
            logger.info("Starting consumer")
            consumeMessages(createConsumer(), "test")
        }
    } catch (e: Exception) {
        logger.info("Exception: $e")
        e.printStackTrace()
        System.exit(1)
    }
}
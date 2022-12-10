import mu.KotlinLogging

val logger = KotlinLogging.logger {}

const val PRODUCTION_MESSAGES_BATCH_SIZE: Int = 500
const val PRODUCTION_DELAY_MILLIS: Long = 10
const val CONSUMPTION_DELAY_MILLIS: Long = 10

fun main(args: Array<String>) {

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    val allEnvs = System.getenv()
    allEnvs.forEach { (k, v) -> println("$k => $v") }

    try {
        prometheusStart()

        if (System.getenv("MYAPP_PRODUCER") ?.toBoolean() == true) {
            produceMessages(createProducer(), "test")
        } else {
            consumeMessages(createConsumer(), "test")
        }
    } catch (e: Exception) {
        println("Exception: $e")
        e.printStackTrace()
        System.exit(1)
    }
}
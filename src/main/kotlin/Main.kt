
fun main(args: Array<String>) {

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    val allEnvs = System.getenv()
    allEnvs.forEach { (k, v) -> println("$k => $v") }

    try {
        prometheusStart()

        if (System.getenv("MYAPP_PRODUCER") ?.toBoolean() == true) {
            createProducer().produceMessages("test")
        } else {
            createConsumer().consumeMessages("test")

        }
    } catch (e: Exception) {
        println("Exception: $e")
        e.printStackTrace()
        System.exit(1)
    }
}
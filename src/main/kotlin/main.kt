import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import command.JCommandUserException
import java.lang.StringBuilder
import java.util.concurrent.Executors

@Parameters(commandNames = arrayOf("help"), commandDescription = "show help")
class HelpCommand {}

@Parameters(commandNames = arrayOf("exit"), commandDescription = "Exit the CLI")
class ExitCommand {}

@Parameters(commandNames = arrayOf("start"), commandDescription = "number of workers to start")
class StartCommand {
    @Parameter(required = true)
    var workersCount: String = "0"
}

@Parameters(commandNames = arrayOf("status"), commandDescription = "Shows current cluster status")
class StatusCommand {}

@Parameters(commandNames = arrayOf("send"), commandDescription = "Sends a data")
class SendCommand {
    @Parameter(required = true, arity = 2)
    var senderAndMessage: List<String> = mutableListOf()
}

@Parameters(commandNames = arrayOf("partition"), commandDescription = "Assign partition id to workers")
class PartitionCommand {
    @Parameter(required = true)
    var partitionAndWorkers: List<String> = mutableListOf()
}

@Parameters(commandNames = arrayOf("heal"), commandDescription = "Puts all workers into single partition")
class HealCommand {}


fun main(args: Array<String>) {

//    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO")
    System.setProperty("org.slf4j.simpleLogger.logFile", "server.log")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss:SSS Z")
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")


    val threadPool = Executors.newFixedThreadPool(50)
    val workerThreads = mutableListOf<Worker>()
    val broker = MessageBroker()


    val cli = JLineCLI()

    cli.registerCommand(HelpCommand::class) { _, console ->
        cli.getJCommander().usage(console)
    }

    cli.registerCommand(StartCommand::class) { command, console ->
        workerThreads.addAll(
                0.rangeTo(Integer.valueOf(command.workersCount) - 1).map { Worker(it.toString(), broker) }
        )
        broker.register(workerThreads)
        workerThreads.forEach { threadPool.submit(it) }
        console.appendln("Completed")
    }

    cli.registerCommand(PartitionCommand::class) { command, console ->
        val partitionId: Int = command.partitionAndWorkers.getOrElse(0) { throw JCommandUserException("Partition id has to be specified") }.toInt()
        val workerIds = command.partitionAndWorkers.subList(1, command.partitionAndWorkers.size)
        broker.partition(partitionId, workerIds)
        console.appendln("Completed")
    }
    cli.registerCommand(HealCommand::class) { _, console ->
        broker.partition(0, workerThreads.map { it.id })
        console.appendln("Completed")
    }


    cli.registerCommand(StatusCommand::class) { _, console ->
        workerThreads.forEach {
            val sb = StringBuilder()
            sb.appendln("Worker id: ${it.id}")
            sb.appendln("Partition id: ${broker.gerPartitionId(it.id)}")
            sb.appendln("Clock: ${it.myClock}")
            sb.appendln("Received: ${it.receivedQueue}")
            sb.appendln("My: ${it.myQueue}")
            console.appendln(sb)
        }
    }

    cli.registerCommand(SendCommand::class) { command, console ->
        workerThreads.filter { it.id == command.senderAndMessage[0] }.forEach { it.sendMessage(command.senderAndMessage[1]) }
        console.appendln("Completed")
    }


    cli.registerCommand(ExitCommand::class) { _ ->
        workerThreads.forEach(Worker::stopWorker)
        threadPool.shutdown()
        cli.stop()
    }

    cli.start()

}
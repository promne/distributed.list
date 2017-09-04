import broker.MessageBroker
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import command.JCommandUserException
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
    @Parameter(names=arrayOf("--partitions","-p"), required = true, description="list of partition ids separated by comma")
    var partitions: Set<Int> = mutableSetOf()

    @Parameter(names=arrayOf("--workers","-w"), required = true, description="list of worker ids separated by comma")
    var workers: Set<String> = mutableSetOf()
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

	fun getWorkerById(workerId: String) = workerThreads.filter { it.id == workerId}.singleOrNull() ?: throw IllegalArgumentException("Unknown worker with id: $workerId")

    val cli = JLineCLI()

    cli.registerCommand(HelpCommand::class) { _, console ->
        cli.getJCommander().usage(console)
    }

    cli.registerCommand(StartCommand::class) { command, console ->
		0.rangeTo(Integer.valueOf(command.workersCount) - 1)
			.map { Worker(it.toString(), broker) }
			.forEach {
				workerThreads.add(it)
				broker.register(it, false)
			}
        workerThreads.forEach { threadPool.submit(it) }
        console.appendln("Completed")
    }

    cli.registerCommand(PartitionCommand::class) { command, console ->
		command.workers.forEach {
			broker.register(getWorkerById(it), false, command.partitions)
		}		
        console.appendln("Completed")
    }
    cli.registerCommand(HealCommand::class) { _, console ->
		workerThreads.forEach { broker.register(it, false) }
        console.appendln("Completed")
    }


    cli.registerCommand(StatusCommand::class) { _, console ->
        workerThreads.forEach {
            val sb = StringBuilder()
            sb.appendln("Worker id: ${it.id}")
            sb.appendln("Partition ids: ${broker.getPartitionIds(it)}")
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
package command

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameters
import mu.KotlinLogging
import java.lang.Exception
import kotlin.reflect.KClass

typealias JCommandRegistryHandler<T> = (T) -> Unit

typealias JCommandRegistryExceptionHandler = (Exception) -> Unit

class JCommandUserException(override var message: String) : Exception()

class JCommandRegistry {

    private val logger = KotlinLogging.logger {}

    private val handlers = mutableMapOf<KClass<out Any>, JCommandRegistryHandler<Any>>()

    var exceptionHandler: JCommandRegistryExceptionHandler = {}

    fun <T : Any> registerHandler(commandClass: KClass<T>, handleCommandBody: JCommandRegistryHandler<T>) {
        commandClass.annotations.find { it is Parameters } ?: throw IllegalArgumentException("Command class is not annotated properly: $commandClass")
        handlers[commandClass]?.let { throw IllegalArgumentException("Command has been already registered: $commandClass") }
        handlers[commandClass] = handleCommandBody as JCommandRegistryHandler<Any>
        logger.info { "Command handler for $commandClass registered in $this" }
    }

    fun execute(command: String) {
        try {
            val jc = newCommandParser()
            jc.parse(*command.trim().split("\\s+".toRegex()).toTypedArray())
            val commandJc = jc.commands.getOrElse(jc.parsedCommand) { throw IllegalArgumentException("Unknown command: $command") }
            val parameters = commandJc.objects.getOrElse(0) { throw IllegalArgumentException("Command parameters not recognized: $command") }
            val handler = handlers.getOrElse(parameters::class) { throw IllegalArgumentException("Command handler is not registered for $parameters") }
            logger.debug { "Handling command $parameters" }
            handler(parameters)
        } catch (e: Exception) {
            exceptionHandler(e)
        }
    }

    fun newCommandParser(): JCommander {
        val jc = JCommander()
        jc.programName = ""
        handlers.keys.forEach { jc.addCommand(it.java.newInstance()) }
        return jc
    }

}
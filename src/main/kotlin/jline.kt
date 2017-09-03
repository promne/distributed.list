import com.beust.jcommander.ParameterException
import command.JCommandRegistry
import command.JCommandUserException
import jline.UnsupportedTerminal
import jline.console.ConsoleReader
import jline.console.completer.AggregateCompleter
import jline.console.completer.ArgumentCompleter
import jline.console.completer.StringsCompleter
import mu.KotlinLogging
import org.fusesource.jansi.Ansi
import java.lang.StringBuilder
import kotlin.reflect.KClass

class JLineCLI {
    // https://www.programcreek.com/java-api-examples/index.php?source_dir=GeoGig-master/src/cli/src/main/java/org/locationtech/geogig/cli/GeogigConsole.java
    private val logger = KotlinLogging.logger {}

    private var exitRun = true

    val interactiveConsole = System.console() != null

    private var consoleReader = ConsoleReader()
    private val consoleOutput = StringBuilder()
    private val commandRegistry = JCommandRegistry()

    init {
        commandRegistry.exceptionHandler = {
            when (it::class) {
                IllegalArgumentException::class -> consoleOutput.append(formatColor(it.message, Ansi.Color.YELLOW))
                ParameterException::class -> consoleOutput.append(formatColor(it.message, Ansi.Color.YELLOW))
                JCommandUserException::class -> consoleOutput.append(formatColor(it.message, Ansi.Color.YELLOW))
                else -> {
                    logger.error(it) { it.message }
                    consoleOutput.append(formatColor(it.message, Ansi.Color.RED))
                }
            }
        }
    }

    fun formatColor(text: String?, color: Ansi.Color = Ansi.Color.DEFAULT) = if (interactiveConsole) Ansi.ansi().fg(color).a(text).reset().toString() else text

    fun getJCommander() = commandRegistry.newCommandParser()

    private fun initConsoleReader() {
        //init console
        val terminal = if (interactiveConsole) null else UnsupportedTerminal()
        consoleReader = ConsoleReader(System.`in`, System.out, terminal)
        consoleReader.prompt = formatColor(">> ", Ansi.Color.GREEN)
        if (interactiveConsole) {
            addCommandCompleter(consoleReader, commandRegistry);
        }
    }

    private fun addCommandCompleter(consoleReader: ConsoleReader, commandRegistry: JCommandRegistry) {
        val completers = commandRegistry.newCommandParser().commands.entries.sortedBy { it.key }.map() { entry ->
            val commandName = entry.key
            val commandParser = entry.value

            val options = commandParser.getParameters().map { it.longestName }.sorted()
            ArgumentCompleter(StringsCompleter(commandName), StringsCompleter(options))
        }
        consoleReader.addCompleter(AggregateCompleter(completers));
    }

    fun start() {
        exitRun = false
        initConsoleReader()
        var line: String?
        do {
            consoleOutput.setLength(0)
            line = consoleReader.readLine()
            if (line.isNotBlank()) {
                commandRegistry.execute(line)
                if (consoleOutput.isNotEmpty()) consoleReader.println(consoleOutput)
            }
        } while (!exitRun)
    }

    fun stop() {
        exitRun = true
    }

    fun <T : Any> registerCommand(commandClass: KClass<T>, block: (T) -> Unit) {
        commandRegistry.registerHandler(commandClass, block)
        initConsoleReader()
    }

    fun <T : Any> registerCommand(commandClass: KClass<T>, block: (T, StringBuilder) -> Unit) {
        registerCommand(commandClass) { it -> block(it, consoleOutput) }
    }

}


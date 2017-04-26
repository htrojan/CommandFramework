package lobbi44.kt.command

import lobbi44.kt.command.util.CommandTree
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.help.GenericCommandHelpTopic
import org.bukkit.help.HelpTopic
import org.bukkit.help.IndexHelpTopic
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.SimplePluginManager
import java.lang.reflect.Method
import java.util.logging.Logger

/**
 * @author lobbi44
 */

class CommandFramework(private val plugin: Plugin, private val logger: Logger) : CommandExecutor {

    private lateinit var bukkitCommands: CommandMap
    private val commandTree = CommandTree<String, Pair<Method, Any>>()

    init {
        if (plugin.server.pluginManager is SimplePluginManager) {
            val manager = plugin.server.pluginManager
            val mapField = SimplePluginManager::class.java.getDeclaredField("commandMap")
            mapField.isAccessible = true
            try {
                bukkitCommands = mapField.get(manager) as CommandMap
            } catch (e: Exception) {
                plugin.logger.severe("Unable to register commands. CommandTree could not be retrieved!")
                e.printStackTrace()
            }
        }
    }

    /**
     * Registers all registered commands' help under an index named after the plugin
     */
    fun registerHelpTopic(permission: String) {
        val topics = mutableListOf<HelpTopic>()
        commandTree.getChildren().forEach({ topics.add(GenericCommandHelpTopic(bukkitCommands.getCommand(it))) })

        val help = IndexHelpTopic(plugin.name, "The registered commands", permission, topics)
        plugin.server.helpMap.addTopic(help)
    }

    /**
     * Registers all commands defined in the functions the object posesses
     * that are marked with the @Command annotation
     *
     * @param obj The object to register the commands from
     */
    fun registerCommands(obj: Any): Int {
        var reg = 0
        obj.javaClass.methods.forEach {
            val commandData = it.getAnnotation(lobbi44.kt.command.annotations.Command::class.java)
            val signatureMatches = it.parameterCount == 1 && it.parameterTypes[0] == CommandEvent::class.java && it.returnType == Boolean::class.java
            if (commandData != null && signatureMatches) {
                registerCommand(commandData, obj, it)
                ++reg
            }
        }
        return reg
    }

    private fun registerCommand(commandData: lobbi44.kt.command.annotations.Command, obj: Any, method: Method) {
        val subCommands = commandData.name.split(".")
        val commandLabel = subCommands[0]
        val isCommandPresent = commandTree.hasChild(commandLabel)
        commandTree.addChain(subCommands, Pair(method, obj))
        //This ia only a subCommand to a already registered commandData in the Bukkit bukkitCommands
        if (isCommandPresent)
            return

        bukkitCommands.register(plugin.name, DelegateCommand(commandLabel, commandData.description, commandData.usage, listOf(), this::onCommand))
    }

    /**
     * This method handles all commands delegated to the commandFramework.
     * The commands' names are split up into their label and their args
     */
    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {
        val pathToCommand: List<String> = if (args == null) listOf(label!!) else listOf(label!!) + args.toList()
        val treeResult = commandTree.getValueIgnored(pathToCommand)
        val cmd = treeResult.value

        if (cmd == null) { //Show the correct usage
            val child = commandTree.getChild(pathToCommand)

            val possibleCommands = child.getChildren()
            val display = toUsageString(label, possibleCommands)
            sender?.sendMessage(display)
            return false
        }

        val nonNullArgs: List<String> = args?.drop(treeResult.depth).orEmpty()

        val result = cmd.first.invoke(cmd.second, CommandEvent(sender!!, command!!, label, nonNullArgs)) as Boolean
        return result
    }

    private fun toUsageString(commandLabel: String?, possibleCommands: Set<String>)
            = "This command could not be found. You may try:\n" +
            possibleCommands.joinToString(separator = "\n", transform = { "/$commandLabel $it" })

    /**
     * This implementation of the Bukkit Command class is used to delegate all executions of the commands to the CommandFramework
     */
    private class DelegateCommand(name: String?, description: String? = null, usageMessage: String? = null, aliases: List<String>
                                  , var executor: (CommandSender?, Command?, String?, Array<out String>?) -> Boolean)
        : Command(name, description, usageMessage, aliases) {


        override fun execute(sender: CommandSender?, commandLabel: String?, args: Array<out String>?): Boolean = executor(sender, this, commandLabel, args)
    }

}
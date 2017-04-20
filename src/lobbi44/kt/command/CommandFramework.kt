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
        logger.info("RegisterCommands called")
        var reg = 0
        for (method in obj.javaClass.methods) {
            logger.info("Method \"${method.name}\" found")
            val command = method.getAnnotation(lobbi44.kt.command.annotations.Command::class.java)
            if (command != null && method.parameterCount == 1 && method.parameterTypes[0] == CommandEvent::class.java && method.returnType == Boolean::class.java) {
                registerCommand(command, obj, method)
                ++reg
                logger.info("This method has been registered")
            } else
                continue
        }
        return reg
    }

    private fun registerCommand(command: lobbi44.kt.command.annotations.Command, obj: Any, method: Method) {
        val subCommands = command.name.split(".")
        val commandLabel = subCommands[0]
        val isCommandPresent = commandTree.hasChild(commandLabel)
        commandTree.addChain(subCommands, Pair(method, obj))
        //This ia only a subCommand to a already registered command in the Bukkit bukkitCommands
        if (isCommandPresent)
            return

        bukkitCommands.register(plugin.name, DelegateCommand(commandLabel, command.description, command.usage, listOf(), this::onCommand))
    }

    /**
     * This method handles all commands delegated to the commandFramework.
     * The commands' names are split up into their label and their args
     */
    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {

        //todo: Check for null more efficiently for better error handling
        val searchList: List<String> = if (args == null) listOf<String>(label!!) else listOf(label!!) + args.toList()
        val cmd = commandTree.getValueIgnored(searchList)

        if (cmd == null) { //Show the correct usage
            val child = commandTree.getChild(searchList)

            val possibleCommands = commandTree.getChildren()
            val display = "This command could not be found. You may try:\n" + possibleCommands.joinToString("\n", transform = { label + it })
            sender?.sendMessage(display)
            return false
        }

        val checkedArgs = args ?: arrayOf() //Just checks for null and inserts an empty array if needed
        val result = cmd.first.invoke(cmd.second, CommandEvent(sender!!, command!!, label, checkedArgs)) as Boolean
        return result
    }

    /**
     * This implementation of the Bukkit Command class is used to delegate all executions of the commands to the CommandFramework
     */
    private class DelegateCommand(name: String?, description: String? = null, usageMessage: String? = null, aliases: List<String>
                                  , var executor: (CommandSender?, Command?, String?, Array<out String>?) -> Boolean)
        : Command(name, description, usageMessage, aliases) {


        override fun execute(sender: CommandSender?, commandLabel: String?, args: Array<out String>?): Boolean = executor(sender, this, commandLabel, args)
    }

}
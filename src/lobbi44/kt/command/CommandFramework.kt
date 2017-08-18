package lobbi44.kt.command

import lobbi44.kt.command.util.CommandTree
import lobbi44.kt.command.util.MethodSignature
import org.bukkit.command.Command
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

class CommandFramework(private val plugin: Plugin, private val logger: Logger) {

    data class FrameworkCommand(val executor: Pair<Method, Any>, val completer: Pair<Method, Any>? = null) {
        constructor(method: Method, obj: Any) : this(Pair(method, obj))
    }


    private val commandSignature = MethodSignature(listOf(CommandEvent::class.java), Boolean::class.java)
    private lateinit var bukkitCommands: CommandMap
    private val commandTree = CommandTree<String, FrameworkCommand>()

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
            if (commandData != null && commandSignature.matches(it)) {
                registerCommand(commandData, obj, it)
                ++reg
            }
        }
        return reg
    }

    private fun registerCommand(commandData: lobbi44.kt.command.annotations.Command, obj: Any, method: Method) {
        val subCommands = commandData.name.split(".")
        val commandLabel = subCommands.first()
        val isRootCommandExisting = commandTree.hasChild(commandLabel)

        if (!isRootCommandExisting) {
            val delegateCommand = DelegateCommand(commandLabel, commandData.description, commandData.usage, listOf(), this::onCommand, this::onTabComplete)
            bukkitCommands.register(plugin.name, delegateCommand)
        }
        commandTree.addChained(subCommands, FrameworkCommand(method, obj))
    }

    /**
     * This method handles all commands delegated to the commandFramework.
     * The commands' names are split up into their label and their args
     */
    fun onCommand(sender: CommandSender?, command: Command?, label: String, args: Array<out String>?): Boolean {
        val pathToCommand: List<String> = if (args == null) listOf(label) else listOf(label) + args.toList()
        val treeResult = commandTree.getValueIgnored(pathToCommand)
        val cmd = treeResult.result

        if (cmd == null) { //Show the correct usage
            val child = commandTree.getChild(pathToCommand)

            val possibleCommands = child.getChildren()
            val display = craftUsageString(label, possibleCommands)
            sender?.sendMessage(display)
            return false
        }

        //-1 cause the label of the command is included in the depth of the tree
        val nonNullArgs: List<String> = args?.drop(treeResult.depth - 1).orEmpty()
        logger.info("Correct supplied arguments: ${nonNullArgs.joinToString()}")
        val executor = cmd.executor
        val result = executor.first.invoke(executor.second, CommandEvent(sender!!, command!!, label, nonNullArgs)) as Boolean

        return result
    }

    /**
     * Requests a list of possible completions for a command argument.

     * @param sender Source of the command.  For players tab-completing a
     * *     command inside of a command block, this will be the player, not
     * *     the command block.
     * *
     * @param bukkitCommand Command which was executed
     * *
     * @param alias The alias used
     * *
     * @param args The arguments passed to the command, including final
     * *     partial argument to be completed and excluding command label
     * *
     * @return A List of possible completions for the final argument, or null
     * *     to default to the command executor
     */
    fun onTabComplete(sender: CommandSender?, bukkitCommand: Command, alias: String?, args: Array<out String>?): MutableList<String> {
        val matchedPath = commandTree.getChild(bukkitCommand.label).getChildFurthest(args!!.asList())
        val command = matchedPath.result?.getValue()

        //Part 1: See if any completers are registered
        if (command != null && command.completer != null) {
            val completer = command.completer
            @Suppress("UNCHECKED_CAST")
            return completer.first.invoke(completer.second, sender, bukkitCommand, alias, args) as MutableList<String>
        }
        //Part 2: Look for subcommands that could match
        else if (command == null && args.lastIndex == matchedPath.depth) {
            val possibleCommands = matchedPath.result?.getChildren()
            val toSearch = args[matchedPath.depth]
            val sorted = possibleCommands?.filter { it.startsWith(toSearch) }?.sorted()
            if (sorted != null)
                return sorted.toMutableList()
        }

        return listOf<String>().toMutableList()
    }

    private fun craftUsageString(commandLabel: String?, possibleCommands: Set<String>)
            = "This command could not be found. You may try:\n" +
            possibleCommands.joinToString(separator = "\n", transform = { "/$commandLabel $it" })

    /**
     * This implementation of the Bukkit Command class is used to delegate all executions of the commands to the CommandFramework
     */
    private class DelegateCommand(name: String?, description: String? = null, usageMessage: String? = null, aliases: List<String>
                                  , var executor: (CommandSender?, Command, String, Array<out String>?) -> Boolean,
                                  var completer: (CommandSender?, Command, String?, Array<out String>?) -> MutableList<String>)
        : Command(name, description, usageMessage, aliases) {


        override fun execute(sender: CommandSender?, commandLabel: String, args: Array<out String>?): Boolean = executor(sender, this, commandLabel, args)

        override fun tabComplete(sender: CommandSender?, alias: String?, args: Array<out String>?): MutableList<String> {
            return completer(sender, this, alias, args)
        }

    }

}
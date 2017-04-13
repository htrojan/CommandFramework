package lobbi44.kt.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

/**
 * @author lobbi44
 */

data class CommandEvent(val commandSender : CommandSender, val command : Command, val label : String, val args : Array<out String>)

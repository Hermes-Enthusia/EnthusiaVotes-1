package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.application.VoteTopCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class VoteTopBukkitCommand(
    private val voteTopCommand: VoteTopCommand,
    private val lang: LangService,
) : Command("votetop") {
    init {
        permission = "enthusiasmvotes.vote"
    }

    override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(lang.msg("error.players_only"))
            return true
        }

        val message = voteTopCommand.execute(sender.name, sender.uniqueId)
        sender.sendMessage(message)
        return true
    }
}
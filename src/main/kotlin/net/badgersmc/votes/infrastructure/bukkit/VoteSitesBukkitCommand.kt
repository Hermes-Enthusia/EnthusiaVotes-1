package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.application.VoteSitesCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class VoteSitesBukkitCommand(
    private val voteSitesCommand: VoteSitesCommand,
    private val lang: LangService,
) : Command("votesites") {
    override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(lang.msg("error.players_only"))
            return true
        }

        val message = voteSitesCommand.execute()
        sender.sendMessage(message)
        return true
    }
}
package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.application.VoteCommand
import net.badgersmc.votes.infrastructure.form.BedrockVoteForm
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class VoteBukkitCommand(
    private val voteCommand: VoteCommand,
    private val bedrockForm: BedrockVoteForm,
    private val lang: LangService,
) : Command("vote") {
    override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(lang.msg("error.players_only"))
            return true
        }

        if (BedrockVoteForm.isBedrockPlayer(sender)) {
            bedrockForm.open(sender)
        } else {
            val message = voteCommand.execute(sender.name, sender.uniqueId)
            sender.sendMessage(message)
        }
        return true
    }
}
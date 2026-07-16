package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.application.VoteRepository

/**
 * Repeating broadcast reminding players to vote.
 * Fires every 5 minutes via Bukkit scheduler.
 */
class VoteReminder(
    private val voteRepository: VoteRepository,
    private val lang: LangService,
    private val plugin: EnthusiaVotesPlugin,
) : Runnable {

    var taskId: Int = -1

    override fun run() {
        val total = voteRepository.getTotalServerVotes()
        plugin.server.onlinePlayers.forEach { player ->
            player.sendMessage(lang.msg("vote.reminder", "total" to total.toString()))
        }
    }
}

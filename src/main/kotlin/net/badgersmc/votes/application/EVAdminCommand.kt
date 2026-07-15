package net.badgersmc.votes.application

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.domain.VotePartyState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import java.util.UUID

class EVAdminCommand(
    private val votePartyService: VotePartyService,
    private val voteRepository: VoteRepository,
    private val lang: LangService,
) {
    private val header = lang.msg("admin.header")
    private val unknown = lang.msg("admin.unknown_subcommand")

    fun execute(playerName: String, playerUuid: UUID, args: List<String>): Component {
        if (args.isEmpty()) return unknown
        return when (args[0].lowercase()) {
            "forceparty" -> forceParty()
            "stats" -> serverStats()
            "party" -> partyStatus()
            else -> unknown
        }
    }

    private fun forceParty(): Component {
        votePartyService.activate()
        return lang.msg("admin.forceparty.success")
    }

    private fun serverStats(): Component {
        val totalVotes = voteRepository.getTotalServerVotes().toString()
        val topVoters = voteRepository.getTopVoters(5)

        val lines = mutableListOf(header)
        lines.add(lang.msg("admin.stats.total", "total" to totalVotes))

        if (topVoters.isEmpty()) {
            lines.add(lang.msg("admin.stats.no_votes"))
        } else {
            lines.add(lang.msg("admin.stats.top_header"))
            for ((i, stats) in topVoters.withIndex()) {
                val uuidPrefix = stats.playerUuid.toString().take(8)
                lines.add(
                    lang.msg(
                        "admin.stats.top_entry",
                        "rank" to (i + 1).toString(),
                        "uuid" to uuidPrefix,
                        "votes" to stats.totalVotes.toString(),
                        "streak" to stats.currentStreak.toString(),
                    )
                )
            }
        }

        return Component.join(JoinConfiguration.separator(Component.newline()), lines)
    }

    private fun partyStatus(): Component {
        val state = votePartyService.getState()
        if (state.active) {
            return lang.msg("admin.party.active")
        } else {
            val current = state.currentVotes.toString()
            val threshold = state.threshold.toString()
            val remaining = (state.threshold - state.currentVotes).toString()
            return lang.msg("admin.party.inactive", "current" to current, "threshold" to threshold, "remaining" to remaining)
        }
    }
}
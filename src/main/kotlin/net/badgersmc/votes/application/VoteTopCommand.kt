package net.badgersmc.votes.application

import net.badgersmc.nexus.i18n.LangService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import java.util.UUID

class VoteTopCommand(
    private val voteRepository: VoteRepository,
    private val lang: LangService,
) {
    fun execute(playerName: String, playerUuid: UUID): Component {
        val topVoters = voteRepository.getTopVoters(10)

        val lines = mutableListOf(lang.msg("votetop.header"))

        for ((i, stats) in topVoters.withIndex()) {
            val uuidPrefix = stats.playerUuid.toString().take(8)
            lines.add(
                lang.msg(
                    "votetop.entry",
                    "rank" to (i + 1).toString(),
                    "uuid" to uuidPrefix,
                    "votes" to stats.totalVotes.toString(),
                    "streak" to stats.currentStreak.toString(),
                )
            )
        }

        return Component.join(JoinConfiguration.separator(Component.newline()), lines)
    }
}
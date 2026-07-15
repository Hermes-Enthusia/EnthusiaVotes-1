package net.badgersmc.votes.application

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.infrastructure.config.VoteConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import java.util.UUID

class VoteCommand(
    private val voteRepository: VoteRepository,
    private val voteConfig: VoteConfig,
    private val lang: LangService,
) {
    fun execute(playerName: String, playerUuid: UUID): Component {
        val stats = voteRepository.getStats(playerUuid)
        val total = stats.totalVotes.toString()
        val streak = stats.currentStreak.toString()
        val best = stats.bestStreak.toString()

        val lines = mutableListOf(
            lang.msg("vote.stats.header", "player" to playerName),
            lang.msg("vote.stats.total", "total" to total),
            lang.msg("vote.stats.streak", "streak" to streak),
            lang.msg("vote.stats.best", "best" to best),
            lang.msg("vote.stats.empty_separator"),
            lang.msg("vote.stats.site_header"),
        )

        for (site in voteConfig.voteSites) {
            lines.add(
                lang.msg("vote.stats.site_entry", "name" to site.name, "url" to site.url)
            )
        }

        return Component.join(JoinConfiguration.separator(Component.newline()), lines)
    }
}
package net.badgersmc.votes.application

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.infrastructure.config.VoteConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration

class VoteSitesCommand(
    private val voteConfig: VoteConfig,
    private val lang: LangService,
) {
    fun execute(): Component {
        val lines = mutableListOf(
            lang.msg("votesites.header"),
        )

        for (site in voteConfig.voteSites) {
            lines.add(
                lang.msg("votesites.site_entry", "name" to site.name, "url" to site.url)
            )
        }

        return Component.join(JoinConfiguration.separator(Component.newline()), lines)
    }
}
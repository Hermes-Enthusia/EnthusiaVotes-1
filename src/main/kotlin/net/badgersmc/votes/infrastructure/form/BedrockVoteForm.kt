package net.badgersmc.votes.infrastructure.form

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.application.VoteRepository
import net.badgersmc.votes.infrastructure.config.VoteConfig
import org.bukkit.entity.Player
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.floodgate.api.FloodgateApi
import java.util.logging.Logger

/** Sends a SimpleForm to Bedrock players for /vote. */
class BedrockVoteForm(
    private val voteRepository: VoteRepository,
    private val voteConfig: VoteConfig,
    private val logger: Logger,
    private val lang: LangService,
) {
    companion object {
        fun isBedrockPlayer(player: Player): Boolean {
            return try {
                FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)
            } catch (_: Exception) {
                false
            }
        }
    }

    fun open(player: Player) {
        val stats = voteRepository.getStats(player.uniqueId)
        val form = SimpleForm.builder()
            .title(lang.raw("bedrock.form.title"))
            .content(
                lang.legacy(
                    "bedrock.form.content",
                    "total" to stats.totalVotes.toString(),
                    "streak" to stats.currentStreak.toString(),
                    "best" to stats.bestStreak.toString(),
                )
            )

        for (site in voteConfig.voteSites) {
            form.button(site.name)
        }

        form.closedOrInvalidResultHandler { _, _ -> /* ignore close */ }
        form.validResultHandler { _, response ->
            val index = response.clickedButtonId()
            if (index < voteConfig.voteSites.size) {
                val site = voteConfig.voteSites[index]
                player.sendMessage(lang.msg("bedrock.form.link_message", "name" to site.name, "url" to site.url))
            }
        }

        try {
            FloodgateApi.getInstance().sendForm(player.uniqueId, form)
        } catch (e: Exception) {
            logger.warning("Failed to open Bedrock vote form for ${player.name}: ${e.message}")
        }
    }
}
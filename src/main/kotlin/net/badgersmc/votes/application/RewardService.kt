package net.badgersmc.votes.application

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.infrastructure.config.VoteConfig
import net.kyori.adventure.text.Component

class RewardService(
    private val config: VoteConfig,
    private val votePartyService: VotePartyService,
    private val lang: LangService,
) {
    fun calculateGold(streak: Int): Int {
        val base = (config.minGold..config.maxGold).random()
        val streakMult = streakMultiplier(streak)
        val partyMult = votePartyService.getCurrentMultiplier()
        return (base * streakMult * partyMult).toInt().coerceAtLeast(1)
    }

    fun streakMultiplier(streak: Int): Double = when {
        streak >= 30 -> 3.0
        streak >= 7 -> 2.0
        streak >= 3 -> 1.5
        else -> 1.0
    }

    fun buildVoteMessage(
        playerName: String,
        gold: Int,
        streak: Int,
        serviceName: String,
    ): Component {
        val streakText = if (streak > 1) " (${streak}x streak!)" else ""
        return lang.msg(
            "voteparty.reward_message",
            "player" to playerName,
            "service" to serviceName,
            "gold" to gold.toString(),
            "streak_text" to streakText,
        )
    }
}
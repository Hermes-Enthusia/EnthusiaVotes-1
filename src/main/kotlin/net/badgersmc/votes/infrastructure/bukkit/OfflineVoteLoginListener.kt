package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.application.GoldDelivery
import net.badgersmc.votes.application.RewardService
import net.badgersmc.votes.application.VoteRepository
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class OfflineVoteLoginListener(
    private val voteRepository: VoteRepository,
    private val goldDelivery: GoldDelivery,
    private val lang: LangService,
    private val rewardService: RewardService,
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId

        rewardService.cacheMultiplier(uuid, voteRepository.getStats(uuid).currentStreak)

        val pendingGold = voteRepository.getPendingOfflineGold(uuid) ?: return

        goldDelivery.deliver(uuid, pendingGold)
        voteRepository.clearOfflineGold(uuid)

        player.sendMessage(lang.msg("voteparty.offline_delivery", "amount" to pendingGold.toString()))
    }
}
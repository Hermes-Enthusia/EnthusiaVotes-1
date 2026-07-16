package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.votes.application.VoteRepository
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class OfflineVoteLoginListener(
    private val voteRepository: VoteRepository,
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId

        val pendingGold = voteRepository.getPendingOfflineGold(uuid)
        if (pendingGold.isEmpty()) return

        val totalGold = pendingGold.sum()
        val goldDelivery = BukkitGoldDelivery()
        goldDelivery.deliver(uuid, totalGold)

        voteRepository.clearOfflineGold(uuid)

        player.sendMessage("§a§l\u2714 §7You received §e$totalGold Raw Gold§7 from votes while you were offline!")
    }
}
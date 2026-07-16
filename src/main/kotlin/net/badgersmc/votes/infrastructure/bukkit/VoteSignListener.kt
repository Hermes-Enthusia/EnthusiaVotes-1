package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.votes.infrastructure.config.VoteConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerInteractEvent

class VoteSignListener(
    private val voteConfig: VoteConfig,
    private val lang: LangService,
) : Listener {

    /**
     * In-memory map of sign locations to their configured vote site name.
     * Lost on server restart — signs are re-created by players.
     */
    private val signLocations: MutableMap<Location, String> = HashMap()

    @EventHandler
    fun onSignChange(event: SignChangeEvent) {
        val player = event.player
        val firstLine = PlainTextComponentSerializer.plainText().serialize(event.line(0) ?: Component.empty())

        if (!firstLine.equals("[Vote]", ignoreCase = true)) return

        val siteName = PlainTextComponentSerializer.plainText().serialize(event.line(1) ?: Component.empty())
        val site = voteConfig.voteSites.find { it.name.equals(siteName, ignoreCase = true) }

        if (site == null) {
            player.sendMessage(lang.msg("sign.invalid_site"))
            event.isCancelled = true
            return
        }

        signLocations[event.block.location] = site.name

        event.line(0, Component.text("[Vote]"))
        event.line(1, Component.text(site.name))

        player.sendMessage(lang.msg("sign.created", "site" to site.name))
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val clickedBlock = event.clickedBlock ?: return
        val state = clickedBlock.state
        if (state !is Sign) return

        val siteName = signLocations[clickedBlock.location] ?: return

        val site = voteConfig.voteSites.find { it.name == siteName }
        if (site == null) {
            signLocations.remove(clickedBlock.location)
            return
        }

        event.player.sendMessage(lang.msg("sign.url", "name" to site.name, "url" to site.url))
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val siteName = signLocations.remove(location)
        if (siteName != null) {
            event.player.sendMessage(lang.msg("sign.removed", "site" to siteName))
        }
    }
}
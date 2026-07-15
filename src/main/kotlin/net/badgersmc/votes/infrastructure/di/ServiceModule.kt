package net.badgersmc.votes.infrastructure.di

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.nexus.i18n.Locale
import net.badgersmc.nexus.scheduler.NexusScheduler
import net.badgersmc.votes.application.*
import net.badgersmc.votes.infrastructure.bukkit.BukkitGoldDelivery
import net.badgersmc.votes.infrastructure.bukkit.EnthusiaVotesPlugin
import net.badgersmc.votes.infrastructure.bukkit.ProxiedDeliveryService
import net.badgersmc.votes.infrastructure.bukkit.VotifierVoteListener
import net.badgersmc.votes.infrastructure.config.VoteConfig
import net.badgersmc.votes.infrastructure.form.BedrockVoteForm
import net.badgersmc.votes.infrastructure.i18n.EnthusiaVotesLang
import net.badgersmc.votes.infrastructure.messaging.BukkitVoteBroadcaster
import net.badgersmc.votes.infrastructure.papi.EnthusiaVotesExpansion
import net.badgersmc.votes.infrastructure.persistence.DatabaseFactory
import net.badgersmc.votes.infrastructure.persistence.SqliteVoteRepository
import java.time.Duration

class VoteScheduler(
    private val plugin: EnthusiaVotesPlugin,
    private val votePartyService: VotePartyService,
    private val config: VoteConfig,
) {
    fun start() {
        if (votePartyService.isPartyActive()) {
            val duration = Duration.ofMinutes(config.votePartyDurationMinutes.toLong())
            val ticks = duration.seconds * 20
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable { votePartyService.deactivate() },
                ticks,
            )
        }
    }

    fun stop() {
        // Tasks handled by Bukkit scheduler lifecycle
    }
}

class ServiceModule(
    val plugin: EnthusiaVotesPlugin,
) {
    val nexusScheduler = NexusScheduler(plugin)

    val databaseFactory: DatabaseFactory
        get() = plugin.databaseFactory ?: error("DatabaseFactory not initialized")

    val voteRepository: VoteRepository by lazy {
        SqliteVoteRepository(databaseFactory)
    }

    val voteConfig: VoteConfig by lazy {
        VoteConfig()
    }

    val votePartySpeaker: VotePartySpeaker by lazy {
        plugin.proxiedDeliveryService ?: NoOpVotePartySpeaker()
    }

    val votePartyService: VotePartyService by lazy {
        VotePartyService(voteConfig, plugin, votePartySpeaker)
    }

    val scheduler: VoteScheduler by lazy {
        VoteScheduler(plugin, votePartyService, voteConfig)
    }

    val lang: LangService by lazy {
        LangService(plugin, Locale("en_US"), EnthusiaVotesLang::class.java)
    }

    val rewardService: RewardService by lazy {
        RewardService(voteConfig, votePartyService, lang)
    }

    val voteBroadcaster: VoteBroadcaster by lazy {
        BukkitVoteBroadcaster()
    }

    val goldDelivery: GoldDelivery by lazy {
        plugin.proxiedDeliveryService ?: BukkitGoldDelivery()
    }

    val voteService: VoteService by lazy {
        VoteService(voteRepository, rewardService, voteBroadcaster, goldDelivery, votePartyService, voteConfig, lang)
    }

    val bedrockVoteForm: BedrockVoteForm by lazy {
        BedrockVoteForm(voteRepository, voteConfig, plugin.logger, lang)
    }

    val voteCommand: VoteCommand by lazy { VoteCommand(voteRepository, voteConfig, lang) }
    val voteSitesCommand: VoteSitesCommand by lazy { VoteSitesCommand(voteConfig, lang) }
    val evAdminCommand: EVAdminCommand by lazy { EVAdminCommand(votePartyService, voteRepository, lang) }

    val voteListener: VotifierVoteListener by lazy {
        VotifierVoteListener(voteService)
    }

    val placeholderExpansion: EnthusiaVotesExpansion by lazy {
        EnthusiaVotesExpansion(voteRepository, votePartyService)
    }
}

private class NoOpVotePartySpeaker : VotePartySpeaker {
    override fun onPartyActivated() {}
    override fun onPartyDeactivated() {}
}
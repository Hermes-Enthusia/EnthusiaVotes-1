package net.badgersmc.votes.infrastructure.di

import net.badgersmc.nexus.i18n.LangService
import net.badgersmc.nexus.i18n.Locale
import net.badgersmc.nexus.scheduler.NexusScheduler
import net.badgersmc.votes.application.*
import net.badgersmc.votes.infrastructure.bukkit.BukkitGoldDelivery
import net.badgersmc.votes.infrastructure.bukkit.EnthusiaVotesPlugin
import net.badgersmc.votes.infrastructure.bukkit.OfflineVoteLoginListener
import net.badgersmc.votes.infrastructure.bukkit.ProxiedDeliveryService
import net.badgersmc.votes.infrastructure.bukkit.VoteReminder
import net.badgersmc.votes.infrastructure.bukkit.VoteSignListener
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
    private val voteReminder: VoteReminder,
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
        // Start repeating reminder — every 5 minutes
        val reminderTicks = 20L * 60 * 5
        voteReminder.taskId = plugin.server.scheduler.runTaskTimer(
            plugin,
            voteReminder,
            reminderTicks,
            reminderTicks,
        ).taskId
    }

    fun stop() {
        if (voteReminder.taskId != -1) {
            plugin.server.scheduler.cancelTask(voteReminder.taskId)
        }
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

    val lang: LangService by lazy {
        LangService(plugin, Locale("en_US"), EnthusiaVotesLang::class.java)
    }

    val votePartySpeaker: VotePartySpeaker by lazy {
        plugin.proxiedDeliveryService ?: NoOpVotePartySpeaker()
    }

    val votePartyService: VotePartyService by lazy {
        VotePartyService(voteConfig, plugin, voteRepository, votePartySpeaker)
    }

    val voteReminder: VoteReminder by lazy {
        VoteReminder(voteRepository, lang, plugin)
    }

    val scheduler: VoteScheduler by lazy {
        VoteScheduler(plugin, votePartyService, voteConfig, voteReminder)
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
    val voteTopCommand: VoteTopCommand by lazy { VoteTopCommand(voteRepository, lang) }
    val evAdminCommand: EVAdminCommand by lazy { EVAdminCommand(votePartyService, voteRepository, lang) }

    val voteListener: VotifierVoteListener by lazy {
        VotifierVoteListener(voteService)
    }

    val offlineVoteLoginListener: OfflineVoteLoginListener by lazy {
        OfflineVoteLoginListener(voteRepository)
    }

    val voteSignListener: VoteSignListener by lazy {
        VoteSignListener(voteConfig, lang)
    }

    val placeholderExpansion: EnthusiaVotesExpansion by lazy {
        EnthusiaVotesExpansion(voteRepository, votePartyService)
    }

    fun resumeGiveawaysOnStartup() {
        val state = voteRepository.loadPartyState() ?: return
        if (!state.active) return

        votePartyService.loadFrom(state)

        // Reschedule deactivation based on elapsed time
        val startedAt = state.startedAt ?: return
        val elapsed = java.time.Duration.between(startedAt, java.time.Instant.now())
        val totalDuration = Duration.ofMinutes(voteConfig.votePartyDurationMinutes.toLong())
        val remaining = totalDuration.minus(elapsed)
        if (remaining.isPositive) {
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable { votePartyService.deactivate() },
                remaining.seconds * 20,
            )
        } else {
            votePartyService.deactivate()
        }
    }
}

private class NoOpVotePartySpeaker : VotePartySpeaker {
    override fun onPartyActivated() {}
    override fun onPartyDeactivated() {}
}

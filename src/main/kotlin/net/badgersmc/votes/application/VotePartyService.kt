package net.badgersmc.votes.application

import net.badgersmc.nexus.scheduler.NexusScheduler
import net.badgersmc.votes.domain.VotePartyState
import net.badgersmc.votes.infrastructure.bukkit.EnthusiaVotesPlugin
import net.badgersmc.votes.infrastructure.config.VoteConfig
import java.time.Duration
import java.time.Instant

class VotePartyService(
    private val config: VoteConfig,
    private val plugin: EnthusiaVotesPlugin,
    private val voteRepository: VoteRepository,
    private val speaker: VotePartySpeaker? = null,
) {
    @Volatile
    private var currentVotes: Int = 0

    @Volatile
    private var _active: Boolean = false

    @Volatile
    private var _startedAt: Instant? = null

    private var partyTask: Any? = null

    fun isPartyActive(): Boolean = _active

    fun getCurrentMultiplier(): Double = if (_active) 2.0 else 1.0

    fun getCurrentVotes(): Int = currentVotes

    fun getVotesNeeded(): Int = config.votePartyThreshold

    fun getRemainingVotes(): Int = (config.votePartyThreshold - currentVotes).coerceAtLeast(0)

    /**
     * Restores party state from a previously persisted snapshot (e.g., after server restart).
     */
    fun loadFrom(partyState: VotePartyState) {
        _active = partyState.active
        currentVotes = partyState.currentVotes
        _startedAt = partyState.startedAt
    }

    /**
     * Increments the vote counter. If the threshold is reached and the party isn't active,
     * activates the party and schedules automatic deactivation.
     * @return VotePartyState reflecting the current state
     */
    fun onVote(): VotePartyState {
        if (_active) {
            return VotePartyState(
                active = true,
                currentVotes = 0,
                threshold = config.votePartyThreshold,
                justActivated = false,
            )
        }

        currentVotes++

        if (currentVotes >= config.votePartyThreshold) {
            activate()
            return VotePartyState(
                active = true,
                currentVotes = 0,
                threshold = config.votePartyThreshold,
                justActivated = true,
            )
        }

        persist()
        return VotePartyState(
            active = false,
            currentVotes = currentVotes,
            threshold = config.votePartyThreshold,
            justActivated = false,
        )
    }

    fun activate() {
        _active = true
        currentVotes = 0
        _startedAt = Instant.now()
        speaker?.onPartyActivated()
        persist()

        val duration = Duration.ofMinutes(config.votePartyDurationMinutes.toLong())
        val ticks = duration.seconds * 20
        partyTask = plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable { deactivate() },
            ticks,
        )
    }

    fun deactivate() {
        _active = false
        currentVotes = 0
        _startedAt = null
        partyTask = null
        speaker?.onPartyDeactivated()
        persist()
    }

    fun getState(): VotePartyState = VotePartyState(
        active = _active,
        currentVotes = currentVotes,
        threshold = config.votePartyThreshold,
        justActivated = false,
        startedAt = _startedAt,
    )

    private fun persist() {
        voteRepository.savePartyState(getState())
    }
}
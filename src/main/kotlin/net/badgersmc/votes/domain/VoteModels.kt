package net.badgersmc.votes.domain

import java.time.Instant
import java.util.UUID

data class VoteRecord(
    val id: Long = 0,
    val playerUuid: UUID,
    val playerName: String,
    val serviceName: String,
    val timestamp: Instant = Instant.now(),
    val goldAwarded: Int,
)

data class PlayerStats(
    val playerUuid: UUID,
    val totalVotes: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastVoteAt: Instant? = null,
)

data class VoteParty(
    val threshold: Int,
    val currentVotes: Int = 0,
    val active: Boolean = false,
    val startedAt: Instant? = null,
)

data class VotePartyState(
    val active: Boolean,
    val currentVotes: Int,
    val threshold: Int,
    val justActivated: Boolean,
    val startedAt: Instant? = null,
)

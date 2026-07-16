package net.badgersmc.votes.application

import net.badgersmc.votes.domain.PlayerStats
import net.badgersmc.votes.domain.VotePartyState
import net.badgersmc.votes.domain.VoteRecord
import java.util.UUID

interface VoteRepository {
    fun saveVote(record: VoteRecord)
    fun getStats(uuid: UUID): PlayerStats
    fun getTotalVotes(uuid: UUID): Int
    fun getTopVoters(limit: Int): List<PlayerStats>
    fun getTotalServerVotes(): Int
    fun savePartyState(state: VotePartyState)
    fun loadPartyState(): VotePartyState?
    fun queueOfflineGold(uuid: UUID, gold: Int)
    fun getPendingOfflineGold(uuid: UUID): List<Int>
    fun clearOfflineGold(uuid: UUID)
}
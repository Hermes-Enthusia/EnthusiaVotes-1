package net.badgersmc.votes.infrastructure.persistence

import net.badgersmc.votes.application.VoteRepository
import net.badgersmc.votes.domain.PlayerStats
import net.badgersmc.votes.domain.VotePartyState
import net.badgersmc.votes.domain.VoteRecord
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class SqliteVoteRepository(
    private val databaseFactory: DatabaseFactory,
) : VoteRepository {

    private val db get() = databaseFactory.database

    override fun saveVote(record: VoteRecord) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(VoteTable, PlayerStatsTable)

            VoteTable.insert {
                it[playerUuid] = record.playerUuid.toString()
                it[playerName] = record.playerName
                it[serviceName] = record.serviceName
                it[timestamp] = record.timestamp.epochSecond
                it[goldAwarded] = record.goldAwarded
            }

            val existing = PlayerStatsTable.selectAll()
                .where { PlayerStatsTable.playerUuid eq record.playerUuid.toString() }
                .singleOrNull()

            if (existing != null) {
                val newStreak = computeNewStreak(record.playerUuid, record.timestamp)
                PlayerStatsTable.update({ PlayerStatsTable.playerUuid eq record.playerUuid.toString() }) {
                    it[totalVotes] = existing[PlayerStatsTable.totalVotes] + 1
                    it[currentStreak] = newStreak
                    it[bestStreak] = maxOf(existing[PlayerStatsTable.bestStreak], newStreak)
                    it[lastVoteAt] = record.timestamp.epochSecond
                }
            } else {
                PlayerStatsTable.insert {
                    it[playerUuid] = record.playerUuid.toString()
                    it[totalVotes] = 1
                    it[currentStreak] = 1
                    it[bestStreak] = 1
                    it[lastVoteAt] = record.timestamp.epochSecond
                }
            }
        }
    }

    override fun getStats(uuid: UUID): PlayerStats = transaction(db) {
        val row = PlayerStatsTable.selectAll()
            .where { PlayerStatsTable.playerUuid eq uuid.toString() }
            .singleOrNull()
        row?.let { toPlayerStats(it) } ?: PlayerStats(playerUuid = uuid)
    }

    override fun getTotalVotes(uuid: UUID): Int = transaction(db) {
        VoteTable.selectAll()
            .where { VoteTable.playerUuid eq uuid.toString() }
            .count().toInt()
    }

    override fun getTopVoters(limit: Int): List<PlayerStats> = transaction(db) {
        PlayerStatsTable.selectAll()
            .orderBy(PlayerStatsTable.totalVotes, SortOrder.DESC)
            .limit(limit)
            .map { toPlayerStats(it) }
    }

    override fun getTotalServerVotes(): Int = transaction(db) {
        VoteTable.selectAll().count().toInt()
    }

    private fun toPlayerStats(row: ResultRow): PlayerStats = PlayerStats(
        playerUuid = UUID.fromString(row[PlayerStatsTable.playerUuid]),
        totalVotes = row[PlayerStatsTable.totalVotes],
        currentStreak = row[PlayerStatsTable.currentStreak],
        bestStreak = row[PlayerStatsTable.bestStreak],
        lastVoteAt = row[PlayerStatsTable.lastVoteAt]?.let {
            Instant.ofEpochSecond(it)
        },
    )

    private fun computeNewStreak(uuid: UUID, now: Instant): Int {
        val existing = getStats(uuid)
        val last = existing.lastVoteAt ?: return 1
        val hoursSince = java.time.Duration.between(last, now).toHours()
        return if (hoursSince in 1..36) existing.currentStreak + 1 else 1
    }

    override fun queueOfflineGold(uuid: UUID, gold: Int) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(OfflineVoteTable)
            val existing = OfflineVoteTable.selectAll()
                .where { OfflineVoteTable.playerUuid eq uuid.toString() }
                .singleOrNull()
            if (existing != null) {
                OfflineVoteTable.update({ OfflineVoteTable.playerUuid eq uuid.toString() }) {
                    it[this.gold] = existing[OfflineVoteTable.gold] + gold
                    it[createdAt] = System.currentTimeMillis()
                }
            } else {
                OfflineVoteTable.insert {
                    it[playerUuid] = uuid.toString()
                    it[this.gold] = gold
                    it[createdAt] = System.currentTimeMillis()
                }
            }
        }
    }

    override fun getPendingOfflineGold(uuid: UUID): List<Int> = transaction(db) {
        OfflineVoteTable.selectAll()
            .where { OfflineVoteTable.playerUuid eq uuid.toString() }
            .orderBy(OfflineVoteTable.createdAt)
            .map { it[OfflineVoteTable.gold] }
    }

    override fun clearOfflineGold(uuid: UUID) {
        transaction(db) {
            OfflineVoteTable.deleteWhere { OfflineVoteTable.playerUuid eq uuid.toString() }
        }
    }

    override fun savePartyState(state: VotePartyState) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(VoteTable, PlayerStatsTable, VotePartyTable)
            VotePartyTable.deleteAll()
            VotePartyTable.insert {
                it[active] = state.active
                it[currentVotes] = state.currentVotes
                it[threshold] = state.threshold
                it[startedAt] = state.startedAt?.epochSecond
            }
        }
    }

    override fun loadPartyState(): VotePartyState? = transaction(db) {
        SchemaUtils.createMissingTablesAndColumns(VoteTable, PlayerStatsTable, VotePartyTable)
        VotePartyTable.selectAll().singleOrNull()?.let { row ->
            VotePartyState(
                active = row[VotePartyTable.active],
                currentVotes = row[VotePartyTable.currentVotes],
                threshold = row[VotePartyTable.threshold],
                justActivated = false,
                startedAt = row[VotePartyTable.startedAt]?.let { Instant.ofEpochSecond(it) },
            )
        }
    }
}

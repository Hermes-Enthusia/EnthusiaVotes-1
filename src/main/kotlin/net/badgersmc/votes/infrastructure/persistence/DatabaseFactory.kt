package net.badgersmc.votes.infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.badgersmc.votes.infrastructure.config.MariaDbConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import javax.sql.DataSource

interface DatabaseFactory {
    val database: Database
    fun close()
}

class LocalDatabaseFactory(
    private val dataFolder: File,
    private val fileName: String,
) : DatabaseFactory {
    override val database: Database by lazy {
        dataFolder.mkdirs()
        val url = "jdbc:sqlite:${dataFolder.absolutePath}/$fileName"
        Database.connect(url, "org.sqlite.JDBC")
    }

    override fun close() {
        // SQLite auto-closes on JVM exit
    }
}

class RemoteDatabaseFactory(
    private val config: MariaDbConfig,
) : DatabaseFactory {
    private val dataSource: DataSource by lazy { createDataSource() }

    override val database: Database by lazy {
        Database.connect(dataSource)
    }

    private fun createDataSource(): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mariadb://${config.host}:${config.port}/${config.database}?useSSL=false"
            username = config.user
            password = config.password
            driverClassName = "org.mariadb.jdbc.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30_000
            maxLifetime = 600_000
            connectionTimeout = 5_000
            isAutoCommit = false
        }
        return HikariDataSource(hikariConfig)
    }

    override fun close() {
        (dataSource as? HikariDataSource)?.close()
    }
}

object VoteTable : Table("votes") {
    val id = long("id").autoIncrement()
    val playerUuid = text("player_uuid")
    val playerName = text("player_name")
    val serviceName = text("service_name")
    val timestamp = long("timestamp")
    val goldAwarded = integer("gold_awarded").default(0)

    override val primaryKey = PrimaryKey(id)
}

object PlayerStatsTable : Table("player_stats") {
    val playerUuid = text("player_uuid")
    val totalVotes = integer("total_votes").default(0)
    val currentStreak = integer("current_streak").default(0)
    val bestStreak = integer("best_streak").default(0)
    val lastVoteAt = long("last_vote_at").nullable()

    override val primaryKey = PrimaryKey(playerUuid)
}

object OfflineVoteTable : Table("offline_votes") {
    val playerUuid = text("player_uuid")
    val gold = integer("gold")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(playerUuid)
}

object VotePartyTable : Table("vote_party") {
    val active = bool("active")
    val currentVotes = integer("current_votes")
    val threshold = integer("threshold")
    val startedAt = long("started_at").nullable()
}

object Migrations {
    fun run(database: Database) {
        transaction(database) {
            // Use create (IF NOT EXISTS) instead of createMissingTablesAndColumns —
            // SQLite doesn't support ALTER TABLE MODIFY COLUMN, so schema migration
            // must be done manually (or delete votes.db for a clean slate).
            SchemaUtils.create(VoteTable, PlayerStatsTable, OfflineVoteTable, VotePartyTable)
        }
    }
}
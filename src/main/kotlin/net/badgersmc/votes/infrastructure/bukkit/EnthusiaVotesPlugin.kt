package net.badgersmc.votes.infrastructure.bukkit

import net.badgersmc.votes.infrastructure.config.MariaDbConfig
import net.badgersmc.votes.infrastructure.config.StorageConfig
import net.badgersmc.votes.infrastructure.di.ServiceModule
import net.badgersmc.votes.infrastructure.persistence.DatabaseFactory
import net.badgersmc.votes.infrastructure.persistence.LocalDatabaseFactory
import net.badgersmc.votes.infrastructure.persistence.Migrations
import net.badgersmc.votes.infrastructure.persistence.RemoteDatabaseFactory
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class EnthusiaVotesPlugin : JavaPlugin() {

    internal var databaseFactory: DatabaseFactory? = null

    lateinit var services: ServiceModule
        private set

    internal var proxiedDeliveryService: ProxiedDeliveryService? = null
        private set

    override fun onEnable() {
        saveDefaultConfig()

        val storageConfig = loadStorageConfig()
        databaseFactory = createDatabaseFactory(storageConfig)
        Migrations.run()

        services = ServiceModule(this)

        // Register proxy plugin messaging channels
        val proxiedDelivery = ProxiedDeliveryService(this, BukkitGoldDelivery())
        proxiedDelivery.register()
        proxiedDeliveryService = proxiedDelivery

        server.commandMap.register(
            "vote",
            VoteBukkitCommand(services.voteCommand, services.bedrockVoteForm, services.lang),
        )

        server.commandMap.register(
            "votesites",
            VoteSitesBukkitCommand(services.voteSitesCommand, services.lang),
        )

        server.commandMap.register(
            "evadmin",
            EVAdminBukkitCommand(services.evAdminCommand),
        )

        server.pluginManager.registerEvents(services.voteListener, this)

        // Register PlaceholderAPI expansion if PAPI is present
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            services.placeholderExpansion.register()
            logger.info("PlaceholderAPI expansion registered.")
        }

        services.scheduler.start()

        logger.info("EnthusiaVotes enabled.")
    }

    override fun onDisable() {
        if (::services.isInitialized) {
            services.scheduler.stop()
            services.nexusScheduler.cancelAll()
            if (services.placeholderExpansion.isRegistered()) {
                services.placeholderExpansion.unregister()
            }
        }
        proxiedDeliveryService?.unregister()
        proxiedDeliveryService = null
        databaseFactory?.close()
        databaseFactory = null
    }

    private fun loadStorageConfig(): StorageConfig {
        val backend = config.getString("storage.backend") ?: "sqlite"
        val file = config.getString("storage.file") ?: "votes.db"
        val mariadb = config.getConfigurationSection("storage.mariadb")
        val mariadbConfig = if (mariadb != null) {
            MariaDbConfig(
                host = mariadb.getString("host", "localhost") ?: "localhost",
                port = mariadb.getInt("port", 3306),
                database = mariadb.getString("database", "enthusiavotes") ?: "enthusiavotes",
                user = mariadb.getString("user", "enthusia") ?: "enthusia",
                password = mariadb.getString("password", "changeme") ?: "changeme",
            )
        } else {
            MariaDbConfig()
        }
        return StorageConfig(
            backend = backend,
            file = file,
            mariadb = mariadbConfig,
        )
    }

    private fun createDatabaseFactory(storageConfig: StorageConfig): DatabaseFactory {
        return when (storageConfig.backend.lowercase()) {
            "mariadb" -> RemoteDatabaseFactory(storageConfig.mariadb)
            else -> LocalDatabaseFactory(dataFolder, storageConfig.file)
        }
    }
}
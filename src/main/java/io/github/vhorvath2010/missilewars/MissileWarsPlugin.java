package io.github.vhorvath2010.missilewars;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.vhorvath2010.missilewars.arenas.Arena;
import io.github.vhorvath2010.missilewars.arenas.ArenaManager;
import io.github.vhorvath2010.missilewars.commands.MissileWarsCommand;
import io.github.vhorvath2010.missilewars.commands.SpectateCommand;
import io.github.vhorvath2010.missilewars.commands.VoteMapCommand;
import io.github.vhorvath2010.missilewars.decks.DeckManager;
import io.github.vhorvath2010.missilewars.events.ArenaGameruleEvents;
import io.github.vhorvath2010.missilewars.events.ArenaInventoryEvents;
import io.github.vhorvath2010.missilewars.events.ArenaLeaveEvents;
import io.github.vhorvath2010.missilewars.events.MapVotingEvents;
import io.github.vhorvath2010.missilewars.events.StructureItemEvents;
import io.github.vhorvath2010.missilewars.events.WorldCreationEvents;
import io.github.vhorvath2010.missilewars.utilities.MissileWarsPlaceholder;
import io.github.vhorvath2010.missilewars.utilities.SQLManager;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

/** Base class for the Missile Wars plugin */
public final class MissileWarsPlugin extends JavaPlugin {

    /** Singleton instance of this class. */
    private static MissileWarsPlugin plugin;
    /** The loaded ArenaManager for the plugin. */
    private ArenaManager arenaManager;
    /** The loaded DeckManager for this plugin. */
    private DeckManager deckManager;
    /** The loaded economy for this plugin */
    private static Economy econ;
    /** The loaded chat API for this plugin */
    private static Chat chat;
    /** The loaded sql manager for this plugin */
    private SQLManager sqlManager;

    @Override
    public void onEnable() {
        // Load instance
        plugin = this;

        // Register serializable data
        ConfigurationSerialization.registerClass(Arena.class);
        
        // Save data files
        log("Loading and saving config files...");
        saveDefaultConfig();
        saveIfNotPresent("messages.yml");
        saveIfNotPresent("sounds.yml");
        saveIfNotPresent("items.yml");
        saveIfNotPresent("maps.yml");
        saveIfNotPresent("ranks.yml");
        log("Loaded all config files.");

        // Load commands and events
        log("Loading commands and events...");
        getCommand("MissileWars").setExecutor(new MissileWarsCommand());
        getCommand("Spectate").setExecutor(new SpectateCommand());
        getCommand("VoteMap").setExecutor(new VoteMapCommand());
        Bukkit.getPluginManager().registerEvents(new ArenaGameruleEvents(), this);
        Bukkit.getPluginManager().registerEvents(new ArenaInventoryEvents(), this);
        Bukkit.getPluginManager().registerEvents(new ArenaLeaveEvents(), this);
        Bukkit.getPluginManager().registerEvents(new StructureItemEvents(), this);
        Bukkit.getPluginManager().registerEvents(new MapVotingEvents(), this);
        Bukkit.getPluginManager().registerEvents(new WorldCreationEvents(), this);
        log("Commands and events loaded.");

        // Load decks
        log("Creating and loading deck items...");
        deckManager = new DeckManager();
        log("All deck items locked and loaded.");

        // Load arenas
        log("Loading up arenas...");
        arenaManager = new ArenaManager(this);
        arenaManager.loadArenas();
        log("All arenas ready to go.");

        // Load placeholders
        log("Hooking into PlaceholderAPI...");
        new MissileWarsPlaceholder().register();
        log("All placeholders registered.");
        
        // Load economy
        log("Hooking into Vault...");
        setupVault();
        log("Economy setup complete.");
        
        // Load MySQL
        log("Setting up the MySQL database...");
        setupDatabase();
        log("MySQL setup complete.");
        
        log("Missile Wars is ready to play :)");
    }

    /**
     * Setup the economy manager
     */
    private void setupVault() {
        RegisteredServiceProvider<Economy> rspE = getServer().getServicesManager().getRegistration(Economy.class);
        econ = rspE.getProvider();
        RegisteredServiceProvider<Chat> rspC = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rspC.getProvider();
    }
    
    /**
     * Setup the server database
     */
    private void setupDatabase() {
        sqlManager = new SQLManager(this);
        log("Testing MySQL connection...");
        if (!sqlManager.testConnection()) {
            log("The plugin cannot function without the database. Shutting down now...");
            this.getPluginLoader().disablePlugin(this);
        }
        log("MySQL connection test complete.");
        
        log("Setting up default tables...");
        sqlManager.setupTables();
    }

    /**
     * Save a resource if it is not present
     *
     * @param resourcePath the path to the resource
     */
    private void saveIfNotPresent(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    @Override
    public void onDisable() {
        // Save arenas to data file
        log("Saving arenas to file...");
        arenaManager.saveArenas();
        log("Arenas saved!");
        
        // Close database connection
        log("Closing MySQL connection...");
        sqlManager.onDisable();
        log("Connection closed. Have a nice day!");
    }

    /**
     * Get the instance of the plugin running.
     *
     * @return the instance of the plugin running
     */
    public static MissileWarsPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the plugin's current DeckManager.
     *
     * @return the plugin's current DeckManager
     */
    public DeckManager getDeckManager() {
        return deckManager;
    }

    /**
     * Gets the plugin's current ArenaManager.
     *
     * @return the plugin's current ArenaManager
     */
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    /**
     * Gets the plugin's current Economy
     * 
     * @return the plugin's current Economy
     */
    public Economy getEconomy() {
        return econ;
    }
    
    /**
     * Gets the vault chat API
     * 
     * @return the vault chat API
     */
    public Chat getChat() {
        return chat;
    }
    
    /**
     * Gets the plugin's current database manager
     * 
     * @return the plugin's database manager
     */
    public SQLManager getSQL() {
        return sqlManager;
    }
    
    /**
     * Send a logging message to the console
     * 
     * @param message
     */
    private void log(String message) {
        Bukkit.getLogger().log(Level.INFO, "[MissileWars] " + message);
    }
}

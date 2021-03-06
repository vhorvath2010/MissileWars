package io.github.vhorvath2010.missilewars.arenas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import io.github.vhorvath2010.missilewars.MissileWarsPlugin;
import io.github.vhorvath2010.missilewars.events.ArenaInventoryEvents;
import io.github.vhorvath2010.missilewars.schematics.SchematicManager;
import io.github.vhorvath2010.missilewars.schematics.VoidChunkGenerator;
import io.github.vhorvath2010.missilewars.teams.MissileWarsPlayer;
import io.github.vhorvath2010.missilewars.utilities.ConfigUtils;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CommandTrait;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SheepTrait;
import net.citizensnpcs.trait.VillagerProfession;

/** Class to manager all Missile Wars arenas. */
public class ArenaManager {

    private MissileWarsPlugin plugin;
    
    /** A list of all loaded arenas. */
    private List<Arena> loadedArenas;

    /** Default constructor */
    public ArenaManager(MissileWarsPlugin plugin) {
        this.plugin = plugin;
        loadedArenas = new ArrayList<>();
    }

    /** Load arenas from data file */
    public void loadArenas() {
        File arenaFile = new File(plugin.getDataFolder(), "arenas.yml");

        // Acquire arenas from data file
        if (arenaFile.exists()) {
            FileConfiguration arenaConfig = new YamlConfiguration();
            try {
                arenaConfig.load(arenaFile);
                if (arenaConfig.contains("arenas")) {
                    loadedArenas = (List<Arena>) arenaConfig.get("arenas");
                }
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        // Load worlds for arenas
        assert loadedArenas != null;
        for (Arena arena : loadedArenas) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Loading arena: " + arena.getName() + "...");
            arena.loadWorldFromDisk();
        }
    }

    /** Clean up and save arenas on server shutdown */
    public void saveArenas() {
        // Remove all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(ConfigUtils.getSpawnLocation());
            player.setGameMode(GameMode.ADVENTURE);
        }
        
        // Unload each Arena
        for (Arena arena : loadedArenas) {
            Bukkit.unloadWorld(arena.getWorld(), false);
        }

        // Save Arenas to file
        File arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        FileConfiguration arenaConfig = new YamlConfiguration();
        arenaConfig.set("arenas", loadedArenas);
        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get an Arena by name.
     *
     * @param name the name of the Arena
     * @return the Arena, or null if it doesn't exist
     */
    public Arena getArena(String name) {
        for (Arena arena : loadedArenas) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        return null;
    }
    
    /**
     * Completely remove an Arena by name.
     *
     * @param name the name of the Arena
     * @return true if the arena was successfully deleted
     */
    public Boolean removeArena(Arena arena) {
        World arenaWorld = arena.getWorld();
        Logger logger = Bukkit.getLogger();
        if (!arenaWorld.getPlayers().isEmpty()) {
            logger.log(Level.WARNING, "An arena with players in it cannot be deleted");
            return false;
        }
        // Reload citizens to load chunks
        try {
            logger.log(Level.INFO, "Reloading citizens to properly remove them...");
            ((Citizens) CitizensAPI.getPlugin()).reload();
        } catch (NPCLoadException e) {
            logger.log(Level.INFO, "Reloading citizens failed somehow :(");
        }
        for (Entity entity : arenaWorld.getEntities()) {
            if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
                logger.log(Level.INFO, "Citizen with UUID " + entity.getUniqueId() + " deleted.");
                CitizensAPI.getNPCRegistry().getNPC(entity).destroy();
            }
        }
        CitizensAPI.getNPCRegistry().saveToStore();
        Bukkit.unloadWorld(arenaWorld, false);
        File worldFolder = new File("mwarena_" + arena.getName());
        try {
            FileUtils.deleteDirectory(worldFolder);
        } catch (IOException e) {
            logger.log(Level.WARNING, "The world file couldn't be removed! Please remove manually.");
        }
        loadedArenas.remove(arena);
        // Remove arena from file
        File arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        FileConfiguration arenaConfig = new YamlConfiguration();
        arenaConfig.set("arenas", loadedArenas);
        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Arena file couldn't be saved!");
        }
        return true;
    }
    
    /**
     * Deletes and re-creates all arenas to implement new settings
     */
    public void performArenaUpgrade() {
        Logger logger = Bukkit.getLogger();
        // If players are in arenas, don't do it
        if (Bukkit.getOnlinePlayers().size() != Bukkit.getWorld("world").getPlayerCount()) {
            logger.log(Level.WARNING, "Some players are in arenas!");
            return;
        }
        logger.log(Level.INFO, "Performing arena upgrades. This might take a while!");
        for (Arena arena : new ArrayList<Arena>(getLoadedArenas())) {
            String name = arena.getName();
            int capacity = arena.getCapacity();
            removeArena(arena);
            createArena(name, capacity);
        }
    }


    /**
     * Get an Arena by index.
     *
     * @param index the index of the Arena
     * @return the Arena, or null if it doesn't exist
     */
    public Arena getArena(int index) {
        if (index < 0 || index >= loadedArenas.size()) {
            return null;
        }
        return loadedArenas.get(index);
    }

    /**
     * Get the Arena that a player with a given UUID is in.
     *
     * @param id the UUID of the player
     * @return the Arena that the player is in, or null
     */
    public Arena getArena(UUID id) {
        for (Arena arena : loadedArenas) {
            if (arena.isInArena(id)) {
                return arena;
            }
        }
        return null;
    }

    /**
     * Create the waiting lobby region for a specific team.
     *
     * @param team the team to create the waiting lobby for
     * @param arena the arena to create the lobby for
     * @param parent the general lobby area
     */
    private void createWaitingLobby(String team, Arena arena, ProtectedRegion parent) {
        // Setup region
        FileConfiguration schematicConfig = ConfigUtils.getConfigFile(plugin.getDataFolder()
                .toString(), "maps.yml");
        WorldGuard wg = WorldGuard.getInstance();
        Vector minLobby = SchematicManager.getVector(schematicConfig, "lobby." + team + "-lobby-region.min", null, null);
        Vector maxLobby = SchematicManager.getVector(schematicConfig, "lobby." + team + "-lobby-region.max", null, null);
        ProtectedRegion lobbyRegion = new ProtectedCuboidRegion(arena.getName() + "-" + team + "-lobby",
                BlockVector3.at(minLobby.getX(), minLobby.getY(), minLobby.getZ()), BlockVector3.at(maxLobby.getX(),
                maxLobby.getY(), maxLobby.getZ()));

        // Adds flags
        Set<String> enterCommands = new HashSet<>();
        enterCommands.add("/kit " + team + "waitinglobby %username%");
        lobbyRegion.setFlag(net.goldtreeservers.worldguardextraflags.flags.Flags.CONSOLE_COMMAND_ON_ENTRY, enterCommands);
        Set<PotionEffect> effects = new HashSet<>();
        effects.add(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 99999999, 5));
        effects.add(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 99999999, 5));
        lobbyRegion.setFlag(net.goldtreeservers.worldguardextraflags.flags.Flags.GIVE_EFFECTS, effects);
        lobbyRegion.setFlag(Flags.INVINCIBILITY, StateFlag.State.DENY);
        lobbyRegion.setFlag(Flags.PVP, StateFlag.State.ALLOW);
        Set<String> leaveCommands = new HashSet<>();
        leaveCommands.add("/umw clear %username%");
        lobbyRegion.setFlag(net.goldtreeservers.worldguardextraflags.flags.Flags.CONSOLE_COMMAND_ON_EXIT, leaveCommands);
        try {
            lobbyRegion.setParent(parent);
        } catch (ProtectedRegion.CircularInheritanceException e) {
            e.printStackTrace();
        }
        wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(arena.getWorld())).addRegion(lobbyRegion);
    }

    /**
     * Create a new Arena given a name with default player capacity.
     *
     * @param name the name of the Arena
     * @param creator the creator of the world
     * @return true if the Arena was created, otherwise false
     */
    public boolean createArena(String name, int capacity) {
        
    	Logger logger = Bukkit.getLogger();
    	
        // Ensure arena world doesn't exist
        if (Bukkit.getWorld("mwarena_" + name) != null) {
            logger.log(Level.WARNING, "An arena with the name " + name + " already exists!");
            return false;
        }

        FileConfiguration schematicConfig = ConfigUtils.getConfigFile(plugin.getDataFolder()
                .toString(), "maps.yml");

        // Create Arena world
        logger.log(Level.INFO, "Generating arena world for " + name);
        WorldCreator arenaCreator = new WorldCreator("mwarena_" + name);
        arenaCreator.generator(new VoidChunkGenerator());
        World arenaWorld = arenaCreator.createWorld();
        assert arenaWorld != null;
        arenaWorld.setAutoSave(false);
        arenaWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        arenaWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        arenaWorld.setGameRule(GameRule.DO_TILE_DROPS, false);
        arenaWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        arenaWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
        arenaWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        arenaWorld.setGameRule(GameRule.NATURAL_REGENERATION, false);
        arenaWorld.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        arenaWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        arenaWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 20);
        WorldBorder border = arenaWorld.getWorldBorder();
        border.setCenter(plugin.getConfig().getInt("worldborder.center.x"), 
                plugin.getConfig().getInt("worldborder.center.z"));
        border.setSize(plugin.getConfig().getInt("worldborder.radius") * 2);
        arenaWorld.setTime(6000);
        logger.log(Level.INFO, "Arena world generated!");

        // Create Arena lobby
        logger.log(Level.INFO, "Generating lobby...");
        if (!SchematicManager.spawnFAWESchematic("lobby", arenaWorld, true, null)) {
            logger.log(Level.SEVERE, "Couldn't generate lobby! Schematic files present?");
            return false;
        } else {
            logger.log(Level.INFO, "Lobby generated!");
        }

        
        // Spawn red NPC
        Vector redVec = SchematicManager.getVector(schematicConfig, "lobby.npc-pos.red", null, null);
        Location redLoc = new Location(arenaWorld, redVec.getX(), redVec.getY(), redVec.getZ());
        redLoc.setYaw(90);
        NPC redNPC = CitizensAPI.getNPCRegistry().createNPC(EntityType.SHEEP,
                ChatColor.RED + "" + ChatColor.BOLD + "Red Team");
        CommandTrait enqueueRed = new CommandTrait();
        enqueueRed.addCommand(new CommandTrait.NPCCommandBuilder("umw enqueuered %player%",
                CommandTrait.Hand.BOTH));
        redNPC.addTrait(enqueueRed);
        SheepTrait redSheepTrait = redNPC.getOrAddTrait(SheepTrait.class);
        redSheepTrait.setColor(DyeColor.RED);
        redLoc.getWorld().loadChunk(redLoc.getChunk());
        redNPC.spawn(redLoc);
        redNPC.data().setPersistent(NPC.SILENT_METADATA, true);
        logger.log(Level.INFO, "Red NPC with UUID " + redNPC.getUniqueId() + " spawned.");

        // Spawn blue NPC
        Vector blueVec = SchematicManager.getVector(schematicConfig, "lobby.npc-pos.blue", null, null);
        Location blueLoc = new Location(arenaWorld, blueVec.getX(), blueVec.getY(), blueVec.getZ());
        blueLoc.setYaw(90);
        NPC blueNPC = CitizensAPI.getNPCRegistry().createNPC(EntityType.SHEEP,
                ChatColor.BLUE + "" + ChatColor.BOLD + "Blue Team");
        CommandTrait enqueueBlue = new CommandTrait();
        enqueueBlue.addCommand(new CommandTrait.NPCCommandBuilder("umw enqueueblue %player%",
                CommandTrait.Hand.BOTH));
        blueNPC.addTrait(enqueueBlue);
        SheepTrait blueSheepTrait = blueNPC.getOrAddTrait(SheepTrait.class);
        blueSheepTrait.setColor(DyeColor.BLUE);
        blueLoc.getWorld().loadChunk(blueLoc.getChunk());
        blueNPC.spawn(blueLoc);
        blueNPC.data().setPersistent(NPC.SILENT_METADATA, true);
        logger.log(Level.INFO, "Blue NPC with UUID " + blueNPC.getUniqueId() + " spawned.");

        // Spawn bar NPC
        Vector barVec = SchematicManager.getVector(schematicConfig, "lobby.npc-pos.bar", null, null);
        Location barLoc = new Location(arenaWorld, barVec.getX(), barVec.getY(), barVec.getZ());
        barLoc.setYaw(-90);
        NPC bartender = CitizensAPI.getNPCRegistry().createNPC(EntityType.VILLAGER,
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Bartender");
        // Add command
        CommandTrait openBar = new CommandTrait();
        openBar.addCommand(new CommandTrait.NPCCommandBuilder("bossshop open bar %player%",
                CommandTrait.Hand.BOTH));
        bartender.addTrait(openBar);
        // Make him look at players
        LookClose lookPlayerTrait = bartender.getOrAddTrait(LookClose.class);
        lookPlayerTrait.lookClose(true);
        lookPlayerTrait.setRange(10);
        // Setup Villager Profession
        VillagerProfession profession = bartender.getOrAddTrait(VillagerProfession.class);
        barLoc.getWorld().loadChunk(barLoc.getChunk());
        bartender.spawn(barLoc);
        profession.setProfession(Villager.Profession.NITWIT);
        bartender.data().setPersistent(NPC.SILENT_METADATA, true);
        logger.log(Level.INFO, "Bartender NPC with UUID " + bartender.getUniqueId() + " spawned.");
        
        CitizensAPI.getNPCRegistry().saveToStore();

        // Spawn barrier wall
        FileConfiguration settings = plugin.getConfig();
        int length = settings.getInt("barrier.length");
        int x = settings.getInt("barrier.center.x");
        int zCenter = settings.getInt("barrier.center.z");
        for (int y = 0; y <= 256; ++y) {
            for (int z = zCenter - length / 2; z < zCenter + length / 2; z++) {
                arenaWorld.getBlockAt(x, y, z).setType(Material.BARRIER);
            }
        }

        // Register arena
        Arena arena = new Arena(name, capacity);
        loadedArenas.add(arena);

        // Setup regions
        WorldGuard wg = WorldGuard.getInstance();
        Vector minLobby = SchematicManager.getVector(schematicConfig, "lobby.main-region.min", null, null);
        Vector maxLobby = SchematicManager.getVector(schematicConfig, "lobby.main-region.max", null, null);
        ProtectedRegion lobbyRegion = new ProtectedCuboidRegion(name + "-lobby", BlockVector3.at(minLobby.getX(),
                minLobby.getY(), minLobby.getZ()), BlockVector3.at(maxLobby.getX(), maxLobby.getY(), maxLobby.getZ()));
        lobbyRegion.setFlag(Flags.INVINCIBILITY, StateFlag.State.ALLOW);
        lobbyRegion.setFlag(Flags.PVP, StateFlag.State.DENY);
        lobbyRegion.setFlag(Flags.TNT, StateFlag.State.DENY);
        lobbyRegion.setFlag(Flags.HUNGER_DRAIN, StateFlag.State.DENY);
        lobbyRegion.setFlag(Flags.ITEM_DROP, StateFlag.State.DENY);
        lobbyRegion.setFlag(Flags.DENY_MESSAGE, "");
        wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(arenaWorld)).addRegion(lobbyRegion);
        createWaitingLobby("red", arena, lobbyRegion);
        createWaitingLobby("blue", arena, lobbyRegion);

        logger.log(Level.INFO, "Arena " + name + " generated. World will save in 10 seconds.");
        
        // Wait to ensure schematic is spawned
        new BukkitRunnable() {
            @Override
            public void run() {
                arenaWorld.save();
                logger.log(Level.INFO, "Saving new arena " + name);
                logger.log(Level.INFO, "Arena " + name + " locked and loaded.");
            }
        }.runTaskLater(plugin, 200);

        return true;
    }

    /**
     * Open the arena selector for a given player.
     *
     * @param player the player
     */
    public void openArenaSelector(Player player) {
        // Stop if player is already in an arena
        if (getArena(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a game!");
            return;
        }
        Inventory selector = Bukkit.createInventory(null, 27,
                ConfigUtils.getConfigText("inventories.game-selector.title", player, null, null));

        // Add Arena items
        for (Arena arena : getLoadedArenas()) {
            ItemStack arenaItem = new ItemStack(Material.TNT, Math.max(1, arena.getNumPlayers()));
            ItemMeta arenaItemMeta = arenaItem.getItemMeta();
            assert arenaItemMeta != null;
            arenaItemMeta.setDisplayName(ConfigUtils.getConfigText("inventories.game-selector.game-item.name",
                    player, arena,null));
            arenaItemMeta.setLore(ConfigUtils.getConfigTextList("inventories.game-selector.game-item.lore",
                    player, arena, null));
            arenaItem.setItemMeta(arenaItemMeta);
            selector.addItem(arenaItem);
        }
        ArenaInventoryEvents.selectingArena.add(player);
        player.openInventory(selector);
    }

    /**
     * Get the {@link MissileWarsPlayer} representing the player with the given UUID
     *
     * @param id the player's UUID
     * @return the {@link MissileWarsPlayer} representing the player with the given UUID if it exists
     */
    public MissileWarsPlayer getPlayer(UUID id) {
        Arena arena = getArena(id);
        if (arena != null) {
            return arena.getPlayerInArena(id);
        }
        return null;
    }
    
    /**
     * Gets a list of the loaded arenas, sorted by highest capacity
     * 
     * @return The list of loaded arenas
     */
    public List<Arena> getLoadedArenas() {
        List<Arena> sortedArenas = loadedArenas;
        sortedArenas.sort(Collections.reverseOrder(Arena.byCapacity));
        return sortedArenas;
    }

}

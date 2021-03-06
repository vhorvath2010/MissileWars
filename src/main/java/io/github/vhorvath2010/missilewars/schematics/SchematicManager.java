package io.github.vhorvath2010.missilewars.schematics;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.Vector3;
import io.github.vhorvath2010.missilewars.MissileWarsPlugin;
import io.github.vhorvath2010.missilewars.utilities.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

/** A class to handle loading/placing of NBT and WorldEdit schematics */
public class SchematicManager {

    /**
     * Get the vector for a given structure/schematic path in a given config.
     *
     * @param config the config to get data from
     * @param path the path to the x, y, z vector data
     * @return the x, y, z as a vector
     */
    public static Vector getVector(FileConfiguration config, String path, String mapType, String mapName) {
        Vector vector = new Vector();
        if (config.contains(path)) {
            vector = new Vector(config.getDouble(path + ".x"), config.getDouble(path + ".y"),
                    config.getDouble(path + ".z"));
        } else {
            // If config does not contain path, then it's definitely a map
            vector = new Vector(ConfigUtils.getMapNumber(mapType, mapName, path + ".x"),
                                ConfigUtils.getMapNumber(mapType, mapName, path + ".y"),
                                ConfigUtils.getMapNumber(mapType, mapName, path + ".z"));
        }
        return vector;
    }

    /**
     * Spawn a structure at a given location with a given rotation.
     *
     * @param structureName the name of the structure
     * @param loc the location to spawn the structure (pre-offset)
     * @param redMissile if the NBT structure is a red missile
     * @param mapName The name of the Arena map the NBT structure is being spawned in
     * @return true if the NBT structure was found and spawned, otherwise false
     */
    public static boolean spawnNBTStructure(String structureName, Location loc, boolean redMissile, String mapName) {
        
        // Don't kill the lobby
        if (loc.getWorld().getName().equals("world")){
            return false;
        }
        
        // Attempt to get structure file
        MissileWarsPlugin plugin = MissileWarsPlugin.getPlugin();
        FileConfiguration structureConfig = ConfigUtils.getConfigFile(MissileWarsPlugin.getPlugin().getDataFolder().toString(),
                "items.yml");

        // Attempt to get structure file
        if (!structureConfig.contains(structureName + ".file")) {
            return false;
        }
        String fileName = structureConfig.getString(structureName + ".file");
        if (fileName == null) {
            return false;
        }
        if (redMissile) {
            fileName = fileName.replaceAll(".nbt", "_red.nbt");
        }
        File structureFile = new File(plugin.getDataFolder() + File.separator + "structures",
                fileName);

        // Load structure data
        StructureManager manager = Bukkit.getStructureManager();
        Structure structure;
        try {
            structure = manager.loadStructure(structureFile);
        } catch (IOException e) {
            return false;
        }

        int bluespawnz = (int) Math.floor(ConfigUtils.getMapNumber("classic", mapName, "blue-spawn.z"));
        int redspawnz = (int) Math.floor(ConfigUtils.getMapNumber("classic", mapName, "red-spawn.z"));

        // Apply offset
        Location spawnLoc = loc.clone();
        
        // Cancel if attempt to grief team spawnpoint
        if (spawnLoc.getBlockZ() == bluespawnz || spawnLoc.getBlockZ() == redspawnz) {
            return false;
        }
        
        Vector offset = getVector(structureConfig, structureName + ".offset", null, null);
        // Flip z if on red team
        StructureRotation rotation = StructureRotation.NONE;
        // Temp hotfix for structure rail rotation bug
        if (redMissile && structureName.contains("thunderbolt")) {
            offset.setZ(-15);
            offset.setX(0);
        } 
        // Normal red missile offset adjustment 
        else if (redMissile) {
            offset.setZ(offset.getZ() * -1);
            offset.setX(offset.getX() * -1);
            rotation = StructureRotation.CLOCKWISE_180;
        }
        spawnLoc = spawnLoc.add(offset);        
        
        // Time to perform no place checks
        int spawnx = spawnLoc.getBlockX();
        int spawny = spawnLoc.getBlockY();
        int spawnz = spawnLoc.getBlockZ();
        
        int sizex = structure.getSize().getBlockX();
        int sizey = structure.getSize().getBlockY();
        int sizez = structure.getSize().getBlockZ();
        
        int barrierx = plugin.getConfig().getInt("barrier.center.x");
        
        int portalx1 = (int) ConfigUtils.getMapNumber("classic", mapName, "portal.x1") - 1;
        int portalx2 = (int) ConfigUtils.getMapNumber("classic", mapName, "portal.x4") + 1;
        int portaly1 = (int) ConfigUtils.getMapNumber("classic", mapName, "portal.y1") - 1;
        int portaly2 = (int) ConfigUtils.getMapNumber("classic", mapName, "portal.y4") + 1;
        
        int portalredz = (int) ConfigUtils.getMapNumber("classic", mapName, "portal.red-z");
        int portalbluez = (int) ConfigUtils.getMapNumber("classic", mapName, "portal.blue-z");
        
        
        // Do not place if hitbox would intersect with barrier
        if (!redMissile && spawnx + sizex > barrierx) {
            return false;
        } else if (redMissile && spawnx >= barrierx) {
            return false;
        }
        
        // Do not place if hitbox would intersect with a portal
        if (!redMissile &&
                ((spawnz <= portalredz && spawnz + sizez > portalredz) ||
                (spawnz <= portalbluez && spawnz + sizez > portalbluez)) && 
                spawnx <= portalx2 && spawnx + sizex > portalx1 &&
                spawny <= portaly2 && spawny + sizey > portaly1) {
            return false;
        } else if (redMissile &&
                ((spawnz >= portalbluez && spawnz - sizez < portalbluez) || 
                (spawnz >= portalredz && spawnz - sizez < portalredz)) &&
                spawnx >= portalx1 && spawnx - sizex < portalx2 &&
                spawny <= portaly2 && spawny + sizey > portaly1) {
            return false;
        }

        //Place structure
        structure.place(spawnLoc, true, rotation, Mirror.NONE, 0, 1, new Random());
        return true;
    }

    /**
     * Spawn a WorldEdit schematic in a given world. The "maps.yml" file should have data on the spawn location and
     * schematic file.
     *
     * @param schematicName the name of the schematic in the maps.yml file
     * @param world the world to spawn the schematic in
     * @param async to run async
     * @return true if the schematic was generated successfully, otherwise false
     */
    public static boolean spawnFAWESchematic(String schematicName, World world, Boolean async, String mapType) {
        // Find schematic data from file
        MissileWarsPlugin plugin = MissileWarsPlugin.getPlugin();
        FileConfiguration schematicConfig = ConfigUtils.getConfigFile(MissileWarsPlugin.getPlugin().getDataFolder()
                .toString(), "maps.yml");

        // Acquire WE clipboard
        String possibleMapType = mapType == null ? "" : mapType + ".";
        if (!schematicConfig.contains(possibleMapType + schematicName + ".file")) {
            System.out.println("No schem file found!");
            return false;
        }
        File schematicFile = new File(plugin.getDataFolder() + File.separator + "maps",
                schematicConfig.getString(possibleMapType + schematicName + ".file"));
        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        try {
            ClipboardReader reader = format.getReader(new FileInputStream(schematicFile));
            clipboard = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Paste WE clipboard
        Vector spawnPos;

        if (mapType == null) {
            spawnPos = getVector(schematicConfig, schematicName + ".pos", null, null);
        } else {
            spawnPos = getVector(schematicConfig, "pos", mapType, schematicName);
        }

        if (async) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    clipboard.paste(BukkitAdapter.adapt(world),
                            Vector3.toBlockPoint(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()));
                }
            }.runTaskAsynchronously(MissileWarsPlugin.getPlugin());
        } else {
            clipboard.paste(BukkitAdapter.adapt(world),
                    Vector3.toBlockPoint(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()));
        }
        return true;
    }

}
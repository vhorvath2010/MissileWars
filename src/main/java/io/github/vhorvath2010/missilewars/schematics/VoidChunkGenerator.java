package io.github.vhorvath2010.missilewars.schematics;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.Vector;

import io.github.vhorvath2010.missilewars.MissileWarsPlugin;
import io.github.vhorvath2010.missilewars.utilities.ConfigUtils;

public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunkData) { }

    @Override
    public void generateBedrock(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunkData) { }

    @Override
    public void generateCaves(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunkData) { }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunkData) { }
    
    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        FileConfiguration schematicConfig = ConfigUtils.getConfigFile(MissileWarsPlugin.getPlugin().getDataFolder()
                .toString(), "maps.yml");
        Vector spawnVec = SchematicManager.getVector(schematicConfig, "lobby.spawn", null, null);
        Location spawnLoc = new Location(world, spawnVec.getX(), spawnVec.getY(), spawnVec.getZ());
        return spawnLoc;
    }
}

package ru.mgc.createengineers.assembly;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionTypes;
import ru.mgc.createengineers.CreateEngineers;
import ru.mgc.createengineers.CreateEngineersNetworking;
import ru.mgc.createengineers.entity.AssemblyEntity;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AssemblyDimensionManager {
    private static final HashMap<String, RuntimeWorldHandle> loadedWorlds = new HashMap<>();
    private static final HashMap<String, AssemblyPersistentState> loadedDataManagers = new HashMap<>();

    public static void initialize() {
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof AssemblyEntity)) return;

            unloadWorld(((AssemblyEntity) entity).getAssemblyID());
        });

        // Load entity
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (!(entity instanceof AssemblyEntity)) return;

            if (!isWorldLoaded(((AssemblyEntity) entity).getAssemblyID())) {
                loadWorld(((AssemblyEntity) entity).getAssemblyID());
            }

            onStartTracking((AssemblyEntity) entity, player);
        });
    }

    private static void onStartTracking(AssemblyEntity entity, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        AssemblyDimensionManager.putWorld(buf, (entity).getAssemblyID());
        ServerPlayNetworking.send(player, CreateEngineersNetworking.AssemblyWorldS2CPacket, buf);
    }

    public static AssemblyPersistentState getAssemblyDataManager(String assemblyId) {
        return loadedDataManagers.get(assemblyId);
    }

    public static void tickWorld(String assemblyId) {
        if (!isWorldLoaded(assemblyId)) return;

        ServerWorld world = getWorld(assemblyId).asWorld();
        world.tick(() -> false);
    }

    public static boolean isWorldLoaded(String assemblyId) {
        return loadedWorlds.containsKey(assemblyId);
    }

    public static void putWorld(PacketByteBuf buf, String assemblyId) {
        List<WorldChunk> chunks = new ArrayList<>();
        List<ChunkPos> chunkPositions = AssemblyDimensionManager.getAssemblyDataManager(assemblyId).getChunksPositions();
        for (ChunkPos chunkPos : chunkPositions) {
            chunks.add(AssemblyDimensionManager.getWorld(assemblyId).asWorld().getChunk(chunkPos.x, chunkPos.z));
        }

        buf.writeString(assemblyId); // Assembly ID;
        buf.writeInt(chunks.size()); // Chunks amount;

        // Write every chunk
        for (WorldChunk chunk : chunks) {
            buf.writeInt(chunk.getPos().x);
            buf.writeInt(chunk.getPos().z);
            ChunkData data = new ChunkData(chunk);
            data.write(buf);
        }
    }

    /*
    Returns world handle
     */
    public static RuntimeWorldHandle getWorld(String id) {
        if (!CreateEngineers.SERVER.isOnThread()) {
            try {
                throw new Exception("WRONG THREAD WORKING WITH WORLDS!!!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (loadedWorlds.containsKey(id)) {
            return loadedWorlds.get(id);
        } else {
            return loadWorld(id);
        }
    }

    public static RuntimeWorldHandle loadWorld(String assemblyId) {
        CreateEngineers.LOGGER.info("Loading assembly world \"{}\"", assemblyId);
        RuntimeWorldHandle handle = getFantasy().getOrOpenPersistentWorld(new Identifier(CreateEngineers.MOD_ID, assemblyId), getFantasyConfig());
        loadedWorlds.put(assemblyId, handle);
        ServerWorld world = handle.asWorld();
        loadedDataManagers.put(assemblyId, world.getPersistentStateManager().getOrCreate(AssemblyPersistentState::createFromNbt, AssemblyPersistentState::new, "assemblies_data"));

        // Updating assembly world forced chunks
        for (ChunkPos pos : getAssemblyDataManager(assemblyId).getChunksPositions()) {
            world.setChunkForced(pos.x, pos.z, true);
        }

        return handle;
    }

    /*
    Unloads world from runtime
     */
    private static void unloadWorld(String id) {
        CreateEngineers.LOGGER.info("Unloading assembly world \"{}\"", id);
        getWorld(id).unload();
        loadedWorlds.remove(id);
        loadedDataManagers.remove(id);
    }

    /*
    Deletes world from files
     */
    public static void deleteWorld(String id) {
        if (!CreateEngineers.SERVER.isOnThread()) {
            try {
                throw new Exception("WRONG THREAD WORKING WITH WORLDS!!!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        CreateEngineers.LOGGER.info("Deleting server side world of assembly \"{}\"...", id);
        RuntimeWorldHandle world = getWorld(id);
        world.delete();
        loadedWorlds.remove(id);
        loadedDataManagers.remove(id);
    }

    private static RuntimeWorldConfig getFantasyConfig() {
        return new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.PEACEFUL)
                .setGameRule(GameRules.DO_DAYLIGHT_CYCLE, false)
                .setGameRule(GameRules.DO_WEATHER_CYCLE, false)
                .setGameRule(GameRules.DO_MOB_SPAWNING, false)
                .setGenerator(new VoidChunkGenerator(
                        CreateEngineers.SERVER.getRegistryManager().get(RegistryKeys.BIOME).getEntry(0).get()))
                .setSeed(1234L);
    }

    private static Fantasy getFantasy() {
        return Fantasy.get(CreateEngineers.SERVER);
    }
}

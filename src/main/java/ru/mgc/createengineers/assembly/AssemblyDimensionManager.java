package ru.mgc.createengineers.assembly;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.registry.RegistryKeys;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AssemblyDimensionManager {
    private static final HashMap<String, RuntimeWorldHandle> loadedWorlds = new HashMap<>();
    private static Fantasy fantasy;

    public static void initialize() {
        /*
        Entity unload from world
         */
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof AssemblyEntity)) return;
            if (entity.isRemoved()) return;

            unloadWorld(((AssemblyEntity) entity).getAssemblyID());
        });

        /*
        Server start
         */
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            fantasy = Fantasy.get(server);
        });

        /*
        Server stop
         */
        ServerLifecycleEvents.SERVER_STOPPING.register((listener) -> {
            CreateEngineers.LOGGER.info("Unloading all server assembly worlds");
            loadedWorlds.clear();
        });

        /*
        Entity load by player
         */
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (!(entity instanceof AssemblyEntity)) return;

            if (!isLoaded(((AssemblyEntity) entity).getAssemblyID())) {
                loadWorld(((AssemblyEntity) entity).getAssemblyID());
            }

            PacketByteBuf buf = PacketByteBufs.create();
            AssemblyDimensionManager.putWorld(buf, ((AssemblyEntity) entity).getAssemblyID());
            ServerPlayNetworking.send(player, CreateEngineersNetworking.AssemblyWorldS2CPacket, buf);
        });
    }

    /*
    Tick world attached to entity
     */
    public static void tickAssemblyEntity(AssemblyEntity entity) {
        String assemblyId = entity.getAssemblyID();
        if (!isLoaded(assemblyId)) return;

        ServerWorld world = getWorld(assemblyId).asWorld();
        world.setTimeOfDay(entity.getWorld().getTimeOfDay());
        world.tick(() -> false);
    }

    /*
    Get assembly's world persistent state (datastore)
     */
    public static AssemblyPersistentState getPersistentState(RuntimeWorldHandle handle) {
        return handle.asWorld().getPersistentStateManager().getOrCreate(AssemblyPersistentState::createFromNbt, AssemblyPersistentState::new, "assemblies_data");
    }


    public static boolean isLoaded(String assemblyId) {
        return loadedWorlds.containsKey(assemblyId);
    }

    public static void putWorld(PacketByteBuf buf, String assemblyId) {
        RuntimeWorldHandle handle = AssemblyDimensionManager.getWorld(assemblyId);
        List<WorldChunk> chunks = new ArrayList<>();
        List<ChunkPos> chunkPositions = AssemblyDimensionManager.getPersistentState(handle).getChunksPositions();
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
    Returns world handle if loaded
     */
    @Nullable
    public static RuntimeWorldHandle getWorld(String id) {
        return loadedWorlds.get(id);
    }

    /*
    Loads world and forces chunks
     */
    public static RuntimeWorldHandle loadWorld(String assemblyId) {
        CreateEngineers.LOGGER.info("Loading assembly world \"{}\"", assemblyId);

        RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(new Identifier(CreateEngineers.MOD_ID, assemblyId), getFantasyConfig());
        AssemblyPersistentState state = getPersistentState(handle);
        ServerWorld world = handle.asWorld();

        handle.setTickWhenEmpty(false);
        loadedWorlds.put(assemblyId, handle);

        // Updating assembly world forced chunks
        for (ChunkPos pos : state.getChunksPositions()) {
            world.setChunkForced(pos.x, pos.z, true);
        }

        return handle;
    }

    /*
    Unloads world from runtime
     */
    private static void unloadWorld(String id) {
        if (!isLoaded(id)) return;

        CreateEngineers.LOGGER.info("Unloading assembly world \"{}\"", id);

        RuntimeWorldHandle handle = loadedWorlds.get(id);
        AssemblyPersistentState state = getPersistentState(handle);
        ServerWorld world = handle.asWorld();

        // No force for all chunks
        for (ChunkPos pos : state.getChunksPositions()) {
            world.setChunkForced(pos.x, pos.z, false);
        }
        
        loadedWorlds.remove(id);
        handle.unload();
    }

    /*
    Deletes world from files
     */
    public static void deleteWorld(String id) {
        if (!isLoaded(id)) return;

        CreateEngineers.LOGGER.info("Deleting server side world of assembly \"{}\"...", id);
        RuntimeWorldHandle handle = loadedWorlds.get(id);
        unloadWorld(id);
        handle.delete();
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
}

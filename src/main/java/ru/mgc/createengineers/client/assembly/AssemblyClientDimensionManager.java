package ru.mgc.createengineers.client.assembly;

import io.github.fabricators_of_create.porting_lib.event.client.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import ru.mgc.createengineers.CreateEngineers;
import ru.mgc.createengineers.CreateEngineersNetworking;
import ru.mgc.createengineers.client.CreateEngineersClient;
import ru.mgc.createengineers.entity.AssemblyEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AssemblyClientDimensionManager {
    private final HashMap<String, Set<Chunk>> worldChunks = new HashMap<>();
    private final HashMap<String, ClientWorld> assemblyWorlds = new HashMap<>();

    public AssemblyClientDimensionManager() {
        ClientPlayNetworking.registerGlobalReceiver(CreateEngineersNetworking.AssemblySpawnEntityS2CPacket, this::unpackAssemblyEntityPacket);
        ClientPlayNetworking.registerGlobalReceiver(CreateEngineersNetworking.AssemblyBlockUpdateS2CPacket, this::unpackBlockUpdatePacket);
        ClientPlayNetworking.registerGlobalReceiver(CreateEngineersNetworking.AssemblyWorldS2CPacket, this::unpackWorldPacket);

        ClientWorldEvents.UNLOAD.register((handler, client) -> {
            CreateEngineers.LOGGER.info("Cleaning assemblies..");
            assemblyWorlds.clear();
            worldChunks.clear();
        });
    }

    public Set<Chunk> getChunks(String id) {
        return worldChunks.get(id);
    }

    private void unpackAssemblyEntityPacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        UUID entityUUID = buf.readUuid();
        int entityId = buf.readVarInt();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String assemblyID = buf.readString();

        // Spawn entity
        client.execute(() -> {
            if (client.world != null) {
                AssemblyEntity entity = new AssemblyEntity(CreateEngineers.ASSEMBLY_ENTITY, client.world);
                entity.updateTrackedPosition(x, y, z);
                entity.setPosition(x, y, z);
                entity.setId(entityId);
                entity.setUuid(entityUUID);
                entity.setAssemblyID(assemblyID);
                client.world.addEntity(entityId, entity);
            }
        });
    }

    private void unpackBlockUpdatePacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String assemblyID = buf.readString();
        BlockPos blockPos = buf.readBlockPos();
        BlockState blockState = buf.readRegistryValue(Block.STATE_IDS);

        // Update block
        ClientWorld world = getWorld(assemblyID);
        if (world == null) return;

        world.setBlockState(blockPos, blockState, Block.NOTIFY_ALL, 512);
    }

    private void unpackWorldPacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String assemblyID = buf.readString();
        int chunksAmount = buf.readInt();

        CreateEngineers.LOGGER.info("Unpacking assembly world \"{}\"..", assemblyID);

        // Create world
        ClientWorld world = createWorld(assemblyID);
        Set<Chunk> chunks = worldChunks.containsKey(assemblyID) ? worldChunks.get(assemblyID)
                : new HashSet<>();
        // Unpack chunks
        for (int i = 0; i < chunksAmount; i++) {
            int chunkX = buf.readInt();
            int chunkZ = buf.readInt();
            ChunkData chunkData = new ChunkData(buf, chunkX, chunkZ);
            world.getChunkManager().loadChunkFromPacket(chunkX, chunkZ, chunkData.getSectionsDataBuf(),
                    chunkData.getHeightmap(), chunkData.getBlockEntities(chunkX, chunkZ));
            chunks.add(world.getChunk(chunkX, chunkZ));
        }
        worldChunks.put(assemblyID, chunks);
    }

    public void deleteWorld(String assemblyID) {
        CreateEngineers.LOGGER.info("Deleting client world of assembly \"{}\"...", assemblyID);
        assemblyWorlds.remove(assemblyID);
        worldChunks.remove(assemblyID);
    }

    public ClientWorld getWorld(String assemblyID) {
        return assemblyWorlds.get(assemblyID);
    }

    private ClientWorld createWorld(String assemblyID) {
        CreateEngineers.LOGGER.info("Creating client world for assembly \"{}\"...", assemblyID);

        MinecraftClient client = CreateEngineersClient.CLIENT;
        ClientWorld clientWorld = client.world;

        assert client.world != null;
        ClientWorld assemblyWorld = new ClientWorld(
                client.getNetworkHandler(),
                client.world.getLevelProperties(),
                RegistryKey.of(RegistryKeys.WORLD, new Identifier(CreateEngineers.MOD_ID, assemblyID)),
                clientWorld.getDimensionEntry(),
                128,
                128,
                clientWorld.getProfilerSupplier(),
                client.worldRenderer,
                false,
                1234L);

        assemblyWorlds.put(assemblyID, assemblyWorld);

        return assemblyWorld;
    }
}

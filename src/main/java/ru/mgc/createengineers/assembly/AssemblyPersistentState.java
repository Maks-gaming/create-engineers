package ru.mgc.createengineers.assembly;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssemblyPersistentState extends PersistentState {
    private final List<ChunkPos> chunks = new ArrayList<>();
    private UUID entityId;
    private Identifier world;

    public static AssemblyPersistentState createFromNbt(NbtCompound tag) {
        AssemblyPersistentState state = new AssemblyPersistentState();

        // Forced chunks
        NbtList chunkList = tag.getList("ForcedChunks", NbtIntArray.INT_ARRAY_TYPE);
        for (NbtElement nbtElement : chunkList) {
            NbtIntArray chunkArray = (NbtIntArray) nbtElement;
            int[] chunk = chunkArray.getIntArray();
            state.chunks.add(new ChunkPos(chunk[0], chunk[1]));
        }

        // Entity ID
        state.entityId = tag.getUuid("EntityUUID");

        // Entity World
        state.world = new Identifier(tag.getString("EntityWorld"));

        return state;
    }

    public UUID getEntityUUID() {
        return entityId;
    }

    public void setEntityUUID(UUID uuid) {
        entityId = uuid;
        markDirty();
    }

    public Identifier getWorldIdentifier() {
        return world;
    }

    public void setWorldIdentifier(Identifier identifier) {
        world = identifier;
        markDirty();
    }

    public void addChunkPosition(ChunkPos chunkPos) {
        if (chunks.contains(chunkPos)) {
            // CreateEngineers.LOGGER.warn("Failed to add chunk {}:{} to assembly persistent state: already exists", chunkPos.x, chunkPos.z);
            return;
        }

        // CreateEngineers.LOGGER.warn("Chunk {}:{} added assembly persistent state", chunkPos.x, chunkPos.z);
        chunks.add(chunkPos);
        markDirty();
    }

    public List<ChunkPos> getChunksPositions() {
        return chunks;
    }

    public NbtCompound writeNbt(NbtCompound tag) {
        // Forced chunks
        NbtList chunkList = new NbtList();
        for (ChunkPos chunk : chunks) {
            chunkList.add(new NbtIntArray(new int[]{chunk.x, chunk.z}));
        }
        tag.put("ForcedChunks", chunkList);

        // Entity ID
        tag.putUuid("EntityUUID", entityId);

        // Entity World
        tag.putString("EntityWorld", world.toString());

        return tag;
    }
}

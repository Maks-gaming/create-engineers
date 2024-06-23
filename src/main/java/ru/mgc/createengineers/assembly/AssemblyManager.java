package ru.mgc.createengineers.assembly;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import ru.mgc.createengineers.CreateEngineers;
import ru.mgc.createengineers.entity.AssemblyEntity;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AssemblyManager {
    public static void assemble(BlockPos center, Set<BlockPos> blockPositions, World originWorld) {
        String assemblyID = UUID.randomUUID().toString();
        RuntimeWorldHandle targetWorldHandle = AssemblyDimensionManager.getWorld(assemblyID);
        AssemblyPersistentState assemblyDataManager = AssemblyDimensionManager.getAssemblyDataManager(assemblyID);
        ServerWorld targetWorld = targetWorldHandle.asWorld();

        for (BlockPos originPosition : blockPositions) {
            BlockPos targetPosition = originPosition.subtract(center);
            Chunk targetChunk = targetWorld.getChunk(targetPosition);
            ChunkPos targetChunkPos = targetChunk.getPos();
            assemblyDataManager.addChunkPosition(targetChunkPos);
            targetWorld.setChunkForced(targetChunkPos.x, targetChunkPos.z, true);

            BlockState originBlockState = originWorld.getBlockState(originPosition);
            // BlockEntity originBlockEntity = originWorld.getBlockEntity(originPosition); // TODO: Fix this

            targetWorld.setBlockState(targetPosition, originBlockState);

            // Clear origin block
            originWorld.removeBlockEntity(originPosition);
            originWorld.setBlockState(originPosition, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL, 0);
        }

        // Summon entity
        AssemblyEntity assemblyEntity = new AssemblyEntity(CreateEngineers.ASSEMBLY_ENTITY, originWorld);
        assemblyDataManager.setEntityUUID(assemblyEntity.getUuid());
        assemblyDataManager.setWorldIdentifier(originWorld.getRegistryKey().getRegistry());
        assemblyEntity.setPosition(center.getX(), center.getY(), center.getZ());
        assemblyEntity.setAssemblyID(assemblyID);
        originWorld.spawnEntity(assemblyEntity);

    }

    public static Set<BlockPos> getAllAssemblyBlocksFromGluedBlocks(World world, BlockPos origin) {
        Set<BlockPos> visited = new HashSet<>();
        getAssemblyBlocksFromGluedBlocks(world, origin, visited);
        return visited;
    }

    private static void getAssemblyBlocksFromGluedBlocks(World world, BlockPos position, Set<BlockPos> visited) {
        // If the position is already visited, return to avoid loops
        if (!visited.add(position)) {
            return;
        }

        // Get all glued sides for the current position
        for (Direction direction : Direction.values()) {
            if (SuperGlueEntity.isGlued(world, position, direction, null)) {
                BlockPos nextPosition = position.offset(direction);
                // Recursively find all glued positions from the new position
                getAssemblyBlocksFromGluedBlocks(world, nextPosition, visited);
            }
        }
    }
}

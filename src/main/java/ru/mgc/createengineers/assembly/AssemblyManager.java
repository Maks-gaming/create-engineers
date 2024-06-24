package ru.mgc.createengineers.assembly;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
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
        // Generate a unique ID for this assembly
        String assemblyID = UUID.randomUUID().toString();

        // Get the target world handle and data manager for the assembly
        RuntimeWorldHandle handle = AssemblyDimensionManager.loadWorld(assemblyID);
        AssemblyPersistentState assemblyDataManager = AssemblyDimensionManager.getPersistentState(handle);
        ServerWorld targetWorld = handle.asWorld();

        for (BlockPos originPosition : blockPositions) {
            // Calculate the target position in the target world
            BlockPos targetPosition = originPosition.subtract(center);

            // Ensure the chunk containing the target position is loaded
            Chunk targetChunk = targetWorld.getChunk(targetPosition);
            ChunkPos targetChunkPos = targetChunk.getPos();
            assemblyDataManager.addChunkPosition(targetChunkPos);
            targetWorld.setChunkForced(targetChunkPos.x, targetChunkPos.z, true);

            // Copy the block state from the origin world to the target world
            BlockState originBlockState = originWorld.getBlockState(originPosition);
            targetWorld.setBlockState(targetPosition, originBlockState);

//            // TODO: Copy the block entity if it exists
//            BlockEntity originBlockEntity = originWorld.getBlockEntity(originPosition);
//            if (originBlockEntity != null) {
//                BlockEntity targetBlockEntity = originWorld.getBlockEntity(originPosition);
//                targetWorld.addBlockEntity(targetPosition, targetBlockEntity);
//            }
        }

        // clear
        for (BlockPos originPosition : blockPositions) {
            originWorld.setBlockState(originPosition, Blocks.AIR.getDefaultState());
        }

        // Summon and configure the assembly entity in the origin world
        AssemblyEntity assemblyEntity = new AssemblyEntity(CreateEngineers.ASSEMBLY_ENTITY, originWorld);
        assemblyDataManager.setEntityUUID(assemblyEntity.getUuid());
        assemblyDataManager.setWorldIdentifier(assemblyEntity.getWorld().getDimensionEntry().getKey().get().getValue());
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

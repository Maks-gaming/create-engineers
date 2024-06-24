package ru.mgc.createengineers.mixin;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.mgc.createengineers.CreateEngineers;
import ru.mgc.createengineers.CreateEngineersNetworking;
import ru.mgc.createengineers.assembly.AssemblyDimensionManager;
import ru.mgc.createengineers.assembly.AssemblyPersistentState;
import ru.mgc.createengineers.entity.AssemblyEntity;
import ru.mgc.createengineers.util.EntityUtils;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Objects;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class AssemblyBlockUpdateMixin extends World {

    protected AssemblyBlockUpdateMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Inject(at = @At("HEAD"), method = "onBlockChanged")
    private void onBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock,
                                CallbackInfo info) {

        // Server only mixin
        if (this.isClient()) return;

        // Assemblies only mixin
        if (!Objects.equals(this.getRegistryKey().getValue().getNamespace(), CreateEngineers.MOD_ID)) return;

        String assemblyID = this.getRegistryKey().getValue().getPath();
        Chunk chunk = this.getChunk(pos);

        // Do nothing if not loaded
        if (!AssemblyDimensionManager.isLoaded(assemblyID)) return;

        // Add chunks
        RuntimeWorldHandle handle = AssemblyDimensionManager.getWorld(assemblyID);
        AssemblyPersistentState assemblyDataManager = AssemblyDimensionManager.getPersistentState(handle);
        assemblyDataManager.addChunkPosition(chunk.getPos());

        ServerWorld entityWorld = CreateEngineers.SERVER.getWorld(RegistryKey.of(RegistryKeys.WORLD, assemblyDataManager.getWorldIdentifier()));

        // World is not loaded
        if (entityWorld == null) return;

        // Getting entity
        AssemblyEntity entity = (AssemblyEntity) entityWorld.getEntity(assemblyDataManager.getEntityUUID());
        if (entity == null) {
            CreateEngineers.LOGGER.warn("Entity is not defined on block update");
            return;
        }

        // Send update packet
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(assemblyID);
        buf.writeBlockPos(pos);
        buf.writeRegistryValue(Block.STATE_IDS, newBlock);

        for (PlayerEntity player : entity.getWorld().getPlayers()) {
            if (!EntityUtils.isEntityWithinSimulationDistance(player, entity))
                return;

            ServerPlayNetworking.send((ServerPlayerEntity) player,
                    CreateEngineersNetworking.AssemblyBlockUpdateS2CPacket, buf);
        }
    }
}
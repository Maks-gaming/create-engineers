package ru.mgc.createengineers.entity;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import ru.mgc.createengineers.CreateEngineers;
import ru.mgc.createengineers.CreateEngineersNetworking;
import ru.mgc.createengineers.assembly.AssemblyDimensionManager;
import ru.mgc.createengineers.client.CreateEngineersClient;

public class AssemblyEntity extends Entity {
    private String assemblyID;

    public AssemblyEntity(EntityType<? extends AssemblyEntity> type, World world) {
        super(type, world);
    }

    public String getAssemblyID() {
        return assemblyID;
    }

    public void setAssemblyID(String id) {
        assemblyID = id;
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected Box calculateBoundingBox() {
        // TODO: calculate
        return super.calculateBoundingBox();
    }

    @Override
    public void tick() {
        super.tick();

        // Server dimension tick
        if (!getWorld().isClient() && assemblyID != null) {
            CreateEngineers.SERVER.execute(() -> AssemblyDimensionManager.tickAssemblyEntity(this));
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("AssemblyID")) {
            setAssemblyID(nbt.getString("AssemblyID"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (assemblyID != null) {
            nbt.putString("AssemblyID", assemblyID);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        // Server-side removal
        AssemblyDimensionManager.deleteWorld(assemblyID);

        super.remove(reason);
    }

    @Override
    public void onRemoved() {
        // Client-side removal
        CreateEngineersClient.dimensionManager.deleteWorld(assemblyID);

        super.onRemoved();
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(this.getUuid());
        buf.writeVarInt(this.getId());
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());
        buf.writeString(assemblyID);

        return ServerPlayNetworking.createS2CPacket(CreateEngineersNetworking.AssemblySpawnEntityS2CPacket, buf);
    }
}

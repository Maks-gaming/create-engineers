package ru.mgc.createengineers.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import ru.mgc.createengineers.CreateEngineers;

public class EntityUtils {
    public static boolean isEntityWithinSimulationDistance(PlayerEntity player, Entity entity) {
        // Retrieve the simulation distance
        int simulationDistance = CreateEngineers.SERVER.isDedicated()
                ? ((MinecraftDedicatedServer) CreateEngineers.SERVER).getProperties().viewDistance
                : 32;

        double simulationDistanceBlocks = simulationDistance * 16;
        return entity.isInRange(player, simulationDistanceBlocks);
    }
}

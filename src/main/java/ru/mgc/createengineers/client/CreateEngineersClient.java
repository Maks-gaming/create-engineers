package ru.mgc.createengineers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import ru.mgc.createengineers.CreateEngineers;
import ru.mgc.createengineers.client.assembly.AssemblyClientDimensionManager;
import ru.mgc.createengineers.client.entity.AssemblyEntityRenderer;

public class CreateEngineersClient implements ClientModInitializer {
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public static AssemblyClientDimensionManager dimensionManager;

    @Override
    public void onInitializeClient() {
        dimensionManager = new AssemblyClientDimensionManager();

        EntityRendererRegistry.register(CreateEngineers.ASSEMBLY_ENTITY, AssemblyEntityRenderer::new);
    }
}

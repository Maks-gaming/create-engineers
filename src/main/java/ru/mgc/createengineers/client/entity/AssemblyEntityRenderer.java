package ru.mgc.createengineers.client.entity;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.mgc.createengineers.client.CreateEngineersClient;
import ru.mgc.createengineers.entity.AssemblyEntity;

import java.util.Set;

public class AssemblyEntityRenderer extends EntityRenderer<AssemblyEntity> {
    public AssemblyEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(AssemblyEntity var1) {
        return null;
    }

    @Override
    public void render(AssemblyEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.translate(0.5f, 0, 0.5f);

        String dimensionUUID = entity.getAssemblyID();
        if (dimensionUUID == null) {
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        // Test rotate
        matrices.multiply(new Quaternionf().rotateY(0.2f));

        Vector3f offset = new Vector3f(0.5f, 0f, 0.5f);
        ClientWorld world = CreateEngineersClient.dimensionManager.getWorld(entity.getAssemblyID());
        if (world == null)
            return;

        Set<Chunk> chunks = CreateEngineersClient.dimensionManager.getChunks(dimensionUUID);
        if (chunks == null) {
            return;
        }

        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        for (Chunk chunk : chunks) {
            for (int x = 0; x < 16; x++) {
                for (int y = -64; y < 320; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = new BlockPos(chunk.getPos().x * 16 + x, y, chunk.getPos().z * 16 + z);
                        // CreateEngineers.LOGGER.info(pos.toString());
                        BlockState state = chunk.getBlockState(pos);
                        if (state.getRenderType() != BlockRenderType.MODEL)
                            continue;

                        Vector3f loc = new Vector3f(pos.getX(), pos.getY(), pos.getZ()).sub(offset);
                        matrices.push();
                        matrices.translate(loc.x, loc.y, loc.z);

                        blockRenderManager.renderBlock(state, pos, world, matrices,
                                vertexConsumers.getBuffer(RenderLayer.getCutout()), true, world.random);
                        matrices.pop();
                    }
                }
            }
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}

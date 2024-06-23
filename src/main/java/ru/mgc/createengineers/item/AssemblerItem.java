package ru.mgc.createengineers.item;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import ru.mgc.createengineers.CreateEngineers;
import ru.mgc.createengineers.CreateEngineersNetworking;
import ru.mgc.createengineers.assembly.AssemblyManager;

import java.util.List;
import java.util.Set;

public class AssemblerItem extends Item {
    public AssemblerItem(Settings settings) {
        super(settings);

        ServerPlayNetworking.registerGlobalReceiver(CreateEngineersNetworking.AssemblerAssembleC2SPacket, (server, playerEntity, handler, buf, responseSender) -> {
            World world = playerEntity.getWorld();
            BlockPos usePosition = buf.readBlockPos();

            // Check for glue
            Set<BlockPos> positions = AssemblyManager.getAllAssemblyBlocksFromGluedBlocks(world,
                    usePosition);
            if (positions.size() == 1) {
                playerEntity.sendMessage(Text.of("No glue"));
                return;
            }

            // Assemble
            CreateEngineers.SERVER.execute(() -> AssemblyManager.assemble(usePosition, positions, world));

            // Remove all glue
            for (BlockPos pos : positions) {
                List<SuperGlueEntity> glueNearby = world.getNonSpectatingEntities(
                        SuperGlueEntity.class,
                        new Box(pos));
                for (SuperGlueEntity entity : glueNearby) {
                    entity.discard();
                }
            }
        });
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // Client only
        if (!context.getWorld().isClient()) {
            return super.useOnBlock(context);
        }

        // Send assemble packet
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(context.getBlockPos());
        ClientPlayNetworking.send(CreateEngineersNetworking.AssemblerAssembleC2SPacket, buf);

        return super.useOnBlock(context);
    }
}

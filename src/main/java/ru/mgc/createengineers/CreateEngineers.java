package ru.mgc.createengineers;

import com.simibubi.create.Create;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mgc.createengineers.assembly.AssemblyDimensionManager;
import ru.mgc.createengineers.entity.AssemblyEntity;
import ru.mgc.createengineers.item.AssemblerItem;

public class CreateEngineers implements ModInitializer {
    public static final String MOD_ID = "createengineers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final AssemblerItem ASSEMBLER_ITEM = new AssemblerItem(new FabricItemSettings());
    public static final EntityType<AssemblyEntity> ASSEMBLY_ENTITY = FabricEntityTypeBuilder
            .create(SpawnGroup.MISC, AssemblyEntity::new).dimensions(EntityDimensions.changing(1.0f, 1.0f)).build();

    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        LOGGER.info("Create {} detected", Create.VERSION);

        AssemblyDimensionManager.initialize();

        // Getting server
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);

        // Registering items
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "assembler"), ASSEMBLER_ITEM);

        // Registering entities
        Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(MOD_ID, "assembly"), ASSEMBLY_ENTITY);
    }
}

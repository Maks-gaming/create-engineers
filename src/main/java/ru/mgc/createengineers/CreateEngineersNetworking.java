package ru.mgc.createengineers;

import net.minecraft.util.Identifier;

public class CreateEngineersNetworking {
    public static final Identifier AssemblerAssembleC2SPacket = new Identifier(CreateEngineers.MOD_ID,
            "assembler_assemble");

    public static final Identifier AssemblySpawnEntityS2CPacket = new Identifier(CreateEngineers.MOD_ID,
            "assembly_spawn");

    public static final Identifier AssemblyBlockUpdateS2CPacket = new Identifier(CreateEngineers.MOD_ID,
            "assembly_block_update");

    public static final Identifier AssemblyWorldS2CPacket = new Identifier(CreateEngineers.MOD_ID,
            "assembly_world");
}

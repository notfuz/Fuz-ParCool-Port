package com.alrex.parcool.common.tags;

import com.alrex.parcool.ParCool;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

public class BlockTags {
    public static final TagKey<Block> HIDE_ABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "hide_able"));
    public static final TagKey<Block> POLE_CLIMBABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, "pole_climbable"));
}

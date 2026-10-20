package com.alrex.parcool.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * ParCool's own render types for the zipline rope.
 *
 * <p>Two are needed because the rope is drawn in one of two shapes: {@link #ZIPLINE_3D} with culling
 * and {@link #ZIPLINE_2D} without, switched by the {@code Enable3DRenderingForZipline} config entry.
 * Neither vanilla type matches: {@code RenderType.leash()} is the closest (it is also
 * {@code POSITION_COLOR_LIGHTMAP} with a lightmap) but it renders as a {@code TRIANGLE_STRIP} with
 * culling disabled, while the rope emits {@code QUADS} and has to be culled in the 3D variant.
 *
 * <p>1.21.4 still has the {@code RenderStateShard} model - {@code RenderSetup} around a
 * {@code RenderPipeline} only arrives in 1.21.5 - so this is the 1.21.1 shape. What 1.21.4 did
 * change is access: {@code RenderType.create(String, VertexFormat, Mode, int, boolean, boolean,
 * CompositeState)} is private here (1.21.1 still had a package-private five-argument overload) and
 * the {@code RenderStateShard} shards are {@code protected}. Both are widened in
 * {@code common/src/main/resources/parcool.accesswidener} for Fabric and in
 * {@code neoforge/src/main/resources/META-INF/accesstransformer.cfg} for NeoForge; without them
 * this class does not compile.
 */
public class RenderTypes {
    public static final RenderType ZIPLINE_3D;
    public static final RenderType ZIPLINE_2D;

    static {
        ZIPLINE_2D = RenderType.create(
                "zipline2d",
                DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256,
                false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_LEASH_SHADER)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .createCompositeState(false)
        );
        ZIPLINE_3D = RenderType.create(
                "zipline3d",
                DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256,
                false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_LEASH_SHADER)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setCullState(RenderStateShard.CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .createCompositeState(false)
        );
    }
}

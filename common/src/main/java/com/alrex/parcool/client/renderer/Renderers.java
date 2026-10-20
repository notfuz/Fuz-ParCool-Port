package com.alrex.parcool.client.renderer;

import com.alrex.parcool.client.renderer.entity.ZiplineRopeRenderer;
import com.alrex.parcool.common.entity.EntityTypes;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;

public class Renderers {
    public static void register() {
        EntityRendererRegistry.register(EntityTypes.ZIPLINE_ROPE, ZiplineRopeRenderer::new);
    }
}

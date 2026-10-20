package com.alrex.parcool.extern;

import dev.architectury.platform.Platform;

import javax.annotation.Nullable;

public abstract class ModManager {
    private boolean installed = false;
    private final String modId;

    public ModManager(String modId) {
        this.modId = modId;
    }

    /** Was {@code ModList.get().getModFileById(modId) != null}. */
    public void init() {
        installed = Platform.isModLoaded(this.modId);
    }

    // These are called after `init`
    public void initInClient() {
    }

    public void initInDedicatedServer() {
    }

    public boolean isInstalled() {
        return installed;
    }

    public String getModID() {
        return modId;
    }

    @Nullable
    protected static boolean modLoaded(String id) {
        return Platform.isModLoaded(id);
    }
}

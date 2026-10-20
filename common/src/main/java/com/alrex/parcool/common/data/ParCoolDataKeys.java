package com.alrex.parcool.common.data;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The four ParCool data slots. This is the direct successor of the removed
 * {@code common.attachment.Attachments} / {@code ClientAttachments} NeoForge attachment registries.
 * <p>
 * Registry keys are kept identical to the original attachment ids
 * ({@code parcool:stamina}, {@code parcool:parkourability},
 * {@code parcool.client:local_stamina}, {@code parcool.client:animation}) so that user-facing
 * identifiers in bug reports and config tooling stay comparable. The two {@code parcool.client:*}
 * slots live in {@link com.alrex.parcool.common.data.client.ClientDataKeys} instead of here, because
 * this class is loaded on a dedicated server and their constructor references drag in
 * {@code net.minecraft.client.player.LocalPlayer} - see that class for the full story.
 */
public final class ParCoolDataKeys {

    private static final Map<String, DataKey<?>> BY_ID = new LinkedHashMap<>();

    /**
     * Server+client stamina value. Persistent (it used to carry
     * {@code AttachmentType#serialize(ReadonlyStamina.CODEC)}).
     */
    public static final DataKey<ReadonlyStamina> STAMINA = DataKey.persistent(
            "parcool:stamina",
            ReadonlyStamina::createDefault,
            new DataKey.NbtSerializer<>() {
                @Override
                public ReadonlyStamina read(CompoundTag tag) {
                    var value = tag.getCompound("value");
                    return new ReadonlyStamina(
                            value.getBoolean("exhausted"),
                            value.getInt("stamina"),
                            value.getInt("max")
                    );
                }

                @Override
                public void write(ReadonlyStamina stamina, CompoundTag tag) {
                    CompoundTag value = new CompoundTag();
                    value.putBoolean("exhausted", stamina.isExhausted());
                    value.putInt("stamina", stamina.value());
                    value.putInt("max", stamina.max());
                    tag.put("value", value);
                }
            }
    );

    /** Per-player action state, behaviour enforcer, and the client/server limitation pair. */
    public static final DataKey<Parkourability> PARKOURABILITY = DataKey.transientKey(
            "parcool:parkourability",
            Parkourability::new
    );

    static {
        register(STAMINA);
        register(PARKOURABILITY);
    }

    private ParCoolDataKeys() {
    }

    private static void register(DataKey<?> key) {
        BY_ID.put(key.id(), key);
    }

    @Nullable
    public static DataKey<?> byId(String id) {
        return BY_ID.get(id);
    }

    // ------------------------------------------------------------------
    // typed accessors used across the mod
    // ------------------------------------------------------------------

    public static ReadonlyStamina getStamina(Entity entity) {
        return ParCoolData.get(entity, STAMINA);
    }

    public static void setStamina(Entity entity, ReadonlyStamina stamina) {
        ParCoolData.set(entity, STAMINA, stamina);
    }

    public static Parkourability getParkourability(Player player) {
        return ParCoolData.get(player, PARKOURABILITY);
    }

}

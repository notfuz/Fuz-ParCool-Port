package com.alrex.parcool.api;

import com.alrex.parcool.ParCool;
import dev.architectury.platform.Platform;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.Objects;

/**
 * ParCool's two player attributes, resolved as {@link Holder}s out of the built-in attribute
 * registry.
 *
 * <p>They are attached to the player by {@code mixin.common.PlayerAttributesMixin}, which extends the
 * builder {@code Player#createAttributes} assembles - what {@code EntityAttributeRegistry} would have
 * done, except that on NeoForge it rejects a second supplier for {@code EntityType.PLAYER} and on
 * Fabric it <i>replaces</i> the player's whole supplier, which costs the player
 * {@code minecraft:generic.max_health} and makes joining a world fail.
 *
 * <p><b>Why the two loaders register them differently.</b> {@code BuiltInRegistries.ATTRIBUTE} is
 * writable only while {@code Bootstrap#bootStrap} is running, and that pass is the one thing that
 * reaches {@code DefaultAttributes}' static initialiser, i.e. {@code Player#createAttributes}, on
 * Fabric. NeoForge closes the built-in registries before the mod constructors run, so a direct
 * {@code Registry.registerForHolder} there dies with "Registry is already frozen (trying to add key
 * parcool:max_stamina)" and NeoForge cannot start at all. There is no vanilla hook that sits inside
 * the writable window on both loaders - {@code Bootstrap#validate} runs before the freeze on Fabric
 * and after it on NeoForge - so this is a real platform split:
 * <ul>
 *     <li><b>Fabric</b>: the static initialiser below registers the two entries, and it is reached
 *     from {@code Bootstrap#bootStrap} through the mixin.</li>
 *     <li><b>NeoForge</b>: {@code :neoforge}'s {@code NeoForgeAttributes} registers them through
 *     NeoForge's own {@code DeferredRegister} (which fires on {@code RegisterEvent}, while the
 *     registry is still open) - exactly what upstream ParCool's NeoForge build does. Architectury's
 *     {@code DeferredRegister} is not an option: it rejects
 *     {@link net.minecraft.core.registries.Registries#ATTRIBUTE} outright
 *     ({@code RegistrarManager._get} asserts on it).</li>
 * </ul>
 * On NeoForge the holders are therefore read back from the registry instead of being written to it;
 * they are consumed as {@link Holder}s by {@code AttributeSupplier.Builder#add} and
 * {@code Player#getAttribute(Holder)}, so a plain read is enough.
 */
public class Attributes {

    public static final Holder<Attribute> MAX_STAMINA =
            resolve("max_stamina", "parcool.max_stamina", 2000, 10, 10000);

    public static final Holder<Attribute> STAMINA_RECOVERY =
            resolve("stamina_recovery", "parcool.stamina_recovery", 20, 0, 10000);

    private Attributes() {
    }

    private static Holder<Attribute> resolve(
            String id, String descriptionId, double defaultValue, double min, double max) {
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, id);
        if (Platform.isNeoForge()) {
            // Registered by :neoforge, so read the holder back instead of creating a second entry.
            return BuiltInRegistries.ATTRIBUTE.get(key)
                    .orElseThrow(() -> new IllegalStateException("ParCool attribute " + key + " is not registered"));
        }
        return Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                key,
                new RangedAttribute(descriptionId, defaultValue, min, max).setSyncable(true));
    }

    /**
     * Forces this class to load, which on Fabric is what runs the two registrations above.
     *
     * <p>Only the Fabric entry point may call this: a static method call initializes the class, and
     * that runs {@code <clinit>}, which on NeoForge would resolve the holders before
     * {@code :neoforge}'s {@code DeferredRegister} has fired (it only registers on the registry event
     * that comes after the mod constructors). ParCool's shared entry point therefore does not touch
     * this class at all - see {@code ParCool#init} and {@code ParCoolFabric}.
     */
    public static void registerAll() {
        Objects.requireNonNull(MAX_STAMINA);
        Objects.requireNonNull(STAMINA_RECOVERY);
    }
}

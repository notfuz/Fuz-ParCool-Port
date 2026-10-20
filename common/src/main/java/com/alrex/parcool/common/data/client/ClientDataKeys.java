package com.alrex.parcool.common.data.client;

import com.alrex.parcool.common.data.DataKey;
import com.alrex.parcool.common.data.ParCoolData;
import net.minecraft.world.entity.player.Player;

/**
 * ParCool's two client-only data slots.
 *
 * <h2>Why they are not in {@link com.alrex.parcool.common.data.ParCoolDataKeys}</h2>
 * {@code ParCoolDataKeys} is loaded on a dedicated server, and its static initialiser used to build
 * these two keys with the constructor references {@code LocalStamina::new} and {@code Animation::new}.
 * A constructor reference compiles to an {@code invokedynamic}, so the bootstrap method has to be
 * linked while the key class initialises - which loads {@code LocalStamina} and {@code Animation},
 * and their method signatures mention {@code net.minecraft.client.player.LocalPlayer}. NeoForge's
 * {@code RuntimeDistCleaner} kills the server for that:
 *
 * <pre>
 * java.lang.BootstrapMethodError: java.lang.RuntimeException: Attempted to load class
 *   net/minecraft/client/player/LocalPlayer for invalid dist DEDICATED_SERVER
 *   at ParCoolDataKeys.&lt;clinit&gt;(ParCoolDataKeys.java:62)
 *   at Parkourability.get(Parkourability.java:23)
 *   at net.minecraft.world.entity.Entity.onGround(Entity.java)
 *   at net.minecraft.server.players.PlayerList.placeNewPlayer(PlayerList.java)
 * </pre>
 *
 * i.e. the dedicated server died the moment a player joined (the client then reported the generic
 * "Connection Lost / Network Protocol Error"). On a production Fabric server the same reference is a
 * plain {@code NoClassDefFoundError}, because the client jar is not on the classpath at all.
 *
 * <p>Nothing outside client code may touch this class.
 */
public final class ClientDataKeys {

    /** Client only: the mutable stamina handler cached per local player. */
    public static final DataKey<LocalStamina> LOCAL_STAMINA = DataKey.transientKey(
            "parcool.client:local_stamina",
            LocalStamina::new
    );

    /** Client only: the active {@code Animator} plus its per-frame {@code AnimationOption}. */
    public static final DataKey<Animation> ANIMATION = DataKey.transientKey(
            "parcool.client:animation",
            Animation::new
    );

    private ClientDataKeys() {
    }

    public static LocalStamina getLocalStamina(Player player) {
        return ParCoolData.get(player, LOCAL_STAMINA);
    }

    public static Animation getAnimation(Player player) {
        return ParCoolData.get(player, ANIMATION);
    }
}

package com.alrex.parcool.common.stamina;

import com.alrex.parcool.common.stamina.handlers.HungerStaminaHandler;
import com.alrex.parcool.common.stamina.handlers.InfiniteStaminaHandler;
import com.alrex.parcool.common.stamina.handlers.ParCoolStaminaHandler;
import com.alrex.parcool.extern.AdditionalMods;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.function.Function;
import java.util.function.Supplier;

public enum StaminaType {
    NONE(InfiniteStaminaHandler::new),
    PARCOOL(ParCoolStaminaHandler::new),
    HUNGER(HungerStaminaHandler::new),
    PARAGLIDER(AdditionalMods::newParagliderStaminaHandler),
    EPIC_FIGHT(AdditionalMods::newEpicFightStaminaHandler);

    private final Function<Player, IParCoolStaminaHandler> constructor;

    StaminaType(Function<Player, IParCoolStaminaHandler> constructor) {
        this.constructor = constructor;
    }

    StaminaType(Supplier<IParCoolStaminaHandler> constructor) {
        this.constructor = (player) -> constructor.get();
    }

    public IParCoolStaminaHandler newHandler(Player player) {
        return constructor.apply(player);
    }

    /**
     * The ordinal arrives from the client (StaminaProcessOnServerPayload is registered C2S), so it is
     * looked up by value instead of being used to index values(): a VAR_INT outside the enum would
     * otherwise be an ArrayIndexOutOfBoundsException thrown straight from the packet decoder.
     */
    public static StaminaType byOrdinal(int ordinal) {
        StaminaType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException("Unknown stamina type " + ordinal);
        }
        return values[ordinal];
    }

    public static final StreamCodec<ByteBuf, StaminaType> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            StaminaType::ordinal,
            StaminaType::byOrdinal
    );
}

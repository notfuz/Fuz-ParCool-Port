package com.alrex.parcool.extern.paraglider;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.Actions;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.data.Parkourability;
import net.minecraft.resources.ResourceLocation;
import tictim.paraglider.api.movement.ParagliderPlayerStates;
import tictim.paraglider.api.movement.PlayerStateCondition;

import java.util.Arrays;
import java.util.List;

public class ParCoolPlayerStates {
    public static final Entry FAST_RUN = Entry.constant(FastRun.class)
            .parentID(
                    ParagliderPlayerStates.IDLE,
                    ParagliderPlayerStates.RUNNING
            )
            .relativePriority(-1);
    public static final Entry FAST_SWIM = Entry.constant(FastSwim.class)
            .parentID(
                    ParagliderPlayerStates.IDLE,
                    ParagliderPlayerStates.SWIMMING
            )
            .relativePriority(-1);
    public static final Entry CLING_TO_CLIFF = Entry.constant(ClingToCliff.class).parentID(ParagliderPlayerStates.MIDAIR);
    public static final Entry DODGE = Entry.instant(Dodge.class);
    public static final Entry CLIMB_UP = Entry.instant(ClimbUp.class)
            .parentID(
                    ParagliderPlayerStates.IDLE,
                    ParagliderPlayerStates.MIDAIR
            );
    public static final Entry TAP = Entry.constant(Tap.class);
    public static final Entry ROLL = Entry.instant(Roll.class);
    public static final Entry HORIZONTAL_WALL_RUN = Entry.constant(HorizontalWallRun.class);
    public static final Entry VERTICAL_WALL_RUN = Entry.instant(VerticalWallRun.class);
    public static final Entry WALL_JUMP = Entry.instant(WallJump.class);
    public static final Entry VAULT = Entry.instant(Vault.class);
    public static final Entry CATLEAP = Entry.instant(CatLeap.class);
    public static final Entry CHARGE_JUMP = Entry.instant(ChargeJump.class);
    public static final Entry RIDE_ZIPLINE = Entry.constant(RideZipline.class);

    public static final List<Entry> ENTRIES = Arrays.asList(
            FAST_RUN,
            FAST_SWIM,
            CLING_TO_CLIFF,
            DODGE,
            CLIMB_UP,
            TAP,
            ROLL,
            HORIZONTAL_WALL_RUN,
            VERTICAL_WALL_RUN,
            WALL_JUMP,
            VAULT,
            CATLEAP,
            CHARGE_JUMP,
            RIDE_ZIPLINE
    );

    public record Entry(
            Class<? extends Action> clazz,
            ResourceLocation stateID,
            List<ResourceLocation> parentID,
            int staminaDelta,
            double priority,
            PlayerStateCondition condition
    ) {
        private static Entry constant(Class<? extends Action> clazz) {
            return new Entry(
                    clazz,
                    ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, clazz.getSimpleName().toLowerCase()),
                    Arrays.asList(
                            ParagliderPlayerStates.IDLE,
                            ParagliderPlayerStates.RUNNING,
                            ParagliderPlayerStates.SWIMMING,
                            ParagliderPlayerStates.MIDAIR
                    ),
                    -Math.min(15, Actions.ACTION_REGISTRIES.get(Actions.getIndexOf(clazz)).getDefaultStaminaConsumption()),
                    5,
                    (c) -> {
                        var parkourability = Parkourability.get(c.player());
                        return ParagliderManager.getInstance().isUsingParagliderStamina(parkourability)
                                && parkourability.get(clazz).isDoing();
                    }
            );
        }

        private static Entry instant(Class<? extends Action> clazz) {
            return new Entry(
                    clazz,
                    ResourceLocation.fromNamespaceAndPath(ParCool.MOD_ID, clazz.getSimpleName().toLowerCase()),
                    Arrays.asList(
                            ParagliderPlayerStates.IDLE,
                            ParagliderPlayerStates.RUNNING,
                            ParagliderPlayerStates.SWIMMING,
                            ParagliderPlayerStates.MIDAIR
                    ),
                    -Math.min(25, Actions.ACTION_REGISTRIES.get(Actions.getIndexOf(clazz)).getDefaultStaminaConsumption() / 6),
                    5,
                    (c) -> {
                        var parkourability = Parkourability.get(c.player());
                        if (!ParagliderManager.getInstance().isUsingParagliderStamina(parkourability)) return false;
                        var action = parkourability.get(clazz);
                        var tickFromStarted = action.getTickFromLastStarted();
                        return 0 <= tickFromStarted && tickFromStarted < 11;
                    }
            );
        }

        public Entry condition(PlayerStateCondition condition) {
            return new Entry(clazz, stateID, parentID, staminaDelta, priority, (c) -> this.condition.test(c) && condition.test(c));
        }

        public Entry priority(double value) {
            return new Entry(clazz, stateID, parentID, staminaDelta, value, condition);
        }

        public Entry relativePriority(double value) {
            return new Entry(clazz, stateID, parentID, staminaDelta, priority + value, condition);
        }

        public Entry staminaDelta(int value) {
            return new Entry(clazz, stateID, parentID, value, priority, condition);
        }

        public Entry parentID(ResourceLocation... value) {
            return new Entry(clazz, stateID, Arrays.stream(value).toList(), staminaDelta, priority, condition);
        }
    }
}
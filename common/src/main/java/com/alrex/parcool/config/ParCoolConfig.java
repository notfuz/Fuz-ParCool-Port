package com.alrex.parcool.config;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.client.animation.Animator;
import com.alrex.parcool.client.animation.AnimatorList;
import com.alrex.parcool.client.gui.ColorTheme;
import com.alrex.parcool.client.hud.Position;
import com.alrex.parcool.client.hud.impl.HUDType;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.Actions;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.stamina.StaminaType;
import dev.architectury.platform.Platform;
import io.netty.buffer.ByteBuf;


import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ParCoolConfig {

    public enum AdvantageousDirection {
        Lower, Higher
    }
	public interface Item<T> {
		T get();

		void set(T value);

		String getPath();

		@Nullable
		ConfigSpec.ConfigValue<T> getInternalInstance();

		void register(ConfigSpec.Builder builder);

		void writeToBuffer(ByteBuf buffer);

		T readFromBuffer(ByteBuf buffer);
	}

	public enum ConfigGroup {
		Animation, CameraAnimation, HUD, Modifier, Control, Stamina, Other
	}

	public static class Client {
		private static final Client instance;
		private static final ConfigSpec configSpec;

		public static Client getInstance() {
			return instance;
		}

		public static ConfigSpec getConfigSpec() {
			return configSpec;
		}

		static {
			var pair = new ConfigSpec.Builder().configure(Client::new);
			instance = pair.getLeft();
			configSpec = pair.getRight();
		}

		/**
		 * The three accessors below are reached with whatever class a caller passes - a third-party
		 * Animator subclass reaches canAnimate, and an Action registered after this config was built
		 * reaches the other two - while {@link Actions#getIndexOf} and {@link AnimatorList#getIndex}
		 * answer {@code -1} for anything they do not know. Indexing the array with that was an
		 * ArrayIndexOutOfBoundsException, so the -1 is turned into a permissive default here instead.
		 */
		public ConfigSpec.BooleanValue getPossibilityOf(Class<? extends Action> action) {
			short index = Actions.getIndexOf(action);
			return index < 0 ? UNKNOWN_ACTION_ENABLED : actionPossibilities[index];
		}

		public ConfigSpec.BooleanValue canAnimate(Class<? extends Animator> animator) {
			short index = AnimatorList.getIndex(animator);
			return index < 0 ? UNKNOWN_ANIMATOR_ENABLED : animatorPossibilities[index];
		}

		public ConfigSpec.IntValue getStaminaConsumptionOf(Class<? extends Action> action) {
			short index = Actions.getIndexOf(action);
			// Unknown action: no configured consumption, so only the server's own floor can apply
			// (ActionInfo takes max of this and the server limitation).
			return index < 0 ? UNKNOWN_ACTION_CONSUMPTION : staminaConsumptions[index];
		}

		public enum Booleans implements Item<Boolean> {
			EnableAnimation(
					ConfigGroup.Animation, "Enable custom animations",
					"enable_animation", true
			),
			EnableFallingAnimation(
					ConfigGroup.Animation, "Enable custom animation of falling",
					"enable_falling_animation", true
			),
            EnableLeanAnimationOfFastRun(
                    ConfigGroup.Animation, "Enable lean animation while FastRun",
                    "enable_lean_animation_fast_run", true
            ),
			EnableFPVAnimation(
					ConfigGroup.CameraAnimation, "Enable first-person-view animations",
					"enable_fpv_animation", false
			),
			EnableCameraAnimationOfDodge(
					ConfigGroup.CameraAnimation, "Enable rotation of camera by Dodge",
					"enable_camera_rotation_dodge", false
			),
			EnableCameraAnimationOfBackWallJump(
					ConfigGroup.CameraAnimation, "Enable rotation of camera by Backward Wall-Jump",
					"enable_camera_rotation_back_wall_jump", true
			),
			EnableCameraAnimationOfRolling(
					ConfigGroup.CameraAnimation, "Enable rotation of camera by Roll",
					"enable_camera_rotation_roll", true
			),
			EnableCameraAnimationOfFlipping(
					ConfigGroup.CameraAnimation, "Enable rotation of camera by Flipping",
					"enable_camera_rotation_flipping", false
			),
			EnableCameraAnimationOfVault(
					ConfigGroup.CameraAnimation, "Enable animation of camera by Vault",
					"enable_camera_animation_vault", false
			),
			EnableCameraAnimationOfHWallRun(
					ConfigGroup.CameraAnimation, "Enable animation of camera by Horizontal-WallRun",
					"enable_camera_animation_h-wall-run", true
			),
			EnableCameraAnimationOfHangDown(
					ConfigGroup.CameraAnimation, "Enable animation of camera by Hang-Down",
					"enable_camera_animation_hang-down", true
			),
			HideStaminaHUDWhenStaminaIsInfinite(
					ConfigGroup.HUD, null,
					"hide_hud_if_stamina_infinite", true
			),
			ShowActionStatusBar(
					ConfigGroup.HUD, "Stamina HUD shows action charge rate, cool time or etc",
					"show_action_status_bar", true
			),
			ShowLightStaminaHUDAlways(
					ConfigGroup.HUD, "Light stamina HUD shows always",
					"show_light_hud_always", false
			),
			EnableStaminaExhaustionPenalty(
					ConfigGroup.Stamina, "Enable slowing down of stamina exhaustion",
					"enable_stamina_exhaustion_penalty", true
			),
			EnableDoubleTappingForDodge(
					ConfigGroup.Control, "Enable double-tapping ctrl for Dodge",
					"enable_double_tapping_for_dodge", false
			),
			EnableWallJumpCooldown(
					ConfigGroup.Control, "Enable cooldown of wall jump",
					"enable_wall_jump_cooldown", true
			),
			EnableCrawlInAir(
					ConfigGroup.Control, "Enable Crawl in air",
					"enable_crawl_in_air", true
			),
			EnableVaultInAir(
					ConfigGroup.Control, "Enable Vault in air",
					"enable_vault_in_air", true
			),
            CanGetOffStepsWhileDodge(
                    ConfigGroup.Control, "Enable getting off steps while doing dodge",
                    "can_get_off_steps_while_dodge", false
			),
			EnableRollWhenCreative(
					ConfigGroup.Control, "Enable Roll when creative mode (experimental)",
					"enable_roll_creative", false
			),
			EnableJustTimeEffectOfBreakfall(
					ConfigGroup.Other, "Enable just timing effect of Breakfall",
					"enable_just_time_effect_breakfall", true
			),
			EnableActionSounds(
					ConfigGroup.Other, "Enable sounds triggered by Action",
					"enable_sounds", true
			),
			EnableActionParticles(
					ConfigGroup.Other, "Enable particles triggered by Action",
					"enable_particles", true
			),
			EnableActionParticlesOfJustTimeBreakfall(
					ConfigGroup.Other, "Enable particles triggered by just-time breakfall",
					"enable_particles_jt_breakfall", true
			),
            Enable3DRenderingForZipline(
                    ConfigGroup.Other, "Enable block like rendering of zipline",
                    "enable_3d_render_zipline", true
            ),
			VaultKeyPressedNeeded(
                    ConfigGroup.Control, "Make Vault need Vault Key Pressed",
					"vault_needs_key_pressed", false
			),
            HideInBlockSneakNeeded(
                    ConfigGroup.Control, "Make HideInBlock need player sneaking",
                    "hideinblock_needs_sneaking", true
            ),
			SubstituteSprintForFastRun(
					ConfigGroup.Control, "enable players to do actions needing Fast-Running by sprint",
					"substitute_sprint", false
			),
			ShowAutoResynchronizationNotification(
					ConfigGroup.Other, "Notify if auto resynchronization of Limitation is executed",
					"notify_limitation_auto_resync", false
			),
			ParCoolIsActive(
					ConfigGroup.Other, "Whether ParCool is active",
					"parcool_activation", true
			);
			public final ConfigGroup Group;
			@Nullable
			public final String Comment;
			public final String Path;
            public final String Translation;
			public final boolean DefaultValue;
			@Nullable
			private ConfigSpec.BooleanValue configInstance = null;

			Booleans(
					ConfigGroup group,
					@Nullable String comment,
					String path,
					boolean defaultValue
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValue;
                Translation = "parcool.config.c." + path;
			}

			@Override
			public String getPath() {
				return Path;
			}

			@Override
			public void register(ConfigSpec.Builder builder) {
				if (Comment != null) {
					builder.comment(Comment);
				}
                builder.translation(Translation);
				configInstance = builder.define(Path, DefaultValue);
			}

			public Boolean get() {
				if (configInstance == null) return DefaultValue;
				return configInstance.get();
			}

			@Override
			public void set(Boolean value) {
				if (configInstance != null) {
					configInstance.set(value);
				}
			}

			public ConfigSpec.BooleanValue getInternalInstance() {
				return configInstance;
			}

			@Override
			public void writeToBuffer(ByteBuf buffer) {
				buffer.writeByte((byte) (get() ? 1 : 0));
			}

			@Override
			public Boolean readFromBuffer(ByteBuf buffer) {
				return buffer.readByte() != 0;
			}
		}

		public enum Integers implements Item<Integer> {
            AcceptableAngleOfWallJump(
                    ConfigGroup.Control, "acceptable walll angle of wall jump : `0` means you exactly opposite to wall, `180` allow you to wall jump for all angle",
                    "acceptable_angle_wall_jump", 110, 0, 180
            ),
            HorizontalOffsetOfStaminaHUD(
                    ConfigGroup.HUD, "horizontal offset of normal HUD",
                    "offset_h_stamina_hud", 3, 0, 100
            ),
            VerticalOffsetOfStaminaHUD(
                    ConfigGroup.HUD, "vertical offset of normal HUD",
                    "offset_v_stamina_hud", 3, 0, 100
			),
            HorizontalOffsetOfLightStaminaHUD(
                    ConfigGroup.HUD, "horizontal offset of light HUD",
                    "offset_h_light_hud", 0, -100, 100
            ),
            VerticalOffsetOfLightStaminaHUD(
                    ConfigGroup.HUD, "vertical offset of light HUD",
                    "offset_v_light_hud", 0, -100, 100
			),
			WallRunContinuableTick(
					ConfigGroup.Modifier, "How long you can do Horizontal Wall Run",
					"wall-run_continuable_tick", 25, Server.Integers.MaxWallRunContinuableTick.Min, Server.Integers.MaxWallRunContinuableTick.Max
			),
			SlidingContinuableTick(
					ConfigGroup.Modifier, "How long you can do Slide",
					"sliding_continuable_tick", 15, Server.Integers.MaxSlidingContinuableTick.Min, Server.Integers.MaxSlidingContinuableTick.Max
			),
			SuccessiveDodgeCoolTime(
					ConfigGroup.Control, "How long duration of dodge is deal as successive dodge",
					"successive_dodge_cool_time", 30, 0, Integer.MAX_VALUE
			),
			DodgeCoolTime(
					ConfigGroup.Control, "Cool time of Dodge action",
					"dodge_cool_time", Dodge.MAX_TICK, Dodge.MAX_TICK, Integer.MAX_VALUE
			),
			MaxSuccessiveDodgeCount(
					ConfigGroup.Control, "Max number of times of successive Dodge action",
					"successive_dodge_count", 3, 1, Integer.MAX_VALUE
			),
			JustTimeBreakfallTick(
					ConfigGroup.Control, "Window time of just time breakfall",
					"justtime_breakfall_tick",
					Server.Integers.MaxJustTimeBreakfallTick.DefaultValue,
					Server.Integers.MaxJustTimeBreakfallTick.Min,
					Server.Integers.MaxJustTimeBreakfallTick.Max
			),
			CoyoteTime(
					ConfigGroup.Control, "coyote time tick (how long tick player can behave like on ground after getting off ledge",
					"coyote_time", Server.Integers.MaxCoyoteTime
			);
			public final ConfigGroup Group;
			@Nullable
			public final String Comment;
			public final String Path;
			public final int DefaultValue;
            public final String Translation;
			public final int Min;
			public final int Max;
			@Nullable
			private ConfigSpec.IntValue configInstance = null;

			Integers(
					ConfigGroup group,
					@Nullable String comment,
					String path,
					int defaultValue,
					int min,
					int max
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValue;
				Min = min;
				Max = max;
                Translation = "parcool.config.c." + path;
			}

			Integers(
					ConfigGroup group,
					@Nullable String comment,
					String path,
					Server.Integers defaultValues
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValues.DefaultValue;
				Min = defaultValues.Min;
				Max = defaultValues.Max;
				Translation = "parcool.config.c." + path;
			}

			@Override
			public String getPath() {
				return Path;
			}

			public void register(ConfigSpec.Builder builder) {
				if (Comment != null) {
					builder.comment(Comment);
				}
                builder.translation(Translation);
				configInstance = builder.defineInRange(Path, DefaultValue, Min, Max);
			}

			@Override
			public Integer get() {
				if (configInstance == null) return DefaultValue;
				return configInstance.get();
			}

			@Override
			public void set(Integer value) {
				if (configInstance != null) {
					configInstance.set(value);
				}
			}

			public ConfigSpec.IntValue getInternalInstance() {
				return configInstance;
			}

			@Override
			public void writeToBuffer(ByteBuf buffer) {
				buffer.writeInt(get());
			}

			@Override
			public Integer readFromBuffer(ByteBuf buffer) {
				return buffer.readInt();
			}
		}

		public enum Doubles implements Item<Double> {
			FastRunSpeedModifier(
					ConfigGroup.Modifier, "FastRun speed modifier",
					"fast-run_modifier", 2, Server.Doubles.MaxFastRunSpeedModifier.Min, Server.Doubles.MaxFastRunSpeedModifier.Max
			),
			FastSwimSpeedModifier(
					ConfigGroup.Modifier, "FastSwim speed modifier",
					"fast-swim_modifier", 2, Server.Doubles.MaxFastSwimSpeedModifier.Min, Server.Doubles.MaxFastSwimSpeedModifier.Max
			),
			DodgeSpeedModifier(
					ConfigGroup.Modifier, "Dodge speed modifier",
					"dodge-speed_modifier", 1, Server.Doubles.MaxDodgeSpeedModifier.Min, Server.Doubles.MaxDodgeSpeedModifier.Max
			),
			SkyDiveSpeedDecreaseRate(
					ConfigGroup.Modifier, "SkyDive speed decreasement rate",
					"sky_dive-speed-decreasement", 0.98, Server.Doubles.MinSkyDiveSpeedDecreaseRate.Min, Server.Doubles.MinSkyDiveSpeedDecreaseRate.Max
			),
			LowestFallDistanceForBreakfall(
					ConfigGroup.Control, "Lowest fall distance needed to trigger breakfall movements",
					"lowest_fall_distance_for_breakfall", Server.Doubles.MinLowestFallDistanceForBreakfall
			),
			DamageCompleteRemovableHeightBreakfall(
					ConfigGroup.Control, "How long distance breakfall can remove damage",
					"max_breakfall_damage_remove_height", Server.Doubles.MaxDamageCompleteRemovableHeightBreakfall
			),
			DamageReductionRateBreakfall(
					ConfigGroup.Control, "Damage reduction rate of Breakfall",
					"max_breakfall_damage_reduction_rate", Server.Doubles.MaxDamageReductionRateBreakfall
			);
			public final ConfigGroup Group;
			@Nullable
			public final String Comment;
			public final String Path;
			public final double DefaultValue;
            public final String Translation;
			public final double Min;
			public final double Max;
			@Nullable
			private ConfigSpec.DoubleValue configInstance = null;

			Doubles(
					ConfigGroup group,
					@Nullable String comment,
					String path,
					double defaultValue,
					double min,
					double max
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValue;
				Min = min;
				Max = max;
                Translation = "parcool.config.c." + path;
			}

			Doubles(
					ConfigGroup group,
					@Nullable String comment,
					String path,
					ParCoolConfig.Server.Doubles defaultValues
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValues.DefaultValue;
				Min = defaultValues.Min;
				Max = defaultValues.Max;
				Translation = "parcool.config.c." + path;
			}

			@Override
			public String getPath() {
				return Path;
			}

			public void register(ConfigSpec.Builder builder) {
				if (Comment != null) {
					builder.comment(Comment);
				}
                builder.translation(Translation);
				configInstance = builder.defineInRange(Path, DefaultValue, Min, Max);
			}

			@Override
			public void writeToBuffer(ByteBuf buffer) {
				buffer.writeDouble(get());
			}

			@Override
			public Double readFromBuffer(ByteBuf buffer) {
				return buffer.readDouble();
			}

			@Override
			public Double get() {
				if (configInstance == null) return DefaultValue;
				return configInstance.get();
			}

			@Override
			public void set(Double value) {
				if (configInstance != null) {
					configInstance.set(value);
				}
			}

			@Nullable
			public ConfigSpec.DoubleValue getInternalInstance() {
				return configInstance;
			}
		}

		/**
		 * Fallbacks for the accessors below when the class is not in the hand-maintained index. They
		 * carry an empty path, so they never appear in the written config; the permissive default is
		 * what the arrays themselves hold for an action the user has not disabled, and the server-side
		 * vetoes in {@link ActionInfo#can} still apply.
		 */
		private static final ConfigSpec.BooleanValue UNKNOWN_ACTION_ENABLED =
				new ConfigSpec.BooleanValue(List.of(), true);
		private static final ConfigSpec.BooleanValue UNKNOWN_ANIMATOR_ENABLED =
				new ConfigSpec.BooleanValue(List.of(), true);
		private static final ConfigSpec.IntValue UNKNOWN_ACTION_CONSUMPTION =
				new ConfigSpec.IntValue(List.of(), 0, 0, Integer.MAX_VALUE);

		private final ConfigSpec.BooleanValue[] actionPossibilities = new ConfigSpec.BooleanValue[Actions.LIST.size()];
		private final ConfigSpec.BooleanValue[] animatorPossibilities = new ConfigSpec.BooleanValue[AnimatorList.ANIMATORS.size()];
		private final ConfigSpec.IntValue[] staminaConsumptions = new ConfigSpec.IntValue[Actions.LIST.size()];
		public final ConfigSpec.EnumValue<HUDType> StaminaHUDType;
		public final ConfigSpec.EnumValue<StaminaType> StaminaType;
		public final ConfigSpec.EnumValue<Vault.TypeSelectionMode> VaultAnimationMode;
		public final ConfigSpec.EnumValue<Position.Horizontal> AlignHorizontalStaminaHUD;
		public final ConfigSpec.EnumValue<Position.Vertical> AlignVerticalStaminaHUD;
		public final ConfigSpec.EnumValue<ColorTheme> GUIColorTheme;
		public final ConfigSpec.EnumValue<FastRun.ControlType> FastRunControl;
		public final ConfigSpec.EnumValue<Crawl.ControlType> CrawlControl;
		public final ConfigSpec.EnumValue<Flipping.ControlType> FlipControl;
		public final ConfigSpec.EnumValue<HorizontalWallRun.ControlType> HWallRunControl;
		public final ConfigSpec.EnumValue<WallJump.ControlType> WallJumpControl;
		public final ConfigSpec.EnumValue<ClingToCliff.ControlType> ClingToCliffControl;

		private static void register(ConfigSpec.Builder builder, ConfigGroup group) {
			Arrays.stream(Booleans.values()).filter(x -> x.Group == group).forEach(x -> x.register(builder));
			Arrays.stream(Integers.values()).filter(x -> x.Group == group).forEach(x -> x.register(builder));
			Arrays.stream(Doubles.values()).filter(x -> x.Group == group).forEach(x -> x.register(builder));
		}

		private Client(ConfigSpec.Builder builder) {
            builder.push("Possibility_of_Actions(Some_do_not_have_to_work)");
			{
				for (int i = 0; i < Actions.LIST.size(); i++) {
					actionPossibilities[i] = builder.define("can_" + Actions.LIST.get(i).getSimpleName(), true);
				}
			}
			builder.pop();
            builder.push("Stamina_HUD_Configuration");
			{
				StaminaHUDType = builder.defineEnum("stamina_hud_type", HUDType.Light);
				AlignHorizontalStaminaHUD = builder.comment("horizontal alignment").defineEnum("align_h_s_hud", Position.Horizontal.Right);
				AlignVerticalStaminaHUD = builder.comment("vertical alignment").defineEnum("align_v_s_hud", Position.Vertical.Bottom);
				register(builder, ConfigGroup.HUD);
			}
			builder.pop();
			builder.push("Animations");
			{
				builder.push("Animators");
				{
					for (int i = 0; i < AnimatorList.ANIMATORS.size(); i++) {
						animatorPossibilities[i] = builder.define("enable_" + AnimatorList.ANIMATORS.get(i).getSimpleName(), true);
					}
				}
				builder.pop();
				register(builder, ConfigGroup.Animation);
				register(builder, ConfigGroup.CameraAnimation);
			}
			builder.pop();
			builder.push("Control");
			{
				FastRunControl = builder.comment("Control of Fast Run").defineEnum("fast-run_control", FastRun.ControlType.PressKey);
				CrawlControl = builder.comment("Control of Crawl").defineEnum("crawl_control", Crawl.ControlType.PressKey);
                FlipControl = builder.comment("Control of Flipping").defineEnum("flip_control", Flipping.ControlType.TapMovementAndJump);
				HWallRunControl = builder.comment("Control of Horizontal Wall Run").defineEnum("h-wall-run_control", HorizontalWallRun.ControlType.PressKey);
				WallJumpControl = builder.comment("Control of Wall Jump").defineEnum("wall-jump_control", WallJump.ControlType.PressKey);
                ClingToCliffControl = builder.comment("Control of Cling To Cliff").defineEnum("cling-to-cliff_control", ClingToCliff.ControlType.PressKey);
				register(builder, ConfigGroup.Control);
			}
			builder.pop();
			builder.push("Modifier");
			{
				register(builder, ConfigGroup.Modifier);
			}
			builder.pop();
            builder.push("Other_Configuration");
			{
				VaultAnimationMode = builder.comment("Vault Animation(Dynamic is to select animation dynamically)").defineEnum("vault_animation_mode", Vault.TypeSelectionMode.Dynamic);
				GUIColorTheme = builder.comment("Color theme of Setting GUI").defineEnum("gui_color_theme", ColorTheme.Blue);
				register(builder, ConfigGroup.Other);
			}
			builder.pop();
			builder.push("Stamina");
			{
                builder.comment("Caution : Max stamina and stamina recovery config is removed because they became attributes.");
                StaminaType = builder.defineEnum("used_stamina", com.alrex.parcool.common.stamina.StaminaType.PARCOOL);
                register(builder, ConfigGroup.Stamina);
				builder.push("Consumption");
				{
					for (int i = 0; i < Actions.LIST.size(); i++) {
						staminaConsumptions[i]
								= builder.defineInRange(
								"stamina_consumption_of_" + Actions.LIST.get(i).getSimpleName(),
								Actions.ACTION_REGISTRIES.get(i).getDefaultStaminaConsumption(),
								0, 10000
						);
					}
				}
                builder.pop();
			}
			builder.pop();
		}
	}

	public static class Server {
		private static final Server instance;
		private static final ConfigSpec configSpec;

		public static Server getInstance() {
			return instance;
		}

		public static ConfigSpec getConfigSpec() {
			return configSpec;
		}

		static {
			var pair = new ConfigSpec.Builder().configure(Server::new);
			instance = pair.getLeft();
			configSpec = pair.getRight();
		}

		public enum Booleans implements Item<Boolean> {
			AllowInfiniteStamina(
					ConfigGroup.Stamina, "Permission of infinite stamina",
					"allow_infinite_stamina", true, true
			),
			AllowDisableWallJumpCooldown(
					ConfigGroup.Control, "Allow disabling cooldown of wall jump",
					"allow_disabling_wall_jump_cooldown", true, true
			),
            DodgeProvideInvulnerableFrame(
                    ConfigGroup.Other, "Enable invulnerable time by Dodge",
                    "enable_dodge_invulnerable_time", true, true
            );
			public final ConfigGroup Group;
			@Nullable
			public final String Comment;
			public final String Path;
			public final boolean DefaultValue;
            public final boolean AdvantageousValue;
			@Nullable
			private ConfigSpec.BooleanValue configInstance = null;

			Booleans(
					ConfigGroup group,
					@Nullable String comment,
					String path,
                    boolean defaultValue,
                    boolean advantageous
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValue;
                AdvantageousValue = advantageous;
			}

			@Override
			public String getPath() {
				return Path;
			}

			@Override
			public void register(ConfigSpec.Builder builder) {
				if (Comment != null) {
					builder.comment(Comment);
				}
				configInstance = builder.define(Path, DefaultValue);
			}

			public Boolean get() {
				if (configInstance == null) return DefaultValue;
				return configInstance.get();
			}

			@Override
			public void set(Boolean value) {
				if (configInstance != null) {
					configInstance.set(value);
				}
			}

			public ConfigSpec.BooleanValue getInternalInstance() {
				return configInstance;
			}

			@Override
			public void writeToBuffer(ByteBuf buffer) {
				buffer.writeByte((byte) (get() ? 1 : 0));
			}

			@Override
			public Boolean readFromBuffer(ByteBuf buffer) {
				return buffer.readByte() != 0;
			}
		}

		public enum Integers implements Item<Integer> {
			MaxStaminaLimit(
					ConfigGroup.Stamina, "Limitation of max stamina value",
                    "max_stamina_limit", Integer.MAX_VALUE, 300, Integer.MAX_VALUE, AdvantageousDirection.Higher
			),
			MaxStaminaRecovery(
					ConfigGroup.Stamina, "Limitation of max stamina recovery",
                    "max_stamina_recovery_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE, AdvantageousDirection.Higher
			),
			SuccessiveDodgeCoolTime(
					ConfigGroup.Control, "How long duration of dodge is deal as successive dodge",
                    "least_successive_dodge_cool_time", 0, 0, Integer.MAX_VALUE, AdvantageousDirection.Lower
			),
			DodgeCoolTime(
					ConfigGroup.Control, "Cool time of Dodge action",
                    "least_dodge_cool_time", Dodge.MAX_TICK, Dodge.MAX_TICK, Integer.MAX_VALUE, AdvantageousDirection.Lower
			),
			MaxSuccessiveDodgeCount(
					ConfigGroup.Control, "Max number of times of successive Dodge action",
                    "max_successive_dodge_count", Integer.MAX_VALUE, 1, Integer.MAX_VALUE, AdvantageousDirection.Higher
			),
			MaxWallRunContinuableTick(
					ConfigGroup.Modifier, "How long you can do Horizontal Wall Run",
					"wall-run_continuable_tick", 40, 15, 100, AdvantageousDirection.Higher
			),
			MaxSlidingContinuableTick(
					ConfigGroup.Modifier, "How long you can do Slide",
					"sliding_continuable_tick", 30, 10, 60, AdvantageousDirection.Higher
			),
			MaxJustTimeBreakfallTick(
					ConfigGroup.Control, "Window time of just time breakfall",
					"justtime_breakfall_tick", 5, 0, Integer.MAX_VALUE, AdvantageousDirection.Higher
			),
			MaxCoyoteTime(
					ConfigGroup.Control, "Max coyote time tick (how long tick player can behave like on ground after getting off ledge",
					"max_coyote_time", 3, 0, 20, AdvantageousDirection.Higher
			);
			public final ConfigGroup Group;
			@Nullable
			public final String Comment;
			public final String Path;
			public final int DefaultValue;
			public final int Min;
			public final int Max;
            public final AdvantageousDirection Advantageous;
			@Nullable
			private ConfigSpec.IntValue configInstance = null;

			Integers(
					ConfigGroup group,
					@Nullable String comment,
					String path,
					int defaultValue,
					int min,
                    int max,
                    AdvantageousDirection advantageous
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValue;
				Min = min;
				Max = max;
                Advantageous = advantageous;
			}

			@Override
			public String getPath() {
				return Path;
			}

			public void register(ConfigSpec.Builder builder) {
				if (Comment != null) {
					builder.comment(Comment);
				}
				configInstance = builder.defineInRange(Path, DefaultValue, Min, Max);
			}

			@Override
			public Integer get() {
				if (configInstance == null) return DefaultValue;
				return configInstance.get();
			}

			@Override
			public void set(Integer value) {
				if (configInstance != null) {
					configInstance.set(value);
				}
			}

			public ConfigSpec.IntValue getInternalInstance() {
				return configInstance;
			}

			@Override
			public void writeToBuffer(ByteBuf buffer) {
				buffer.writeInt(get());
			}

			@Override
			public Integer readFromBuffer(ByteBuf buffer) {
				return buffer.readInt();
			}
		}

		public enum Doubles implements Item<Double> {
			MaxFastRunSpeedModifier(
					ConfigGroup.Modifier, "FastRun speed modifier",
					"max_fast-run_modifier", 2, 0.001, 10, AdvantageousDirection.Higher
			),
			MaxFastSwimSpeedModifier(
					ConfigGroup.Modifier, "FastSwim speed modifier",
					"max_fast-swim_modifier", 2, 0.001, 10, AdvantageousDirection.Higher
			),
			MaxDodgeSpeedModifier(
					ConfigGroup.Modifier, "Dodge speed modifier",
					"max_dodge-speed_modifier", 1, 0.5, 3, AdvantageousDirection.Higher
			),
			MinSkyDiveSpeedDecreaseRate(
					ConfigGroup.Modifier, "SkyDive speed decrease rate",
					"min_sky-dive_speed_decrease", 0.98, 0.001, 1, AdvantageousDirection.Lower
			),
			MinLowestFallDistanceForBreakfall(
					ConfigGroup.Control, "Lowest fall distance needed to trigger breakfall movements",
					"lowest_fall_distance_for_breakfall", 2, 0, 10, AdvantageousDirection.Lower
			),
			MaxDamageCompleteRemovableHeightBreakfall(
					ConfigGroup.Control, "How long breakfall can remove damage",
					"max_breakfall_damage_remove_height", 6, 0, Double.MAX_VALUE, AdvantageousDirection.Higher
			),
			MaxDamageReductionRateBreakfall(
					ConfigGroup.Control, "Damage reduction rate of Breakfall",
					"max_breakfall_damage_reduction_rate", 0.6, 0, 1, AdvantageousDirection.Higher
			);
			public final ConfigGroup Group;
			@Nullable
			public final String Comment;
			public final String Path;
			public final double DefaultValue;
			public final double Min;
			public final double Max;
            public final AdvantageousDirection Advantageous;
			@Nullable
			private ConfigSpec.DoubleValue configInstance = null;

			Doubles(
					ConfigGroup group,
					@Nullable String comment,
					String path,
					double defaultValue,
					double min,
                    double max,
                    AdvantageousDirection advantageous
			) {
				Group = group;
				Comment = comment;
				Path = path;
				DefaultValue = defaultValue;
				Min = min;
				Max = max;
                Advantageous = advantageous;
			}

			@Override
			public String getPath() {
				return Path;
			}

			public void register(ConfigSpec.Builder builder) {
				if (Comment != null) {
					builder.comment(Comment);
				}
				configInstance = builder.defineInRange(Path, DefaultValue, Min, Max);
			}

			@Override
			public void writeToBuffer(ByteBuf buffer) {
				buffer.writeDouble(get());
			}

			@Override
			public Double readFromBuffer(ByteBuf buffer) {
				return buffer.readDouble();
			}

			@Override
			public Double get() {
				if (configInstance == null) return DefaultValue;
				return configInstance.get();
			}

			@Override
			public void set(Double value) {
				if (configInstance != null) {
					configInstance.set(value);
				}
			}

			@Nullable
			public ConfigSpec.DoubleValue getInternalInstance() {
				return configInstance;
			}
		}

		private final ConfigSpec.BooleanValue[] actionPermissions = new ConfigSpec.BooleanValue[Actions.LIST.size()];

		public boolean getPermissionOf(Class<? extends Action> action) {
			return actionPermissions[Actions.getIndexOf(action)].get();
		}

		private final ConfigSpec.IntValue[] leastStaminaConsumptions = new ConfigSpec.IntValue[Actions.LIST.size()];

		public int getLeastStaminaConsumptionOf(Class<? extends Action> action) {
			return leastStaminaConsumptions[Actions.getIndexOf(action)].get();
		}

		public final ConfigSpec.BooleanValue LimitationEnabled;
		public final ConfigSpec.EnumValue<StaminaType> StaminaType;

		private static void register(ConfigSpec.Builder builder, ConfigGroup group) {
			Arrays.stream(Server.Booleans.values()).filter(x -> x.Group == group).forEach(x -> x.register(builder));
			Arrays.stream(Server.Integers.values()).filter(x -> x.Group == group).forEach(x -> x.register(builder));
			Arrays.stream(Server.Doubles.values()).filter(x -> x.Group == group).forEach(x -> x.register(builder));
		}


		Server(ConfigSpec.Builder builder) {
			builder.push("Limitations");
			{
				LimitationEnabled = builder.comment("Whether these limitations will be imposed to players").define("limitation_imposed", false);
                builder.push("Action_Permissions");
				{
					for (int i = 0; i < Actions.LIST.size(); i++) {
						actionPermissions[i]
								= builder.define("permit_" + Actions.LIST.get(i).getSimpleName(), true);
					}
				}
				builder.pop();

				builder.push("Stamina");
				{
					StaminaType = builder.defineEnum("forced_stamina", com.alrex.parcool.common.stamina.StaminaType.NONE);
                    builder.push("Least_Consumption");
					{
						for (int i = 0; i < Actions.LIST.size(); i++) {
                            leastStaminaConsumptions[i] = builder.defineInRange(
									"stamina_consumption_of_" + Actions.LIST.get(i).getSimpleName(),
									Actions.ACTION_REGISTRIES.get(i).getDefaultStaminaConsumption(),
									0, 10000
							);
						}
					}
                    builder.pop();
					register(builder, ConfigGroup.Stamina);
				}
				builder.pop();
				builder.push("Control");
				{
					register(builder, ConfigGroup.Control);
				}
				builder.pop();
				builder.push("Modifier");
				{
					register(builder, ConfigGroup.Modifier);
				}
				builder.pop();
				// ConfigGroup.Other was never registered on the server side upstream, which left
				// DodgeProvideInvulnerableFrame permanently at its hard-coded default and made
				// set() a silent no-op even though the key is synced to clients and editable by
				// /parcool limitation. Registering the group makes the key real.
				builder.push("Other");
				{
					register(builder, ConfigGroup.Other);
				}
				builder.pop();
			}
			builder.pop();
		}
	}

	/**
	 * Loads both side-specific specs from {@code <config>/parcool-client.json} and
	 * {@code <config>/parcool-server.json}.
	 * <p>
	 * Was {@code ModContainer#registerConfig(Type.CLIENT/SERVER, ...)} plus FML's config loading
	 * lifecycle. Architectury has no config API, so the two specs are loaded explicitly here; the
	 * {@code Item#writeToBuffer}/{@code Item#readFromBuffer} methods (used by the client/server sync
	 * payloads) are unchanged.
	 */
	public static void load() {
		Path configDir = Platform.getConfigFolder();
		Path client = configDir.resolve(ParCool.MOD_ID + "-client.json");
		Path server = configDir.resolve(ParCool.MOD_ID + "-server.json");
		Client.getConfigSpec().load(client);
		Server.getConfigSpec().load(server);
		// First run: write both files so the user has a documented, editable starting point.
		if (!java.nio.file.Files.exists(client)) Client.getConfigSpec().save(client);
		if (!java.nio.file.Files.exists(server)) Server.getConfigSpec().save(server);
	}

	/** Writes both specs back; the settings screens call {@code ConfigValue#save()} per change. */
	public static void save() {
		Path configDir = Platform.getConfigFolder();
		Client.getConfigSpec().save(configDir.resolve(ParCool.MOD_ID + "-client.json"));
		Server.getConfigSpec().save(configDir.resolve(ParCool.MOD_ID + "-server.json"));
	}
}

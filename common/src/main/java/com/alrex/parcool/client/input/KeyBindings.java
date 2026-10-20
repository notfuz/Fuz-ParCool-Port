package com.alrex.parcool.client.input;

import com.alrex.parcool.compat.IKeyMappingDuck;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import com.alrex.parcool.utilities.VectorUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;

public class KeyBindings {


    /**
     * Resolved lazily, never in a static field: Fabric invokes the client entrypoint from inside
     * {@code Minecraft}'s constructor, so a class initialiser that runs at mod-init time would cache
     * a {@code null} Minecraft/Options and NPE on the first input poll.
     */
    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static Options options() {
        return Minecraft.getInstance().options;
    }
    private static final KeyMapping keyBindEnable = new KeyMapping("key.parcool.Enable", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.parcool");
	private static final KeyMapping keyBindCrawl = new KeyMapping("key.parcool.Crawl", GLFW.GLFW_KEY_C, "key.categories.parcool");
	private static final KeyMapping keyBindGrabWall = new KeyMapping("key.parcool.ClingToCliff", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.parcool");
	private static final KeyMapping keyBindBreakfall = new KeyMapping("key.parcool.Breakfall", GLFW.GLFW_KEY_R, "key.categories.parcool");
	private static final KeyMapping keyBindFastRunning = new KeyMapping("key.parcool.FastRun", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.parcool");
	private static final KeyMapping keyBindFlipping = new KeyMapping("key.parcool.Flipping", GLFW.GLFW_KEY_UNKNOWN, "key.categories.parcool");
	private static final KeyMapping keyBindVault = new KeyMapping("key.parcool.Vault", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.parcool");
	private static final KeyMapping keyBindDodge = new KeyMapping("key.parcool.Dodge", GLFW.GLFW_KEY_R, "key.categories.parcool");
    private static final KeyMapping keyBindRideZipline = new KeyMapping("key.parcool.RideZipline", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.parcool");
	private static final KeyMapping keyBindWallJump = new KeyMapping("key.parcool.WallJump", GLFW.GLFW_KEY_SPACE, "key.categories.parcool");
	private static final KeyMapping keyBindHangDown = new KeyMapping("key.parcool.HangDown", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.parcool");
	private static final KeyMapping keyBindWallSlide = new KeyMapping("key.parcool.WallSlide", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.parcool");
    private static final KeyMapping keyBindHideInBlock = new KeyMapping("key.parcool.HideInBlock", GLFW.GLFW_KEY_C, "key.categories.parcool");
	private static final KeyMapping keyBindHorizontalWallRun = new KeyMapping("key.parcool.HorizontalWallRun", GLFW.GLFW_KEY_R, "key.categories.parcool");
	private static final KeyMapping keyBindQuickTurn = new KeyMapping("key.parcool.QuickTurn", GLFW.GLFW_KEY_UNKNOWN, "key.categories.parcool");
	private static final KeyMapping keyBindOpenSettings = new KeyMapping("key.parcool.openSetting", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.parcool");
	private static final Vec3 forwardVector = new Vec3(0, 0, 1);

    /** All ParCool bindings, in registration order. */
    private static final KeyMapping[] ALL = {
            keyBindEnable, keyBindCrawl, keyBindGrabWall, keyBindBreakfall, keyBindFastRunning,
            keyBindDodge, keyBindRideZipline, keyBindWallSlide, keyBindWallJump, keyBindVault,
            keyBindHorizontalWallRun, keyBindHideInBlock, keyBindOpenSettings, keyBindQuickTurn,
            keyBindFlipping, keyBindHangDown
    };

    /** Every ParCool binding, for diagnostics and the register loop. */
    public static KeyMapping[] all() {
        return ALL;
    }

    /** Was NeoForge's patched {@code KeyMapping#getKey()}. */
    public static InputConstants.Key keyOf(KeyMapping mapping) {
        return ((IKeyMappingDuck) mapping).parcool$getKey();
    }

    /** True when both bindings resolve to the same physical key (the sneak-key conflict checks). */
    public static boolean isSameKey(KeyMapping a, KeyMapping b) {
        return keyOf(a).equals(keyOf(b));
    }

    /**
     * Ctrl+P / Alt+P equivalent on loaders without NeoForge's {@code KeyModifier}.
     * <p>
     * NeoForge's six-argument {@code KeyMapping} constructor attached a {@code KeyModifier} to the
     * binding itself, so vanilla's "is this key down" check only reported true while the modifier was
     * held. Vanilla has no such notion, so the modifier is sampled from the live window state and
     * combined with the raw physical key state. This keeps the upstream UX identical on both loaders.
     */
    private static boolean isMetaKeyDown(KeyMapping mapping, int modifier) {
        if (mc().screen != null) return false;
        InputConstants.Key key = keyOf(mapping);
        if (key.getType() == InputConstants.Type.MOUSE) return false;
        if (!isPollableKeysym(key)) return false;
        boolean modifierDown = switch (modifier) {
            case GLFW.GLFW_MOD_CONTROL -> Screen.hasControlDown();
            case GLFW.GLFW_MOD_ALT -> Screen.hasAltDown();
            default -> true;
        };
        if (!modifierDown) return false;
        return InputConstants.isKeyDown(mc().getWindow().getWindow(), key.getValue());
    }

    /** Ctrl + the enable binding. */
    public static boolean isKeyEnableParCoolDown() {
        return isMetaKeyDown(keyBindEnable, GLFW.GLFW_MOD_CONTROL);
    }

    /**
     * Physical key state of a ParCool binding.
     *
     * <h2>Why this exists</h2>
     * Minecraft 1.21.1 stores bindings in {@code KeyMapping.MAP}, a {@code Map<Key, KeyMapping>} with
     * <b>one entry per physical key</b>: {@code KeyMapping.set(key, state)} and
     * {@code KeyMapping.click(key)} look the key up in that map and only ever touch the single mapping
     * stored there. ParCool binds 16 keys that vanilla already owns — right mouse (five bindings),
     * {@code R} (three), {@code C} (two), {@code Space}, {@code P} (two) — so registering them makes
     * the <i>last</i> ParCool binding win the map entry and the vanilla mapping for that key stops
     * receiving press/release events. In practice that meant right-click no longer placed blocks,
     * space no longer jumped, and so on.
     *
     * <p>Reading the raw window state sidesteps the map entirely, so several bindings can share one
     * physical key and vanilla keeps working. The {@link net.minecraft.client.KeyMapping} objects
     * are still created and registered so the keys appear in the Controls screen and can be
     * rebound; they are only used for their name, category and bound key, never for their state.
     */
    /**
     * Whether {@code glfwGetKey} can be handed this keysym.
     *
     * <h2>Why this exists</h2>
     * An unbound ParCool action - Flipping and QuickTurn default to {@link GLFW#GLFW_KEY_UNKNOWN} -
     * carries the keysym {@code -1}. {@code glfwGetKey} rejects it: GLFW raises
     * {@code GLFW_INVALID_ENUM} ({@code 0x00010003}, which is the {@code 65539} in the log) with the
     * message {@code "Invalid key -1"}, and {@code Window#defaultErrorCallback} turns that into the
     * three-line "########## GL ERROR ##########" block. Both of the unbound bindings are polled on
     * every {@link KeyRecorder#onClientTick()}, so the game printed roughly forty of those blocks a
     * second for as long as a world was loaded.
     *
     * <p>GLFW's own bounds check rejects anything below {@link GLFW#GLFW_KEY_SPACE} (32) as well, so
     * the test is the range GLFW accepts rather than a special case for {@code -1}.
     */
    private static boolean isPollableKeysym(InputConstants.Key key) {
        return key.getValue() >= GLFW.GLFW_KEY_SPACE;
    }

    /**
     * Wall Jump has no key of its own by default, so it cannot clash with vanilla's jump key: while the
     * binding is unbound it follows the vanilla jump key. Binding it in Controls overrides that.
     */
    public static boolean isWallJumpDown() {
        InputConstants.Key wallJumpKey = keyOf(keyBindWallJump);
        if (wallJumpKey.getType() == InputConstants.Type.KEYSYM && wallJumpKey.getValue() == GLFW.GLFW_KEY_UNKNOWN) return Boolean.TRUE.equals(isKeyJumpDown());
        return isDown(keyBindWallJump);
    }

    public static boolean isDown(KeyMapping mapping) {
        // Bindings are polled from the raw window state, which keeps reporting a held letter while chat,
        // inventory or any other screen is open. Vanilla releases its keys when a screen opens; do the
        // same, so typing "u" or "k" in chat can never trigger an action.
        if (mc().screen != null) return false;
        InputConstants.Key key = keyOf(mapping);
        long window = mc().getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> isPollableKeysym(key) && InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            // A raw scancode binding cannot be polled through the keycode API; fall back to the
            // mapping's own state, which is correct as long as the key is not shared.
            case SCANCODE -> mapping.isDown();
        };
    }

    /** Alt + the settings binding. */
    public static boolean isKeyOpenSettingsDown() {
        return isMetaKeyDown(keyBindOpenSettings, GLFW.GLFW_MOD_ALT);
    }

	public static KeyMapping getKeySprint() {
		return options().keySprint;
	}

	public static Boolean isKeyJumpDown() {
		return mc().player != null
				&& mc().player.input != null
				&& mc().player.input.keyPresses.jump();
	}

	public static KeyMapping getKeySneak() {
		return options().keyShift;
	}

	public static Vec3 getCurrentMoveVector() {
		var player = Minecraft.getInstance().player;
		if (player == null) return Vec3.ZERO;
		var vector = player.input.getMoveVector();
		if (VectorUtil.isZero(vector)) return Vec3.ZERO;
		double length = vector.length();
		return new Vec3(vector.x / length, 0, vector.y / length);
	}

	public static Vec3 getForwardVector() {
		return forwardVector;
	}

	public static Boolean isAnyMovingKeyDown() {
		return mc().player != null
				&& mc().player.input != null
				&& (mc().player.input.keyPresses.left()
				|| mc().player.input.keyPresses.right()
				|| mc().player.input.keyPresses.forward()
				|| mc().player.input.keyPresses.backward());
	}

	public static Boolean isLeftAndRightDown() {
		return mc().player != null && mc().player.input != null
				&& mc().player.input.keyPresses.left() && mc().player.input.keyPresses.right();
	}

	public static Boolean isKeyForwardDown() {
		return mc().player != null && mc().player.input != null && mc().player.input.keyPresses.forward();
	}

	public static Boolean isKeyLeftDown() {
		return mc().player != null && mc().player.input != null && mc().player.input.keyPresses.left();
	}

	public static Boolean isKeyRightDown() {
		return mc().player != null && mc().player.input != null && mc().player.input.keyPresses.right();
	}

	public static Boolean isKeyBackDown() {
		return mc().player != null && mc().player.input != null && mc().player.input.keyPresses.backward();
	}

    public static KeyMapping getKeyBindEnable() {
        return keyBindEnable;
    }

	public static KeyMapping getKeyCrawl() {
		return keyBindCrawl;
	}

	public static KeyMapping getKeyQuickTurn() {
		return keyBindQuickTurn;
	}

	public static KeyMapping getKeyGrabWall() {
		return keyBindGrabWall;
	}

	public static KeyMapping getKeyVault() {
		return keyBindVault;
	}

	public static KeyMapping getKeyActivateParCool() {
		return keyBindOpenSettings;
	}

	public static KeyMapping getKeyBreakfall() {
		return keyBindBreakfall;
	}

	public static KeyMapping getKeyFastRunning() {
		return keyBindFastRunning;
	}

	public static KeyMapping getKeyDodge() {
		return keyBindDodge;
	}

    public static KeyMapping getKeyRideZipline() {
        return keyBindRideZipline;
    }

	public static KeyMapping getKeyWallSlide() {
		return keyBindWallSlide;
	}

	public static KeyMapping getKeyHangDown() {
		return keyBindHangDown;
	}

    public static KeyMapping getKeyHideInBlock() {
        return keyBindHideInBlock;
    }

	public static KeyMapping getKeyHorizontalWallRun() {
		return keyBindHorizontalWallRun;
	}

	public static KeyMapping getKeyWallJump() {
		return keyBindWallJump;
	}

	public static KeyMapping getKeyFlipping() {
		return keyBindFlipping;
	}

	/**
	 * Was {@code RegisterKeyMappingsEvent}. {@link KeyMappingRegistry} is the Architectury wrapper.
	 * <p>
	 * The two "meta" bindings (enable / open settings) used NeoForge's six-argument
	 * {@code KeyMapping} constructor, which carries a {@code KeyConflictContext} and a
	 * {@code KeyModifier} (Ctrl / Alt). Neither exists on Fabric, so those two are registered with the
	 * vanilla constructor and the modifier is checked at press time instead — see
	 * {@link #isKeyActivateParCool()} and {@link #isKeyOpenSettingsPressed()}.
	 */
	public static void register() {
		for (KeyMapping mapping : ALL) {
			KeyMappingRegistry.register(mapping);
		}
		restoreVanillaBindings();
	}

    /**
     * Hands every key ParCool shares with vanilla back to the other mapping.
     *
     * <p>Minecraft 1.21.1 stores bindings in {@code KeyMapping.MAP} with exactly one
     * {@link KeyMapping} per physical key, and every {@code new KeyMapping(...)} overwrites that
     * entry. ParCool binds 16 keys vanilla also owns (the right mouse button five times, {@code R}
     * three times, {@code C} twice, plus {@code Space}, {@code P} and left-control), so registering
     * them can evict {@code keyUse} / {@code keySprint} from the map — and a mapping that is not in
     * the map never receives press or release events. That is exactly why right-click stopped
     * placing blocks and left-control stopped sprinting; unbinding ParCool's keys in the Controls
     * screen restored vanilla, which confirms the diagnosis.
     *
     * <p>Called both right after registration and once more on the first client tick, because on
     * Fabric the client entrypoint runs before {@code Options} exists, so vanilla's mappings are
     * still created after us.
     *
     * <h4>Why reflection instead of a {@code @Accessor} mixin</h4>
     * The two fields are read reflectively because they are not the same on both loaders. NeoForge
     * replaces {@code KeyMapping#MAP}'s {@code Map<Key, KeyMapping>} with its own
     * {@code KeyMappingLookup}, which already allows several mappings per physical key - so the whole
     * repair is a no-op there, while a shared mixin declaring {@code Map} for that field aborts the
     * whole NeoForge client with
     * {@code InvalidAccessorException: No candidates were found matching MAP:Ljava/util/Map;}.
     */
    public static void restoreVanillaBindings() {
        Object map = readStaticMap(BINDING_MAP);
        Object all = readStaticMap(ALL_BINDINGS);
        if (map == null || all == null) {
            // Expected on NeoForge; on Fabric it means the field layout changed again.
            com.alrex.parcool.ParCool.LOGGER.debug("[parcool] KeyMapping has no single-mapping-per-key table on this loader; shared keys need no repair");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<InputConstants.Key, KeyMapping> bindingMap = (Map<InputConstants.Key, KeyMapping>) map;
        @SuppressWarnings("unchecked")
        Map<String, KeyMapping> everyMapping = (Map<String, KeyMapping>) all;

        int restored = 0;
        for (KeyMapping mapping : ALL) {
            InputConstants.Key key = keyOf(mapping);
            KeyMapping owner = bindingMap.get(key);
            if (owner != null && !isParCool(owner)) continue;

            for (KeyMapping candidate : everyMapping.values()) {
                if (isParCool(candidate)) continue;
                if (keyOf(candidate).equals(key)) {
                    bindingMap.put(key, candidate);
                    restored++;
                    break;
                }
            }
        }
        com.alrex.parcool.ParCool.LOGGER.debug("[parcool] vanilla bindings restored on {} shared keys", restored);
    }

    /** {@code KeyMapping.MAP} — which single {@link KeyMapping} currently receives events for a key. */
    private static final Field BINDING_MAP = findStaticMapField(InputConstants.Key.class);
    /** {@code KeyMapping.ALL} — every registered mapping, by name. */
    private static final Field ALL_BINDINGS = findStaticMapField(String.class);

    /**
     * Finds the static {@code Map<keyType, KeyMapping>} field of {@link KeyMapping} by its
     * <em>type</em>, never by its name.
     *
     * <h4>Why not {@code getDeclaredField("MAP")}</h4>
     * A released Fabric client runs Minecraft with intermediary names ({@code field_1657} and so on);
     * the readable names {@code MAP} / {@code ALL} exist only in a development environment. A lookup by
     * name therefore succeeds in {@code runClient} and returns {@code null} in a real install, which
     * silently turned {@link #restoreVanillaBindings()} into a no-op and left ParCool owning the right
     * mouse button (so eating and block placing stopped working). Generic type arguments survive
     * remapping, so they identify the two tables under any mapping set.
     */
    private static Field findStaticMapField(Class<?> keyType) {
        Field contentMatch = null;
        for (Field field : KeyMapping.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (!Map.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
            } catch (RuntimeException e) {
                continue;
            }
            if (field.getGenericType() instanceof java.lang.reflect.ParameterizedType generic) {
                java.lang.reflect.Type[] args = generic.getActualTypeArguments();
                if (args.length == 2 && args[0] == keyType && args[1] == KeyMapping.class) return field;
            }
            // No usable generic signature: fall back to looking at what the table actually holds.
            if (contentMatch == null && holdsKeysOf(field, keyType)) contentMatch = field;
        }
        return contentMatch;
    }

    private static boolean holdsKeysOf(Field field, Class<?> keyType) {
        try {
            Object value = field.get(null);
            if (!(value instanceof Map<?, ?> map) || map.isEmpty()) return false;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                return keyType.isInstance(entry.getKey()) && entry.getValue() instanceof KeyMapping;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return false;
    }

    private static Object readStaticMap(Field field) {
        if (field == null) return null;
        try {
            Object value = field.get(null);
            return value instanceof Map<?, ?> ? value : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static boolean isParCool(KeyMapping mapping) {
        return mapping.getName().startsWith("key.parcool.");
    }
}

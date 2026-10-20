package com.alrex.parcool.common.data.client;

import com.alrex.parcool.api.unstable.animation.AnimationOption;
import com.alrex.parcool.api.unstable.animation.AnimationPart;
import com.alrex.parcool.api.unstable.animation.ParCoolAnimationInfoEvent;
import com.alrex.parcool.client.animation.Animator;
import com.alrex.parcool.client.animation.PassiveCustomAnimation;
import com.alrex.parcool.client.animation.PlayerModelRotator;
import com.alrex.parcool.client.animation.PlayerModelTransformer;
import com.alrex.parcool.common.data.client.ClientDataKeys;
import com.alrex.parcool.common.data.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.api.event.ParCoolEventBus;
import com.alrex.parcool.common.event.CompatEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public class Animation {

	public static Animation get(Player player) {
		return ClientDataKeys.getAnimation(player);
	}

	private Animator animator = null;
    private AnimationOption option = new AnimationOption();
	private final PassiveCustomAnimation passiveAnimation = new PassiveCustomAnimation();

	public void setAnimator(Animator animator) {
		if (!ParCoolConfig.Client.Booleans.EnableAnimation.get()) return;
		if (!ParCoolConfig.Client.getInstance().canAnimate(animator.getClass()).get()) return;
		this.animator = animator;
	}

	/**
	 * Builds the animator and hands it to {@link #setAnimator(Animator)}.
	 *
	 * <p>The {@link com.alrex.parcool.common.action.Action} implementations live in {@code common} and
	 * are therefore loaded - and linked, i.e. verified - on a dedicated server, where
	 * {@code Parkourability} instantiates all of them. Verification resolves the target of every
	 * {@code new}, so a {@code new ChargeJumpAnimator()} inside {@code ChargeJump} drags the whole
	 * client-only animation package - and through it {@code LocalPlayer} - into the server, which dies
	 * with "Attempted to load class net.minecraft.client.player.LocalPlayer which is not present on
	 * the dedicated server". A {@code ldc} of a class constant is <i>not</i> resolved during
	 * verification, so the action implementations pass the animator class and this client-only class
	 * does the construction. Same animator, same constructor arguments, same result.
	 *
	 * <p>Every animator this is called with is a {@code public} class with a {@code public} constructor
	 * of the matching arity, which is what {@link Class#getConstructors()} returns; a mismatch is a
	 * hard error here rather than a silently skipped animation.
	 */
	public void setAnimator(Class<? extends Animator> type, Object... args) {
		setAnimator(instantiate(type, args));
	}

	private static Animator instantiate(Class<? extends Animator> type, Object[] args) {
		for (var constructor : type.getConstructors()) {
			if (constructor.getParameterCount() != args.length) continue;
			try {
				return type.cast(constructor.newInstance(args));
			} catch (ReflectiveOperationException | IllegalArgumentException e) {
				throw new IllegalStateException("Could not create the animator " + type.getName(), e);
			}
		}
		throw new IllegalStateException(
				"No constructor of " + type.getName() + " takes " + args.length + " arguments");
	}

	public boolean animatePre(Player player, PlayerModelTransformer modelTransformer) {
		Parkourability parkourability = Parkourability.get(player);
        if (animator != null && animator.shouldRemoved(player, parkourability)) animator = null;
        if (animator == null) return false;
        modelTransformer.setOption(option);
		if (shouldCancelAnimation(player)) return false;
		return animator.animatePre(player, parkourability, modelTransformer);
	}

	public void animatePost(Player player, PlayerModelTransformer modelTransformer) {
		Parkourability parkourability = Parkourability.get(player);
		if (shouldCancelAnimation(player)) return;
		if (animator == null) {
			passiveAnimation.animate(player, parkourability, modelTransformer);
			return;
		}
		animator.animatePost(player, parkourability, modelTransformer);
	}

	public boolean rotatePre(AbstractClientPlayer player, PlayerModelRotator rotator) {
		Parkourability parkourability = Parkourability.get(player);
		if (animator != null && animator.shouldRemoved(player, parkourability)) animator = null;
		if (animator == null) return false;
		if (shouldCancelAnimation(player) || option.isCanceled(AnimationPart.ROTATION)) return false;
		return animator.rotatePre(player, parkourability, rotator);
	}

    public void rotatePost(AbstractClientPlayer player, PlayerModelRotator rotator) {
		Parkourability parkourability = Parkourability.get(player);
		if (shouldCancelAnimation(player) || option.isCanceled(AnimationPart.ROTATION)) return;
		if (animator == null) {
			passiveAnimation.rotate(player, parkourability, rotator);
			return;
		}
		animator.rotatePost(player, parkourability, rotator);
	}

    public void cameraSetup(CompatEvents.ViewportEvent.ComputeCameraAngles event, LocalPlayer player, Parkourability parkourability) {
		if (animator == null) return;
		if (option.isCanceled(AnimationPart.CAMERA)) return;
		if (animator.shouldRemoved(player, parkourability)) {
			animator = null;
			return;
		}
		animator.onCameraSetUp(event, player, parkourability);
	}

    public void tick(AbstractClientPlayer player, Parkourability parkourability) {
		passiveAnimation.tick(player, parkourability);
		if (animator != null) {
            animator.tick(player);
		}
	}

	public void onRenderTick(CompatEvents.RenderFrameEvent event, Player player, Parkourability parkourability) {
		if (animator != null) {
			animator.onRenderTick(event, player, parkourability);
		}
        if (event instanceof CompatEvents.RenderFrameEvent.Pre) {
            updateAnimationInfo((AbstractClientPlayer) player);
        }
	}

	public void updateAnimationInfo(AbstractClientPlayer player) {
		ParCoolAnimationInfoEvent animationEvent = new ParCoolAnimationInfoEvent(player, animator);
		ParCoolEventBus.post(animationEvent);
		option = animationEvent.getOption();
	}

	public boolean shouldCancelAnimation(Player player) {
		if (player.isLocalPlayer()
				&& Minecraft.getInstance().options.getCameraType().isFirstPerson()
				&& !ParCoolConfig.Client.Booleans.EnableFPVAnimation.get()
		) {
			return true;
		}
		return this.option.isAnimationCanceled();
	}

	public boolean hasAnimator() {
		return animator != null;
	}

	public void removeAnimator() {
		animator = null;
	}
}

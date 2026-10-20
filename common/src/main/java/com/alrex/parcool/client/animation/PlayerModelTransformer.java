package com.alrex.parcool.client.animation;

import com.alrex.parcool.api.unstable.animation.AnimationOption;
import com.alrex.parcool.api.unstable.animation.AnimationPart;
import com.alrex.parcool.utilities.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Using Radians
 */
public class PlayerModelTransformer {
	private final Player player;
	private final PlayerModel model;
	private final float ageInTicks;
	private final float limbSwing;
	private final float limbSwingAmount;
	private final float netHeadYaw;
	private final float headPitch;
	/**
	 * 1.21.4 keeps the attack time and the two arm poses on {@code ArmedEntityRenderState} instead of
	 * on {@code PlayerModel}, so the three values the animators used to read off the model are handed
	 * to the transformer alongside the pose floats. Same values, same frame.
	 */
	private final float attackTime;
	private final HumanoidModel.ArmPose leftArmPose;
	private final HumanoidModel.ArmPose rightArmPose;
    private AnimationOption option = new AnimationOption();

	public float getPartialTick() {
		// The `false` overload, the same one PlayerRendererMixin uses: two different partial ticks in one
		// frame made the limbs (transformer) and the body rotation (rotator) interpolate against
		// different clocks, and the model visibly came apart on lag spikes and while the game is paused.
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	public float getHeadPitch() {
		return headPitch;
	}

	public float getNetHeadYaw() {
		return netHeadYaw;
	}

	public float getLimbSwing() {
		return limbSwing;
	}

	public float getLimbSwingAmount() {
		return limbSwingAmount;
	}

	public PlayerModel getRawModel() {
		return model;
	}

	/** Was {@code PlayerModel#attackTime}. */
	public float getAttackTime() {
		return attackTime;
	}

	/** Was {@code PlayerModel#leftArmPose}. */
	public HumanoidModel.ArmPose getLeftArmPose() {
		return leftArmPose;
	}

	/** Was {@code PlayerModel#rightArmPose}. */
	public HumanoidModel.ArmPose getRightArmPose() {
		return rightArmPose;
	}

	public PlayerModelTransformer(
			Player player,
			PlayerModel model,
			boolean slim,
			float ageInTicks,
			float limbSwing,
			float limbSwingAmount,
			float netHeadYaw,
			float headPitch,
			float attackTime,
			HumanoidModel.ArmPose leftArmPose,
			HumanoidModel.ArmPose rightArmPose
	) {
		this.player = player;
		this.model = model;
		this.ageInTicks = ageInTicks;
		this.limbSwing = limbSwing;
		this.limbSwingAmount = limbSwingAmount;
		this.netHeadYaw = netHeadYaw;
		this.headPitch = headPitch;
		this.attackTime = attackTime;
		this.leftArmPose = leftArmPose;
		this.rightArmPose = rightArmPose;
	}

    public void setOption(AnimationOption option) {
        this.option = option;
    }

	/**
	 * @param angleX swing arm frontward or backward
	 * @param angleY rotate arm around
	 * @param angleZ swing arm upward or downward
	 */
	public PlayerModelTransformer rotateRightArm(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.RIGHT_ARM)) return this;
		var rightArm = model.rightArm;
		if (rightArm.visible) {
			setRotations(rightArm, angleX, angleY, angleZ);
		}
		return this;
	}

	public PlayerModelTransformer rotateRightArm(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.RIGHT_ARM)) return this;
		var rightArm = model.rightArm;
		if (rightArm.visible) {
			setRotations(rightArm,
					MathUtil.lerp(rightArm.xRot, angleX, factor),
					MathUtil.lerp(rightArm.yRot, angleY, factor),
					MathUtil.lerp(rightArm.zRot, angleZ, factor)
			);
		}
		return this;
	}

	/**
	 * @param angleX swing arm frontward or backward
	 * @param angleY rotate arm around
	 * @param angleZ swing arm upward or downward
	 */
	public PlayerModelTransformer rotateLeftArm(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.LEFT_ARM)) return this;
		var leftArm = model.leftArm;
		if (leftArm.visible) {
			setRotations(leftArm, angleX, angleY, angleZ);
		}
		return this;
	}

	public PlayerModelTransformer rotateLeftArm(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.LEFT_ARM)) return this;
		var leftArm = model.leftArm;
		if (leftArm.visible) {
			setRotations(leftArm,
					MathUtil.lerp(leftArm.xRot, angleX, factor),
					MathUtil.lerp(leftArm.yRot, angleY, factor),
					MathUtil.lerp(leftArm.zRot, angleZ, factor)
			);
		}
		return this;
	}

	/**
	 * @param angleX swing leg frontward or backward
	 * @param angleY rotate leg around
	 * @param angleZ swing leg upward or downward
	 */
	public PlayerModelTransformer rotateRightLeg(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
		var rightLeg = model.rightLeg;
		if (rightLeg.visible) {
			setRotations(rightLeg, angleX, angleY, angleZ);
		}
		return this;
	}

	public PlayerModelTransformer rotateRightLeg(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
		var rightLeg = model.rightLeg;
		if (rightLeg.visible) {
			setRotations(rightLeg,
					MathUtil.lerp(rightLeg.xRot, angleX, factor),
					MathUtil.lerp(rightLeg.yRot, angleY, factor),
					MathUtil.lerp(rightLeg.zRot, angleZ, factor)
			);
		}
		return this;
	}

	/**
	 * @param angleX swing leg frontward or backward
	 * @param angleY rotate leg around
	 * @param angleZ swing leg upward or downward
	 */
	public PlayerModelTransformer rotateLeftLeg(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
		var leftLeg = model.leftLeg;
		if (leftLeg.visible) {
			setRotations(leftLeg, angleX, angleY, angleZ);
		}
		return this;
	}

	public PlayerModelTransformer rotateLeftLeg(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
		var leftLeg = model.leftLeg;
		if (leftLeg.visible) {
			setRotations(leftLeg,
					MathUtil.lerp(leftLeg.xRot, angleX, factor),
					MathUtil.lerp(leftLeg.yRot, angleY, factor),
					MathUtil.lerp(leftLeg.zRot, angleZ, factor)
			);
		}
		return this;
	}

	public PlayerModelTransformer addRotateRightArm(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.RIGHT_ARM)) return this;
		var arm = model.rightArm;
		if (arm.visible) {
			setRotations(arm, arm.xRot + angleX, arm.yRot + angleY, arm.zRot + angleZ);
		}
		return this;
	}

	public PlayerModelTransformer addRotateLeftArm(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.LEFT_ARM)) return this;
		var arm = model.leftArm;
		if (arm.visible) {
			setRotations(arm, arm.xRot + angleX, arm.yRot + angleY, arm.zRot + angleZ);
		}
		return this;
	}

	public PlayerModelTransformer addRotateRightLeg(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
		var leg = model.rightLeg;
		if (leg.visible) {
			setRotations(leg, leg.xRot + angleX, leg.yRot + angleY, leg.zRot + angleZ);
		}
		return this;
	}

	public PlayerModelTransformer addRotateLeftLeg(float angleX, float angleY, float angleZ) {
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
		var leg = model.leftLeg;
		if (leg.visible) {
			setRotations(leg, leg.xRot + angleX, leg.yRot + angleY, leg.zRot + angleZ);
		}
		return this;
	}

    public PlayerModelTransformer addRotateRightArm(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.RIGHT_ARM)) return this;
        return addRotateRightArm(angleX * factor, angleY * factor, angleZ * factor);
    }

    public PlayerModelTransformer addRotateLeftArm(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.LEFT_ARM)) return this;
        return addRotateLeftArm(angleX * factor, angleY * factor, angleZ * factor);
    }

    public PlayerModelTransformer addRotateRightLeg(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
        return addRotateRightLeg(angleX * factor, angleY * factor, angleZ * factor);
    }

    public PlayerModelTransformer addRotateLeftLeg(float angleX, float angleY, float angleZ, float factor) {
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
        return addRotateLeftLeg(angleX * factor, angleY * factor, angleZ * factor);
    }

	public PlayerModelTransformer makeArmsNatural() {
        if (option.isCanceled(AnimationPart.RIGHT_ARM)) return this;
        if (option.isCanceled(AnimationPart.LEFT_ARM)) return this;
		AnimationUtils.bobArms(model.rightArm, model.leftArm, ageInTicks);
		return this;
	}

	public PlayerModelTransformer makeLegsMoveDynamically(float factor) {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
		model.rightLeg.zRot += Mth.cos(ageInTicks * 0.56F) * 0.8F * factor + 0.05F;
		model.leftLeg.zRot -= Mth.cos(ageInTicks * 0.56F) * 0.8F * factor + 0.05F;
		model.rightLeg.xRot += Mth.sin(ageInTicks * 0.56F) * 0.8F * factor;
		model.leftLeg.xRot -= Mth.sin(ageInTicks * 0.56F) * 0.8F * factor;
		return this;
	}

	public PlayerModelTransformer makeArmsMoveDynamically(float factor) {
        if (option.isCanceled(AnimationPart.RIGHT_ARM)) return this;
        if (option.isCanceled(AnimationPart.LEFT_ARM)) return this;
		model.rightArm.zRot += Mth.cos(ageInTicks * 0.56F) * 0.8F * factor + 0.05F;
		model.leftArm.zRot -= Mth.cos(ageInTicks * 0.56F) * 0.8F * factor + 0.05F;
		model.rightArm.xRot += Mth.sin(ageInTicks * 0.56F) * 0.8F * factor;
		model.leftArm.xRot -= Mth.sin(ageInTicks * 0.56F) * 0.8F * factor;
		return this;
	}

	public PlayerModelTransformer makeLegsLittleMoving() {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
		AnimationUtils.bobArms(model.rightLeg, model.leftLeg, ageInTicks);
		return this;
	}

	public PlayerModelTransformer makeLegsShakingDynamically(float factor) {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
		model.rightLeg.zRot += Mth.cos(ageInTicks * 0.56F) * 0.8F * factor + 0.05F;
		model.leftLeg.zRot += Mth.cos(ageInTicks * 0.56F) * 0.8F * factor + 0.05F;
		model.rightLeg.xRot += Mth.sin(ageInTicks * 0.56F) * 0.2F * factor;
		model.leftLeg.xRot -= Mth.sin(ageInTicks * 0.56F) * 0.2F * factor;
		return this;
	}

	public PlayerModelTransformer rotateAdditionallyHeadPitch(float pitchDegree) {
        if (option.isCanceled(AnimationPart.HEAD)) return this;
		model.head.xRot = (float) Math.toRadians(pitchDegree + headPitch);
		return this;
	}

	public PlayerModelTransformer rotateHeadPitch(float pitchDegree) {
        if (option.isCanceled(AnimationPart.HEAD)) return this;
		model.head.xRot = (float) Math.toRadians(pitchDegree);
		return this;
	}

    public PlayerModelTransformer rotateHeadYaw(float yawDegree) {
        if (option.isCanceled(AnimationPart.HEAD)) return this;
        model.head.yRot = (float) Math.toRadians(yawDegree);
        return this;
    }

    public PlayerModelTransformer rotateHeadYawRadian(float yawRadian) {
        if (option.isCanceled(AnimationPart.HEAD)) return this;
        model.head.yRot = yawRadian;
        return this;
    }

	public PlayerModelTransformer rotateAdditionallyHeadYaw(float yawDegree) {
        if (option.isCanceled(AnimationPart.HEAD)) return this;
		model.head.yRot = (float) Math.toRadians(yawDegree + netHeadYaw);
		return this;
	}

    public PlayerModelTransformer rotateAdditionallyHeadRoll(float rollDegree) {
        if (option.isCanceled(AnimationPart.HEAD)) return this;
        // netHeadYaw is the *yaw*: adding it here made every roll swing the head sideways as well
        // (SpeedVault, VerticalWallRun, HorizontalWallRun, Sliding). Vanilla keeps head.zRot at 0, so
        // "additionally" means "add to zero" here too, exactly like rotateAdditionallyHeadPitch.
        model.head.zRot = (float) Math.toRadians(rollDegree);
        return this;
    }

    public PlayerModelTransformer translateRightArm(float xOffset, float yOffset, float zOffset) {
        if (option.isCanceled(AnimationPart.RIGHT_ARM)) return this;
        getRawModel().rightArm.x += xOffset;
        getRawModel().rightArm.y += yOffset;
        getRawModel().rightArm.z += zOffset;
        return this;
    }

    public PlayerModelTransformer translateLeftArm(float xOffset, float yOffset, float zOffset) {
        if (option.isCanceled(AnimationPart.LEFT_ARM)) return this;
        getRawModel().leftArm.x += xOffset;
        getRawModel().leftArm.y += yOffset;
        getRawModel().leftArm.z += zOffset;
        return this;
    }

    public PlayerModelTransformer translateRightLeg(float xOffset, float yOffset, float zOffset) {
        if (option.isCanceled(AnimationPart.RIGHT_LEG)) return this;
        getRawModel().rightLeg.x += xOffset;
        getRawModel().rightLeg.y += yOffset;
        getRawModel().rightLeg.z += zOffset;
        return this;
    }

    public PlayerModelTransformer translateLeftLeg(float xOffset, float yOffset, float zOffset) {
        if (option.isCanceled(AnimationPart.LEFT_LEG)) return this;
        getRawModel().leftLeg.x += xOffset;
        getRawModel().leftLeg.y += yOffset;
        getRawModel().leftLeg.z += zOffset;
        return this;
    }

    public PlayerModelTransformer translateHead(float xOffset, float yOffset, float zOffset) {
        if (option.isCanceled(AnimationPart.HEAD)) return this;
        getRawModel().head.x += xOffset;
        getRawModel().head.y += yOffset;
        getRawModel().head.z += zOffset;
        return this;
    }

    public void end() {
    }

	/**
	 * Puts the second skin layer (jacket, hat, sleeves, pants) on the limbs.
	 *
	 * <h2>Why this no longer copies the body part's transform</h2>
	 * In 1.21.1 every model part was rendered separately, so the sleeve had to be given the arm's
	 * transform by hand - that is exactly what {@code ModelPart#copyFrom} was for, and what this method
	 * did. 1.21.4 turned the model into a tree: {@code left_sleeve} / {@code right_sleeve} /
	 * {@code left_pants} / {@code right_pants} / {@code jacket} are <i>children</i> of the limb (see
	 * {@code PlayerModel}'s constructor) and {@code ModelPart#render} recurses into
	 * {@link ModelPart#children} after applying the parent's own transform. The layer therefore already
	 * follows the limb, and copying the parent's transform onto it as well applies that transform
	 * twice - the sleeve ends up at double the arm's rotation and offset, i.e. a second set of arms
	 * floating beside the real ones. Vanilla 1.21.4 therefore leaves the layer at its initial
	 * {@code PartPose.ZERO}, and so does this: the layer is reset to identity and inherits the limb.
	 * The visible result is the same as 1.21.1 - the layer sits exactly on the limb.
	 */
	public void copyFromBodyToWear() {
		resetSecondLayer();
	}

	/** The layer parts back to their initial (identity) pose, so the tree transform is the only one. */
	private void resetSecondLayer() {
		resetModel(model.jacket);
		resetModel(model.hat);
		resetModel(model.leftSleeve);
		resetModel(model.rightSleeve);
		resetModel(model.leftPants);
		resetModel(model.rightPants);
	}

	private void setRotations(ModelPart renderer, float angleX, float angleY, float angleZ) {
		renderer.xRot = angleX;
		renderer.yRot = angleY;
		renderer.zRot = angleZ;
	}

	public void reset() {
		resetModel(model.head);
		resetModel(model.body);
		{
			resetModel(model.rightArm);
            model.rightArm.x = -5.0F;
            model.rightArm.y = 2.0F;
			model.rightArm.z = 0.0F;
		}
		{
			resetModel(model.leftArm);
            model.leftArm.x = 5.0F;
            model.leftArm.y = 2.0F;
			model.leftArm.z = 0.0F;
		}
		{
			resetModel(model.leftLeg);
			model.leftLeg.x = 1.9F;
			model.leftLeg.y = 12.0F;
			model.leftLeg.z = 0.0F;
		}
		{
			resetModel(model.rightLeg);
			model.rightLeg.x = -1.9F;
			model.rightLeg.y = 12.0F;
			model.rightLeg.z = 0.0F;
		}
		// The sleeve / pants / jacket / hat are children of the limbs in 1.21.4 and inherit their
		// transform from the tree, so they only have to be back at their initial pose here.
		resetSecondLayer();
	}

	public void resetModel(ModelPart model) {
		model.xRot = 0;
		model.yRot = 0;
		model.zRot = 0;
		model.x = 0;
		model.y = 0;
		model.z = 0;
	}
}

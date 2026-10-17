package com.miguel.autowalktoggle;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

/** What to do when a route point is reached: lock one of the movement keys, or stop walking. */
public enum MoveAction {
	FORWARD("forward"),
	BACK("back"),
	LEFT("left"),
	RIGHT("right"),
	STOP("stop");

	private final String id;

	MoveAction(String id) {
		this.id = id;
	}

	/** The KeyMapping this action locks (whatever key it is bound to in Controls), or null for STOP. */
	public KeyMapping key(Options options) {
		return switch (this) {
			case FORWARD -> options.keyUp;
			case BACK -> options.keyDown;
			case LEFT -> options.keyLeft;
			case RIGHT -> options.keyRight;
			case STOP -> null;
		};
	}

	public MoveAction next() {
		MoveAction[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	/** For example "Forward (W)", using the key currently bound in Controls. */
	public Component label(Options options) {
		Component name = Component.translatable("autowalktoggle.action." + id);
		KeyMapping key = key(options);

		if (key == null) {
			return name;
		}

		return Component.empty().append(name).append(" (").append(key.getTranslatedKeyMessage()).append(")");
	}

	/**
	 * Horizontal unit vector {x, z} in which this key pushes the player when looking at the given yaw (degrees),
	 * matching vanilla's input-to-world conversion. {0, 0} for STOP.
	 */
	public double[] direction(float yaw) {
		double radians = Math.toRadians(yaw);
		double sin = Math.sin(radians);
		double cos = Math.cos(radians);

		return switch (this) {
			case FORWARD -> new double[] {-sin, cos};
			case BACK -> new double[] {sin, -cos};
			case LEFT -> new double[] {cos, sin};
			case RIGHT -> new double[] {-cos, -sin};
			case STOP -> new double[] {0, 0};
		};
	}
}

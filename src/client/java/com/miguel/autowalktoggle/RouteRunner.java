package com.miguel.autowalktoggle;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Follows the route: while a movement key is locked, switches to the next point's key exactly when the player
 * should turn.
 *
 * <p>Precision: the player moves ~0.2-0.28 blocks per tick and keeps sliding after a key is released, so waiting
 * until the point is crossed would always overshoot. Every tick we simulate how far the player would still slide
 * along the current direction if the key were switched right now (vanilla physics: each tick the velocity plus the
 * new key's acceleration is moved, then multiplied by the ground friction), and switch on the tick that leaves the
 * player closest to the exact point. The remaining error is at most half a tick of movement (~0.1 block, 1-2 pixels).
 */
public final class RouteRunner {
	/** How far sideways from the path a point may be and still count (blocks). */
	private static final double MAX_SIDEWAYS = 1.0;
	/** How far above/below a point the player may be (blocks). */
	private static final double MAX_VERTICAL = 1.5;
	/** When stuck (e.g. against a wall), a point this close counts as reached (blocks). */
	private static final double STANDING_RADIUS = 0.3;

	private static Route route = Route.load();
	private static boolean running = false;
	private static int nextIndex = 0;

	private RouteRunner() {
	}

	public static Route route() {
		return route;
	}

	/** Replaces the route (from the menu) and saves it. */
	public static void setRoute(Route newRoute) {
		route = newRoute;
		route.save();

		if (nextIndex >= route.points.size()) {
			nextIndex = 0;
		}

		if (route.points.isEmpty()) {
			running = false;
		}
	}

	public static boolean isRunning() {
		return running;
	}

	/** 1-based number of the next point, for status messages. */
	public static int nextPointNumber() {
		return nextIndex + 1;
	}

	/** Starts the route from its first point. Returns false if the route is empty. */
	public static boolean start(Minecraft client) {
		if (route.points.isEmpty()) {
			return false;
		}

		running = true;
		nextIndex = 0;

		KeyMapping startKey = route.start.key(client.options);

		if (startKey != null) {
			AutoWalkController.requestLock(startKey);
		} else {
			AutoWalkController.setEnabled(true);
		}

		return true;
	}

	public static void stop() {
		running = false;
	}

	/** Called by AutoWalkController at the start of every tick in which a key is locked and no menu is open. */
	static void onLockedTick(Minecraft client) {
		LocalPlayer player = client.player;

		if (!running || player == null || route.points.isEmpty()) {
			return;
		}

		Route.Point target = route.points.get(nextIndex);

		if (!shouldTurnNow(player, target)) {
			return;
		}

		AutoWalkController.changeLockedKey(target.action.key(client.options));
		nextIndex++;

		if (nextIndex >= route.points.size()) {
			nextIndex = 0;

			if (!route.loop) {
				running = false;
			}
		}
	}

	private static boolean shouldTurnNow(LocalPlayer player, Route.Point target) {
		if (Math.abs(player.getY() - target.y) > MAX_VERTICAL) {
			return false;
		}

		double dx = target.x - player.getX();
		double dz = target.z - player.getZ();

		// Velocity that will be moved this tick (before adding this tick's key acceleration).
		Vec3 velocity = player.getDeltaMovement();
		double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

		if (speed < 0.01) {
			// Not moving (pushed against a wall, for example): only turn if standing on the point.
			return dx * dx + dz * dz <= STANDING_RADIUS * STANDING_RADIUS;
		}

		double dirX = velocity.x / speed;
		double dirZ = velocity.z / speed;
		double ahead = dx * dirX + dz * dirZ;
		double sideways = Math.abs(dx * dirZ - dz * dirX);

		if (sideways > MAX_SIDEWAYS || ahead < -0.5) {
			return false;
		}

		double friction = player.onGround() ? groundFriction(player) * 0.91 : 0.91;
		// At constant speed: move = speed / friction = speed + acceleration.
		double acceleration = speed * (1 - friction) / friction;
		double step = speed + acceleration;

		// Part of the new key's acceleration that pushes along the current direction (only braking counts).
		double[] newDirection = target.action.direction(player.getYRot());
		double newAcceleration = Math.min(0, acceleration * (newDirection[0] * dirX + newDirection[1] * dirZ));
		double slide = slideDistance(speed, newAcceleration, friction);

		// Turning now stops the player at (ahead - slide) from the point; waiting one more tick moves that by -step.
		return ahead - slide <= step / 2;
	}

	/** Distance still travelled along the current direction after switching keys, simulating vanilla physics. */
	private static double slideDistance(double speed, double acceleration, double friction) {
		double distance = 0;
		double velocity = speed;

		for (int tick = 0; tick < 200; tick++) {
			double move = velocity + acceleration;

			if (move <= 0.0001) {
				break;
			}

			distance += move;
			velocity = move * friction;
		}

		return distance;
	}

	private static double groundFriction(LocalPlayer player) {
		return player.level().getBlockState(player.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
	}
}

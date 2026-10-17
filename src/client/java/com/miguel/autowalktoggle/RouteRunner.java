package com.miguel.autowalktoggle;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Follows the route: while the mod is enabled and a movement key is locked, switches to a point's key exactly when
 * the player should turn. There is nothing to start: if there are points, they work.
 *
 * <p>Points have no order: any point the player walks towards is used, so stopping, walking elsewhere or coming
 * back to any position just works. A point is "disarmed" after it triggers, and when the player starts walking
 * near it, so it does not override the direction the player chose; it re-arms once the player is more than
 * {@link #REARM_DISTANCE} away.
 *
 * <p>Precision: the player moves ~0.2-0.28 blocks per tick and keeps sliding after a key is released, so waiting
 * until the point is crossed would always overshoot. Every tick we simulate how far the player would still slide
 * along the current direction if the key were switched right now (vanilla physics: each tick the velocity plus the
 * new key's acceleration is moved, then multiplied by the ground friction), and switch on the tick that leaves the
 * player closest to the exact point. The remaining error is at most half a tick of movement (~0.1 block, 1-2 pixels).
 */
public final class RouteRunner {
	/** How far sideways from the path a point may be and still count (blocks). */
	private static final double MAX_SIDEWAYS = 0.75;
	/** How far above/below a point the player may be (blocks). */
	private static final double MAX_VERTICAL = 1.5;
	/** A point slightly behind (lag) still counts; further behind it is ignored (blocks). */
	private static final double MAX_BEHIND = 0.15;
	/** When stuck (e.g. against a wall), a point this close counts as reached (blocks). */
	private static final double STANDING_RADIUS = 0.3;
	/** A disarmed point works again once the player is this far from it (blocks). */
	private static final double REARM_DISTANCE = 1.0;

	private static Route route = Route.load();
	private static final Set<Integer> disarmed = new HashSet<>();

	/** Standing still at a point with a wait time; the action to do when the wait ends. */
	private static boolean waiting = false;
	private static int waitTicksLeft = 0;
	private static MoveAction actionAfterWait = MoveAction.STOP;

	private RouteRunner() {
	}

	public static Route route() {
		return route;
	}

	/** Replaces the route (from the menu) and saves it. */
	public static void setRoute(Route newRoute) {
		route = newRoute;
		route.save();
		disarmed.clear();
	}

	/** Disconnect / world change / mod disabled: forget any wait in progress. */
	static void reset() {
		waiting = false;
		disarmed.clear();
	}

	static boolean isWaiting() {
		return waiting;
	}

	static void cancelWait() {
		waiting = false;
	}

	/**
	 * Called every tick (no menu open) while waiting at a point. Returns the key to lock when the wait is over,
	 * or null while still waiting (also null if the point's action is STOP).
	 */
	static KeyMapping tickWait(Minecraft client) {
		if (--waitTicksLeft > 0) {
			return null;
		}

		waiting = false;
		return actionAfterWait.key(client.options);
	}

	/**
	 * The player started walking (tapped a key, /autowalk w|s, route start): points they are standing on or next to
	 * must not change the direction they just chose.
	 */
	static void onLockStarted(Minecraft client) {
		LocalPlayer player = client.player;

		if (player == null) {
			return;
		}

		for (int i = 0; i < route.points.size(); i++) {
			if (horizontalDistance(player, route.points.get(i)) <= REARM_DISTANCE) {
				disarmed.add(i);
			}
		}
	}

	/** Called by AutoWalkController at the start of every tick in which a key is locked and no menu is open. */
	static void onLockedTick(Minecraft client) {
		LocalPlayer player = client.player;

		if (player == null || route.points.isEmpty()) {
			return;
		}

		disarmed.removeIf(index -> index >= route.points.size()
				|| horizontalDistance(player, route.points.get(index)) > REARM_DISTANCE);

		int best = -1;
		double bestDistance = Double.MAX_VALUE;

		for (int i = 0; i < route.points.size(); i++) {
			if (disarmed.contains(i)) {
				continue;
			}

			Route.Point point = route.points.get(i);

			if (shouldTurnNow(player, point, firstAction(point))) {
				double distance = horizontalDistance(player, point);

				if (distance < bestDistance) {
					best = i;
					bestDistance = distance;
				}
			}
		}

		if (best < 0) {
			return;
		}

		Route.Point target = route.points.get(best);
		MoveAction firstAction = firstAction(target);
		AutoWalkController.changeLockedKey(firstAction.key(client.options));
		disarmed.add(best);

		if (firstAction != target.action) {
			waiting = true;
			waitTicksLeft = waitTicks(target);
			actionAfterWait = target.action;
		}
	}

	private static int waitTicks(Route.Point point) {
		return (int) Math.round(point.wait * 20);
	}

	/** With a wait, the player first stops exactly on the point (releases the key), then turns. */
	private static MoveAction firstAction(Route.Point point) {
		return waitTicks(point) > 0 ? MoveAction.STOP : point.action;
	}

	private static double horizontalDistance(LocalPlayer player, Route.Point point) {
		double dx = point.x - player.getX();
		double dz = point.z - player.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static boolean shouldTurnNow(LocalPlayer player, Route.Point target, MoveAction action) {
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

		if (sideways > MAX_SIDEWAYS || ahead < -MAX_BEHIND) {
			return false;
		}

		double friction = player.onGround() ? groundFriction(player) * 0.91 : 0.91;
		// At constant speed: move = speed / friction = speed + acceleration.
		double acceleration = speed * (1 - friction) / friction;
		double step = speed + acceleration;

		// Part of the new key's acceleration that pushes along the current direction (only braking counts).
		double[] newDirection = action.direction(player.getYRot());
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

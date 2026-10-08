package com.miguel.autowalktoggle;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * All the "sticky movement key" logic.
 *
 * <p>How the forcing works: Minecraft reads the movement keys (KeyMapping#isDown) inside the client tick,
 * while real keyboard events are processed between ticks. So:
 * <ul>
 *   <li>At START of each tick we read the real keyboard state of every movement key, detect new presses
 *       and, if a key is locked, force it down with setDown(true) so the player's movement this tick sees it.</li>
 *   <li>At END of each tick we hand the locked key back its real keyboard state, so that between ticks the
 *       KeyMapping again mirrors the physical key and the next START can read it reliably.</li>
 * </ul>
 * Thanks to this, no mixin is needed and the locked key never "fights" with vanilla's own key handling.
 */
public final class AutoWalkController {
	/** Mod state; starts disabled as requested. */
	private static boolean enabled = false;

	/** The movement key currently locked, or null if none. Only one at a time. */
	private static KeyMapping lockedKey = null;

	/** Real (physical) state of the locked key, read at the start of the current tick. */
	private static boolean lockedKeyPhysicallyDown = false;

	/** Physical state of each movement key at the previous tick, used to ignore OS key-repeat "clicks". */
	private static final boolean[] wasDown = new boolean[4];

	/** Level seen in the previous tick, to detect world / server / dimension changes. */
	private static ClientLevel lastLevel = null;

	/**
	 * Key requested by /autowalk w|s. The command runs while the chat is still open (and an open menu releases
	 * the lock), so it is locked on the first tick in which no menu is open.
	 */
	private static KeyMapping pendingKey = null;

	private AutoWalkController() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;

		if (!value) {
			pendingKey = null;
			releaseLockedKey();
		}
	}

	/** /autowalk w|s: enables the mod and locks the given movement key as soon as the chat closes. */
	public static void requestLock(KeyMapping key) {
		enabled = true;
		pendingKey = key;
	}

	/** Releases the locked key immediately (sets it up) and forgets it. Safe to call at any time. */
	public static void releaseLockedKey() {
		if (lockedKey != null) {
			lockedKey.setDown(false);
			lockedKey = null;
		}

		lockedKeyPhysicallyDown = false;
	}

	/** ClientTickEvents.START_CLIENT_TICK */
	public static void onStartTick(Minecraft client) {
		KeyMapping[] keys = movementKeys(client.options);

		// 1. Detect NEW presses. Always drain the clicks (even when disabled) so that old presses are never
		//    replayed later. A click only counts as a new press if the key was physically up in the previous
		//    tick: holding a key generates repeated clicks through OS key repeat, and those must be ignored.
		KeyMapping pressedLocked = null;
		KeyMapping pressedOther = null;

		for (int i = 0; i < keys.length; i++) {
			KeyMapping key = keys[i];
			boolean clicked = false;

			while (key.consumeClick()) {
				clicked = true;
			}

			// Between ticks the KeyMapping mirrors the physical key (see onEndTick), so this is the real state.
			boolean physicallyDown = key.isDown();
			boolean newPress = clicked && !wasDown[i];
			wasDown[i] = physicallyDown;

			if (!newPress) {
				continue;
			}

			if (key == lockedKey) {
				pressedLocked = key;
			} else if (pressedOther == null) {
				pressedOther = key;
			}
		}

		// 2. World / server / dimension change: release.
		if (client.level != lastLevel) {
			lastLevel = client.level;
			releaseLockedKey();
		}

		// 3. Disabled, menu open, dead or not in game: behave exactly like vanilla.
		if (!enabled || mustRelease(client)) {
			releaseLockedKey();
			return;
		}

		// 4. /autowalk w|s: lock the requested key now that no menu is open.
		if (pendingKey != null) {
			if (lockedKey != null && lockedKey != pendingKey) {
				lockedKey.setDown(lockedKeyPhysicallyDown);
			}

			lockedKey = pendingKey;
			pendingKey = null;
			lockedKeyPhysicallyDown = lockedKey.isDown();
			lockedKey.setDown(true);
			return;
		}

		// 5. A key is already locked.
		if (lockedKey != null) {
			if (pressedLocked != null || pressedOther != null) {
				// a) Same key pressed again, or b) another direction pressed: unlock.
				// The KeyMapping already holds its real keyboard state, so we just stop forcing it.
				// The other key is NOT locked by this press: it simply works as in vanilla.
				lockedKey = null;
				lockedKeyPhysicallyDown = false;
				return;
			}

			lockedKeyPhysicallyDown = lockedKey.isDown();
			lockedKey.setDown(true);
			return;
		}

		// 6. Nothing locked: a new press locks that key.
		if (pressedOther != null) {
			lockedKey = pressedOther;
			lockedKeyPhysicallyDown = lockedKey.isDown();
			lockedKey.setDown(true);
		}
	}

	/** ClientTickEvents.END_CLIENT_TICK */
	public static void onEndTick(Minecraft client) {
		if (lockedKey == null) {
			return;
		}

		// A menu may have been opened during this tick (inventory key, death screen...).
		if (!enabled || mustRelease(client)) {
			releaseLockedKey();
			return;
		}

		// Give the KeyMapping back its real keyboard state until the next tick starts.
		lockedKey.setDown(lockedKeyPhysicallyDown);
	}

	/** Called on disconnect / join. */
	public static void reset() {
		pendingKey = null;
		releaseLockedKey();
		lastLevel = null;
	}

	private static boolean mustRelease(Minecraft client) {
		// Since 26.2 the current screen lives in Minecraft#gui instead of Minecraft#screen.
		return client.gui.screen() != null
				|| client.player == null
				|| client.level == null
				|| client.player.isDeadOrDying();
	}

	/** The player's movement KeyMappings: they use whatever keys are configured in Controls. */
	private static KeyMapping[] movementKeys(Options options) {
		return new KeyMapping[] {options.keyUp, options.keyDown, options.keyLeft, options.keyRight};
	}
}

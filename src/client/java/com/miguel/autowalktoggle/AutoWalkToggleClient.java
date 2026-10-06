package com.miguel.autowalktoggle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class AutoWalkToggleClient implements ClientModInitializer {
	public static final String MOD_ID = "autowalktoggle";

	@Override
	public void onInitializeClient() {
		// /autowalk on|off|toggle|status (client-only command).
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> AutoWalkCommand.register(dispatcher));

		// Sticky key logic: START detects presses and forces the locked key, END gives it back its real state.
		ClientTickEvents.START_CLIENT_TICK.register(AutoWalkController::onStartTick);
		ClientTickEvents.END_CLIENT_TICK.register(AutoWalkController::onEndTick);

		// Release the locked key when leaving or joining a world/server.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AutoWalkController.reset());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> AutoWalkController.reset());
	}
}

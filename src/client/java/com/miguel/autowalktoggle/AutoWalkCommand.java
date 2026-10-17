package com.miguel.autowalktoggle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * Client-side command /autowalk [on|off|toggle|status].
 * It is handled entirely by the client: it is never sent to the server, so it works on vanilla servers.
 */
public final class AutoWalkCommand {
	private AutoWalkCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("autowalk")
				.executes(AutoWalkCommand::status)
				.then(ClientCommands.literal("on").executes(context -> setEnabled(context, true)))
				.then(ClientCommands.literal("off").executes(context -> setEnabled(context, false)))
				.then(ClientCommands.literal("toggle").executes(context -> setEnabled(context, !AutoWalkController.isEnabled())))
				.then(ClientCommands.literal("status").executes(AutoWalkCommand::status))
				.then(ClientCommands.literal("w").executes(context -> lock(context, context.getSource().getClient().options.keyUp, "autowalktoggle.command.forward")))
				.then(ClientCommands.literal("s").executes(context -> lock(context, context.getSource().getClient().options.keyDown, "autowalktoggle.command.backward")))
				.then(ClientCommands.literal("menu").executes(context -> {
					RouteScreen.requestOpen();
					return 1;
				}))
				.then(ClientCommands.literal("route")
						.executes(AutoWalkCommand::routeStatus)
						.then(ClientCommands.literal("start").executes(AutoWalkCommand::routeStart))
						.then(ClientCommands.literal("stop").executes(AutoWalkCommand::routeStop))
						.then(ClientCommands.literal("status").executes(AutoWalkCommand::routeStatus))));
	}

	private static int routeStart(CommandContext<FabricClientCommandSource> context) {
		if (RouteRunner.start(context.getSource().getClient())) {
			send(context, Component.translatable("autowalktoggle.command.route.started", RouteRunner.route().points.size()).withStyle(ChatFormatting.GREEN));
		} else {
			send(context, Component.translatable("autowalktoggle.command.route.empty").withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	private static int routeStop(CommandContext<FabricClientCommandSource> context) {
		RouteRunner.stop();
		send(context, Component.translatable("autowalktoggle.command.route.stopped").withStyle(ChatFormatting.RED));
		return 1;
	}

	private static int routeStatus(CommandContext<FabricClientCommandSource> context) {
		int points = RouteRunner.route().points.size();

		if (RouteRunner.isActive()) {
			send(context, Component.translatable("autowalktoggle.command.route.running", points).withStyle(ChatFormatting.GREEN));
		} else {
			send(context, Component.translatable("autowalktoggle.command.route.idle", points).withStyle(ChatFormatting.YELLOW));
		}

		return 1;
	}

	/** /autowalk w|s: walk as if the forward / back key had been tapped with the mod enabled. */
	private static int lock(CommandContext<FabricClientCommandSource> context, KeyMapping key, String messageKey) {
		AutoWalkController.requestLock(key);
		send(context, Component.translatable(messageKey).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int setEnabled(CommandContext<FabricClientCommandSource> context, boolean enabled) {
		AutoWalkController.setEnabled(enabled);

		if (enabled) {
			send(context, Component.translatable("autowalktoggle.command.enabled").withStyle(ChatFormatting.GREEN));
		} else {
			send(context, Component.translatable("autowalktoggle.command.disabled").withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	private static int status(CommandContext<FabricClientCommandSource> context) {
		if (AutoWalkController.isEnabled()) {
			send(context, Component.translatable("autowalktoggle.command.status.enabled").withStyle(ChatFormatting.GREEN));
		} else {
			send(context, Component.translatable("autowalktoggle.command.status.disabled").withStyle(ChatFormatting.RED));
		}

		return 1;
	}

	private static void send(CommandContext<FabricClientCommandSource> context, Component message) {
		context.getSource().sendFeedback(Component.literal("[AutoWalk] ").withStyle(ChatFormatting.GOLD).append(message));
	}
}

package com.miguel.autowalktoggle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
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
				.then(ClientCommands.literal("status").executes(AutoWalkCommand::status)));
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

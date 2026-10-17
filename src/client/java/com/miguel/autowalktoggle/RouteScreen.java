package com.miguel.autowalktoggle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Route editor opened with /autowalk menu: one row per point (X, Y, Z with decimals, wait, action), plus the
 * start option. Rows are edited on a {@link Draft} (as text, so half-typed numbers survive) and only turned into a
 * {@link Route} when saving. Adding, deleting or changing page reopens the screen, which rebuilds the widgets.
 */
public class RouteScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int FIRST_ROW_Y = 44;
	private static final int TOTAL_WIDTH = 366;

	private static boolean openRequested = false;

	private final Draft draft;
	private final int page;
	private final Component status;

	public RouteScreen(Draft draft, int page, Component status) {
		super(Component.translatable("autowalktoggle.menu.title"));
		this.draft = draft;
		this.page = page;
		this.status = status;
	}

	/** /autowalk menu: the chat is still open while the command runs, so the menu is opened on the next tick. */
	public static void requestOpen() {
		openRequested = true;
	}

	/** ClientTickEvents.END_CLIENT_TICK */
	public static void onEndTick(Minecraft client) {
		if (openRequested && client.gui.screen() == null && client.player != null) {
			openRequested = false;
			client.gui.setScreen(new RouteScreen(Draft.from(RouteRunner.route()), 0, null));
		}
	}

	@Override
	protected void init() {
		int left = (this.width - TOTAL_WIDTH) / 2;
		int rowsPerPage = rowsPerPage();
		int pageCount = Math.max(1, (draft.rows.size() + rowsPerPage - 1) / rowsPerPage);
		int currentPage = Math.min(page, pageCount - 1);

		addLabel(this.title, this.width / 2, 8, true);

		if (status != null) {
			addLabel(status, this.width / 2, 20, true);
		}

		addLabel(Component.literal("X"), left + 22 + 28, 32, true);
		addLabel(Component.literal("Y"), left + 82 + 20, 32, true);
		addLabel(Component.literal("Z"), left + 126 + 28, 32, true);
		addLabel(Component.translatable("autowalktoggle.menu.wait"), left + 186 + 17, 32, true);
		addLabel(Component.translatable("autowalktoggle.menu.action"), left + 224 + 42, 32, true);

		for (int i = 0; i < rowsPerPage; i++) {
			int index = currentPage * rowsPerPage + i;

			if (index >= draft.rows.size()) {
				break;
			}

			addRow(index, left, FIRST_ROW_Y + i * ROW_HEIGHT, currentPage);
		}

		int bottom1 = this.height - 50;
		int bottom2 = this.height - 26;

		addRenderableWidget(Button.builder(Component.translatable("autowalktoggle.menu.add"), button -> addPointHere(rowsPerPage))
				.bounds(left, bottom1, 176, 20).build());
		addRenderableWidget(Button.builder(startLabel(), button -> {
			draft.start = draft.start.next();
			button.setMessage(startLabel());
		}).bounds(left + 180, bottom1, 186, 20).build());

		addRenderableWidget(Button.builder(Component.literal("<"), button -> reopen(currentPage - 1, null))
				.bounds(left, bottom2, 20, 20).build()).active = currentPage > 0;
		addLabel(Component.literal((currentPage + 1) + "/" + pageCount), left + 39, bottom2 + 6, true);
		addRenderableWidget(Button.builder(Component.literal(">"), button -> reopen(currentPage + 1, null))
				.bounds(left + 58, bottom2, 20, 20).build()).active = currentPage < pageCount - 1;
		addRenderableWidget(Button.builder(Component.translatable("autowalktoggle.menu.run"), button -> run(currentPage))
				.bounds(left + 82, bottom2, 90, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("autowalktoggle.menu.stop"), button -> {
			RouteRunner.stop();
			reopen(currentPage, Component.translatable("autowalktoggle.menu.stopped").withStyle(ChatFormatting.YELLOW));
		}).bounds(left + 176, bottom2, 80, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("autowalktoggle.menu.save"), button -> saveAndClose(currentPage))
				.bounds(left + 260, bottom2, 106, 20).build());
	}

	private void addRow(int index, int left, int y, int currentPage) {
		Row row = draft.rows.get(index);

		addLabel(Component.literal("#" + (index + 1)), left + 9, y + 6, true);
		EditBox xBox = addCoordinateBox(left + 22, y, 56, row.x, "X", value -> row.x = value);
		EditBox yBox = addCoordinateBox(left + 82, y, 40, row.y, "Y", value -> row.y = value);
		EditBox zBox = addCoordinateBox(left + 126, y, 56, row.z, "Z", value -> row.z = value);
		addCoordinateBox(left + 186, y, 34, row.wait, "s", value -> row.wait = value);

		addRenderableWidget(Button.builder(row.action.label(this.minecraft.options), button -> {
			row.action = row.action.next();
			button.setMessage(row.action.label(this.minecraft.options));
		}).bounds(left + 224, y, 84, 20).build());

		// Fill this row with the player's exact current position.
		addRenderableWidget(Button.builder(Component.translatable("autowalktoggle.menu.here"), button -> {
			LocalPlayer player = this.minecraft.player;

			if (player != null) {
				xBox.setValue(format(player.getX()));
				yBox.setValue(format(player.getY()));
				zBox.setValue(format(player.getZ()));
			}
		}).bounds(left + 312, y, 30, 20).build());

		addRenderableWidget(Button.builder(Component.literal("x"), button -> {
			draft.rows.remove(index);
			reopen(currentPage, null);
		}).bounds(left + 346, y, 20, 20).build());
	}

	private EditBox addCoordinateBox(int x, int y, int width, String value, String hint, java.util.function.Consumer<String> responder) {
		EditBox box = new EditBox(this.font, x, y, width, 20, Component.literal(hint));
		// Text is validated when saving (EditBox has no input filter since 26.2).
		box.setMaxLength(14);
		box.setValue(value);
		box.setHint(Component.literal(hint));
		box.setResponder(responder);
		return addRenderableWidget(box);
	}

	private void addLabel(Component text, int x, int y, boolean centered) {
		int textWidth = this.font.width(text);
		int labelX = centered ? x - textWidth / 2 : x;
		addRenderableWidget(new StringWidget(labelX, y, textWidth, 9, text, this.font));
	}

	private void addPointHere(int rowsPerPage) {
		if (draft.rows.size() >= Route.MAX_POINTS) {
			reopen(page, Component.translatable("autowalktoggle.menu.full", Route.MAX_POINTS).withStyle(ChatFormatting.RED));
			return;
		}

		LocalPlayer player = this.minecraft.player;
		Row row = new Row();

		if (player != null) {
			row.x = format(player.getX());
			row.y = format(player.getY());
			row.z = format(player.getZ());
		}

		draft.rows.add(row);
		reopen((draft.rows.size() - 1) / rowsPerPage, null);
	}

	private void run(int currentPage) {
		Route route = buildRoute(currentPage);

		if (route == null) {
			return;
		}

		if (route.points.isEmpty()) {
			reopen(currentPage, Component.translatable("autowalktoggle.menu.empty").withStyle(ChatFormatting.RED));
			return;
		}

		RouteRunner.setRoute(route);
		this.minecraft.gui.setScreen(null);
		// The lock is applied on the next tick, once this menu is closed.
		RouteRunner.start(this.minecraft);
	}

	private void saveAndClose(int currentPage) {
		Route route = buildRoute(currentPage);

		if (route != null) {
			RouteRunner.setRoute(route);
			this.minecraft.gui.setScreen(null);
		}
	}

	/** Esc: save if every row is valid; otherwise stay open and show which row is wrong. */
	@Override
	public void onClose() {
		Route route = buildRoute(page);

		if (route != null) {
			RouteRunner.setRoute(route);
			super.onClose();
		}
	}

	/** Parses the draft. On an invalid row, reopens the menu with an error and returns null. */
	private Route buildRoute(int currentPage) {
		Route route = new Route();
		route.start = draft.start;

		for (int i = 0; i < draft.rows.size(); i++) {
			Row row = draft.rows.get(i);
			Double x = parse(row.x);
			Double y = parse(row.y);
			Double z = parse(row.z);
			Double wait = row.wait.isBlank() ? Double.valueOf(0) : parse(row.wait);

			if (x == null || y == null || z == null || wait == null || wait < 0 || wait > Route.MAX_WAIT_SECONDS) {
				reopen(currentPage, Component.translatable("autowalktoggle.menu.invalid", i + 1).withStyle(ChatFormatting.RED));
				return null;
			}

			route.points.add(new Route.Point(x, y, z, row.action, wait));
		}

		return route;
	}

	private void reopen(int newPage, Component newStatus) {
		this.minecraft.gui.setScreen(new RouteScreen(draft, Math.max(0, newPage), newStatus));
	}

	private int rowsPerPage() {
		return Math.max(1, (this.height - FIRST_ROW_Y - 60) / ROW_HEIGHT);
	}

	private Component startLabel() {
		if (draft.start == MoveAction.STOP) {
			return Component.translatable("autowalktoggle.menu.start_manual");
		}

		return Component.translatable("autowalktoggle.menu.start_with", draft.start.label(this.minecraft.options));
	}

	private static Double parse(String text) {
		try {
			return Double.parseDouble(text.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}

	/** 0.5 -> "0.5", 2.0 -> "2". */
	private static String formatSeconds(double seconds) {
		String text = String.format(Locale.ROOT, "%.2f", seconds);
		return text.replaceAll("\\.?0+$", "");
	}

	/** One editable row: coordinates kept as text while editing. */
	static final class Row {
		String x = "";
		String y = "";
		String z = "";
		String wait = "0";
		MoveAction action = MoveAction.FORWARD;
	}

	/** Editable copy of the route, kept across reopens of the screen. */
	static final class Draft {
		final List<Row> rows = new ArrayList<>();
		MoveAction start;

		static Draft from(Route route) {
			Draft draft = new Draft();
			draft.start = route.start;

			for (Route.Point point : route.points) {
				Row row = new Row();
				row.x = format(point.x);
				row.y = format(point.y);
				row.z = format(point.z);
				row.wait = formatSeconds(point.wait);
				row.action = point.action;
				draft.rows.add(row);
			}

			return draft;
		}
	}
}

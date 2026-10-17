package com.miguel.autowalktoggle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

/** The list of route points, saved to config/autowalktoggle-route.json. */
public final class Route {
	public static final int MAX_POINTS = 32;
	public static final double MAX_WAIT_SECONDS = 60;

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoWalkToggleClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Turn points. They have no order: whichever one the player walks into is used. */
	public List<Point> points = new ArrayList<>();

	public static final class Point {
		public double x;
		public double y;
		public double z;
		public MoveAction action = MoveAction.STOP;
		/** Seconds to stand still at the point before pressing the new key (0 = turn immediately). */
		public double wait = 0;

		public Point() {
		}

		public Point(double x, double y, double z, MoveAction action, double wait) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.action = action;
			this.wait = wait;
		}
	}

	public static Route load() {
		Path file = file();

		if (Files.exists(file)) {
			try {
				Route route = GSON.fromJson(Files.readString(file), Route.class);

				if (route != null) {
					route.sanitize();
					return route;
				}
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Could not read {}, starting with an empty route", file, e);
			}
		}

		return new Route();
	}

	public void save() {
		Path file = file();

		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this));
		} catch (IOException e) {
			LOGGER.warn("Could not save {}", file, e);
		}
	}

	private void sanitize() {
		if (points == null) {
			points = new ArrayList<>();
		}

		points.removeIf(point -> point == null);
		points.forEach(point -> {
			if (point.action == null) {
				point.action = MoveAction.STOP;
			}

			point.wait = Math.max(0, Math.min(MAX_WAIT_SECONDS, point.wait));
		});

		while (points.size() > MAX_POINTS) {
			points.remove(points.size() - 1);
		}
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("autowalktoggle-route.json");
	}
}

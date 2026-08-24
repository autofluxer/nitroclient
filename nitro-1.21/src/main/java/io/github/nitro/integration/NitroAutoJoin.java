package io.github.nitro.integration;

import io.github.nitro.ui.NitroJoinFailedScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * One-shot launcher auto-join. Never retries automatically on failure.
 * Failed joins surface {@link NitroJoinFailedScreen} with a manual Retry button.
 */
public final class NitroAutoJoin {

	/** True after the env-driven auto-join has been consumed for this process. */
	private static boolean consumed;
	/** True while a join we started is in-flight (ConnectScreen). */
	private static boolean connecting;
	private static String lastTarget;
	private static String lastName = "Server";

	private NitroAutoJoin() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(NitroAutoJoin::onTick);
	}

	private static void onTick(MinecraftClient client) {
		if (consumed || connecting || client.world != null) {
			return;
		}
		if (client.currentScreen instanceof ConnectScreen
				|| client.currentScreen instanceof DisconnectedScreen
				|| client.currentScreen instanceof NitroJoinFailedScreen) {
			return;
		}

		String envTarget = System.getenv("NITRO_AUTO_JOIN");
		String token = System.getenv("NITRO_LAUNCHER_JOIN");
		if (envTarget == null || envTarget.isBlank() || token == null || token.isBlank()) {
			return;
		}

		// Consume immediately so a failed/cancelled connect can never loop.
		consumed = true;
		lastTarget = envTarget.trim();
		lastName = displayNameFor(lastTarget);
		connect(client, lastTarget, lastName, true);
	}

	public static Screen maybeReplaceDisconnected(Screen screen) {
		if (!(screen instanceof DisconnectedScreen) || !connecting) {
			return screen;
		}
		connecting = false;
		String host = lastTarget != null ? lastTarget : "server";
		Text reason = Text.literal("Could not connect to " + host + ".")
				.formatted(Formatting.GRAY);
		return new NitroJoinFailedScreen(host, lastName, reason);
	}

	public static void onScreenChange(Screen screen) {
		if (screen instanceof ConnectScreen) {
			return;
		}
		// Left the connecting UI without a world — treat as finished attempt.
		if (connecting && !(screen instanceof NitroJoinFailedScreen)) {
			if (screen instanceof DisconnectedScreen) {
				// Handled via maybeReplaceDisconnected.
				return;
			}
			connecting = false;
		}
	}

	public static void retryLast(MinecraftClient client) {
		if (client == null || lastTarget == null || lastTarget.isBlank()) {
			return;
		}
		connect(client, lastTarget, lastName, false);
	}

	public static void clearConnecting() {
		connecting = false;
	}

	private static void connect(MinecraftClient client, String host, String name, boolean fromAutoJoin) {
		connecting = true;
		client.execute(() -> {
			try {
				if (fromAutoJoin && client.inGameHud != null) {
					client.inGameHud.getChatHud().addMessage(
							Text.literal("Connecting to " + name + " (" + host + ")…")
									.formatted(Formatting.AQUA));
				}
				ServerInfo info = new ServerInfo(name, host, ServerInfo.ServerType.OTHER);
				Screen parent = client.currentScreen instanceof TitleScreen || client.currentScreen == null
						? client.currentScreen
						: client.currentScreen;
				ConnectScreen.connect(parent, client, ServerAddress.parse(host), info, false, null);
			} catch (Throwable error) {
				connecting = false;
				client.setScreen(new NitroJoinFailedScreen(
						host,
						name,
						Text.literal(error.getMessage() == null ? "Connection failed." : error.getMessage())
								.formatted(Formatting.GRAY)));
			}
		});
	}

	private static String displayNameFor(String host) {
		String h = host.toLowerCase();
		if (h.contains("nitrosmp") || h.contains("nitro")) {
			return "Nitro SMP";
		}
		if (h.contains("hypixel")) {
			return "Hypixel";
		}
		if (h.contains("minehut")) {
			return "Minehut";
		}
		if (h.contains("cubecraft")) {
			return "CubeCraft";
		}
		return host;
	}
}

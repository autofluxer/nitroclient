package io.github.nitro.discord;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class DiscordPresence {

	private static final String VERSION = "1.21.11";
	private static final DiscordIpcClient IPC = new DiscordIpcClient();
	private static String lastKey = "";
	private static boolean started;

	private DiscordPresence() {
	}

	public static void start() {
		if (started) {
			return;
		}
		started = true;
		IPC.connect();
		setMenus();
	}

	public static void tick(MinecraftClient client) {
		if (!started || client == null) {
			return;
		}
		if (client.world == null || client.player == null) {
			setMenus();
			return;
		}
		if (client.isInSingleplayer()) {
			set("Nitro Client", "Singleplayer · " + VERSION);
			return;
		}
		String server = resolveServer(client);
		set("Nitro Client", "Playing on " + server + " · " + VERSION);
	}

	public static void setMenus() {
		set("Nitro Client", "Main Menu · " + VERSION);
	}

	public static void shutdown() {
		IPC.shutdown();
		started = false;
		lastKey = "";
	}

	private static void set(String details, String state) {
		String key = details + "|" + state;
		if (key.equals(lastKey)) {
			return;
		}
		lastKey = key;
		IPC.setActivity(details, state);
	}

	private static String resolveServer(MinecraftClient client) {
		try {
			ServerInfo info = client.getCurrentServerEntry();
			if (info != null) {
				if (info.address != null && !info.address.isBlank()) {
					return cleanAddress(info.address);
				}
				if (info.name != null && !info.name.isBlank()) {
					return info.name;
				}
			}
		} catch (Throwable ignored) {
		}
		return "Multiplayer";
	}

	private static String cleanAddress(String address) {
		String a = address.trim();
		if (a.endsWith(".")) {
			a = a.substring(0, a.length() - 1);
		}
		if (a.length() > 48) {
			a = a.substring(0, 45) + "...";
		}
		return a;
	}
}

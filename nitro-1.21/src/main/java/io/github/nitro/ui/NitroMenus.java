package io.github.nitro.ui;

import io.github.nitro.config.NitroConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public final class NitroMenus {

	private NitroMenus() {
	}

	public static void openMainMenu(MinecraftClient client) {
		if (client == null || !NitroConfig.INSTANCE.fancyMainMenu) {
			return;
		}
		if (isInGame(client) || isWorldTearingDown(client)) {
			return;
		}
		client.setScreen(new NitroTitleScreen());
	}

	public static Screen sanitizeScreen(MinecraftClient client, Screen screen) {
		screen = mainMenuOr(screen);
		return blockMenuIfInGame(client, screen);
	}

	public static Screen mainMenuOr(Screen screen) {
		if (!NitroConfig.INSTANCE.fancyMainMenu || screen == null) {
			return screen;
		}
		if (screen.getClass() == TitleScreen.class) {
			MinecraftClient client = MinecraftClient.getInstance();
			// Never force Nitro title mid-disconnect / while the integrated server is still saving.
			if (client != null && isWorldTearingDown(client)) {
				return screen;
			}
			return new NitroTitleScreen();
		}
		if (screen instanceof GameMenuScreen gameMenu) {
			return NitroPauseScreen.from(gameMenu);
		}
		return screen;
	}

	public static Screen blockMenuIfInGame(MinecraftClient client, Screen screen) {
		if (screen == null || client == null || !NitroConfig.INSTANCE.fancyMainMenu) {
			return screen;
		}
		// Allow save / disconnect / loading screens to proceed — blocking them freezes "Saving world…".
		if (isTransientExitScreen(screen)) {
			return screen;
		}
		// Critical: do NOT swap Title → NitroTitle while world/server still exist.
		// That was freezing Leave World on "Saving world…" then the reload splash.
		if (screen instanceof TitleScreen || screen instanceof NitroTitleScreen) {
			if (isWorldTearingDown(client)) {
				return screen;
			}
			if (!isInGame(client)) {
				return screen instanceof TitleScreen ? new NitroTitleScreen() : screen;
			}
			// World still present with no network handler = disconnect in progress.
			if (client.getNetworkHandler() == null) {
				return screen;
			}
		}
		if (!isInGame(client)) {
			return screen;
		}
		if (isBlockingMenuScreen(screen)) {
			return null;
		}
		return screen;
	}

	public static boolean shouldUseNitroMainMenu(MinecraftClient client) {
		if (!NitroConfig.INSTANCE.fancyMainMenu || client == null || isInGame(client)) {
			return false;
		}
		if (isWorldTearingDown(client)) {
			return false;
		}
		if (client.getNetworkHandler() != null) {
			return false;
		}
		Screen current = client.currentScreen;
		if (isTransientExitScreen(current)) {
			return false;
		}
		return true;
	}

	/** World/player still present, or integrated server still shutting down / saving. */
	public static boolean isWorldTearingDown(MinecraftClient client) {
		return client.world != null
				|| client.player != null
				|| client.isIntegratedServerRunning();
	}

	private static boolean isInGame(MinecraftClient client) {
		return client.world != null || client.player != null;
	}

	private static boolean isTransientExitScreen(Screen screen) {
		return screen instanceof MessageScreen
				|| screen instanceof ProgressScreen
				|| screen instanceof LevelLoadingScreen
				|| screen instanceof ConnectScreen;
	}

	private static boolean isBlockingMenuScreen(Screen screen) {
		// Do not block TitleScreen / NitroTitleScreen — that traps Leave World on "Saving world…".
		return screen instanceof MultiplayerScreen
				|| screen instanceof SelectWorldScreen
				|| screen instanceof CreateWorldScreen;
	}
}

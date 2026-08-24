package io.github.nitro;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.hud.CpsTracker;
import io.github.nitro.hud.HudEditorState;
import io.github.nitro.integration.NitroAutoJoin;
import io.github.nitro.module.NitroKeys;
import io.github.nitro.module.NitroModules;
import io.github.nitro.module.impl.ZoomModule;
import io.github.nitro.spotify.SpotifyHudInput;
import io.github.nitro.spotify.SpotifyKeys;
import io.github.nitro.spotify.SpotifyManager;
import io.github.nitro.ui.NitroSettingsPopup;
import io.github.nitro.ui.VideoMenuBackground;
import io.github.nitro.ui.clickgui.ClickGuiScreen;
import io.github.nitro.ui.theme.MenuThemeAnimator;
import io.github.nitro.ui.theme.MenuThemes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NitroMod implements ClientModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger("nitroclient");
	private static final String UI_BUILD = "friends-2026-08-17";

	@Override
	public void onInitializeClient() {
		LOGGER.info("Nitro Client UI build: {}", UI_BUILD);
		NitroConfig.load();
		NitroConfig.INSTANCE.menuTheme = "nitro";
		MenuThemes.apply("nitro");
		NitroConfig.save();
		LOGGER.info("Nitro menu theme: {}", MenuThemes.currentId());
		NitroModules.init();
		SpotifyManager.INSTANCE.init();
		io.github.nitro.client.NitroHelloNetworking.registerPayloadTypes();
		io.github.nitro.client.NitroHelloNetworking.registerClient();
		NitroAutoJoin.register();
		io.github.nitro.discord.DiscordPresence.start();

		KeyBindingHelper.registerKeyBinding(NitroKeys.OPEN_MODULES);
		KeyBindingHelper.registerKeyBinding(ZoomModule.ZOOM_KEY);
		KeyBindingHelper.registerKeyBinding(SpotifyKeys.PLAY_PAUSE);
		KeyBindingHelper.registerKeyBinding(SpotifyKeys.NEXT);
		KeyBindingHelper.registerKeyBinding(SpotifyKeys.PREVIOUS);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean active = io.github.nitro.client.NitroClientActivity.isGameActive();
			if (active || !NitroConfig.INSTANCE.pauseOverlaysWhenUnfocused) {
				CpsTracker.tick(client);
				if (NitroConfig.INSTANCE.fancyMainMenu) {
					MenuThemeAnimator.tick();
				}
				NitroModules.clientTick();
				SpotifyKeys.tick();
				SpotifyHudInput.tick(client);
			} else {
				VideoMenuBackground.setPlaying(false);
			}
			if (client.player != null || client.currentScreen != null) {
				io.github.nitro.discord.DiscordPresence.tick(client);
			}
			io.github.nitro.client.NitroHelloNetworking.tick(client);
			io.github.nitro.client.NitroPresence.tick(client);
			io.github.nitro.client.NitroFriendsPresence.tick(client);
			while (NitroKeys.OPEN_MODULES.wasPressed()) {
				openClickGui(client);
			}
		});

		HudRenderCallback.EVENT.register(NitroModules::hudRender);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SpotifyManager.INSTANCE.shutdown());
	}

	private static void openClickGui(MinecraftClient client) {
		if (client.currentScreen instanceof ClickGuiScreen
				|| client.currentScreen instanceof NitroSettingsPopup) {
			HudEditorState.active = false;
			HudEditorState.endDrag();
			client.setScreen(null);
			return;
		}
		client.setScreen(new NitroSettingsPopup());
	}
}

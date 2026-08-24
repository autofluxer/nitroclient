package io.github.nitro.spotify;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class SpotifyKeys {

	public static final KeyBinding PLAY_PAUSE = new KeyBinding(
			"key.nitro.spotify_play_pause",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyBinding.Category.MISC);

	public static final KeyBinding NEXT = new KeyBinding(
			"key.nitro.spotify_next",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyBinding.Category.MISC);

	public static final KeyBinding PREVIOUS = new KeyBinding(
			"key.nitro.spotify_previous",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			KeyBinding.Category.MISC);

	private SpotifyKeys() {
	}

	public static void tick() {
		while (PLAY_PAUSE.wasPressed()) {
			if (SpotifyManager.INSTANCE.isConnected()) {
				SpotifyManager.INSTANCE.togglePlayPause();
			}
		}
		while (NEXT.wasPressed()) {
			if (SpotifyManager.INSTANCE.isConnected()) {
				SpotifyManager.INSTANCE.next();
			}
		}
		while (PREVIOUS.wasPressed()) {
			if (SpotifyManager.INSTANCE.isConnected()) {
				SpotifyManager.INSTANCE.previous();
			}
		}
	}
}

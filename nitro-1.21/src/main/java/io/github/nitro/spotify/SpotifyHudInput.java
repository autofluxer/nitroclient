package io.github.nitro.spotify;

import io.github.nitro.module.impl.SpotifyHudModule;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Clickable HUD transport buttons when the game cursor is unlocked.
 */
public final class SpotifyHudInput {

	private static boolean wasDown;

	private SpotifyHudInput() {
	}

	public static void tick(MinecraftClient client) {
		if (client == null || client.getWindow() == null) {
			return;
		}
		boolean locked = client.mouse != null && client.mouse.isCursorLocked();
		if (locked) {
			wasDown = false;
			return;
		}
		long handle = client.getWindow().getHandle();
		boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		if (down && !wasDown) {
			double scale = client.getWindow().getScaleFactor();
			double mx = client.mouse.getX() / scale;
			double my = client.mouse.getY() / scale;
			SpotifyHudModule.handleClick(mx, my);
		}
		wasDown = down;
	}
}

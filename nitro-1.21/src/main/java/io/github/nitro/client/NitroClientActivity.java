package io.github.nitro.client;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Tracks whether Nitro overlays should render / animate.
 * When Minecraft is minimized or unfocused, HUD and heavy menu video pause.
 */
public final class NitroClientActivity {

	private NitroClientActivity() {
	}

	public static boolean isGameActive() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		long handle = client.getWindow().getHandle();
		if (handle == 0L) {
			return false;
		}
		try {
			if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE) {
				return false;
			}
		} catch (Throwable ignored) {
		}
		return client.isWindowFocused();
	}

	/** True when HUD modules / overlays may draw. */
	public static boolean shouldRenderOverlays() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}
		if (client.options != null && client.options.hudHidden) {
			return false;
		}
		return isGameActive();
	}

	/** True when expensive menu video should advance frames. */
	public static boolean shouldAnimateMenus() {
		return isGameActive();
	}
}

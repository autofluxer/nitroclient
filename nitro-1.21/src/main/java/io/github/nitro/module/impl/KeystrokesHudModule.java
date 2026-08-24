package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;

public final class KeystrokesHudModule extends NitroModule implements PositionedHudModule {

	public KeystrokesHudModule() {
		super("keystrokes", "nitro.module.keystrokes.name", "nitro.module.keystrokes.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "keystrokes";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		if (!layout().visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options.hudHidden) {
			return;
		}
		long window = client.getWindow().getHandle();
		int x = layout().x;
		int y = layout().y;
		drawKey(context, x + 22, y, 20, 20, "W", pressed(window, GLFW.GLFW_KEY_W));
		drawKey(context, x, y + 22, 20, 20, "A", pressed(window, GLFW.GLFW_KEY_A));
		drawKey(context, x + 22, y + 22, 20, 20, "S", pressed(window, GLFW.GLFW_KEY_S));
		drawKey(context, x + 44, y + 22, 20, 20, "D", pressed(window, GLFW.GLFW_KEY_D));
		drawKey(context, x + 66, y + 22, 28, 20, "SPC", pressed(window, GLFW.GLFW_KEY_SPACE));
		drawKey(context, x, y + 44, 30, 18, "LMB", pressedMouse(window, GLFW.GLFW_MOUSE_BUTTON_LEFT));
		drawKey(context, x + 32, y + 44, 30, 18, "RMB", pressedMouse(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT));
	}

	private static boolean pressed(long window, int key) {
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}

	private static boolean pressedMouse(long window, int button) {
		return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
	}

	private void drawKey(DrawContext context, int x, int y, int w, int h, String label, boolean down) {
		NitroUiDraw.hudGlassChip(context, x, y, w, h, down);
		var renderer = MinecraftClient.getInstance().textRenderer;
		context.drawCenteredTextWithShadow(renderer, label, x + w / 2, y + (h - 8) / 2, NitroTheme.foreground());
	}
}

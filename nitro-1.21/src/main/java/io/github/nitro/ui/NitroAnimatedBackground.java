package io.github.nitro.ui;

import io.github.nitro.ui.background.ThemeBackgroundEngine;
import net.minecraft.client.gui.DrawContext;

public final class NitroAnimatedBackground {

	private NitroAnimatedBackground() {
	}

	public static void setSkyPalette(float[] palette) {
		ThemeBackgroundEngine.setSkyPalette(palette);
	}

	public static void render(DrawContext context, int width, int height, float delta, int mouseX, int mouseY) {
		ThemeBackgroundEngine.render(context, width, height, delta, mouseX, mouseY);
	}
}

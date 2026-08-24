package io.github.nitro.ui;

import io.github.nitro.config.NitroConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * Title-screen backdrop: Minecraft panorama, a static world plate, or the video loop.
 */
public final class TitleBackgroundRenderer {

	private TitleBackgroundRenderer() {
	}

	public static void drawStaticOrVideo(DrawContext context, int width, int height) {
		String mode = NitroConfig.INSTANCE.titleBackground;
		if ("video".equals(mode) && VideoMenuBackground.hasFrames()) {
			VideoMenuBackground.setPlaying(true);
			VideoMenuBackground.draw(context, width, height, 1.06F);
			return;
		}
		VideoMenuBackground.setPlaying(false);
		if ("menu".equals(mode)) {
			NitroDraw.drawCoverBackground(context, NitroDraw.BG_MENU, width, height, 1.04F);
		} else {
			NitroDraw.drawCoverBackground(context, NitroDraw.BG_JUNGLE_1, width, height, 1.04F);
		}
	}

	public static void drawOverlay(DrawContext context, Screen screen, float fade) {
		int alpha = Math.round(NitroConfig.INSTANCE.titleOverlayAlpha * NitroEasingClamp(fade));
		if (alpha > 0) {
			context.fill(0, 0, screen.width, screen.height, (alpha << 24));
		}
		int band = Math.max(28, screen.height / 9);
		int edge = Math.round(0x55 * NitroEasingClamp(fade));
		context.fillGradient(0, 0, screen.width, band, edge << 24, 0x00000000);
		context.fillGradient(0, screen.height - band, screen.width, screen.height, 0x00000000, (edge + 0x18) << 24);
	}

	public static boolean usePanorama() {
		String mode = NitroConfig.INSTANCE.titleBackground;
		return mode == null || mode.isBlank() || "panorama".equals(mode);
	}

	private static float NitroEasingClamp(float fade) {
		return Math.max(0F, Math.min(1F, fade));
	}
}

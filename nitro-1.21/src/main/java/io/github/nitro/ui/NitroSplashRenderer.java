package io.github.nitro.ui;

import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class NitroSplashRenderer {

	private static final long MIN_DISPLAY_MS = 900L;

	private static long startedAt = -1L;
	/** Only the first cold boot uses the branded hold; mid-session reloads must never stall. */
	private static boolean allowHold = true;

	public static void reset() {
		startedAt = -1L;
	}

	/** Call when a SplashOverlay is created. Mid-session resource reloads skip the hold. */
	public static void beginOverlay(boolean reloading) {
		startedAt = -1L;
		allowHold = !reloading;
	}

	public static boolean shouldHold() {
		if (!allowHold || startedAt < 0L) {
			return false;
		}
		return System.currentTimeMillis() - startedAt < MIN_DISPLAY_MS;
	}

	public static float displayProgress() {
		if (startedAt < 0L) {
			return 0F;
		}
		long elapsed = System.currentTimeMillis() - startedAt;
		return Math.min(1F, elapsed / (float) MIN_DISPLAY_MS);
	}

	public static void render(DrawContext context, int width, int height, float delta) {
		if (startedAt < 0L) {
			startedAt = System.currentTimeMillis();
		}

		NitroDraw.drawCoverBackground(context, NitroDraw.BG_MENU, width, height);
		context.fill(0, 0, width, height, 0x77000000);

		int size = MathHelper.clamp(Math.min(width, height) / 7, 56, 96);
		int x = width / 2 - size / 2;
		int y = height / 2 - size - 36;
		NitroUiDraw.softGlow(context, width / 2, y + size / 2, 110, NitroUiDraw.withAlpha(NitroTheme.accent(), 0x28));
		NitroDraw.blit(context, NitroDraw.LOGO, x, y, size);

		MinecraftClient mc = MinecraftClient.getInstance();
		String title = "NITRO CLIENT";
		String version = "Minecraft 1.21.11";
		String status = "Starting client…";

		int titleY = height / 2 + 4;
		context.drawTextWithShadow(mc.textRenderer, title, width / 2 - mc.textRenderer.getWidth(title) / 2, titleY, NitroTheme.foreground());
		context.drawText(mc.textRenderer, version, width / 2 - mc.textRenderer.getWidth(version) / 2, titleY + 14, NitroTheme.muted(), false);
		context.drawText(mc.textRenderer, status, width / 2 - mc.textRenderer.getWidth(status) / 2, titleY + 30, NitroTheme.accent(), false);

		int barW = Math.min(280, width - 80);
		int barX = width / 2 - barW / 2;
		int barY = height - 28;
		NitroUiDraw.fillRoundRect(context, barX, barY, barW, 4, 2, 0x66000000);
		float progress = displayProgress();
		int fillW = Math.max(2, (int) (barW * progress));
		NitroUiDraw.fillRoundRect(context, barX, barY, fillW, 4, 2, NitroTheme.accent());
	}
}

package io.github.nitro.ui;

import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;

public final class NitroLogoRenderer {

	private NitroLogoRenderer() {
	}

	public static void drawPanelHeader(DrawContext context, net.minecraft.client.gui.screen.Screen screen) {
		int panelX = MainMenuLayout.panelX(screen);
		int panelY = MainMenuLayout.panelY(screen);
		int innerW = MainMenuLayout.innerWidth(screen);
		int logoSize = 52;
		int logoX = panelX + MainMenuLayout.PAD + innerW / 2 - logoSize / 2;
		int logoY = panelY + MainMenuLayout.PAD + 4;
		drawLogo(context, logoX, logoY, logoSize);

		var client = net.minecraft.client.MinecraftClient.getInstance();
		String title = "NITRO CLIENT";
		int titleW = client.textRenderer.getWidth(title);
		int textY = logoY + logoSize + 10;
		context.drawText(client.textRenderer, title,
				panelX + MainMenuLayout.panelWidth(screen) / 2 - titleW / 2, textY, 0xFFFFFFFF, false);

		String sub = "1.21.11";
		int subW = client.textRenderer.getWidth(sub);
		context.drawText(client.textRenderer, sub,
				panelX + MainMenuLayout.panelWidth(screen) / 2 - subW / 2, textY + 12, NitroTheme.muted(), false);
	}

	public static void drawLogo(DrawContext context, int x, int y, int size) {
		drawLogo(context, x, y, size, 1F);
	}

	public static void drawLogo(DrawContext context, int x, int y, int size, float alpha) {
		NitroDraw.blit(context, NitroDraw.TITLE_LOGO, x, y, size, alpha);
	}
}

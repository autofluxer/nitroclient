/*
 * Sol Client - an open source Minecraft client
 * Copyright (C) 2021-2023  TheKodeToad and Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.solclient.client.ui.screen;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class SplashScreen {

	private static final int ACCENT = 0xFF3DB8FF;
	private static final int ACCENT_GLOW = 0x406DD0FF;
	private static final int BG_TOP = 0xFF0A1520;
	private static final int BG_MID = 0xFF0D1B2A;
	private static final int BG_BOT = 0xFF081420;
	private static final Identifier LOGO_TEXTURE = new Identifier("sol_client", "textures/gui/nitro_logo.png");

	public static final SplashScreen INSTANCE = new SplashScreen();

	private static final int STAGES = 18;

	private int stage;

	public void reset() {
		stage = 0;
	}

	public void draw() {
		if (stage > STAGES) {
			throw new IndexOutOfBoundsException(Integer.toString(stage));
		}

		MinecraftClient mc = MinecraftClient.getInstance();
		Window window = new Window(mc);
		int factor = window.getScaleFactor();
		int width = window.getWidth() * factor;
		int height = window.getHeight() * factor;

		drawBackground(width, height);
		drawAurora(width, height);
		drawLogo(width, height, mc);
		if (canDrawText(mc)) {
			drawText(width, height, mc);
		} else {
			drawTextPlaceholder(width, height);
		}
		drawProgress(width, height);

		stage++;
	}

	private static boolean canDrawText(MinecraftClient mc) {
		return mc != null && mc.textRenderer != null;
	}

	private void drawBackground(int width, int height) {
		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(7, VertexFormats.POSITION_COLOR);
		buffer.vertex(0, 0, 0).color(extractR(BG_TOP), extractG(BG_TOP), extractB(BG_TOP), 255).next();
		buffer.vertex(width, 0, 0).color(extractR(BG_TOP), extractG(BG_TOP), extractB(BG_TOP), 255).next();
		buffer.vertex(width, height / 2F, 0).color(extractR(BG_MID), extractG(BG_MID), extractB(BG_MID), 255)
				.next();
		buffer.vertex(0, height / 2F, 0).color(extractR(BG_MID), extractG(BG_MID), extractB(BG_MID), 255).next();
		buffer.vertex(0, height / 2F, 0).color(extractR(BG_MID), extractG(BG_MID), extractB(BG_MID), 255).next();
		buffer.vertex(width, height / 2F, 0).color(extractR(BG_MID), extractG(BG_MID), extractB(BG_MID), 255).next();
		buffer.vertex(width, height, 0).color(extractR(BG_BOT), extractG(BG_BOT), extractB(BG_BOT), 255).next();
		buffer.vertex(0, height, 0).color(extractR(BG_BOT), extractG(BG_BOT), extractB(BG_BOT), 255).next();
		tessellator.draw();

		GlStateManager.enableTexture();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private void drawAurora(int width, int height) {
		float pulse = (MathHelper.sin(System.currentTimeMillis() / 1800F) + 1F) * 0.5F;
		int bandY = (int) (height * 0.42F);
		int bandH = (int) (height * 0.18F);
		int alpha = (int) (28 + pulse * 24);

		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(7, VertexFormats.POSITION_COLOR);
		buffer.vertex(0, bandY, 0).color(0.24F, 0.72F, 1F, 0F).next();
		buffer.vertex(width, bandY, 0).color(0.24F, 0.72F, 1F, 0F).next();
		buffer.vertex(width, bandY + bandH, 0).color(0.24F, 0.72F, 1F, alpha / 255F).next();
		buffer.vertex(0, bandY + bandH, 0).color(0.24F, 0.72F, 1F, alpha / 255F).next();
		tessellator.draw();

		GlStateManager.enableTexture();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private void drawLogo(int width, int height, MinecraftClient mc) {
		if (mc == null || mc.getTextureManager() == null)
			return;

		int size = Math.min(width, height) / 8;
		size = MathHelper.clamp(size, 48, 96);
		int x = width / 2 - size / 2;
		int y = height / 2 - size - 28 + (int) MainMenuLogoRenderer.bob();

		try {
			mc.getTextureManager().bindTexture(LOGO_TEXTURE);
			GlStateManager.enableBlend();
			GlStateManager.color(1F, 1F, 1F, 1F);
			DrawableHelper.drawTexture(x, y, 0, 0, size, size, size, size);
			GlStateManager.color(1F, 1F, 1F, 1F);
		} catch (Exception ignored) {
			int pad = 8;
			DrawableHelper.fill(x - pad, y - pad, x + size + pad, y + size + pad, 0x66152238);
			DrawableHelper.fill(x - pad, y - pad, x + size + pad, y - pad + 2, ACCENT);
		}
	}

	private void drawTextPlaceholder(int width, int height) {
		int titleY = height / 2 + 8;
		int barW = 148;
		int barH = 10;
		DrawableHelper.fill(width / 2 - barW / 2, titleY, width / 2 + barW / 2, titleY + barH, 0x88E8F4FF);
		DrawableHelper.fill(width / 2 - 72, titleY + 18, width / 2 + 72, titleY + 24, 0x559EC5E8);
		DrawableHelper.fill(width / 2 - 40, titleY + 32, width / 2 + 40, titleY + 36, 0x663DB8FF);
		DrawableHelper.fill(width / 2 - 110, height - 52, width / 2 + 110, height - 46, 0x446B8FAD);
	}

	private void drawText(int width, int height, MinecraftClient mc) {
		String title = "NITRO CLIENT";
		String version = "Minecraft 1.8.9";
		String status = loadingStatus();
		String credit = "Client made by jovanstar & velja";

		int titleY = height / 2 + 8;
		int titleW = mc.textRenderer.getStringWidth(title);
		mc.textRenderer.draw(title, width / 2 - titleW / 2, titleY, 0xFFE8F4FF, true);

		int versionW = mc.textRenderer.getStringWidth(version);
		mc.textRenderer.draw(version, width / 2 - versionW / 2, titleY + 14, 0xFF9EC5E8, false);

		int statusW = mc.textRenderer.getStringWidth(status);
		mc.textRenderer.draw(status, width / 2 - statusW / 2, titleY + 30, ACCENT, false);

		int creditW = mc.textRenderer.getStringWidth(credit);
		mc.textRenderer.draw(credit, width / 2 - creditW / 2, height - 52, 0xFF6B8FAD, false);
	}

	private String loadingStatus() {
		if (stage < 4)
			return "Starting…";
		if (stage < 10)
			return "Loading resources…";
		if (stage < 15)
			return "Loading mods…";
		return "Almost ready…";
	}

	private void drawProgress(int width, int height) {
		int barW = Math.min(width - 80, 420);
		int barH = 4;
		int x = width / 2 - barW / 2;
		int y = height - 36;
		float progress = stage / (float) STAGES;
		float pulse = (MathHelper.sin(System.currentTimeMillis() / 220F) + 1F) * 0.5F;
		int fillW = Math.max(8, (int) (barW * progress));

		DrawableHelper.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0x55223248);
		DrawableHelper.fill(x, y, x + barW, y + barH, 0xFF152238);
		DrawableHelper.fill(x, y, x + fillW, y + barH, ACCENT);

		int glowW = Math.min(fillW + 6, barW);
		DrawableHelper.fill(x, y - 2, x + glowW, y, ACCENT_GLOW | ((int) (80 + pulse * 60) << 24));
	}

	private static int extractR(int colour) {
		return (colour >> 16) & 0xFF;
	}

	private static int extractG(int colour) {
		return (colour >> 8) & 0xFF;
	}

	private static int extractB(int colour) {
		return colour & 0xFF;
	}

}

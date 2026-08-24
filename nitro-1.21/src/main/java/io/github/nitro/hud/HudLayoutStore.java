package io.github.nitro.hud;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;

public final class HudLayoutStore {

	private HudLayoutStore() {
	}

	public static HudElementLayout get(String id) {
		return NitroConfig.INSTANCE.hudLayouts.computeIfAbsent(id, key -> defaults(id));
	}

	public static void save(String id, HudElementLayout layout) {
		NitroConfig.INSTANCE.hudLayouts.put(id, layout.copy());
		NitroConfig.save();
	}

	public static void resetAll() {
		NitroConfig.INSTANCE.hudLayouts.clear();
		NitroConfig.save();
	}

	/** Glass chip + white text (accent used only for the chip edge). */
	public static void drawLabel(DrawContext context, String text, HudElementLayout layout, int color) {
		if (!layout.visible) {
			return;
		}
		var renderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
		var matrices = context.getMatrices();
		float scale = Math.max(0.75F, layout.scale);
		int padX = 5;
		int padY = 3;
		int textW = renderer.getWidth(text);
		int chipW = textW + padX * 2;
		int chipH = 9 + padY * 2;

		matrices.pushMatrix();
		matrices.translate(layout.x, layout.y);
		matrices.scale(scale, scale);
		NitroUiDraw.hudGlassChip(context, 0, 0, chipW, chipH);
		context.drawTextWithShadow(renderer, net.minecraft.text.Text.literal(text), padX, padY, color);
		matrices.popMatrix();
	}

	public static void drawRowChip(DrawContext context, String text, int x, int y, boolean rightAlign) {
		var renderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
		int padX = 5;
		int padY = 2;
		int textW = renderer.getWidth(text);
		int chipW = textW + padX * 2 + 3;
		int chipH = 11;
		int drawX = rightAlign ? x - chipW : x;
		NitroUiDraw.hudGlassChip(context, drawX, y, chipW, chipH);
		context.fill(drawX + chipW - 2, y + 2, drawX + chipW, y + chipH - 2, NitroTheme.accent());
		context.drawTextWithShadow(renderer, text, drawX + padX, y + padY, NitroTheme.foreground());
	}

	private static HudElementLayout defaults(String id) {
		HudElementLayout layout = new HudElementLayout();
		return switch (id) {
			case "fps" -> {
				layout.x = 4;
				layout.y = 4;
				yield layout;
			}
			case "cps" -> {
				layout.x = 4;
				layout.y = 20;
				yield layout;
			}
			case "coordinates" -> {
				layout.x = 4;
				layout.y = heightAnchor() - 48;
				yield layout;
			}
			case "ping" -> {
				layout.x = 4;
				layout.y = 36;
				yield layout;
			}
			case "watermark" -> {
				layout.x = 6;
				layout.y = 6;
				layout.scale = 1.0F;
				yield layout;
			}
			case "clock" -> {
				layout.x = rightAnchor() - 80;
				layout.y = 4;
				yield layout;
			}
			case "direction" -> {
				layout.x = rightAnchor() - 60;
				layout.y = 22;
				yield layout;
			}
			case "arraylist" -> {
				layout.x = rightAnchor() - 4;
				layout.y = 42;
				yield layout;
			}
			case "keystrokes" -> {
				layout.x = rightAnchor() - 90;
				layout.y = heightAnchor() - 70;
				yield layout;
			}
			case "armor" -> {
				layout.x = rightAnchor() - 100;
				layout.y = Math.max(8, heightAnchor() / 2 - 48);
				yield layout;
			}
			case "potions" -> {
				layout.x = 4;
				layout.y = 80;
				yield layout;
			}
			case "bps" -> {
				layout.x = 4;
				layout.y = 52;
				yield layout;
			}
			case "combo" -> {
				layout.x = rightAnchor() - 80;
				layout.y = heightAnchor() / 2;
				yield layout;
			}
			case "reach" -> {
				layout.x = 4;
				layout.y = 68;
				yield layout;
			}
			case "target" -> {
				layout.x = rightAnchor() / 2 - 40;
				layout.y = 60;
				yield layout;
			}
			case "spotify" -> {
				layout.x = 8;
				layout.y = heightAnchor() - 96;
				layout.scale = 1F;
				yield layout;
			}
			default -> layout;
		};
	}

	private static int heightAnchor() {
		var client = net.minecraft.client.MinecraftClient.getInstance();
		return client != null ? client.getWindow().getScaledHeight() : 480;
	}

	private static int rightAnchor() {
		var client = net.minecraft.client.MinecraftClient.getInstance();
		return client != null ? client.getWindow().getScaledWidth() : 854;
	}
}

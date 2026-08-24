package io.github.nitro.hud;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class HudEditorState {

	public static boolean active;
	public static boolean snapGrid = false;
	public static String draggingId;
	public static int dragOffsetX;
	public static int dragOffsetY;
	private static int previewOriginX;
	private static int previewOriginY;
	private static int previewW;
	private static int previewH;

	private HudEditorState() {
	}

	public static void beginDrag(String id, int mouseX, int mouseY) {
		HudElementLayout layout = HudLayoutStore.get(id);
		draggingId = id;
		dragOffsetX = mouseX - layout.x;
		dragOffsetY = mouseY - layout.y;
	}

	public static void beginDragPreview(String id, int mouseX, int mouseY, int boxX, int boxY, int boxW, int boxH) {
		HudElementLayout layout = HudLayoutStore.get(id);
		int stageX = boxX + 10;
		int stageY = boxY + 40;
		int stageW = boxW - 20;
		int stageH = boxH - 52;
		float scaleX = stageW / (float) screenW();
		float scaleY = stageH / (float) screenH();
		int px = stageX + (int) (layout.x * scaleX);
		int py = stageY + (int) (layout.y * scaleY);
		draggingId = id;
		dragOffsetX = mouseX - px;
		dragOffsetY = mouseY - py;
		previewOriginX = stageX;
		previewOriginY = stageY;
		previewW = stageW;
		previewH = stageH;
	}

	public static void drag(int mouseX, int mouseY) {
		if (draggingId == null) {
			return;
		}
		HudElementLayout layout = HudLayoutStore.get(draggingId);
		int x = mouseX - dragOffsetX;
		int y = mouseY - dragOffsetY;
		if (snapGrid) {
			x = Math.round(x / 8F) * 8;
			y = Math.round(y / 8F) * 8;
		}
		layout.x = clamp(x, 0, Math.max(0, screenW() - 40));
		layout.y = clamp(y, 0, Math.max(0, screenH() - 12));
	}

	public static void dragPreview(int mouseX, int mouseY, int boxX, int boxY, int boxW, int boxH) {
		if (draggingId == null) {
			return;
		}
		int stageX = boxX + 10;
		int stageY = boxY + 40;
		int stageW = boxW - 20;
		int stageH = boxH - 52;
		HudElementLayout layout = HudLayoutStore.get(draggingId);
		int px = mouseX - dragOffsetX;
		int py = mouseY - dragOffsetY;
		px = clamp(px, stageX, stageX + stageW - 40);
		py = clamp(py, stageY, stageY + stageH - 12);
		float scaleX = screenW() / (float) stageW;
		float scaleY = screenH() / (float) stageH;
		layout.x = (int) ((px - stageX) * scaleX);
		layout.y = (int) ((py - stageY) * scaleY);
		previewOriginX = stageX;
		previewOriginY = stageY;
		previewW = stageW;
		previewH = stageH;
	}

	public static void endDrag() {
		if (draggingId != null) {
			HudLayoutStore.save(draggingId, HudLayoutStore.get(draggingId));
		}
		draggingId = null;
	}

	public static void drawOverlay(DrawContext context, int width, int height) {
		if (!active) {
			return;
		}
		// Light veil only — HudEditorScreen already dims. Avoid double-darkening.
		if (!(MinecraftClient.getInstance().currentScreen instanceof io.github.nitro.ui.HudEditorScreen)) {
			context.fill(0, 0, width, height, 0x44000000);
		}
		if (snapGrid) {
			drawGrid(context, 0, 0, width, height);
		}
	}

	public static void drawPreview(DrawContext context, int x, int y, int w, int h) {
		NitroUiDraw.fillRoundRect(context, x, y, w, h, 14, 0xF0101218);
		NitroUiDraw.strokeRoundRect(context, x, y, w, h, 14, NitroUiDraw.withAlpha(NitroTheme.accent(), 0x44));

		var client = MinecraftClient.getInstance();
		var renderer = client.textRenderer;

		context.drawText(renderer, Text.literal("Layout preview"), x + 14, y + 12, NitroTheme.muted(), false);
		context.drawText(renderer, Text.literal("Drag chips to move"), x + 14, y + 24, 0xFF6A7380, false);

		int stageX = x + 10;
		int stageY = y + 40;
		int stageW = w - 20;
		int stageH = h - 52;
		NitroUiDraw.fillRoundRect(context, stageX, stageY, stageW, stageH, 12, 0xFF0A0C10);
		NitroUiDraw.strokeRoundRect(context, stageX, stageY, stageW, stageH, 12, 0x14FFFFFF);

		if (client.player != null) {
			int cx = stageX + stageW / 2;
			int size = Math.min(60, stageH / 3);
			int x1 = cx - 40;
			int y1 = stageY + stageH / 2 - 72;
			int x2 = cx + 40;
			int y2 = stageY + stageH / 2 + 56;
			try {
				net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
						context, x1, y1, x2, y2, size, 0.0625F,
						(float) cx, (float) (stageY + stageH / 2 - 10), client.player);
			} catch (Throwable ignored) {
				NitroUiDraw.fillRoundRect(context, cx - 24, stageY + stageH / 2 - 40, 48, 80, 10, 0x66101820);
			}
		}

		float scaleX = stageW / (float) screenW();
		float scaleY = stageH / (float) screenH();

		context.enableScissor(stageX + 1, stageY + 1, stageX + stageW - 1, stageY + stageH - 1);
		for (String id : HudElements.ALL) {
			HudElementLayout layout = HudLayoutStore.get(id);
			if (!layout.visible) {
				continue;
			}
			String label = Text.translatable("nitro.hud." + id).getString();
			int tw = Math.min(renderer.getWidth(label) + 14, stageW - 8);
			int th = 16;
			int px = stageX + (int) (layout.x * scaleX);
			int py = stageY + (int) (layout.y * scaleY);
			px = clamp(px, stageX + 4, stageX + stageW - tw - 4);
			py = clamp(py, stageY + 4, stageY + stageH - th - 4);
			boolean selected = id.equals(draggingId);
			int fill = selected
					? NitroUiDraw.withAlpha(NitroTheme.accent(), 0x88)
					: 0xCC1A1E28;
			NitroUiDraw.fillRoundRect(context, px, py, tw, th, 6, fill);
			NitroUiDraw.strokeRoundRect(context, px, py, tw, th, 6,
					selected ? NitroTheme.accent() : 0x33FFFFFF);
			String clipped = renderer.trimToWidth(label, tw - 10);
			context.drawText(renderer, clipped, px + 5, py + 4, NitroTheme.foreground(), false);
		}
		context.disableScissor();
	}

	public static String hitPreview(double mouseX, double mouseY, int boxX, int boxY, int boxW, int boxH) {
		var renderer = MinecraftClient.getInstance().textRenderer;
		int stageX = boxX + 10;
		int stageY = boxY + 40;
		int stageW = boxW - 20;
		int stageH = boxH - 52;
		float scaleX = stageW / (float) screenW();
		float scaleY = stageH / (float) screenH();
		for (int i = HudElements.ALL.size() - 1; i >= 0; i--) {
			String id = HudElements.ALL.get(i);
			HudElementLayout layout = HudLayoutStore.get(id);
			if (!layout.visible) {
				continue;
			}
			String label = Text.translatable("nitro.hud." + id).getString();
			int tw = Math.min(renderer.getWidth(label) + 14, stageW - 8);
			int th = 16;
			int px = stageX + (int) (layout.x * scaleX);
			int py = stageY + (int) (layout.y * scaleY);
			px = clamp(px, stageX + 4, stageX + stageW - tw - 4);
			py = clamp(py, stageY + 4, stageY + stageH - th - 4);
			if (mouseX >= px && mouseX <= px + tw && mouseY >= py && mouseY <= py + th) {
				return id;
			}
		}
		return null;
	}

	public static String hitScreen(double mouseX, double mouseY) {
		var renderer = MinecraftClient.getInstance().textRenderer;
		for (int i = HudElements.ALL.size() - 1; i >= 0; i--) {
			String id = HudElements.ALL.get(i);
			HudElementLayout layout = HudLayoutStore.get(id);
			if (!layout.visible) {
				continue;
			}
			String label = Text.translatable("nitro.hud." + id).getString();
			float scale = Math.max(0.75F, layout.scale);
			// Generous hit box so chips are easy to grab while Nitro Settings is open.
			int tw = Math.max(48, (int) (renderer.getWidth(label) * scale) + 28);
			int th = Math.max(18, (int) (14 * scale) + 10);
			int px = layout.x - 6;
			int py = layout.y - 4;
			if (mouseX >= px && mouseX <= px + tw && mouseY >= py && mouseY <= py + th) {
				return id;
			}
		}
		return null;
	}

	private static void drawGrid(DrawContext context, int x, int y, int w, int h) {
		int grid = NitroUiDraw.withAlpha(NitroTheme.accent(), 0x12);
		for (int gx = x; gx < x + w; gx += 8) {
			context.fill(gx, y, gx + 1, y + h, grid);
		}
		for (int gy = y; gy < y + h; gy += 8) {
			context.fill(x, gy, x + w, gy + 1, grid);
		}
	}

	private static int screenW() {
		var client = MinecraftClient.getInstance();
		return client != null ? client.getWindow().getScaledWidth() : 854;
	}

	private static int screenH() {
		var client = MinecraftClient.getInstance();
		return client != null ? client.getWindow().getScaledHeight() : 480;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}

package io.github.nitro.ui;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.config.NitroConfig;
import io.github.nitro.hud.HudEditorState;
import io.github.nitro.hud.HudElements;
import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.ui.clickgui.ClickGuiScreen;
import io.github.nitro.ui.clickgui.ClickGuiTab;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

/**
 * Studio HUD editor: soft background, player preview in the center, drag HUD chips around them.
 */
public final class HudEditorScreen extends Screen {

	private static final int HUB_H = 34;
	private static final int SIDE = 34;
	private static final int MODS_W = 132;
	private static final int GAP = 8;
	private static final int LOGO = 40;

	private int hubX;
	private int hubY;
	private int hubW;
	private int stageX;
	private int stageY;
	private int stageW;
	private int stageH;

	public HudEditorScreen() {
		super(Text.literal("HUD Editor"));
	}

	@Override
	protected void init() {
		NitroConfig.reloadTheme();
		HudEditorState.active = true;
		HudEditorState.snapGrid = false;
		clearChildren();
		warmLayouts();

		stageW = Math.min(420, width - 80);
		stageH = Math.min(280, height - 120);
		stageX = (width - stageW) / 2;
		stageY = Math.max(56, (height - stageH) / 2 - 10);

		hubW = SIDE + GAP + MODS_W + GAP + SIDE;
		hubX = (width - hubW) / 2;
		hubY = Math.max(10, stageY - 46);

		addDrawableChild(new NitroActionButton(hubX, hubY, SIDE, HUB_H, Text.literal("☺"),
				NitroActionButton.Style.TRANSPARENT,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.MODULES))));
		addDrawableChild(new LunarMenuButton(hubX + SIDE + GAP, hubY, MODS_W, HUB_H, Text.literal("MODS"), false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.MODULES))));
		addDrawableChild(new NitroActionButton(hubX + SIDE + GAP + MODS_W + GAP, hubY, SIDE, HUB_H, Text.literal("⌂"),
				NitroActionButton.Style.TRANSPARENT,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.MODULES))));
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		NitroDraw.drawCoverBackground(context, NitroDraw.BG_JUNGLE_2, width, height, 1.12F);
		context.fill(0, 0, width, height, 0xB0000000);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		NitroLogoRenderer.drawLogo(context, width / 2 - LOGO / 2, Math.max(6, hubY - LOGO - 4), LOGO);

		// Stage panel
		NitroUiDraw.fillRoundRect(context, stageX - 2, stageY - 2, stageW + 4, stageH + 4, 14, 0x66000000);
		NitroUiDraw.fillRoundRect(context, stageX, stageY, stageW, stageH, 12, 0xE6121216);
		NitroUiDraw.strokeRoundRect(context, stageX, stageY, stageW, stageH, 12, 0x33FFFFFF);

		drawPlayerPreview(context, mouseX, mouseY);

		String stageHint = "Your skin";
		context.drawText(textRenderer, stageHint,
				stageX + (stageW - textRenderer.getWidth(stageHint)) / 2,
				stageY + stageH - 14, 0x66FFFFFF, false);

		super.render(context, mouseX, mouseY, delta);

		drawHudChips(context);

		String hint = "Drag HUD chips · Right Shift to close";
		context.drawText(textRenderer, hint, width / 2 - textRenderer.getWidth(hint) / 2, height - 14, 0x88FFFFFF, false);
	}

	private void drawPlayerPreview(DrawContext context, int mouseX, int mouseY) {
		if (client.player == null) {
			drawSilhouette(context);
			return;
		}
		int cx = stageX + stageW / 2;
		int cy = stageY + stageH / 2 + 20;
		int size = Math.min(70, stageH / 3);
		int x1 = cx - 40;
		int y1 = stageY + 16;
		int x2 = cx + 40;
		int y2 = stageY + stageH - 20;
		try {
			InventoryScreen.drawEntity(context, x1, y1, x2, y2, size, 0.0625F, mouseX, mouseY, client.player);
		} catch (Throwable ignored) {
			drawSilhouette(context);
		}
	}

	private void drawSilhouette(DrawContext context) {
		int cx = stageX + stageW / 2;
		int cy = stageY + stageH / 2;
		NitroUiDraw.fillRoundRect(context, cx - 28, cy - 48, 56, 96, 12, 0x66101820);
		NitroUiDraw.strokeRoundRect(context, cx - 28, cy - 48, 56, 96, 12, NitroUiDraw.withAlpha(NitroTheme.accent(), 0x66));
	}

	private void drawHudChips(DrawContext context) {
		for (String id : HudElements.ALL) {
			HudElementLayout layout = HudLayoutStore.get(id);
			if (!layout.visible) {
				continue;
			}
			String label = Text.translatable("nitro.hud." + id).getString();
			int tw = textRenderer.getWidth(label) + 14;
			int th = 16;
			int x = layout.x;
			int y = layout.y;
			boolean sel = id.equals(HudEditorState.draggingId);
			NitroUiDraw.fillRoundRect(context, x, y, tw, th, 5, sel ? 0xCC1A3A4A : 0xBB18181C);
			NitroUiDraw.strokeRoundRect(context, x, y, tw, th, 5,
					sel ? NitroUiDraw.withAlpha(NitroTheme.accent(), 0xEE) : 0x44FFFFFF);
			context.drawText(textRenderer, label, x + 7, y + 4, NitroTheme.foreground(), false);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			String hit = hitChip(click.x(), click.y());
			if (hit != null) {
				HudEditorState.beginDrag(hit, (int) click.x(), (int) click.y());
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	private String hitChip(double mouseX, double mouseY) {
		for (int i = HudElements.ALL.size() - 1; i >= 0; i--) {
			String id = HudElements.ALL.get(i);
			HudElementLayout layout = HudLayoutStore.get(id);
			if (!layout.visible) {
				continue;
			}
			String label = Text.translatable("nitro.hud." + id).getString();
			int tw = textRenderer.getWidth(label) + 14;
			int th = 16;
			if (mouseX >= layout.x && mouseX <= layout.x + tw && mouseY >= layout.y && mouseY <= layout.y + th) {
				return id;
			}
		}
		return null;
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (HudEditorState.draggingId != null) {
			HudEditorState.drag((int) click.x(), (int) click.y());
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (HudEditorState.draggingId != null) {
			HudEditorState.endDrag();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public void close() {
		HudEditorState.active = false;
		HudEditorState.endDrag();
		client.setScreen(null);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	public static void warmLayouts() {
		for (String id : HudElements.ALL) {
			HudLayoutStore.get(id);
		}
	}
}

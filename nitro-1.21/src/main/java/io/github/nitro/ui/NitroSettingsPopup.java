package io.github.nitro.ui;

import io.github.nitro.hud.HudEditorState;
import io.github.nitro.ui.clickgui.ClickGuiScreen;
import io.github.nitro.ui.clickgui.ClickGuiTab;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * In-game Feather hub: logo, NITRO SETTINGS, four icon chips.
 */
public final class NitroSettingsPopup extends Screen {

	private static final int BTN_W = 168;
	private static final int BTN_H = 24;
	private static final int CHIP = 28;
	private static final int GAP = 5;

	private int hubX;
	private int hubY;

	public NitroSettingsPopup() {
		super(Text.literal("Nitro Settings"));
	}

	@Override
	protected void init() {
		HudEditorState.active = true;
		HudEditorState.snapGrid = false;
		clearChildren();

		int rowW = CHIP * 4 + GAP * 3;
		int blockW = Math.max(BTN_W, rowW);
		hubX = (width - blockW) / 2;
		hubY = height / 2 - 36;

		int settingsX = hubX + (blockW - BTN_W) / 2;
		int settingsY = hubY + 40;
		addDrawableChild(new LunarMenuButton(settingsX, settingsY, BTN_W, BTN_H,
				Text.literal("NITRO SETTINGS"), LunarMenuButton.Icon.MODS, LunarMenuButton.Style.NORMAL,
				b -> {
					if (client != null) {
						client.setScreen(new ClickGuiScreen(this));
					}
				}));

		int chipX = hubX + (blockW - rowW) / 2;
		int chipY = settingsY + BTN_H + 6;
		addDrawableChild(new TitleIconButton(chipX, chipY, TitleIconButton.Kind.MODS, "Mods", false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.MODULES))));
		addDrawableChild(new TitleIconButton(chipX + CHIP + GAP, chipY, TitleIconButton.Kind.CHAT, "Social", false,
				b -> openDiscord()));
		addDrawableChild(new TitleIconButton(chipX + (CHIP + GAP) * 2, chipY, TitleIconButton.Kind.STORE, "Cosmetics", false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.MODULES))));
		addDrawableChild(new TitleIconButton(chipX + (CHIP + GAP) * 3, chipY, TitleIconButton.Kind.HUD, "HUD", false,
				b -> client.setScreen(new ClickGuiScreen(this, ClickGuiTab.HUD))));
	}

	private static void openDiscord() {
		try {
			Util.getOperatingSystem().open(java.net.URI.create("https://discord.gg/nitrosmp"));
		} catch (Throwable ignored) {
		}
	}

	@Override
	public void close() {
		HudEditorState.active = false;
		HudEditorState.endDrag();
		if (client != null) {
			client.setScreen(null);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0x66000000);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int logo = 32;
		NitroLogoRenderer.drawLogo(context, width / 2 - logo / 2, hubY, logo);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0 && HudEditorState.active) {
			String hit = HudEditorState.hitScreen(click.x(), click.y());
			if (hit != null) {
				HudEditorState.beginDrag(hit, (int) click.x(), (int) click.y());
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
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
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (HudEditorState.draggingId != null) {
			HudEditorState.drag((int) click.x(), (int) click.y());
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}
}

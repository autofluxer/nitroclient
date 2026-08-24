package io.github.nitro.ui;

import net.minecraft.client.gui.screen.Screen;

public final class MainMenuLayout {

	public static final int SIDEBAR_W = 220;
	public static final int PAD = 16;
	public static final int GAP = 6;
	public static final int ROW_H = 34;
	public static final int HEADER_H = 64;
	public static final int FOOTER_H = 32;
	public static final int MARGIN = 0;

	private MainMenuLayout() {
	}

	public static int sidebarX(Screen screen) {
		return 0;
	}

	public static int sidebarY(Screen screen) {
		return 0;
	}

	public static int sidebarH(Screen screen) {
		return screen.height;
	}

	public static int innerLeft(Screen screen) {
		return SIDEBAR_W + PAD;
	}

	public static int innerWidth(Screen screen) {
		return Math.max(200, screen.width - SIDEBAR_W - PAD * 2);
	}

	public static int serverRowY(Screen screen) {
		return HEADER_H + PAD;
	}

	public static int navRowY(Screen screen, int row) {
		return serverRowY(screen) + ROW_H + GAP + row * (ROW_H + GAP);
	}

	public static int themesRowY(Screen screen) {
		return navRowY(screen, 3) + ROW_H + GAP + 8;
	}

	public static int utilityRowY(Screen screen) {
		return themesRowY(screen) + ROW_H + GAP + 8;
	}

	public static int panelX(Screen screen) {
		return sidebarX(screen);
	}

	public static int panelY(Screen screen) {
		return sidebarY(screen);
	}

	public static int panelWidth(Screen screen) {
		return SIDEBAR_W;
	}

	public static int panelHeight(Screen screen) {
		return sidebarH(screen);
	}

	public static int contentWidth(Screen screen) {
		return Math.min(720, Math.max(420, screen.width - 96));
	}

	public static int contentLeft(Screen screen) {
		return (screen.width - contentWidth(screen)) / 2;
	}

	public static int modalPanelY(Screen screen) {
		return Math.max(24, (screen.height - modalPanelHeight(screen) - FOOTER_H) / 2);
	}

	public static int modalPanelHeight(Screen screen) {
		return Math.min(screen.height - 80, 520);
	}

	public static int clickGuiWidth(Screen screen) {
		return screen.width - ClickGuiLayoutMargin(screen) * 2;
	}

	public static int clickGuiHeight(Screen screen) {
		return screen.height - ClickGuiLayoutMargin(screen) * 2;
	}

	private static int ClickGuiLayoutMargin(Screen screen) {
		return 28;
	}
}

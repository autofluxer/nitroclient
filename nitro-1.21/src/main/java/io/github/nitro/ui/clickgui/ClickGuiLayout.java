package io.github.nitro.ui.clickgui;

import net.minecraft.client.gui.screen.Screen;

public final class ClickGuiLayout {

	public static final int MARGIN = 28;
	public static final int SIDEBAR_W = 152;
	public static final int HEADER_H = 56;
	public static final int ROW_H = 32;
	public static final int GAP = 6;

	private ClickGuiLayout() {
	}

	public static int panelX(Screen screen) {
		return MARGIN;
	}

	public static int panelY(Screen screen) {
		return MARGIN;
	}

	public static int panelW(Screen screen) {
		return screen.width - MARGIN * 2;
	}

	public static int panelH(Screen screen) {
		return screen.height - MARGIN * 2;
	}

	public static int sidebarX(Screen screen) {
		return panelX(screen);
	}

	public static int sidebarY(Screen screen) {
		return panelY(screen);
	}

	public static int sidebarH(Screen screen) {
		return panelH(screen);
	}

	public static int contentX(Screen screen) {
		return panelX(screen) + SIDEBAR_W + 1;
	}

	public static int contentY(Screen screen) {
		return panelY(screen);
	}

	public static int contentW(Screen screen) {
		return panelW(screen) - SIDEBAR_W - 1;
	}

	public static int contentH(Screen screen) {
		return panelH(screen);
	}

	public static int navStartY(Screen screen) {
		return panelY(screen) + HEADER_H;
	}

	public static int previewW(Screen screen) {
		return Math.min(contentW(screen) - 32, 420);
	}

	public static int previewH(Screen screen) {
		return (int) (previewW(screen) * 9F / 16F);
	}
}

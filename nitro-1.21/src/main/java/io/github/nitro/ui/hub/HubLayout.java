package io.github.nitro.ui.hub;

import net.minecraft.client.gui.screen.Screen;

public final class HubLayout {

	public static final int PANEL_MARGIN = 36;
	public static final int HEADER_H = 52;
	public static final int TAB_H = 34;
	public static final int TOOLBAR_H = 44;
	public static final int CARD_W = 210;
	public static final int CARD_H = 132;
	public static final int CARD_GAP = 14;
	public static final int PAD = 20;

	private HubLayout() {
	}

	public static int panelX(Screen screen) {
		return PANEL_MARGIN;
	}

	public static int panelY(Screen screen) {
		return PANEL_MARGIN;
	}

	public static int panelWidth(Screen screen) {
		return screen.width - PANEL_MARGIN * 2;
	}

	public static int panelHeight(Screen screen) {
		return screen.height - PANEL_MARGIN * 2;
	}

	public static int gridTop(Screen screen) {
		return panelY(screen) + HEADER_H + TAB_H + TOOLBAR_H + PAD;
	}

	public static int gridLeft(Screen screen) {
		return panelX(screen) + PAD;
	}

	public static int gridWidth(Screen screen) {
		return panelWidth(screen) - PAD * 2;
	}

	public static int gridHeight(Screen screen) {
		return panelHeight(screen) - HEADER_H - TAB_H - TOOLBAR_H - PAD * 2;
	}

	public static int columns(Screen screen) {
		return Math.max(1, (gridWidth(screen) + CARD_GAP) / (CARD_W + CARD_GAP));
	}

	public static int cardX(Screen screen, int index, int columns) {
		int col = index % columns;
		return gridLeft(screen) + col * (CARD_W + CARD_GAP);
	}

	public static int cardY(Screen screen, int index, int columns, double scrollY) {
		int row = index / columns;
		return (int) (gridTop(screen) + row * (CARD_H + CARD_GAP) - scrollY);
	}

	public static int contentHeight(int itemCount, int columns) {
		int rows = (itemCount + columns - 1) / columns;
		return rows * (CARD_H + CARD_GAP);
	}
}

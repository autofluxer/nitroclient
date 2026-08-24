package io.github.nitro.ui;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;

public class NitroSubScreen extends Screen {

	protected final Screen parent;

	protected NitroSubScreen(Text title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	protected void drawChrome(DrawContext context) {
		int left = MainMenuLayout.contentLeft(this);
		int top = MainMenuLayout.modalPanelY(this);
		int w = MainMenuLayout.contentWidth(this);
		int h = MainMenuLayout.modalPanelHeight(this);
		NitroUiDraw.glassPanel(context, left, top, w, h);

		int titleW = textRenderer.getWidth(title);
		context.drawTextWithShadow(textRenderer, title, left + w / 2 - titleW / 2, top + 16, NitroTheme.foreground());
		NitroUiDraw.divider(context, left + MainMenuLayout.PAD, top + 36, w - MainMenuLayout.PAD * 2);
	}

	protected int listTop() {
		return MainMenuLayout.modalPanelY(this) + 46;
	}

	protected int listLeft() {
		return MainMenuLayout.contentLeft(this) + MainMenuLayout.PAD;
	}

	protected int listWidth() {
		return MainMenuLayout.contentWidth(this) - MainMenuLayout.PAD * 2;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		NitroAnimatedBackground.render(context, width, height, delta, mouseX, mouseY);
	}

	@Override
	public void close() {
		if (NitroConfig.INSTANCE.fancyMainMenu && (parent == null || parent.getClass() == TitleScreen.class)) {
			NitroMenus.openMainMenu(client);
		} else {
			client.setScreen(parent);
		}
	}
}

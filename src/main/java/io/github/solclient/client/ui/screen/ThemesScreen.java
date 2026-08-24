/*
 * Nitro Client - main menu theme picker
 */
package io.github.solclient.client.ui.screen;

import org.lwjgl.nanovg.NanoVG;

import io.github.solclient.client.SolClient;
import io.github.solclient.client.ui.ScreenAnimation;
import io.github.solclient.client.ui.Theme;
import io.github.solclient.client.ui.component.*;
import io.github.solclient.client.ui.component.controller.Controller;
import io.github.solclient.client.ui.component.handler.ClickHandler;
import io.github.solclient.client.ui.screen.MainMenuActionButton.Style;
import io.github.solclient.client.util.MinecraftUtils;
import io.github.solclient.client.util.data.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

public class ThemesScreen extends PanoramaBackgroundScreen {

	private final Screen parent;
	private final ScreenAnimation animation = new ScreenAnimation();

	public ThemesScreen(Screen parent) {
		super(new ThemesPanel(parent));
		this.parent = parent;
		background = false;
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		drawPanorama(mouseX, mouseY, partialTicks);
		super.render(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void wrap(Runnable task) {
		animation.wrap(task);
	}

	@Override
	public void removed() {
		super.removed();
		animation.close();
		SolClient.INSTANCE.saveAll();
	}

	private static ClickHandler click(Runnable action) {
		return (info, button) -> {
			if (button != 0) {
				return false;
			}
			MinecraftUtils.playClickSound(true);
			action.run();
			return true;
		};
	}

	private static class ThemesPanel extends Component {

		ThemesPanel(Screen parent) {
			MainMenuActionButton back = new MainMenuActionButton(
					(c, t) -> I18n.translate("gui.back"), Theme.button(), Theme.fg())
					.style(Style.SECONDARY).withIcon("exit")
					.onClick(click(() -> mc.setScreen(parent)));
			add(back, row(back, 0));

			MainMenuActionButton random = new MainMenuActionButton(
					(c, t) -> I18n.translate("sol_client.main_menu.themes.random"), Theme.accent(), Theme.fg())
					.style(Style.ACCENT).withIcon("options")
					.onClick(click(() -> {
						MenuThemes.applyRandom();
						SolClient.INSTANCE.saveAll();
						mc.setScreen(new ThemesScreen(parent));
					}));
			add(random, row(random, 1));

			int row = 2;
			for (MenuThemePreset preset : MenuThemes.all()) {
				boolean selected = preset.getId().equals(MenuThemes.currentId());
				MainMenuActionButton themeBtn = new MainMenuActionButton(
						(c, t) -> themeLabel(preset, selected), selected ? Theme.accent() : Theme.button(),
						Theme.fg())
						.style(selected ? Style.ACCENT : Style.SECONDARY)
						.onClick(click(() -> {
							MenuThemes.apply(preset.getId());
							SolClient.INSTANCE.saveAll();
							mc.setScreen(new ThemesScreen(parent));
						}));
				add(themeBtn, row(themeBtn, row));
				row++;
			}
		}

		@Override
		public void render(ComponentRenderInfo info) {
			drawTitle();
			super.render(info);
		}

		private void drawTitle() {
			String title = I18n.translate("sol_client.main_menu.themes.title");
			NanoVG.nvgFontSize(nvg, 18);
			float w = regularFont.getWidth(nvg, title);
			NanoVG.nvgFillColor(nvg, Theme.getCurrent().fg.nvg());
			regularFont.renderString(nvg, title, screen.width / 2F - w / 2F, MainMenuLayout.blockTop(screen) - 28);
		}

		private static String themeLabel(MenuThemePreset preset, boolean selected) {
			String name = I18n.translate(preset.getNameKey());
			if (selected) {
				return name + " · " + I18n.translate("sol_client.main_menu.themes.selected");
			}
			return name;
		}

		private static Controller<Rectangle> row(MainMenuActionButton btn, int index) {
			return (c, b) -> {
				Screen screen = c.getScreen();
				int w = Math.min(300, MainMenuLayout.contentWidth(screen));
				int h = MainMenuLayout.buttonHeight(screen);
				btn.width(w);
				btn.height(h);
				int x = screen.width / 2 - w / 2;
				int y = MainMenuLayout.blockTop(screen) + index * (h + MainMenuLayout.gap());
				return new Rectangle(x, y, w, h);
			};
		}
	}

}

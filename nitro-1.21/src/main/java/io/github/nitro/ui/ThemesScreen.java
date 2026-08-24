package io.github.nitro.ui;

import io.github.nitro.ui.clickgui.ThemeCardWidget;
import io.github.nitro.ui.theme.MenuThemeAnimator;
import io.github.nitro.ui.theme.MenuThemePreset;
import io.github.nitro.ui.theme.MenuThemes;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Lunar-style theme picker grid. */
public final class ThemesScreen extends NitroSubScreen {

	public ThemesScreen(Screen parent) {
		super(Text.translatable("nitro.menu.themes"), parent);
	}

	@Override
	protected void init() {
		clearChildren();
		int left = Math.max(24, (width - Math.min(720, width - 48)) / 2);
		int top = Math.max(40, height / 2 - 180);
		int panelW = Math.min(720, width - 48);

		addDrawableChild(new NitroActionButton(left, top, 100, 26, Text.translatable("gui.back"),
				NitroActionButton.Style.GHOST, button -> client.setScreen(parent)));
		addDrawableChild(new NitroActionButton(left + panelW - 150, top, 150, 26, Text.literal("Reset to default"),
				NitroActionButton.Style.NAV, button -> {
					MenuThemes.apply("nitro");
					client.setScreen(new ThemesScreen(parent));
				}));

		int cardW = 150;
		int cardH = 78;
		int gap = 12;
		int cols = Math.max(1, (panelW + gap) / (cardW + gap));
		int i = 0;
		int startY = top + 40;
		for (MenuThemePreset preset : MenuThemes.all()) {
			boolean selected = preset.getId().equals(MenuThemes.currentId());
			int col = i % cols;
			int row = i / cols;
			int x = left + col * (cardW + gap);
			int y = startY + row * (cardH + gap);
			addDrawableChild(new ThemeCardWidget(x, y, cardW, cardH, preset, selected, () -> {
				MenuThemes.apply(preset.getId());
				client.setScreen(new ThemesScreen(parent));
			}));
			i++;
		}
	}

	@Override
	public void tick() {
		MenuThemeAnimator.tick();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		drawChrome(context);
		super.render(context, mouseX, mouseY, delta);

		int left = Math.max(24, (width - Math.min(720, width - 48)) / 2);
		context.drawTextWithShadow(textRenderer, Text.literal("Select Theme"), left, Math.max(40, height / 2 - 180) - 18,
				NitroTheme.foreground());
		context.drawTextWithShadow(textRenderer, Text.literal("Active: " + MenuThemes.currentId()),
				left, height - 28, NitroTheme.muted());
	}
}

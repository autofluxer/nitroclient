package io.github.nitro.ui.clickgui;

import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.MenuThemePreset;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class ThemeCardWidget extends ClickableWidget {

	private final MenuThemePreset preset;
	private final boolean selected;
	private final Runnable onSelect;

	public ThemeCardWidget(int x, int y, int width, int height, MenuThemePreset preset, boolean selected, Runnable onSelect) {
		super(x, y, width, height, Text.translatable(preset.getNameKey()));
		this.preset = preset;
		this.selected = selected;
		this.onSelect = onSelect;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 12,
				hovered || selected ? 0x44141618 : 0x33000000);
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, 12,
				selected ? NitroUiDraw.withAlpha(NitroTheme.accent(), 204) : (hovered ? 0x66FFFFFF : 0x33FFFFFF));
		int thumbH = 36;
		NitroUiDraw.fillRoundRect(context, getX() + 8, getY() + 8, width - 16, thumbH, 8, preset.background() | 0xFF000000);
		context.fill(getX() + 12, getY() + 12, getX() + 28, getY() + 28, preset.accent() | 0xFF000000);
		context.fill(getX() + 32, getY() + 12, getX() + 48, getY() + 28, preset.secondaryAccent() | 0xFF000000);
		var tr = MinecraftClient.getInstance().textRenderer;
		String name = getMessage().getString();
		context.drawTextWithShadow(tr, name, getX() + 10, getY() + height - 22, NitroTheme.foreground());
		int dot = selected ? NitroTheme.accent() : 0x55FFFFFF;
		NitroUiDraw.fillRoundRect(context, getX() + width - 18, getY() + height - 18, 8, 8, 4, dot);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		onSelect.run();
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

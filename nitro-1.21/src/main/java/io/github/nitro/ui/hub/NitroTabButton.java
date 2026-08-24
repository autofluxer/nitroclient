package io.github.nitro.ui.hub;

import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class NitroTabButton extends ClickableWidget {

	public interface PressAction {
		void onPress(NitroTabButton button);
	}

	private final Text label;
	private final boolean active;
	private final PressAction onPress;

	public NitroTabButton(int x, int y, int width, int height, Text label, boolean active, PressAction onPress) {
		super(x, y, width, height, label);
		this.label = label;
		this.active = active;
		this.onPress = onPress;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if (hovered && !active) {
			NitroUiDraw.fillRoundRect(context, getX() + 4, getY() + 4, width - 8, height - 8, 6, NitroTheme.rowGlassHover());
		}
		if (active) {
			context.fill(getX() + 8, getY() + height - 2, getX() + width - 8, getY() + height, NitroTheme.accent());
		}
		var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
		int color = active ? NitroTheme.foreground() : (hovered ? NitroTheme.foreground() : NitroTheme.muted());
		context.drawCenteredTextWithShadow(textRenderer, label, getX() + width / 2, getY() + (height - 8) / 2, color);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		onPress.onPress(this);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

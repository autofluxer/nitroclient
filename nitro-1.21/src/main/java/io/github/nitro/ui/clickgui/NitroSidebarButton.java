package io.github.nitro.ui.clickgui;

import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Left-nav item — old Feather: left-aligned text, thin accent bar when selected. */
public final class NitroSidebarButton extends ClickableWidget {

	public interface PressAction {
		void onPress(NitroSidebarButton button);
	}

	private final Text label;
	private final boolean active;
	private final PressAction onPress;
	private float hover;

	public NitroSidebarButton(int x, int y, int width, int height, Text label, boolean active, PressAction onPress) {
		super(x, y, width, height, label);
		this.label = label;
		this.active = active;
		this.onPress = onPress;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 12F);

		int fill = active
				? NitroUiDraw.withAlpha(NitroTheme.accent(), 0x22)
				: NitroUiDraw.withAlpha(0xFFFFFF, (int) (0x0C * hover));
		if ((fill >>> 24) > 2) {
			NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 2, fill);
		}
		if (active) {
			context.fill(getX(), getY() + 3, getX() + 2, getY() + height - 3, NitroTheme.accent());
		}

		var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
		int color = active ? 0xFFFFFFFF : (hover > 0.3F ? 0xFFE4E4E4 : 0xFF9AA0A8);
		context.drawText(textRenderer, label, getX() + 10, getY() + (height - 8) / 2, color, false);
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

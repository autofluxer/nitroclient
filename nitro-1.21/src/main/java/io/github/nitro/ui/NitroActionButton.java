package io.github.nitro.ui;

import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Shared Click GUI / pause button — solid dark pills, smooth hover, crisp text.
 */
public class NitroActionButton extends ClickableWidget {

	public enum Style {
		FEATURED,
		NAV,
		NAV_ACTIVE,
		GHOST,
		TRANSPARENT,
		GLASS,
		DANGER,
		ICON_ONLY
	}

	public interface PressAction {
		void onPress(NitroActionButton button);
	}

	private final Text message;
	private final Style style;
	private final PressAction onPress;
	private float hover;

	public NitroActionButton(int x, int y, int width, int height, Text message, Style style, PressAction onPress) {
		super(x, y, width, height, message);
		this.message = message;
		this.style = style;
		this.onPress = onPress;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 7F);
		int radius = Math.min(8, Math.min(width, height) / 2);
		int textColor = NitroTheme.foreground();

		if (style == Style.GLASS) {
			int top = NitroUiDraw.lerpColor(0xD4282C34, 0xEE3A404C, hover);
			int bot = NitroUiDraw.lerpColor(0xD4121418, 0xEE1C2028, hover);
			NitroUiDraw.fillVGradientRoundRect(context, getX(), getY(), width, height, radius, top, bot);
			NitroUiDraw.topSheen(context, getX(), getY(), width, height, radius, 0.55F + 0.45F * hover);
			int border = NitroUiDraw.lerpColor(0x26FFFFFF, NitroUiDraw.withAlpha(NitroTheme.accent(), 0xAA), hover);
			NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, radius, border);
		} else if (style == Style.DANGER) {
			int top = NitroUiDraw.lerpColor(0xFFE24A4A, 0xFFFF5A5A, hover);
			int bot = NitroUiDraw.lerpColor(0xFF9A1C1C, 0xFFC42828, hover);
			NitroUiDraw.outerGlow(context, getX(), getY(), width, height, radius, 0x66E03C3C, 3);
			NitroUiDraw.fillVGradientRoundRect(context, getX(), getY(), width, height, radius, top, bot);
			NitroUiDraw.topSheen(context, getX(), getY(), width, height, radius, 0.7F + 0.3F * hover);
			NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, radius, 0x55FFFFFF);
			textColor = 0xFFFFFFFF;
		} else if (style == Style.FEATURED) {
			int left = NitroUiDraw.lerpColor(FeatherPalette.RED, FeatherPalette.RED_HOVER, hover);
			int right = NitroUiDraw.lerpColor(FeatherPalette.RED_DARK, 0xFF5A1010, hover * 0.35F);
			NitroUiDraw.outerGlow(context, getX(), getY(), width, height, radius, 0x55E03C3C, 2);
			NitroUiDraw.fillHGradientRoundRect(context, getX(), getY(), width, height, radius, left, right);
			NitroUiDraw.topSheen(context, getX(), getY(), width, height, radius, 0.6F + 0.3F * hover);
			textColor = 0xFFFFFFFF;
		} else {
			int fillIdle;
			int fillHover;
			int borderIdle;
			int borderHover;
			switch (style) {
				case NAV_ACTIVE -> {
					fillIdle = 0xFF222228;
					fillHover = 0xFF2A2A32;
					borderIdle = NitroUiDraw.withAlpha(NitroTheme.accent(), 0xBB);
					borderHover = NitroUiDraw.withAlpha(NitroTheme.accent(), 0xEE);
				}
				case GHOST, TRANSPARENT, ICON_ONLY -> {
					fillIdle = 0xB818181C;
					fillHover = 0xD0222228;
					borderIdle = 0x28FFFFFF;
					borderHover = 0x48FFFFFF;
				}
				default -> { // NAV
					fillIdle = 0xFF18181C;
					fillHover = 0xFF222228;
					borderIdle = 0x22FFFFFF;
					borderHover = 0x44FFFFFF;
				}
			}
			int fill = NitroUiDraw.lerpColor(fillIdle, fillHover, hover);
			int border = NitroUiDraw.lerpColor(borderIdle, borderHover, hover);
			NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, radius, fill);
			if ((border >>> 24) > 8) {
				NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, radius, border);
			}
		}

		var tr = MinecraftClient.getInstance().textRenderer;
		String label = tr.trimToWidth(message.getString(), Math.max(4, width - 14));
		int textW = tr.getWidth(label);
		int textX = getX() + (width - textW) / 2;
		int textY = getY() + (height - 8) / 2;
		context.drawText(tr, label, textX, textY, textColor, false);
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

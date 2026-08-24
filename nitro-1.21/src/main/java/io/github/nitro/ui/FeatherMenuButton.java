package io.github.nitro.ui;

import io.github.nitro.ui.animation.NitroEasing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * Old-Feather title button: vanilla proportions, dark glass, smooth hover, no icons.
 */
public final class FeatherMenuButton extends ClickableWidget {

	public static final int WIDTH = 200;
	public static final int HEIGHT = 20;
	public static final int GAP = 4;
	private static final int RADIUS = 2;
	private static final float HOVER_SPEED = 10F;

	public interface PressAction {
		void onPress(FeatherMenuButton button);
	}

	private final PressAction onPress;
	private float hover;
	private float alpha = 1F;
	private boolean pressed;

	public FeatherMenuButton(int x, int y, int w, int h, Text label, PressAction onPress) {
		super(x, y, w, h, label);
		this.onPress = onPress;
	}

	public void setMenuAlpha(float alpha) {
		this.alpha = NitroEasing.clamp01(alpha);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered && active ? 1F : 0F, Math.max(0.016F, delta), HOVER_SPEED);
		float press = pressed && hovered ? 1F : 0F;

		int fillIdle = 0xB2141418;
		int fillHover = 0xD028282E;
		int fillPress = 0xC01C1C22;
		int fill = NitroUiDraw.lerpColor(fillIdle, fillHover, hover);
		if (press > 0F) {
			fill = NitroUiDraw.lerpColor(fill, fillPress, press);
		}
		int border = NitroUiDraw.lerpColor(0x28FFFFFF, 0x58FFFFFF, hover);

		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, RADIUS, mulAlpha(fill, alpha));
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, RADIUS, mulAlpha(border, alpha));

		var tr = MinecraftClient.getInstance().textRenderer;
		String label = getMessage().getString();
		int textW = tr.getWidth(label);
		int textColor = mulAlpha(NitroUiDraw.lerpColor(0xFFE8E8E8, 0xFFFFFFFF, hover), alpha);
		context.drawText(tr, label, getX() + (width - textW) / 2, getY() + (height - 8) / 2, textColor, false);
	}

	private static int mulAlpha(int argb, float alpha) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
		return (a << 24) | (argb & 0x00FFFFFF);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		if (!active || alpha < 0.85F) {
			return;
		}
		pressed = true;
		onPress.onPress(this);
	}

	@Override
	public void onRelease(Click click) {
		pressed = false;
		super.onRelease(click);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

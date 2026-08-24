package io.github.nitro.ui;

import io.github.nitro.ui.animation.NitroEasing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Quiet corner / footer text control. */
public final class LunarToolbarButton extends ClickableWidget {

	public interface PressAction {
		void onPress(LunarToolbarButton button);
	}

	public static final int HEIGHT = 18;

	private final PressAction onPress;
	private float hover;
	private float alpha = 1F;

	public LunarToolbarButton(int x, int y, int width, Text label, PressAction onPress) {
		super(x, y, width, HEIGHT, label);
		this.onPress = onPress;
	}

	public void setMenuAlpha(float alpha) {
		this.alpha = NitroEasing.clamp01(alpha);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 10F);
		int fill = NitroUiDraw.lerpColor(0x0013141A, 0x6613141A, hover);
		fill = (Math.round(((fill >>> 24) & 0xFF) * alpha) << 24) | (fill & 0x00FFFFFF);
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 5, fill);

		var tr = MinecraftClient.getInstance().textRenderer;
		String label = getMessage().getString();
		int tw = tr.getWidth(label);
		int textA = Math.round((0xA0 + 0x5F * hover) * alpha);
		context.drawText(tr, label, getX() + (width - tw) / 2, getY() + 5, (textA << 24) | 0xFFFFFF, false);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		if (alpha < 0.85F) {
			return;
		}
		onPress.onPress(this);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

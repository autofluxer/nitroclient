package io.github.nitro.ui;

import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Bottom-right Discord / community promo card. */
public final class DiscordCardButton extends ClickableWidget {

	public interface PressAction {
		void onPress(DiscordCardButton button);
	}

	public static final int WIDTH = 148;
	public static final int HEIGHT = 46;

	private final PressAction onPress;
	private float hover;
	private float alpha = 1F;

	public DiscordCardButton(int x, int y, PressAction onPress) {
		super(x, y, WIDTH, HEIGHT, Text.literal("Discord"));
		this.onPress = onPress;
	}

	public void setMenuAlpha(float alpha) {
		this.alpha = NitroEasing.clamp01(alpha);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 12F);
		int fill = NitroUiDraw.lerpColor(0xC012151A, 0xE0181E28, hover);
		fill = mulAlpha(fill, alpha);
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 8, fill);
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, 8,
				mulAlpha(NitroUiDraw.lerpColor(0x22FFFFFF, NitroTheme.accent(), hover * 0.65F), alpha));

		NitroIcons.draw(context, NitroIcons.Id.DISCORD, getX() + 10, getY() + 11, 24);
		var tr = MinecraftClient.getInstance().textRenderer;
		context.drawText(tr, "JOIN OUR DISCORD", getX() + 42, getY() + 12, mulAlpha(0xFFFFFFFF, alpha), false);
		context.drawText(tr, "CLICK HERE", getX() + 42, getY() + 25,
				mulAlpha(NitroUiDraw.lerpColor(0xFF8FBBDD, NitroTheme.accentHover(), hover), alpha), false);
	}

	private static int mulAlpha(int argb, float alpha) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
		return (a << 24) | (argb & 0x00FFFFFF);
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

package io.github.nitro.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * HobbyShop Lunar {@code QuitButton} — same 12×12 chrome at (width-17, 7), plain "x" (no icon texture).
 */
public final class LunarQuitButton extends ClickableWidget {

	private int hoverFade;

	public LunarQuitButton(int x, int y) {
		super(x, y, 12, 12, Text.literal("x"));
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if (hovered) {
			if (hoverFade < 40) {
				hoverFade = Math.min(40, hoverFade + 10);
			}
		} else if (hoverFade > 0) {
			hoverFade = Math.max(0, hoverFade - 10);
		}

		NitroUiDraw.fillRoundRect(context, getX() - 1, getY() - 1, width + 2, height + 2, 2, 0x3C1E1E1E);
		// HobbyShop QuitButton: Color(255, 255 - hoverFade*4, 255 - hoverFade*4, 38 + hoverFade)
		int g = Math.max(0, 255 - hoverFade * 4);
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 2, ((38 + hoverFade) << 24) | (0xFF << 16) | (g << 8) | g);
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, 2, 0x1EFFFFFF);

		var tr = MinecraftClient.getInstance().textRenderer;
		context.drawText(tr, "x", getX() + 3, getY() + 2, 0xB7E8E8E8, false);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		MinecraftClient.getInstance().stop();
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

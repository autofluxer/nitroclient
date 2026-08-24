package io.github.nitro.ui;

import io.github.nitro.integration.NitroServerStatus;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class NitroScreenChrome {

	private NitroScreenChrome() {
	}

	public static void drawFooter(DrawContext context, int width, int height, net.minecraft.client.font.TextRenderer textRenderer) {
		int barY = height - MainMenuLayout.FOOTER_H;
		context.fillGradient(0, barY, width, height, 0x00000000, NitroUiDraw.withAlpha(NitroTheme.background(), 0xD0));
		context.drawTextWithShadow(textRenderer, Text.translatable("nitro.menu.credits"), 16, height - 20, NitroTheme.muted());

		String status = serverStatusLabel();
		int statusColor = switch (NitroServerStatus.getState()) {
			case ONLINE -> NitroTheme.success();
			case OFFLINE -> NitroTheme.danger();
			default -> NitroTheme.muted();
		};
		int statusW = textRenderer.getWidth(status);
		context.drawTextWithShadow(textRenderer, Text.literal(status), width - statusW - 16, height - 20, statusColor);
	}

	private static String serverStatusLabel() {
		return switch (NitroServerStatus.getState()) {
			case ONLINE -> {
				if (NitroServerStatus.getPlayersOnline() >= 0 && NitroServerStatus.getPlayersMax() > 0) {
					yield NitroServerStatus.getPlayersOnline() + "/" + NitroServerStatus.getPlayersMax()
							+ " online  ·  " + NitroServerStatus.getPingMs() + " ms";
				}
				yield "Online  ·  " + NitroServerStatus.getPingMs() + " ms";
			}
			case OFFLINE -> "Nitro SMP offline";
			default -> "Checking server…";
		};
	}
}

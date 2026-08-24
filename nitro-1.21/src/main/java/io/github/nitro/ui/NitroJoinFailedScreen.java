package io.github.nitro.ui;

import io.github.nitro.integration.NitroAutoJoin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Shown after a single failed auto-join — Retry is manual only. */
public final class NitroJoinFailedScreen extends Screen {

	private final String host;
	private final String serverName;
	private final Text reason;

	public NitroJoinFailedScreen(String host, String serverName, Text reason) {
		super(Text.literal("Connection Failed"));
		this.host = host == null ? "" : host;
		this.serverName = serverName == null || serverName.isBlank() ? host : serverName;
		this.reason = reason == null ? Text.literal("Connection failed.") : reason;
	}

	@Override
	protected void init() {
		NitroAutoJoin.clearConnecting();
		int btnW = 200;
		int x = (width - btnW) / 2;
		int y = height / 2 + 24;

		addDrawableChild(ButtonWidget.builder(Text.literal("Retry"), b -> {
			if (client != null) {
				NitroAutoJoin.retryLast(client);
			}
		}).dimensions(x, y, btnW, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Back to Menu"), b -> {
			if (client != null) {
				client.setScreen(new NitroTitleScreen());
			}
		}).dimensions(x, y + 24, btnW, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(
				textRenderer,
				Text.literal("Could not join " + serverName).formatted(Formatting.RED),
				width / 2,
				height / 2 - 28,
				0xFFFFFF);
		context.drawCenteredTextWithShadow(textRenderer, reason, width / 2, height / 2 - 10, 0xA0A0A0);
		if (!host.isBlank()) {
			context.drawCenteredTextWithShadow(
					textRenderer,
					Text.literal(host).formatted(Formatting.DARK_GRAY),
					width / 2,
					height / 2 + 6,
					0x808080);
		}
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}

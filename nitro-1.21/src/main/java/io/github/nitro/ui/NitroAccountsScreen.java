package io.github.nitro.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

public final class NitroAccountsScreen extends Screen {

	private final Screen parent;

	public NitroAccountsScreen(Screen parent) {
		super(Text.literal("Accounts"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int btnW = 160;
		int x = (width - btnW) / 2;
		int y = height / 2 + 40;
		addDrawableChild(new LunarToolbarButton(x, y, btnW, Text.literal("Back"),
				b -> client.setScreen(parent)));
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		NitroDraw.drawCoverBackground(context, NitroDraw.BG_JUNGLE_2, width, height, 1.08F);
		context.fill(0, 0, width, height, 0xB0000000);
		int panelW = 280;
		int panelH = 120;
		int px = (width - panelW) / 2;
		int py = height / 2 - 70;
		NitroUiDraw.fillRoundRect(context, px, py, panelW, panelH, 8, 0xFF14161A);
		NitroUiDraw.strokeRoundRect(context, px, py, panelW, panelH, 8, 0x22FFFFFF);
		var tr = client.textRenderer;
		String title = "Accounts";
		context.drawText(tr, title, px + (panelW - tr.getWidth(title)) / 2, py + 16, 0xFFFFFFFF, false);
		Session session = client.getSession();
		String name = session != null ? session.getUsername() : "Unknown";
		String line = "Signed in as " + name;
		context.drawText(tr, line, px + (panelW - tr.getWidth(line)) / 2, py + 48, 0xFFBEC2CC, false);
		String hint = "Manage accounts in the Nitro launcher.";
		context.drawText(tr, hint, px + (panelW - tr.getWidth(hint)) / 2, py + 68, 0x88FFFFFF, false);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}
}

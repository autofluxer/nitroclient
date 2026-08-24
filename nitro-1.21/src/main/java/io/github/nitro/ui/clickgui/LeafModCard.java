package io.github.nitro.ui.clickgui;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModules;
import io.github.nitro.ui.FeatherPalette;
import io.github.nitro.ui.NitroIcons;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.SpotifyScreen;
import io.github.nitro.ui.animation.NitroEasing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;

/** Feather mod card: title, large icon, gear + Enabled/Disabled. */
public final class LeafModCard extends ClickableWidget {

	public static final int CARD_W = 124;
	public static final int CARD_H = 104;

	private final NitroModule module;
	private float hover;
	private float enabledAnim;

	public LeafModCard(int x, int y, int width, int height, NitroModule module) {
		this(x, y, width, height, module, 0);
	}

	public LeafModCard(int x, int y, int width, int height, NitroModule module, int cascadeIndex) {
		super(x, y, width, height, module.getName());
		this.module = module;
		this.enabledAnim = module.isEnabled() ? 1F : 0F;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 11F);
		enabledAnim = NitroEasing.approach(enabledAnim, module.isEnabled() ? 1F : 0F, Math.max(0.016F, delta), 10F);

		int fill = NitroUiDraw.lerpColor(FeatherPalette.CARD, FeatherPalette.CARD_HOVER, hover);
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 6, fill);
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, 6, 0x14FFFFFF);

		var tr = MinecraftClient.getInstance().textRenderer;
		String name = tr.trimToWidth(module.getName().getString(), width - 14);
		context.drawText(tr, name, getX() + 8, getY() + 7, 0xFFFFFFFF, false);

		int icon = 28;
		NitroIcons.draw(context, iconFor(module), getX() + (width - icon) / 2, getY() + 28, icon);

		int gearX = getX() + 8;
		int gearY = getY() + height - 22;
		boolean gearHover = mouseX >= gearX && mouseX < gearX + 14 && mouseY >= gearY && mouseY < gearY + 14;
		NitroIcons.draw(context, NitroIcons.Id.GEAR, gearX, gearY, 12, gearHover ? 1F : 0.65F);

		boolean on = module.isEnabled();
		int btnW = width - 32;
		int btnH = 16;
		int btnX = getX() + width - btnW - 8;
		int btnY = getY() + height - 24;
		int btn = on
				? (hoveredToggle(mouseX, mouseY, btnX, btnY, btnW, btnH) ? FeatherPalette.ENABLED_HOVER : FeatherPalette.ENABLED)
				: (hoveredToggle(mouseX, mouseY, btnX, btnY, btnW, btnH) ? FeatherPalette.DISABLED_HOVER : FeatherPalette.DISABLED);
		NitroUiDraw.fillRoundRect(context, btnX, btnY, btnW, btnH, 3, btn);
		String status = on ? "Enabled" : "Disabled";
		int sw = tr.getWidth(status);
		context.drawText(tr, status, btnX + (btnW - sw) / 2, btnY + 4, 0xFFFFFFFF, false);
	}

	private static boolean hoveredToggle(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static NitroIcons.Id iconFor(NitroModule module) {
		return switch (module.getId()) {
			case "fps", "cps", "ping", "bps", "reach", "combo" -> NitroIcons.Id.SPEED;
			case "coordinates", "direction" -> NitroIcons.Id.LAYOUT;
			case "clock" -> NitroIcons.Id.MONITOR;
			case "armor", "potion", "target" -> NitroIcons.Id.DIAMOND;
			case "keystrokes", "arraylist", "watermark" -> NitroIcons.Id.GRID;
			case "spotify" -> NitroIcons.Id.CHAT;
			default -> NitroIcons.Id.MONITOR;
		};
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		int localX = (int) click.x() - getX();
		int localY = (int) click.y() - getY();
		var client = MinecraftClient.getInstance();
		if (localX >= 6 && localX < 24 && localY >= height - 24) {
			if ("spotify".equals(module.getId())) {
				client.setScreen(new SpotifyScreen(client.currentScreen));
			}
			return;
		}
		NitroModules.toggle(module.getId());
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

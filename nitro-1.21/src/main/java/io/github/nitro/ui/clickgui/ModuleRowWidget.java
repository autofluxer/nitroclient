package io.github.nitro.ui.clickgui;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModules;
import io.github.nitro.ui.NitroIcons;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.SpotifyScreen;
import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;

/** Compact Feather-style module row: name, optional gear, small toggle. */
public final class ModuleRowWidget extends ClickableWidget {

	public static final int HEIGHT = 22;

	private final NitroModule module;
	private float hover;
	private float onAnim;

	public ModuleRowWidget(int x, int y, int width, int height, NitroModule module) {
		super(x, y, width, height, module.getName());
		this.module = module;
		this.onAnim = module.isEnabled() ? 1F : 0F;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 12F);
		onAnim = NitroEasing.approach(onAnim, module.isEnabled() ? 1F : 0F, Math.max(0.016F, delta), 11F);

		int fill = NitroUiDraw.lerpColor(0xB2141418, 0xC8222228, hover);
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 2, fill);
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, 2,
				NitroUiDraw.lerpColor(0x18FFFFFF, 0x28FFFFFF, hover));
		if (onAnim > 0.05F) {
			context.fill(getX(), getY() + 2, getX() + 2, getY() + height - 2,
					NitroUiDraw.withAlpha(NitroTheme.accent(), (int) (0xCC * onAnim)));
		}

		var tr = MinecraftClient.getInstance().textRenderer;
		String name = tr.trimToWidth(module.getName().getString(), width - 70);
		context.drawText(tr, name, getX() + 8, getY() + (height - 8) / 2, 0xFFEDEDED, false);

		int gearX = getX() + width - 52;
		int gearY = getY() + (height - 12) / 2;
		if ("spotify".equals(module.getId())) {
			boolean gearHover = mouseX >= gearX && mouseX < gearX + 14 && mouseY >= gearY && mouseY < gearY + 12;
			NitroIcons.draw(context, NitroIcons.Id.GEAR, gearX, gearY, 12, gearHover ? 1F : 0.7F);
		}

		int trackW = 22;
		int trackH = 12;
		int trackX = getX() + width - trackW - 8;
		int trackY = getY() + (height - trackH) / 2;
		int track = NitroUiDraw.lerpColor(0xFF2A2A30, NitroTheme.accent(), onAnim);
		NitroUiDraw.fillRoundRect(context, trackX, trackY, trackW, trackH, 6, track);
		int knob = 8;
		int knobX = trackX + 2 + Math.round((trackW - knob - 4) * onAnim);
		NitroUiDraw.fillRoundRect(context, knobX, trackY + 2, knob, knob, 4, 0xFFF4F4F4);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		int localX = (int) click.x() - getX();
		if ("spotify".equals(module.getId()) && localX >= width - 52 && localX < width - 34) {
			var client = MinecraftClient.getInstance();
			client.setScreen(new SpotifyScreen(client.currentScreen));
			return;
		}
		NitroModules.toggle(module.getId());
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

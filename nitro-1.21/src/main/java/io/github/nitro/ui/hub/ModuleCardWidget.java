package io.github.nitro.ui.hub;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModules;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.clickgui.ClickGuiScreen;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class ModuleCardWidget extends ClickableWidget {

	private final NitroModule module;

	public ModuleCardWidget(int x, int y, int width, int height, NitroModule module) {
		super(x, y, width, height, module.getName());
		this.module = module;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean enabled = module.isEnabled();
		int border = enabled ? NitroTheme.accent() : NitroUiDraw.withAlpha(NitroTheme.panelBorder(), 0x88);
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 10,
				hovered ? NitroTheme.rowGlassHover() : NitroTheme.rowGlass());
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, 10, border);

		var textRenderer = MinecraftClient.getInstance().textRenderer;
		int pad = 12;
		context.drawTextWithShadow(textRenderer, module.getName(), getX() + pad, getY() + 10, NitroTheme.foreground());

		String category = Text.translatable(module.getCategory().getTranslationKey()).getString();
		int chipW = textRenderer.getWidth(category) + 10;
		int chipX = getX() + pad;
		int chipY = getY() + 24;
		context.fill(chipX, chipY, chipX + chipW, chipY + 12, NitroUiDraw.withAlpha(NitroTheme.accent(), 0x44));
		context.drawTextWithShadow(textRenderer, Text.literal(category), chipX + 5, chipY + 2, NitroTheme.accent());

		String desc = module.getDescription().getString();
		if (desc.length() > 72) {
			desc = desc.substring(0, 69) + "...";
		}
		context.drawTextWithShadow(textRenderer, Text.literal(desc), getX() + pad, getY() + 44, NitroTheme.muted());

		String statusKey = enabled ? "nitro.hub.status.enabled" : "nitro.hub.status.disabled";
		int statusColor = enabled ? 0xFF4ADE80 : NitroTheme.muted();
		context.drawTextWithShadow(textRenderer, Text.translatable(statusKey), getX() + pad, getY() + height - 22, statusColor);

		String action = enabled ? "ON" : "OFF";
		int actionW = textRenderer.getWidth(action) + 16;
		int actionX = getX() + width - pad - actionW;
		int actionY = getY() + height - 26;
		context.fill(actionX, actionY, actionX + actionW, actionY + 16,
				enabled ? NitroUiDraw.withAlpha(NitroTheme.accent(), 0x55) : NitroUiDraw.withAlpha(NitroTheme.muted(), 0x33));
		context.drawCenteredTextWithShadow(textRenderer, Text.literal(action), actionX + actionW / 2, actionY + 4,
				enabled ? NitroTheme.foreground() : NitroTheme.muted());
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		NitroModules.toggle(module.getId());
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof ClickGuiScreen gui) {
			gui.reinit();
		}
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

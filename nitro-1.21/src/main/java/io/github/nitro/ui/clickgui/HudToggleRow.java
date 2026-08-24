package io.github.nitro.ui.clickgui;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class HudToggleRow extends ClickableWidget {

	private final String elementId;
	private final Text label;
	private float hover;
	private float onAnim;

	public HudToggleRow(int x, int y, int width, int height, String elementId, Text label) {
		super(x, y, width, height, label);
		this.elementId = elementId;
		this.label = label;
		HudElementLayout layout = HudLayoutStore.get(elementId);
		this.onAnim = layout.visible ? 1F : 0F;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		HudElementLayout layout = HudLayoutStore.get(elementId);
		hover = NitroEasing.approach(hover, hovered ? 1F : 0F, Math.max(0.016F, delta), 10F);
		onAnim = NitroEasing.approach(onAnim, layout.visible ? 1F : 0F, Math.max(0.016F, delta), 10F);
		int fill = NitroUiDraw.lerpColor(NitroTheme.button(), NitroTheme.buttonHover(), hover);
		NitroUiDraw.fillRoundRect(context, getX(), getY(), width, height, 10, fill);
		NitroUiDraw.strokeRoundRect(context, getX(), getY(), width, height, 10, 0x14FFFFFF);
		var tr = MinecraftClient.getInstance().textRenderer;
		String name = tr.trimToWidth(label.getString(), width - 70);
		context.drawText(tr, name, getX() + 14, getY() + (height - 8) / 2, NitroTheme.foreground(), false);
		int trackW = 40;
		int trackH = 20;
		int trackX = getX() + width - trackW - 12;
		int trackY = getY() + (height - trackH) / 2;
		int trackColor = NitroUiDraw.lerpColor(0xFF3A3A34, NitroTheme.accent(), onAnim);
		NitroUiDraw.fillRoundRect(context, trackX, trackY, trackW, trackH, trackH / 2, trackColor);
		int knob = 14;
		int knobPad = 3;
		int knobX = trackX + knobPad + Math.round((trackW - knob - knobPad * 2) * onAnim);
		int knobY = trackY + (trackH - knob) / 2;
		NitroUiDraw.fillRoundRect(context, knobX, knobY, knob, knob, knob / 2, 0xFFFFFFFF);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		HudElementLayout layout = HudLayoutStore.get(elementId);
		layout.visible = !layout.visible;
		HudLayoutStore.save(elementId, layout);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}

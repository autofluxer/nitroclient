package io.github.nitro.module;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.hud.HudEditorState;
import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;

public interface PositionedHudModule extends HudModule {

	String hudId();

	default HudElementLayout layout() {
		return HudLayoutStore.get(hudId());
	}

	default void renderPositioned(DrawContext context, String text) {
		HudElementLayout layout = layout();
		if (!layout.visible) {
			return;
		}
		var client = net.minecraft.client.MinecraftClient.getInstance();
		if (client == null || client.options.hudHidden) {
			return;
		}
		HudLayoutStore.drawLabel(context, text, layout, NitroTheme.foreground());
		if (HudEditorState.active) {
			int w = client.textRenderer.getWidth(text) + 14;
			int h = 16;
			float scale = Math.max(0.75F, layout.scale);
			NitroUiDraw.strokeRoundRect(context, layout.x - 2, layout.y - 2,
					(int) (w * scale) + 2, (int) (h * scale) + 2, 4,
					NitroUiDraw.withAlpha(NitroTheme.accent(), 0xAA));
		}
	}
}

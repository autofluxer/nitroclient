package io.github.nitro.module.impl;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.hud.HudEditorState;
import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;

public final class TargetHudModule extends NitroModule implements PositionedHudModule {

	public TargetHudModule() {
		super("target", "nitro.module.target.name", "nitro.module.target.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "target";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		HudElementLayout layout = layout();
		if (!layout.visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden
				|| client.targetedEntity == null || !(client.targetedEntity instanceof LivingEntity living)) {
			return;
		}

		String name = living.getName().getString();
		float hp = living.getHealth();
		float max = Math.max(1F, living.getMaxHealth());
		var tr = client.textRenderer;
		int w = Math.max(90, tr.getWidth(name) + 16);
		int h = 28;
		int x = layout.x;
		int y = layout.y;

		NitroUiDraw.hudGlassChip(context, x, y, w, h);
		String hpText = (int) hp + " HP";
		context.drawTextWithShadow(tr, tr.trimToWidth(name, w - 14), x + 6, y + 4, NitroTheme.foreground());
		context.drawTextWithShadow(tr, hpText, x + w - 6 - tr.getWidth(hpText), y + 4, NitroTheme.muted());

		int barX = x + 6;
		int barY = y + 17;
		int barW = w - 12;
		int barH = 5;
		NitroUiDraw.fillRoundRect(context, barX, barY, barW, barH, 2, NitroUiDraw.withAlpha(0x000000, 0x66));
		int fillW = Math.max(1, Math.round(barW * Math.min(1F, hp / max)));
		NitroUiDraw.fillRoundRect(context, barX, barY, fillW, barH, 2, NitroTheme.accent());

		if (HudEditorState.active) {
			NitroUiDraw.strokeRoundRect(context, x - 2, y - 2, w + 4, h + 4, 5,
					NitroUiDraw.withAlpha(NitroTheme.accent(), 0xAA));
		}
	}
}

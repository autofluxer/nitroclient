package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;

public final class PotionHudModule extends NitroModule implements PositionedHudModule {

	public PotionHudModule() {
		super("potions", "nitro.module.potions.name", "nitro.module.potions.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "potions";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		if (!layout().visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}
		int y = layout().y;
		int x = layout().x;
		var tr = client.textRenderer;
		for (StatusEffectInstance effect : client.player.getStatusEffects()) {
			String name = effect.getEffectType().value().getName().getString();
			int secs = effect.getDuration() / 20;
			String time = String.format("%d:%02d", secs / 60, secs % 60);
			String line = name + "  " + time;
			int padX = 5;
			int chipW = tr.getWidth(line) + padX * 2;
			int chipH = 12;
			NitroUiDraw.hudGlassChip(context, x, y, chipW, chipH);
			context.drawTextWithShadow(tr, name, x + padX, y + 2, NitroTheme.foreground());
			int tw = tr.getWidth(time);
			context.drawTextWithShadow(tr, time, x + chipW - padX - tw, y + 2, NitroTheme.muted());
			y += 13;
		}
	}
}

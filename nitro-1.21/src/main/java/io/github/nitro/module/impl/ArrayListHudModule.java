package io.github.nitro.module.impl;

import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.NitroModules;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArrayListHudModule extends NitroModule implements PositionedHudModule {

	public ArrayListHudModule() {
		super("arraylist", "nitro.module.arraylist.name", "nitro.module.arraylist.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "arraylist";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		if (!layout().visible) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options.hudHidden) {
			return;
		}
		List<String> lines = new ArrayList<>();
		for (NitroModule module : NitroModules.all()) {
			if (module.isEnabled() && !(module instanceof ArrayListHudModule)) {
				lines.add(module.getName().getString());
			}
		}
		lines.sort(Comparator.comparingInt((String s) -> client.textRenderer.getWidth(s)).reversed());
		int y = layout().y;
		int x = layout().x;
		for (String line : lines) {
			HudLayoutStore.drawRowChip(context, line, x, y, true);
			y += 12;
		}
	}
}

package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class CoordinatesHudModule extends NitroModule implements PositionedHudModule {

	public CoordinatesHudModule() {
		super("coordinates", "nitro.module.coordinates.name", "nitro.module.coordinates.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "coordinates";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		var pos = client.player.getBlockPos();
		renderPositioned(context, String.format("XYZ %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
	}
}

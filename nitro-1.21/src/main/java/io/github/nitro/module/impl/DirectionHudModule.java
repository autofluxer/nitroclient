package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Direction;

public final class DirectionHudModule extends NitroModule implements PositionedHudModule {

	public DirectionHudModule() {
		super("direction", "nitro.module.direction.name", "nitro.module.direction.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "direction";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		Direction dir = client.player.getHorizontalFacing();
		renderPositioned(context, dir.asString().toUpperCase());
	}
}

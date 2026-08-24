package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class FpsHudModule extends NitroModule implements PositionedHudModule {

	public FpsHudModule() {
		super("fps", "nitro.module.fps.name", "nitro.module.fps.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "fps";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		renderPositioned(context, net.minecraft.client.MinecraftClient.getInstance().getCurrentFps() + " FPS");
	}
}

package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class WatermarkHudModule extends NitroModule implements PositionedHudModule {

	public WatermarkHudModule() {
		super("watermark", "nitro.module.watermark.name", "nitro.module.watermark.desc", NitroModuleCategory.HUD);
		setEnabled(true);
	}

	@Override
	public String hudId() {
		return "watermark";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		renderPositioned(context, "NITRO CLIENT");
	}
}

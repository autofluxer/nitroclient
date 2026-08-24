package io.github.nitro.module.impl;

import io.github.nitro.hud.CpsTracker;
import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class CpsHudModule extends NitroModule implements PositionedHudModule {

	public CpsHudModule() {
		super("cps", "nitro.module.cps.name", "nitro.module.cps.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "cps";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		renderPositioned(context, CpsTracker.leftCps() + " CPS");
	}
}

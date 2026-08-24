package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class ComboHudModule extends NitroModule implements PositionedHudModule {

	private int combo;

	public ComboHudModule() {
		super("combo", "nitro.module.combo.name", "nitro.module.combo.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "combo";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		if (combo <= 0) {
			return;
		}
		renderPositioned(context, combo + " Combo");
	}
}

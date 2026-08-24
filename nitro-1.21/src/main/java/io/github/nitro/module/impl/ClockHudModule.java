package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ClockHudModule extends NitroModule implements PositionedHudModule {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	public ClockHudModule() {
		super("clock", "nitro.module.clock.name", "nitro.module.clock.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "clock";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		renderPositioned(context, LocalTime.now().format(FORMAT));
	}
}

package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Vec3d;

public final class BpsHudModule extends NitroModule implements PositionedHudModule {

	private Vec3d lastPos = Vec3d.ZERO;
	private long lastTime;

	public BpsHudModule() {
		super("bps", "nitro.module.bps.name", "nitro.module.bps.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "bps";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		Vec3d pos = client.player.getEntityPos();
		long now = System.currentTimeMillis();
		double bps = 0;
		if (lastTime > 0) {
			double dist = pos.distanceTo(lastPos);
			double seconds = (now - lastTime) / 1000.0;
			if (seconds > 0) {
				bps = dist / seconds;
			}
		}
		lastPos = pos;
		lastTime = now;
		renderPositioned(context, String.format("%.2f BPS", bps));
	}
}

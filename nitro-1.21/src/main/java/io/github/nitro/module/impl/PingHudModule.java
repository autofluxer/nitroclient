package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.RenderTickCounter;

public final class PingHudModule extends NitroModule implements PositionedHudModule {

	public PingHudModule() {
		super("ping", "nitro.module.ping.name", "nitro.module.ping.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "ping";
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayNetworkHandler handler = client.getNetworkHandler();
		int ping = handler != null && handler.getPlayerListEntry(client.getSession().getUuidOrNull()) != null
				? handler.getPlayerListEntry(client.getSession().getUuidOrNull()).getLatency()
				: 0;
		renderPositioned(context, ping + " ms");
	}
}

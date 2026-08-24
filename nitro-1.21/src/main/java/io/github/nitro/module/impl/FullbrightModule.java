package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.TickableModule;
import net.minecraft.client.MinecraftClient;

public final class FullbrightModule extends NitroModule implements TickableModule {

	private double savedGamma = 1.0;

	public FullbrightModule() {
		super("fullbright", "nitro.module.fullbright.name", "nitro.module.fullbright.desc", NitroModuleCategory.UTILITY);
	}

	@Override
	protected void onEnable() {
		applyGamma();
	}

	@Override
	protected void onDisable() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.options.getGamma().setValue(savedGamma);
		}
	}

	@Override
	public void onClientTick() {
		applyGamma();
	}

	private void applyGamma() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		double current = client.options.getGamma().getValue();
		if (current < 14.0) {
			savedGamma = current;
			client.options.getGamma().setValue(16.0);
		}
	}
}

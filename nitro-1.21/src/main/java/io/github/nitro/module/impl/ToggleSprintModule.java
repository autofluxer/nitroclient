package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.TickableModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ToggleSprintModule extends NitroModule implements TickableModule {

	public ToggleSprintModule() {
		super("toggle_sprint", "nitro.module.toggle_sprint.name", "nitro.module.toggle_sprint.desc",
				NitroModuleCategory.UTILITY);
	}

	@Override
	public void onClientTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.currentScreen != null) {
			return;
		}
		if (client.options.forwardKey.isPressed() && !client.player.isSneaking() && client.player.getHungerManager().getFoodLevel() > 6) {
			client.player.setSprinting(true);
		}
	}
}

package io.github.nitro.module.impl;

import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ZoomModule extends NitroModule {

	public static final KeyBinding ZOOM_KEY = new KeyBinding(
			"key.nitro.zoom",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_C,
			KeyBinding.Category.MISC);
	public static final float ZOOM_FACTOR = 4.0F;

	private ZoomModule() {
		super("zoom", "nitro.module.zoom.name", "nitro.module.zoom.desc", NitroModuleCategory.UTILITY);
	}

	public static ZoomModule create() {
		return new ZoomModule();
	}

	public static boolean isZooming() {
		NitroModule module = io.github.nitro.module.NitroModules.get("zoom");
		return module != null && module.isEnabled() && ZOOM_KEY.isPressed();
	}

	public static float adjustFov(float fov) {
		return isZooming() ? fov / ZOOM_FACTOR : fov;
	}
}

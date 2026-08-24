package io.github.nitro.module;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class NitroKeys {

	public static final KeyBinding OPEN_MODULES = new KeyBinding(
			"key.nitro.modules",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT,
			KeyBinding.Category.MISC);

	private NitroKeys() {
	}
}

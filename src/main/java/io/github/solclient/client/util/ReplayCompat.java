/*
 * Sol Client - an open source Minecraft client
 * Copyright (C) 2021-2023  TheKodeToad and Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.solclient.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;

/**
 * Optional ReplayMod integration without compile-time classpath dependencies.
 */
@UtilityClass
public class ReplayCompat {

	private static Boolean present;
	private static Method handleScrollMethod;

	public static boolean isPresent() {
		if (present == null) {
			present = classExists("com.replaymod.core.ReplayMod");
		}
		return present;
	}

	public static void handleScroll(int divided) {
		if (!isPresent()) {
			return;
		}
		try {
			if (handleScrollMethod == null) {
				Class<?> clazz = Class.forName("com.replaymod.replay.InputReplayTimer");
				handleScrollMethod = clazz.getMethod("handleScroll", int.class);
			}
			handleScrollMethod.invoke(null, divided);
		} catch (ReflectiveOperationException ignored) {
			// ReplayMod not loaded or API changed
		}
	}

	public static boolean isSpectatingEntityInReplay() {
		if (!isPresent()) {
			return false;
		}
		try {
			Class<?> replayClass = Class.forName("com.replaymod.replay.ReplayModReplay");
			Field instanceField = replayClass.getField("instance");
			Object instance = instanceField.get(null);
			Object handler = instance.getClass().getMethod("getReplayHandler").invoke(instance);
			if (handler == null) {
				return false;
			}
			Object camera = MinecraftClient.getInstance().getCameraEntity();
			Class<?> cameraEntityClass = Class.forName("com.replaymod.replay.camera.CameraEntity");
			return camera != null && !cameraEntityClass.isInstance(camera);
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	public static boolean isReplayRendering(Object gameRenderer) {
		if (!isPresent() || gameRenderer == null) {
			return false;
		}
		try {
			Class<?> iface = Class.forName("com.replaymod.render.hooks.EntityRendererHandler$IEntityRenderer");
			if (!iface.isInstance(gameRenderer)) {
				return false;
			}
			Object handler = iface.getMethod("replayModRender_getHandler").invoke(gameRenderer);
			return handler != null;
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static boolean classExists(String name) {
		try {
			Class.forName(name, false, ReplayCompat.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}

}

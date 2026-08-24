package io.github.nitro.hud;

import org.lwjgl.glfw.GLFW;

public final class CpsTracker {

	private static final int SAMPLES = 120;
	private static final long[] LEFT = new long[SAMPLES];
	private static final long[] RIGHT = new long[SAMPLES];
	private static int index;
	private static boolean lastLeft;
	private static boolean lastRight;

	private CpsTracker() {
	}

	public static void tick(net.minecraft.client.MinecraftClient client) {
		if (client == null) {
			return;
		}
		long window = client.getWindow().getHandle();
		boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		if (left && !lastLeft) {
			record(LEFT);
		}
		if (right && !lastRight) {
			record(RIGHT);
		}
		lastLeft = left;
		lastRight = right;
	}

	public static int leftCps() {
		return count(LEFT);
	}

	public static int rightCps() {
		return count(RIGHT);
	}

	private static void record(long[] buffer) {
		buffer[index % SAMPLES] = System.currentTimeMillis();
		index++;
	}

	private static int count(long[] buffer) {
		long now = System.currentTimeMillis();
		int total = 0;
		for (long stamp : buffer) {
			if (stamp > now - 1000L) {
				total++;
			}
		}
		return total;
	}
}

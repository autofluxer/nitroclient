package io.github.nitro.ui.animation;

public final class NitroEasing {

	private NitroEasing() {
	}

	public static float lerp(float from, float to, float progress) {
		return from + (to - from) * clamp01(progress);
	}

	public static int lerpColor(int from, int to, float progress) {
		float t = clamp01(progress);
		int a = (int) (((from >> 24) & 0xFF) + ((((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * t));
		int r = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
		int g = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
		int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static float easeOutCubic(float t) {
		float p = clamp01(t);
		float inv = 1F - p;
		return 1F - inv * inv * inv;
	}

	public static float easeInOutCubic(float t) {
		float p = clamp01(t);
		return p < 0.5F ? 4F * p * p * p : 1F - (float) Math.pow(-2F * p + 2F, 3) / 2F;
	}

	public static float smoothStep(float t) {
		float p = clamp01(t);
		return p * p * (3F - 2F * p);
	}

	public static float approach(float current, float target, float deltaSeconds, float speed) {
		if (current == target) {
			return target;
		}
		float step = speed * deltaSeconds;
		if (current < target) {
			return Math.min(target, current + step);
		}
		return Math.max(target, current - step);
	}

	public static float clamp01(float value) {
		return Math.max(0F, Math.min(1F, value));
	}
}

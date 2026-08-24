package io.github.nitro.ui;

import io.github.nitro.client.NitroClientActivity;
import io.github.nitro.config.NitroConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Full-bleed animated menu background from pre-extracted PNG frames.
 * Only frames that actually exist are used — never draws missing-texture pink/black.
 */
public final class VideoMenuBackground {

	private static final int DEFAULT_SOURCE_FPS = 24;
	/** Stretch playback so the loop feels cinematic (keeps every frame). */
	private static final float CINEMATIC_SLOWDOWN = 1.85F;
	private static final float TEX_ASPECT = 16F / 9F;

	private static Identifier[] FRAME_IDS = new Identifier[0];
	private static int frameCount = 0;
	private static int sourceFps = DEFAULT_SOURCE_FPS;
	private static boolean loaded;
	private static boolean framesAvailable;

	private static long startNanos = System.nanoTime();
	private static boolean playing = true;
	private static int frozenFrame = 0;
	private static int lastDrawnFrame = -1;

	private VideoMenuBackground() {
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		int declaredCount = 40;
		int fps = DEFAULT_SOURCE_FPS;
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null) {
				Optional<Resource> meta = client.getResourceManager()
						.getResource(Identifier.of("nitro", "textures/gui/bg_video/frames.txt"));
				if (meta.isPresent()) {
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(meta.get().getInputStream(), StandardCharsets.UTF_8))) {
						String line1 = reader.readLine();
						String line2 = reader.readLine();
						if (line1 != null && !line1.isBlank()) {
							String trimmed = line1.trim();
							if (trimmed.contains("=")) {
								for (String part : trimmed.split("[,;\\s]+")) {
									String[] kv = part.split("=", 2);
									if (kv.length == 2) {
										if (kv[0].equalsIgnoreCase("count") || kv[0].equalsIgnoreCase("frames")) {
											declaredCount = parsePositive(kv[1], declaredCount);
										} else if (kv[0].equalsIgnoreCase("fps")) {
											fps = parsePositive(kv[1], fps);
										}
									}
								}
							} else {
								declaredCount = parsePositive(trimmed, declaredCount);
							}
						}
						if (line2 != null && !line2.isBlank()) {
							String t = line2.trim();
							if (t.contains("=")) {
								String[] kv = t.split("=", 2);
								if (kv.length == 2 && kv[0].toLowerCase().contains("fps")) {
									fps = parsePositive(kv[1], fps);
								}
							} else {
								fps = parsePositive(t.replaceAll("[^0-9]", ""), fps);
							}
						}
					}
				}
			}
		} catch (Exception ignored) {
		}

		declaredCount = Math.max(1, Math.min(480, declaredCount));
		fps = Math.max(1, Math.min(120, fps));
		sourceFps = fps;

		// Only keep frames that exist — prevents pink/black missing textures.
		List<Identifier> present = new ArrayList<>();
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			for (int i = 0; i < declaredCount; i++) {
				Identifier id = Identifier.of("nitro", "textures/gui/bg_video/frame_%02d.png".formatted(i));
				try {
					if (client.getResourceManager().getResource(id).isPresent()) {
						present.add(id);
					}
				} catch (Exception ignored) {
				}
			}
		}

		FRAME_IDS = present.toArray(Identifier[]::new);
		frameCount = FRAME_IDS.length;
		framesAvailable = frameCount > 0;
	}

	private static int parsePositive(String raw, int fallback) {
		try {
			int v = Integer.parseInt(raw.trim());
			return v > 0 ? v : fallback;
		} catch (Exception e) {
			return fallback;
		}
	}

	public static void setPlaying(boolean play) {
		ensureLoaded();
		if (play == playing) {
			return;
		}
		if (!play) {
			frozenFrame = currentFrameIndex();
			playing = false;
		} else {
			double progress = frameCount <= 0 ? 0 : frozenFrame / (double) frameCount;
			double loopSeconds = loopSeconds();
			startNanos = System.nanoTime() - (long) (progress * loopSeconds * 1_000_000_000L);
			playing = true;
		}
	}

	public static boolean isPlaying() {
		return playing;
	}

	public static boolean hasFrames() {
		ensureLoaded();
		return framesAvailable && frameCount > 0;
	}

	public static void tick(float delta) {
	}

	private static double loopSeconds() {
		ensureLoaded();
		// Keep every frame; stretch timeline for a calmer cinematic pace.
		return Math.max(2.5, (frameCount / (double) Math.max(1, sourceFps)) * CINEMATIC_SLOWDOWN);
	}

	private static int currentFrameIndex() {
		ensureLoaded();
		if (frameCount <= 0) {
			return 0;
		}
		if (!playing || !NitroConfig.INSTANCE.animatedMenuBackground || !NitroClientActivity.shouldAnimateMenus()) {
			return Math.floorMod(frozenFrame, frameCount);
		}
		double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
		double loop = loopSeconds();
		double t = (seconds % loop) / loop;
		return Math.floorMod((int) (t * frameCount), frameCount);
	}

	public static Identifier currentFrame() {
		ensureLoaded();
		if (!framesAvailable || FRAME_IDS.length == 0) {
			return NitroDraw.BG_MENU;
		}
		return FRAME_IDS[currentFrameIndex()];
	}

	public static void draw(DrawContext context, int width, int height) {
		draw(context, width, height, 1F);
	}

	public static void draw(DrawContext context, int width, int height, float zoom) {
		ensureLoaded();
		Identifier tex = (!NitroConfig.INSTANCE.animatedMenuBackground || !framesAvailable)
				? NitroDraw.BG_MENU
				: FRAME_IDS[currentFrameIndex()];
		if (framesAvailable && NitroConfig.INSTANCE.animatedMenuBackground) {
			lastDrawnFrame = currentFrameIndex();
			frozenFrame = lastDrawnFrame;
		}
		// Slight extra zoom softens the plate for a shallow depth-of-field feel.
		float z = Math.max(1.06F, zoom);
		NitroDraw.drawCoverBackground(context, tex, width, height, z, TEX_ASPECT);
		drawCinematicBlurVeil(context, width, height);
	}

	/** Soft focus veil so UI glass/buttons read cleanly over the video. */
	private static void drawCinematicBlurVeil(DrawContext context, int width, int height) {
		// Center softens, edges darken — reads like a subtle background blur.
		context.fill(0, 0, width, height, 0x3A000000);
		context.fillGradient(0, 0, width, height / 3, 0x55000000, 0x00000000);
		context.fillGradient(0, height - height / 3, width, height, 0x00000000, 0x66000000);
		int side = Math.max(48, width / 8);
		context.fillGradient(0, 0, side, height, 0x40000000, 0x00000000);
		context.fillGradient(width - side, 0, width, height, 0x00000000, 0x40000000);
	}

	public static int lastFrame() {
		return lastDrawnFrame;
	}
}

package io.github.nitro.module.impl;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.config.NitroConfig;
import io.github.nitro.config.SpotifyUiSettings;
import io.github.nitro.hud.HudEditorState;
import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.PositionedHudModule;
import io.github.nitro.module.TickableModule;
import io.github.nitro.spotify.SpotifyManager;
import io.github.nitro.spotify.SpotifyMessages;
import io.github.nitro.spotify.SpotifyPlaybackState;
import io.github.nitro.ui.NitroDraw;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.SpotifyScreen;
import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * Compact premium Spotify HUD widget (connected + disconnected empty states).
 */
public final class SpotifyHudModule extends NitroModule implements PositionedHudModule, TickableModule {

	private static final int BASE_W = 220;
	private static final int COVER = 42;

	private static int lastX;
	private static int lastY;
	private static int lastW;
	private static int lastH;
	private static float lastScale = 1F;
	private static boolean lastVisible;
	private static boolean lastConnected;
	private static int connectBtnX;
	private static int connectBtnY;
	private static int connectBtnW;
	private static int connectBtnH;

	private float appear;
	private float progressSmooth;
	private String lastTrackId = "";

	public SpotifyHudModule() {
		super("spotify", "nitro.module.spotify.name", "nitro.module.spotify.desc", NitroModuleCategory.HUD);
	}

	@Override
	public String hudId() {
		return "spotify";
	}

	@Override
	protected void onEnable() {
		SpotifyManager.INSTANCE.setModuleWantsPoll(true);
		SpotifyManager.INSTANCE.requestRefresh();
	}

	@Override
	protected void onDisable() {
		SpotifyManager.INSTANCE.setModuleWantsPoll(false);
		appear = 0F;
	}

	@Override
	public void onClientTick() {
		SpotifyManager.INSTANCE.tick();
	}

	@Override
	public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
		HudElementLayout layout = layout();
		SpotifyUiSettings settings = NitroConfig.INSTANCE.spotify;
		if (settings == null) {
			settings = new SpotifyUiSettings();
		}
		lastVisible = layout.visible && settings.showHud;
		if (!lastVisible) {
			appear = NitroEasing.approach(appear, 0F, 0.05F, 10F);
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options.hudHidden) {
			return;
		}

		appear = NitroEasing.approach(appear, 1F, 0.05F, 10F);
		float alphaMul = NitroEasing.easeOutCubic(appear) * settings.hudOpacity;
		if (alphaMul < 0.02F) {
			return;
		}

		boolean connected = SpotifyManager.INSTANCE.isConnected();
		lastConnected = connected;
		SpotifyPlaybackState state = SpotifyManager.INSTANCE.state();
		float scale = Math.max(0.75F, layout.scale * settings.hudScale);

		int w = BASE_W;
		int h;
		if (!connected) {
			h = 78;
		} else if (!state.hasTrack()) {
			h = 64;
			if (!settings.showAlbumCover) {
				w -= COVER;
			}
		} else {
			h = 72;
			if (!settings.showAlbumCover) {
				w -= COVER + 6;
			}
			if (!settings.showProgressBar) {
				h -= 16;
			}
			if (!settings.showControls) {
				h -= 16;
			}
			if (settings.showDevice) {
				h += 10;
			}
		}

		lastX = layout.x;
		lastY = layout.y;
		lastW = w;
		lastH = h;
		lastScale = scale;

		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(layout.x, layout.y);
		matrices.scale(scale, scale);

		int panel = NitroUiDraw.withAlpha(0x10151C, (int) (0xF0 * alphaMul));
		NitroUiDraw.fillRoundRect(context, 0, 0, w, h, 12, panel);
		NitroUiDraw.strokeRoundRect(context, 0, 0, w, h, 12,
				NitroUiDraw.withAlpha(NitroTheme.accent(), (int) (0x55 * alphaMul)));
		// Soft top sheen
		NitroUiDraw.fillRoundRect(context, 1, 1, w - 2, 14, 10,
				NitroUiDraw.withAlpha(0xFFFFFF, (int) (0x08 * alphaMul)));

		var tr = client.textRenderer;
		int fg = NitroUiDraw.withAlpha(NitroTheme.foreground(), (int) (0xFF * alphaMul));
		int muted = NitroUiDraw.withAlpha(NitroTheme.muted(), (int) (0xFF * alphaMul));
		int accent = NitroUiDraw.withAlpha(NitroTheme.accent(), (int) (0xFF * alphaMul));

		if (!connected) {
			renderDisconnected(context, tr, w, h, fg, muted, accent, alphaMul);
		} else if (!state.hasTrack()) {
			renderIdle(context, tr, settings, w, h, fg, muted, alphaMul);
		} else {
			renderPlaying(context, tr, settings, state, w, h, fg, muted, accent, alphaMul);
		}

		matrices.popMatrix();

		if (HudEditorState.active) {
			NitroUiDraw.strokeRoundRect(context, layout.x - 2, layout.y - 2,
					(int) (w * scale) + 4, (int) (h * scale) + 4, 12,
					NitroUiDraw.withAlpha(NitroTheme.accent(), 0xAA));
		}
	}

	private void renderDisconnected(DrawContext context, net.minecraft.client.font.TextRenderer tr,
			int w, int h, int fg, int muted, int accent, float alphaMul) {
		context.drawText(tr, "SPOTIFY", 12, 12, accent, false);
		context.drawText(tr, "Connect your Spotify account", 12, 28, fg, false);
		context.drawText(tr, "to control music in Nitro.", 12, 40, muted, false);

		int btnW = 118;
		int btnH = 18;
		int btnX = 12;
		int btnY = h - 26;
		connectBtnX = btnX;
		connectBtnY = btnY;
		connectBtnW = btnW;
		connectBtnH = btnH;
		NitroUiDraw.fillRoundRect(context, btnX, btnY, btnW, btnH, 6, accent);
		String label = "CONNECT SPOTIFY";
		int lw = tr.getWidth(label);
		context.drawText(tr, label, btnX + (btnW - lw) / 2, btnY + 5,
				NitroUiDraw.withAlpha(0x0A0E14, (int) (0xFF * alphaMul)), false);
	}

	private void renderIdle(DrawContext context, net.minecraft.client.font.TextRenderer tr,
			SpotifyUiSettings settings, int w, int h, int fg, int muted, float alphaMul) {
		int textX = 12;
		if (settings.showAlbumCover) {
			NitroUiDraw.fillRoundRect(context, 10, 11, COVER, COVER, 8,
					NitroUiDraw.withAlpha(0x000000, (int) (0x55 * alphaMul)));
			textX = 10 + COVER + 10;
		}
		String name = SpotifyManager.INSTANCE.displayName();
		context.drawText(tr, "Spotify", textX, 16, fg, false);
		String line = !name.isBlank() ? name : SpotifyMessages.NO_MUSIC;
		context.drawText(tr, tr.trimToWidth(line, w - textX - 12), textX, 30, muted, false);
		connectBtnW = 0;
	}

	private void renderPlaying(DrawContext context, net.minecraft.client.font.TextRenderer tr,
			SpotifyUiSettings settings, SpotifyPlaybackState state,
			int w, int h, int fg, int muted, int accent, float alphaMul) {
		if (!state.track().id().equals(lastTrackId)) {
			lastTrackId = state.track().id();
			progressSmooth = state.progressMs();
		}

		int textX = 12;
		if (settings.showAlbumCover) {
			Identifier cover = SpotifyManager.INSTANCE.art().getOrRequest(state.track().coverUrl());
			NitroUiDraw.fillRoundRect(context, 10, 10, COVER, COVER, 8,
					NitroUiDraw.withAlpha(0x000000, (int) (0x55 * alphaMul)));
			if (cover != null) {
				NitroDraw.blit(context, cover, 10, 10, COVER, COVER);
			}
			textX = 10 + COVER + 10;
		}

		int titleY = 12;
		if (settings.showSongName) {
			context.drawText(tr, tr.trimToWidth(state.track().name(), w - textX - 10), textX, titleY, fg, false);
			titleY += 12;
		}
		if (settings.showArtist) {
			context.drawText(tr, tr.trimToWidth(state.track().artists(), w - textX - 10), textX, titleY, muted, false);
			titleY += 12;
		}
		if (settings.showDevice && state.deviceName() != null && !state.deviceName().isBlank()) {
			context.drawText(tr, tr.trimToWidth(state.deviceName(), w - textX - 10), textX, titleY, muted, false);
		}

		int bottom = h - (settings.showControls ? 18 : 8);
		if (settings.showProgressBar) {
			long duration = Math.max(1L, state.track().durationMs());
			float target = state.playing()
					? state.progressMs() + Math.max(0L, System.currentTimeMillis() - state.updatedAtMs())
					: state.progressMs();
			target = Math.min(duration, target);
			progressSmooth = NitroEasing.approach(progressSmooth, target, 0.05F, 8F);
			float pct = Math.min(1F, progressSmooth / duration);
			int barX = 12;
			int barW = w - 24;
			int barY = bottom - 14;
			NitroUiDraw.fillRoundRect(context, barX, barY, barW, 3, 2,
					NitroUiDraw.withAlpha(0x000000, (int) (0x70 * alphaMul)));
			int fill = Math.max(2, Math.round(barW * pct));
			NitroUiDraw.fillRoundRect(context, barX, barY, fill, 3, 2, accent);
			NitroUiDraw.fillRoundRect(context, barX + fill - 2, barY - 2, 5, 7, 2, fg);
			context.drawText(tr, formatMs((long) progressSmooth), barX, barY + 6, muted, false);
			String end = formatMs(duration);
			context.drawText(tr, end, barX + barW - tr.getWidth(end), barY + 6, muted, false);
		}

		if (settings.showControls) {
			int cy = h - 10;
			int cx = w / 2;
			drawControlChip(context, cx - 34, cy, "◀", alphaMul);
			drawControlChip(context, cx, cy, state.playing() ? "❚❚" : "▶", alphaMul);
			drawControlChip(context, cx + 34, cy, "▶", alphaMul);
		}
		connectBtnW = 0;
	}

	private static void drawControlChip(DrawContext context, int cx, int cy, String label, float alphaMul) {
		var tr = MinecraftClient.getInstance().textRenderer;
		int tw = tr.getWidth(label);
		NitroUiDraw.fillRoundRect(context, cx - 10, cy - 7, 20, 14, 5,
				NitroUiDraw.withAlpha(0xFFFFFF, (int) (0x10 * alphaMul)));
		context.drawText(tr, label, cx - tw / 2, cy - 4,
				NitroUiDraw.withAlpha(NitroTheme.foreground(), (int) (0xFF * alphaMul)), false);
	}

	public static void handleClick(double mx, double my) {
		if (!lastVisible) {
			return;
		}
		float scale = lastScale;
		double lx = (mx - lastX) / scale;
		double ly = (my - lastY) / scale;
		if (lx < 0 || ly < 0 || lx > lastW || ly > lastH) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!lastConnected) {
			if (connectBtnW > 0
					&& lx >= connectBtnX && lx <= connectBtnX + connectBtnW
					&& ly >= connectBtnY && ly <= connectBtnY + connectBtnH
					&& client != null) {
				client.setScreen(new SpotifyScreen(client.currentScreen));
			}
			return;
		}

		SpotifyUiSettings settings = NitroConfig.INSTANCE.spotify;
		if (settings != null && !settings.showControls) {
			return;
		}
		int cy = lastH - 10;
		if (Math.abs(ly - cy) > 10) {
			return;
		}
		int cx = lastW / 2;
		if (Math.abs(lx - (cx - 34)) <= 12) {
			SpotifyManager.INSTANCE.previous();
		} else if (Math.abs(lx - cx) <= 12) {
			SpotifyManager.INSTANCE.togglePlayPause();
		} else if (Math.abs(lx - (cx + 34)) <= 12) {
			SpotifyManager.INSTANCE.next();
		}
	}

	private static String formatMs(long ms) {
		long totalSec = Math.max(0L, ms / 1000L);
		long m = totalSec / 60L;
		long s = totalSec % 60L;
		return m + ":" + (s < 10 ? "0" : "") + s;
	}
}

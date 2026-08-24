package io.github.nitro.ui;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.config.SpotifyUiSettings;
import io.github.nitro.module.NitroModules;
import io.github.nitro.spotify.SpotifyClientConfig;
import io.github.nitro.spotify.SpotifyDevice;
import io.github.nitro.spotify.SpotifyManager;
import io.github.nitro.spotify.SpotifyMessages;
import io.github.nitro.spotify.SpotifyPlaybackState;
import io.github.nitro.spotify.SpotifySearchItem;
import io.github.nitro.spotify.SpotifyTrack;
import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Spotify player / settings screen integrated with Nitro UI chrome.
 */
public final class SpotifyScreen extends NitroSubScreen {

	private float appear;
	private TextFieldWidget searchField;
	private TextFieldWidget clientIdField;
	private String statusLine = "";
	private boolean connecting;
	private int listScroll;

	private final List<ToggleHit> toggles = new ArrayList<>();
	private final List<StepHit> steps = new ArrayList<>();
	private final List<SearchHit> searchHits = new ArrayList<>();
	private final List<TrackHit> trackHits = new ArrayList<>();
	private final List<DeviceHit> deviceHits = new ArrayList<>();
	private ButtonHit prevHit;
	private ButtonHit playHit;
	private ButtonHit nextHit;
	private ButtonHit connectHit;
	private ButtonHit cancelHit;
	private ButtonHit disconnectHit;
	private ButtonHit openEditorHit;

	public SpotifyScreen(Screen parent) {
		super(Text.translatable("nitro.spotify.title"), parent);
	}

	@Override
	protected void init() {
		appear = 0F;
		SpotifyManager.INSTANCE.refreshDevices();
		SpotifyManager.INSTANCE.refreshRecent();
		SpotifyManager.INSTANCE.requestRefresh();
		if (SpotifyManager.INSTANCE.isConnected()) {
			SpotifyManager.INSTANCE.refreshProfile();
		}

		int left = listLeft();
		int top = listTop();
		int w = listWidth();

		clientIdField = new TextFieldWidget(textRenderer, left, top + 28, w - 90, 18,
				Text.literal("Client ID"));
		clientIdField.setMaxLength(128);
		clientIdField.setText(SpotifyClientConfig.resolveClientId());
		clientIdField.setPlaceholder(Text.translatable("nitro.spotify.client_id"));
		addDrawableChild(clientIdField);
		addDrawableChild(new NitroActionButton(left + w - 82, top + 26, 82, 20,
				Text.translatable("nitro.spotify.save_id"), NitroActionButton.Style.NAV,
				b -> {
					SpotifyClientConfig.saveClientId(clientIdField.getText());
					statusLine = Text.translatable("nitro.spotify.id_saved").getString();
				}));

		searchField = new TextFieldWidget(textRenderer, left, top + 200, w - 90, 18,
				Text.translatable("nitro.spotify.search"));
		searchField.setMaxLength(80);
		searchField.setPlaceholder(Text.translatable("nitro.spotify.search"));
		addDrawableChild(searchField);

		addDrawableChild(new NitroActionButton(left + w - 82, top + 198, 82, 20,
				Text.translatable("nitro.spotify.search_btn"), NitroActionButton.Style.NAV,
				b -> {
					SpotifyManager.INSTANCE.search(searchField.getText());
					listScroll = 0;
					statusLine = "";
				}));
	}

	@Override
	public void tick() {
		SpotifyManager.INSTANCE.tick();
		connecting = SpotifyManager.INSTANCE.isAuthorizing();
		if (connecting && SpotifyManager.INSTANCE.isConnected()) {
			connecting = false;
			statusLine = Text.translatable("nitro.spotify.status.connected").getString();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		appear = NitroEasing.approach(appear, 1F, Math.max(0.016F, delta), 7F);
		float a = NitroEasing.easeOutCubic(appear);
		super.render(context, mouseX, mouseY, delta);
		drawChrome(context);

		int left = listLeft();
		int top = listTop();
		int w = listWidth();
		SpotifyPlaybackState state = SpotifyManager.INSTANCE.state();
		SpotifyUiSettings settings = NitroConfig.INSTANCE.spotify;
		toggles.clear();
		steps.clear();
		searchHits.clear();
		trackHits.clear();
		deviceHits.clear();
		cancelHit = null;
		openEditorHit = null;

		boolean connected = SpotifyManager.INSTANCE.isConnected();
		String conn = connected
				? Text.translatable("nitro.spotify.status.connected_mark").getString()
				: Text.translatable("nitro.spotify.status.disconnected").getString();
		context.drawTextWithShadow(textRenderer,
				Text.translatable("nitro.spotify.status", conn).getString(),
				left, top, NitroTheme.muted());

		if (connected) {
			String account = SpotifyManager.INSTANCE.displayName();
			if (!account.isBlank()) {
				context.drawTextWithShadow(textRenderer,
						Text.translatable("nitro.spotify.account", account).getString(),
						left, top + 12, NitroTheme.foreground());
			}
		} else if (connecting) {
			context.drawTextWithShadow(textRenderer,
					Text.translatable("nitro.spotify.waiting").getString(),
					left, top + 12, NitroTheme.accent());
		} else {
			context.drawTextWithShadow(textRenderer,
					Text.translatable("nitro.spotify.client_id_hint").getString(),
					left, top + 12, NitroTheme.muted());
		}

		// Now playing card
		int cardY = top + 54;
		NitroUiDraw.contentWell(context, left, cardY, w, 118, 10);
		Identifier cover = null;
		if (state.hasTrack()) {
			cover = SpotifyManager.INSTANCE.art().getOrRequest(state.track().coverUrl());
		}
		NitroUiDraw.fillRoundRect(context, left + 12, cardY + 12, 64, 64, 8, 0x66000000);
		if (cover != null) {
			NitroDraw.blit(context, cover, left + 12, cardY + 12, 64, 64);
		}

		String title = state.hasTrack() ? state.track().name() : "Spotify";
		String artist = state.hasTrack() ? state.track().artists()
				: (connected ? SpotifyMessages.NO_MUSIC : SpotifyMessages.NOT_CONNECTED);
		String album = state.hasTrack() ? state.track().album() : "";
		context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(title, w - 100), left + 88, cardY + 16,
				NitroTheme.foreground());
		context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(artist, w - 100), left + 88, cardY + 30,
				NitroTheme.muted());
		if (!album.isBlank()) {
			context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(album, w - 100), left + 88, cardY + 42,
					NitroTheme.muted());
		}

		if (state.hasTrack()) {
			long duration = Math.max(1L, state.track().durationMs());
			float pct = Math.min(1F, state.progressMs() / (float) duration);
			int barX = left + 88;
			int barW = w - 110;
			int barY = cardY + 58;
			NitroUiDraw.fillRoundRect(context, barX, barY, barW, 4, 2, 0x66000000);
			NitroUiDraw.fillRoundRect(context, barX, barY, Math.max(2, Math.round(barW * pct)), 4, 2, NitroTheme.accent());
			context.drawTextWithShadow(textRenderer, formatMs(state.progressMs()), barX, barY + 8, NitroTheme.muted());
			String end = formatMs(duration);
			context.drawTextWithShadow(textRenderer, end, barX + barW - textRenderer.getWidth(end), barY + 8,
					NitroTheme.muted());
		}

		int ctrlY = cardY + 90;
		prevHit = button(context, left + w / 2 - 60, ctrlY, 28, 18, "◀", mouseX, mouseY);
		playHit = button(context, left + w / 2 - 14, ctrlY, 28, 18, state.playing() ? "❚❚" : "▶", mouseX, mouseY);
		nextHit = button(context, left + w / 2 + 32, ctrlY, 28, 18, "▶", mouseX, mouseY);

		if (connected) {
			disconnectHit = button(context, left + w - 110, top - 2, 110, 18,
					Text.translatable("nitro.spotify.disconnect").getString(), mouseX, mouseY, true);
			connectHit = null;
		} else if (connecting) {
			cancelHit = button(context, left + w - 90, top - 2, 90, 18,
					Text.translatable("nitro.spotify.cancel").getString(), mouseX, mouseY, true);
			connectHit = null;
			disconnectHit = null;
		} else {
			connectHit = button(context, left + w - 130, top - 2, 130, 18,
					Text.translatable("nitro.spotify.connect").getString(), mouseX, mouseY, false);
			disconnectHit = null;
		}

		String err = SpotifyManager.INSTANCE.lastError();
		if (err != null && !err.isBlank() && !err.equals(SpotifyMessages.NO_MUSIC)
				&& !err.equals(SpotifyMessages.NOT_CONNECTED)) {
			statusLine = err;
		}
		if (!statusLine.isBlank()) {
			context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(statusLine, w), left, cardY + 108,
					NitroTheme.danger());
		}

		// Settings
		int optY = top + 230;
		context.drawTextWithShadow(textRenderer, Text.translatable("nitro.spotify.options").getString(),
				left, optY, NitroTheme.foreground());
		optY += 14;
		boolean modOn = NitroModules.get("spotify") != null && NitroModules.get("spotify").isEnabled();
		optY = toggleRow(context, left, optY, w, "Module Enabled", modOn, v -> NitroModules.setEnabled("spotify", v), mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Show HUD", settings.showHud, v -> settings.showHud = v, mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Album Cover", settings.showAlbumCover, v -> settings.showAlbumCover = v, mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Progress Bar", settings.showProgressBar, v -> settings.showProgressBar = v, mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Artist", settings.showArtist, v -> settings.showArtist = v, mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Song Name", settings.showSongName, v -> settings.showSongName = v, mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Device", settings.showDevice, v -> settings.showDevice = v, mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Controls", settings.showControls, v -> settings.showControls = v, mouseX, mouseY);
		optY = toggleRow(context, left, optY, w, "Auto Refresh", settings.autoRefresh, v -> settings.autoRefresh = v, mouseX, mouseY);
		optY = stepRow(context, left, optY, w, "HUD Scale",
				String.format("%.2f", settings.hudScale),
				() -> settings.hudScale = Math.max(0.75F, settings.hudScale - 0.05F),
				() -> settings.hudScale = Math.min(2F, settings.hudScale + 0.05F),
				mouseX, mouseY);
		optY = stepRow(context, left, optY, w, "HUD Opacity",
				String.format("%.0f%%", settings.hudOpacity * 100F),
				() -> settings.hudOpacity = Math.max(0.25F, settings.hudOpacity - 0.05F),
				() -> settings.hudOpacity = Math.min(1F, settings.hudOpacity + 0.05F),
				mouseX, mouseY);
		openEditorHit = button(context, left, optY, 150, 16,
				Text.translatable("nitro.spotify.open_hud_editor").getString(), mouseX, mouseY, false);
		optY += 20;

		// Devices
		int col2 = left + w / 2 + 8;
		int sideTop = top + 200;
		context.drawTextWithShadow(textRenderer, Text.translatable("nitro.spotify.devices").getString(),
				col2, sideTop, NitroTheme.foreground());
		int dy = sideTop + 14;
		List<SpotifyDevice> devices = SpotifyManager.INSTANCE.devices();
		if (devices.isEmpty()) {
			context.drawTextWithShadow(textRenderer, SpotifyMessages.NO_DEVICE, col2, dy, NitroTheme.muted());
		} else {
			for (SpotifyDevice device : devices) {
				boolean active = device.active() || device.id().equals(settings.preferredDeviceId);
				String label = (active ? "● " : "○ ") + device.name();
				context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(label, w / 2 - 16), col2, dy,
						active ? NitroTheme.accent() : NitroTheme.muted());
				deviceHits.add(new DeviceHit(col2, dy, w / 2 - 16, 12, device.id()));
				dy += 12;
				if (dy > height - 40) {
					break;
				}
			}
		}

		// Search / recent
		int listY = Math.max(optY, dy) + 8;
		List<SpotifySearchItem> search = SpotifyManager.INSTANCE.searchResults();
		List<SpotifyTrack> recent = SpotifyManager.INSTANCE.recent();
		context.drawTextWithShadow(textRenderer,
				(!search.isEmpty()
						? Text.translatable("nitro.spotify.results")
						: Text.translatable("nitro.spotify.recent")).getString(),
				left, listY, NitroTheme.foreground());
		listY += 14;
		int index = 0;
		if (!search.isEmpty()) {
			for (SpotifySearchItem item : search) {
				if (index++ < listScroll) {
					continue;
				}
				if (listY > height - 36) {
					break;
				}
				NitroUiDraw.fillRoundRect(context, left, listY - 2, w, 24, 5, 0x2218181C);
				String type = item.type() == null ? "" : item.type();
				context.drawTextWithShadow(textRenderer,
						textRenderer.trimToWidth("[" + type + "] " + item.name(), w - 70), left + 6, listY,
						NitroTheme.foreground());
				context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(item.subtitle(), w - 70),
						left + 6, listY + 10, NitroTheme.muted());
				ButtonHit play = button(context, left + w - 54, listY, 48, 18,
						Text.translatable("nitro.spotify.play").getString(), mouseX, mouseY, false);
				searchHits.add(new SearchHit(play, item));
				listY += 28;
			}
		} else {
			for (SpotifyTrack track : recent) {
				if (index++ < listScroll) {
					continue;
				}
				if (listY > height - 36) {
					break;
				}
				NitroUiDraw.fillRoundRect(context, left, listY - 2, w, 22, 5, 0x2218181C);
				context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(track.name(), w - 70), left + 6, listY,
						NitroTheme.foreground());
				context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(track.artists(), w - 70), left + 6, listY + 10,
						NitroTheme.muted());
				ButtonHit play = button(context, left + w - 54, listY, 48, 18,
						Text.translatable("nitro.spotify.play").getString(), mouseX, mouseY, false);
				trackHits.add(new TrackHit(play, track));
				listY += 26;
			}
		}

		if (a < 1F) {
			context.fill(0, 0, width, height, NitroUiDraw.withAlpha(0x000000, (int) ((1F - a) * 40)));
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double mouseX = click.x();
		double mouseY = click.y();
		if (prevHit != null && prevHit.contains(mouseX, mouseY)) {
			SpotifyManager.INSTANCE.previous();
			return true;
		}
		if (playHit != null && playHit.contains(mouseX, mouseY)) {
			SpotifyManager.INSTANCE.togglePlayPause();
			return true;
		}
		if (nextHit != null && nextHit.contains(mouseX, mouseY)) {
			SpotifyManager.INSTANCE.next();
			return true;
		}
		if (cancelHit != null && cancelHit.contains(mouseX, mouseY)) {
			SpotifyManager.INSTANCE.cancelConnect();
			connecting = false;
			statusLine = SpotifyMessages.AUTH_CANCELLED;
			return true;
		}
		if (connectHit != null && connectHit.contains(mouseX, mouseY) && !connecting) {
			if (clientIdField != null) {
				SpotifyClientConfig.saveClientId(clientIdField.getText());
			}
			if (SpotifyClientConfig.resolveClientId().isBlank()) {
				statusLine = SpotifyMessages.CLIENT_ID_MISSING;
				return true;
			}
			connecting = true;
			statusLine = Text.translatable("nitro.spotify.waiting").getString();
			SpotifyManager.INSTANCE.connect().whenComplete((ok, err) -> {
				MinecraftClient mc = MinecraftClient.getInstance();
				Runnable done = () -> {
					connecting = false;
					if (err != null) {
						statusLine = err.getMessage() != null ? err.getMessage() : SpotifyMessages.AUTH_FAILED;
					} else {
						statusLine = Text.translatable("nitro.spotify.status.connected_mark").getString();
						SpotifyManager.INSTANCE.refreshDevices();
						SpotifyManager.INSTANCE.refreshRecent();
						SpotifyManager.INSTANCE.refreshProfile();
						NitroModules.setEnabled("spotify", true);
					}
				};
				if (mc != null) {
					mc.execute(done);
				} else {
					done.run();
				}
			});
			return true;
		}
		if (disconnectHit != null && disconnectHit.contains(mouseX, mouseY)) {
			SpotifyManager.INSTANCE.disconnect();
			statusLine = Text.translatable("nitro.spotify.status.disconnected").getString();
			return true;
		}
		if (openEditorHit != null && openEditorHit.contains(mouseX, mouseY)) {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc != null) {
				mc.setScreen(new HudEditorScreen());
			}
			return true;
		}
		for (ToggleHit toggle : toggles) {
			if (toggle.contains(mouseX, mouseY)) {
				toggle.toggle.run();
				NitroConfig.INSTANCE.spotify.normalize();
				NitroConfig.save();
				return true;
			}
		}
		for (StepHit step : steps) {
			if (step.contains(mouseX, mouseY)) {
				step.action.run();
				NitroConfig.INSTANCE.spotify.normalize();
				NitroConfig.save();
				return true;
			}
		}
		for (DeviceHit device : deviceHits) {
			if (device.contains(mouseX, mouseY)) {
				SpotifyManager.INSTANCE.selectDevice(device.id);
				return true;
			}
		}
		for (SearchHit hit : searchHits) {
			if (hit.button.contains(mouseX, mouseY)) {
				SpotifyManager.INSTANCE.playSearchItem(hit.item);
				statusLine = "";
				return true;
			}
		}
		for (TrackHit hit : trackHits) {
			if (hit.button.contains(mouseX, mouseY)) {
				SpotifyManager.INSTANCE.playTrack(hit.track);
				statusLine = "";
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount > 0 && listScroll > 0) {
			listScroll--;
			return true;
		}
		if (verticalAmount < 0) {
			listScroll++;
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void close() {
		NitroConfig.save();
		super.close();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private int toggleRow(DrawContext context, int x, int y, int w, String label, boolean value,
			java.util.function.Consumer<Boolean> setter, int mouseX, int mouseY) {
		int half = w / 2 - 12;
		context.drawTextWithShadow(textRenderer, label, x, y + 3, NitroTheme.muted());
		String btn = value ? "ON" : "OFF";
		ButtonHit hit = button(context, x + half - 40, y, 36, 14, btn, mouseX, mouseY, false);
		toggles.add(new ToggleHit(hit, () -> setter.accept(!value)));
		return y + 16;
	}

	private int stepRow(DrawContext context, int x, int y, int w, String label, String value,
			Runnable dec, Runnable inc, int mouseX, int mouseY) {
		int half = w / 2 - 12;
		context.drawTextWithShadow(textRenderer, label, x, y + 3, NitroTheme.muted());
		ButtonHit minus = button(context, x + half - 70, y, 18, 14, "-", mouseX, mouseY, false);
		context.drawTextWithShadow(textRenderer, value, x + half - 48, y + 3, NitroTheme.foreground());
		ButtonHit plus = button(context, x + half - 8, y, 18, 14, "+", mouseX, mouseY, false);
		steps.add(new StepHit(minus, dec));
		steps.add(new StepHit(plus, inc));
		return y + 16;
	}

	private ButtonHit button(DrawContext context, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
		return button(context, x, y, w, h, label, mouseX, mouseY, false);
	}

	private ButtonHit button(DrawContext context, int x, int y, int w, int h, String label,
			int mouseX, int mouseY, boolean danger) {
		boolean hover = mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
		int fill = danger
				? (hover ? 0xFF4A1E28 : 0xFF3A1820)
				: (hover ? 0xFF2A2A32 : 0xFF222228);
		NitroUiDraw.fillRoundRect(context, x, y, w, h, 5, fill);
		NitroUiDraw.strokeRoundRect(context, x, y, w, h, 5,
				NitroUiDraw.withAlpha(danger ? NitroTheme.danger() : NitroTheme.accent(), hover ? 0xCC : 0x66));
		int tw = textRenderer.getWidth(label);
		context.drawText(textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, NitroTheme.foreground(), false);
		return new ButtonHit(x, y, w, h);
	}

	private static String formatMs(long ms) {
		long totalSec = Math.max(0L, ms / 1000L);
		long m = totalSec / 60L;
		long s = totalSec % 60L;
		return m + ":" + (s < 10 ? "0" : "") + s;
	}

	private record ButtonHit(int x, int y, int w, int h) {
		boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	private record ToggleHit(ButtonHit hit, Runnable toggle) {
		boolean contains(double mx, double my) {
			return hit.contains(mx, my);
		}
	}

	private record StepHit(ButtonHit hit, Runnable action) {
		boolean contains(double mx, double my) {
			return hit.contains(mx, my);
		}
	}

	private record TrackHit(ButtonHit button, SpotifyTrack track) {
	}

	private record SearchHit(ButtonHit button, SpotifySearchItem item) {
	}

	private record DeviceHit(int x, int y, int w, int h, String id) {
		boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}
}

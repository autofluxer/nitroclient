package io.github.nitro.spotify;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.config.SpotifyUiSettings;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class SpotifyManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("nitroclient");
	public static final SpotifyManager INSTANCE = new SpotifyManager();

	private final SpotifyAuthManager auth = new SpotifyAuthManager();
	private final SpotifyApiClient api = new SpotifyApiClient(auth);
	private final SpotifyArtCache artCache = new SpotifyArtCache();
	private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "nitro-spotify");
		t.setDaemon(true);
		return t;
	});
	private final AtomicBoolean polling = new AtomicBoolean(false);
	private final AtomicLong nextPollAt = new AtomicLong(0L);
	private final AtomicLong backoffUntil = new AtomicLong(0L);

	private volatile SpotifyPlaybackState state = SpotifyPlaybackState.disconnected(SpotifyMessages.NOT_CONNECTED);
	private volatile List<SpotifyDevice> devices = List.of();
	private volatile List<SpotifyTrack> recent = List.of();
	private volatile List<SpotifySearchItem> searchResults = List.of();
	private volatile SpotifyProfileStore profile = SpotifyProfileStore.load();
	private volatile String lastError = "";
	private volatile boolean moduleWantsPoll;

	private SpotifyManager() {
	}

	public void init() {
		SpotifyClientConfig.load();
		profile = SpotifyProfileStore.load();
		if (auth.isConnected()) {
			state = SpotifyPlaybackState.idle(SpotifyMessages.NO_MUSIC);
			schedulePoll(0L);
			refreshProfile();
		} else {
			state = SpotifyPlaybackState.disconnected(SpotifyMessages.NOT_CONNECTED);
		}
	}

	public void setModuleWantsPoll(boolean wants) {
		moduleWantsPoll = wants;
		if (wants && auth.isConnected()) {
			schedulePoll(0L);
		}
	}

	public void tick() {
		if (!moduleWantsPoll && !(MinecraftClient.getInstance() != null
				&& MinecraftClient.getInstance().currentScreen instanceof io.github.nitro.ui.SpotifyScreen)) {
			return;
		}
		SpotifyUiSettings settings = NitroConfig.INSTANCE.spotify;
		if (settings == null || !settings.autoRefresh || !auth.isConnected()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < backoffUntil.get() || now < nextPollAt.get()) {
			return;
		}
		schedulePoll(settings.refreshIntervalMs);
		requestRefresh();
	}

	public SpotifyPlaybackState state() {
		return state;
	}

	public SpotifyArtCache art() {
		return artCache;
	}

	public boolean isConnected() {
		return auth.isConnected();
	}

	public boolean isAuthorizing() {
		return auth.isAuthorizing();
	}

	public String displayName() {
		return profile != null && profile.hasName() ? profile.displayName : "";
	}

	public String product() {
		return profile != null && profile.product != null ? profile.product : "";
	}

	public String lastError() {
		return lastError == null ? "" : lastError;
	}

	public List<SpotifyDevice> devices() {
		return devices;
	}

	public List<SpotifyTrack> recent() {
		return recent;
	}

	public List<SpotifySearchItem> searchResults() {
		return searchResults;
	}

	public CompletableFuture<Boolean> connect() {
		lastError = "";
		return auth.beginLogin().whenComplete((ok, err) -> {
			if (err != null) {
				lastError = messageOf(err);
				state = SpotifyPlaybackState.disconnected(lastError);
				return;
			}
			if (Boolean.TRUE.equals(ok)) {
				lastError = "";
				state = SpotifyPlaybackState.idle(SpotifyMessages.NO_MUSIC);
				schedulePoll(0L);
				requestRefresh();
				refreshDevices();
				refreshRecent();
				refreshProfile();
			}
		});
	}

	public void cancelConnect() {
		auth.cancelLogin();
		lastError = SpotifyMessages.AUTH_CANCELLED;
		if (!auth.isConnected()) {
			state = SpotifyPlaybackState.disconnected(SpotifyMessages.NOT_CONNECTED);
		}
	}

	public void disconnect() {
		auth.disconnect();
		if (profile != null) {
			profile.clear();
		}
		profile = new SpotifyProfileStore();
		state = SpotifyPlaybackState.disconnected(SpotifyMessages.NOT_CONNECTED);
		devices = List.of();
		recent = List.of();
		searchResults = List.of();
		lastError = "";
	}

	public void requestRefresh() {
		if (!auth.isConnected() || !polling.compareAndSet(false, true)) {
			return;
		}
		worker.execute(() -> {
			try {
				SpotifyPlaybackState next = api.fetchCurrentlyPlaying();
				state = next;
				lastError = next.statusMessage() == null ? "" : next.statusMessage();
				if (next.hasTrack() && next.track().coverUrl() != null) {
					artCache.getOrRequest(next.track().coverUrl());
				}
			} catch (Exception e) {
				handleApiError(e);
			} finally {
				polling.set(false);
			}
		});
	}

	public void refreshDevices() {
		runApi(() -> devices = Collections.unmodifiableList(api.fetchDevices()));
	}

	public void refreshRecent() {
		runApi(() -> recent = Collections.unmodifiableList(api.recentlyPlayed(10)));
	}

	public void refreshProfile() {
		runApi(() -> {
			SpotifyProfileStore next = api.fetchProfile();
			next.save();
			profile = next;
		});
	}

	public void search(String query) {
		if (query == null || query.isBlank()) {
			searchResults = List.of();
			return;
		}
		runApi(() -> searchResults = Collections.unmodifiableList(api.searchCatalog(query.trim(), 8)));
	}

	public void togglePlayPause() {
		SpotifyPlaybackState current = state;
		boolean play = !current.playing();
		String device = preferredDevice(current);
		runApi(() -> {
			api.playPause(play, device);
			schedulePoll(400L);
			requestRefresh();
		});
	}

	public void next() {
		String device = preferredDevice(state);
		runApi(() -> {
			api.next(device);
			schedulePoll(400L);
			requestRefresh();
		});
	}

	public void previous() {
		String device = preferredDevice(state);
		runApi(() -> {
			api.previous(device);
			schedulePoll(400L);
			requestRefresh();
		});
	}

	public void playTrack(SpotifyTrack track) {
		if (track == null || track.uri() == null || track.uri().isBlank()) {
			return;
		}
		playUri(track.uri());
	}

	public void playSearchItem(SpotifySearchItem item) {
		if (item == null || item.uri() == null || item.uri().isBlank()) {
			return;
		}
		playUri(item.uri());
	}

	private void playUri(String uri) {
		String device = preferredDevice(state);
		runApi(() -> {
			api.playUri(uri, device);
			schedulePoll(500L);
			requestRefresh();
		});
	}

	public void selectDevice(String deviceId) {
		if (deviceId == null || deviceId.isBlank()) {
			return;
		}
		NitroConfig.INSTANCE.spotify.preferredDeviceId = deviceId;
		NitroConfig.save();
		runApi(() -> {
			api.transferPlayback(deviceId, state.playing());
			refreshDevices();
			schedulePoll(400L);
			requestRefresh();
		});
	}

	public void shutdown() {
		auth.shutdown();
		artCache.shutdown();
		worker.shutdownNow();
	}

	private void runApi(ApiTask task) {
		worker.execute(() -> {
			try {
				task.run();
				if (lastError.equals(SpotifyMessages.RATE_LIMITED)
						|| lastError.equals(SpotifyMessages.RECONNECTING)) {
					/* keep transient error until next success */
				} else {
					lastError = "";
				}
			} catch (Exception e) {
				handleApiError(e);
			}
		});
	}

	private void handleApiError(Exception e) {
		String msg = messageOf(e);
		lastError = msg;
		LOGGER.debug("Spotify API: {}", msg);
		if (msg.equals(SpotifyMessages.RATE_LIMITED)) {
			backoffUntil.set(System.currentTimeMillis() + 10_000L);
		} else if (msg.equals(SpotifyMessages.RECONNECTING)) {
			backoffUntil.set(System.currentTimeMillis() + 3_000L);
			try {
				auth.refreshAccessToken();
			} catch (Exception ignored) {
				state = SpotifyPlaybackState.disconnected(SpotifyMessages.NOT_CONNECTED);
			}
		} else if (msg.equals(SpotifyMessages.NOT_CONNECTED)) {
			state = SpotifyPlaybackState.disconnected(msg);
		}
	}

	private void schedulePoll(long delayMs) {
		nextPollAt.set(System.currentTimeMillis() + Math.max(0L, delayMs));
	}

	private String preferredDevice(SpotifyPlaybackState current) {
		String preferred = NitroConfig.INSTANCE.spotify != null
				? NitroConfig.INSTANCE.spotify.preferredDeviceId : "";
		if (preferred != null && !preferred.isBlank()) {
			return preferred;
		}
		return current != null ? current.deviceId() : "";
	}

	private static String messageOf(Throwable err) {
		Throwable cur = err;
		while (cur.getCause() != null && cur.getCause() != cur) {
			cur = cur.getCause();
		}
		String msg = cur.getMessage();
		return msg == null || msg.isBlank() ? SpotifyMessages.NETWORK_ERROR : msg;
	}

	@FunctionalInterface
	private interface ApiTask {
		void run() throws Exception;
	}

	public void ifError(Consumer<String> consumer) {
		String err = lastError;
		if (err != null && !err.isBlank()) {
			consumer.accept(err);
		}
	}
}

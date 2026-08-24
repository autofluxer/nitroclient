package io.github.nitro.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class SpotifyApiClient {

	private static final String API = "https://api.spotify.com/v1";
	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(6))
			.build();
	private final SpotifyAuthManager auth;

	public SpotifyApiClient(SpotifyAuthManager auth) {
		this.auth = auth;
	}

	public SpotifyPlaybackState fetchCurrentlyPlaying() throws IOException, InterruptedException {
		HttpResponse<String> response = get("/me/player/currently-playing?additional_types=track");
		if (response.statusCode() == 204 || response.body() == null || response.body().isBlank()) {
			return SpotifyPlaybackState.idle(SpotifyMessages.NO_MUSIC);
		}
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = get("/me/player/currently-playing?additional_types=track");
		}
		mapError(response.statusCode(), response.body());
		JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
		boolean playing = root.has("is_playing") && root.get("is_playing").getAsBoolean();
		long progress = root.has("progress_ms") && !root.get("progress_ms").isJsonNull()
				? root.get("progress_ms").getAsLong() : 0L;
		SpotifyTrack track = SpotifyTrack.EMPTY;
		if (root.has("item") && root.get("item").isJsonObject()) {
			track = parseTrack(root.getAsJsonObject("item"));
		}
		String deviceId = "";
		String deviceName = "";
		if (root.has("device") && root.get("device").isJsonObject()) {
			JsonObject device = root.getAsJsonObject("device");
			deviceId = text(device, "id");
			deviceName = text(device, "name");
		}
		String status = track.isEmpty() ? SpotifyMessages.NO_MUSIC : "";
		return new SpotifyPlaybackState(true, playing, track, progress, deviceId, deviceName, status, System.currentTimeMillis());
	}

	public List<SpotifyDevice> fetchDevices() throws IOException, InterruptedException {
		HttpResponse<String> response = get("/me/player/devices");
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = get("/me/player/devices");
		}
		mapError(response.statusCode(), response.body());
		List<SpotifyDevice> devices = new ArrayList<>();
		JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
		if (!root.has("devices") || !root.get("devices").isJsonArray()) {
			return devices;
		}
		for (JsonElement el : root.getAsJsonArray("devices")) {
			if (!el.isJsonObject()) {
				continue;
			}
			JsonObject d = el.getAsJsonObject();
			devices.add(new SpotifyDevice(
					text(d, "id"),
					text(d, "name"),
					text(d, "type"),
					d.has("is_active") && d.get("is_active").getAsBoolean(),
					d.has("is_restricted") && d.get("is_restricted").getAsBoolean(),
					d.has("volume_percent") && !d.get("volume_percent").isJsonNull()
							? d.get("volume_percent").getAsInt() : 0
			));
		}
		return devices;
	}

	public SpotifyProfileStore fetchProfile() throws IOException, InterruptedException {
		HttpResponse<String> response = get("/me");
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = get("/me");
		}
		mapError(response.statusCode(), response.body());
		JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
		SpotifyProfileStore profile = new SpotifyProfileStore();
		profile.id = text(root, "id");
		profile.displayName = text(root, "display_name");
		if (profile.displayName.isBlank()) {
			profile.displayName = text(root, "id");
		}
		profile.product = text(root, "product");
		return profile;
	}

	public List<SpotifySearchItem> searchCatalog(String query, int limit) throws IOException, InterruptedException {
		String q = URLEncoder.encode(query, StandardCharsets.UTF_8);
		int lim = Math.min(8, Math.max(1, limit));
		String path = "/search?type=track,album,artist,playlist&limit=" + lim + "&q=" + q;
		HttpResponse<String> response = get(path);
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = get(path);
		}
		mapError(response.statusCode(), response.body());
		List<SpotifySearchItem> out = new ArrayList<>();
		JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
		appendTracks(root, out);
		appendAlbums(root, out);
		appendArtists(root, out);
		appendPlaylists(root, out);
		return out;
	}

	public List<SpotifyTrack> searchTracks(String query, int limit) throws IOException, InterruptedException {
		List<SpotifySearchItem> items = searchCatalog(query, limit);
		List<SpotifyTrack> tracks = new ArrayList<>();
		for (SpotifySearchItem item : items) {
			if (item.isTrack()) {
				tracks.add(new SpotifyTrack(item.id(), item.name(), item.subtitle(), "", item.uri(), item.coverUrl(), 0L));
			}
		}
		return tracks;
	}

	public List<SpotifyTrack> recentlyPlayed(int limit) throws IOException, InterruptedException {
		HttpResponse<String> response = get("/me/player/recently-played?limit=" + Math.min(20, Math.max(1, limit)));
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = get("/me/player/recently-played?limit=" + Math.min(20, Math.max(1, limit)));
		}
		mapError(response.statusCode(), response.body());
		List<SpotifyTrack> tracks = new ArrayList<>();
		JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
		if (!root.has("items") || !root.get("items").isJsonArray()) {
			return tracks;
		}
		for (JsonElement el : root.getAsJsonArray("items")) {
			if (!el.isJsonObject()) {
				continue;
			}
			JsonObject item = el.getAsJsonObject();
			if (item.has("track") && item.get("track").isJsonObject()) {
				tracks.add(parseTrack(item.getAsJsonObject("track")));
			}
		}
		return tracks;
	}

	public void playPause(boolean play, String deviceId) throws IOException, InterruptedException {
		String path = play ? "/me/player/play" : "/me/player/pause";
		if (deviceId != null && !deviceId.isBlank()) {
			path += "?device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
		}
		HttpResponse<String> response = put(path, play ? "{}" : null);
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = put(path, play ? "{}" : null);
		}
		mapError(response.statusCode(), response.body());
	}

	public void next(String deviceId) throws IOException, InterruptedException {
		String path = "/me/player/next";
		if (deviceId != null && !deviceId.isBlank()) {
			path += "?device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
		}
		HttpResponse<String> response = post(path);
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = post(path);
		}
		mapError(response.statusCode(), response.body());
	}

	public void previous(String deviceId) throws IOException, InterruptedException {
		String path = "/me/player/previous";
		if (deviceId != null && !deviceId.isBlank()) {
			path += "?device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
		}
		HttpResponse<String> response = post(path);
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = post(path);
		}
		mapError(response.statusCode(), response.body());
	}

	public void playUri(String uri, String deviceId) throws IOException, InterruptedException {
		String path = "/me/player/play";
		if (deviceId != null && !deviceId.isBlank()) {
			path += "?device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
		}
		String body;
		if (uri != null && (uri.startsWith("spotify:album:") || uri.startsWith("spotify:playlist:")
				|| uri.startsWith("spotify:artist:"))) {
			body = "{\"context_uri\":\"" + uri.replace("\"", "") + "\"}";
		} else {
			body = "{\"uris\":[\"" + uri.replace("\"", "") + "\"]}";
		}
		HttpResponse<String> response = put(path, body);
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = put(path, body);
		}
		mapError(response.statusCode(), response.body());
	}

	private static void appendTracks(JsonObject root, List<SpotifySearchItem> out) {
		if (!root.has("tracks") || !root.get("tracks").isJsonObject()) {
			return;
		}
		JsonArray items = root.getAsJsonObject("tracks").getAsJsonArray("items");
		if (items == null) {
			return;
		}
		for (JsonElement el : items) {
			if (!el.isJsonObject()) {
				continue;
			}
			SpotifyTrack track = parseTrack(el.getAsJsonObject());
			out.add(new SpotifySearchItem("track", track.id(), track.name(), track.artists(), track.uri(), track.coverUrl()));
		}
	}

	private static void appendAlbums(JsonObject root, List<SpotifySearchItem> out) {
		if (!root.has("albums") || !root.get("albums").isJsonObject()) {
			return;
		}
		JsonArray items = root.getAsJsonObject("albums").getAsJsonArray("items");
		if (items == null) {
			return;
		}
		for (JsonElement el : items) {
			if (!el.isJsonObject()) {
				continue;
			}
			JsonObject o = el.getAsJsonObject();
			out.add(new SpotifySearchItem("album", text(o, "id"), text(o, "name"),
					artistsLine(o), text(o, "uri"), firstImage(o)));
		}
	}

	private static void appendArtists(JsonObject root, List<SpotifySearchItem> out) {
		if (!root.has("artists") || !root.get("artists").isJsonObject()) {
			return;
		}
		JsonArray items = root.getAsJsonObject("artists").getAsJsonArray("items");
		if (items == null) {
			return;
		}
		for (JsonElement el : items) {
			if (!el.isJsonObject()) {
				continue;
			}
			JsonObject o = el.getAsJsonObject();
			out.add(new SpotifySearchItem("artist", text(o, "id"), text(o, "name"),
					"Artist", text(o, "uri"), firstImage(o)));
		}
	}

	private static void appendPlaylists(JsonObject root, List<SpotifySearchItem> out) {
		if (!root.has("playlists") || !root.get("playlists").isJsonObject()) {
			return;
		}
		JsonArray items = root.getAsJsonObject("playlists").getAsJsonArray("items");
		if (items == null) {
			return;
		}
		for (JsonElement el : items) {
			if (el == null || !el.isJsonObject()) {
				continue;
			}
			JsonObject o = el.getAsJsonObject();
			String owner = "";
			if (o.has("owner") && o.get("owner").isJsonObject()) {
				owner = text(o.getAsJsonObject("owner"), "display_name");
			}
			out.add(new SpotifySearchItem("playlist", text(o, "id"), text(o, "name"),
					owner.isBlank() ? "Playlist" : owner, text(o, "uri"), firstImage(o)));
		}
	}

	private static String artistsLine(JsonObject o) {
		StringBuilder artists = new StringBuilder();
		if (o.has("artists") && o.get("artists").isJsonArray()) {
			for (JsonElement el : o.getAsJsonArray("artists")) {
				if (!el.isJsonObject()) {
					continue;
				}
				if (!artists.isEmpty()) {
					artists.append(", ");
				}
				artists.append(text(el.getAsJsonObject(), "name"));
			}
		}
		return artists.toString();
	}

	private static String firstImage(JsonObject o) {
		if (!o.has("images") || !o.get("images").isJsonArray()) {
			return "";
		}
		JsonArray images = o.getAsJsonArray("images");
		if (images.isEmpty() || !images.get(0).isJsonObject()) {
			return "";
		}
		return text(images.get(0).getAsJsonObject(), "url");
	}

	public void transferPlayback(String deviceId, boolean play) throws IOException, InterruptedException {
		String body = "{\"device_ids\":[\"" + deviceId.replace("\"", "") + "\"],\"play\":" + play + "}";
		HttpResponse<String> response = put("/me/player", body);
		if (response.statusCode() == 401) {
			auth.refreshAccessToken();
			response = put("/me/player", body);
		}
		mapError(response.statusCode(), response.body());
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		String token = auth.validAccessToken();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(API + path))
				.timeout(Duration.ofSeconds(8))
				.header("Authorization", "Bearer " + token)
				.GET()
				.build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> put(String path, String jsonBody) throws IOException, InterruptedException {
		String token = auth.validAccessToken();
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(API + path))
				.timeout(Duration.ofSeconds(8))
				.header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json");
		if (jsonBody == null) {
			builder.PUT(HttpRequest.BodyPublishers.noBody());
		} else {
			builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
		}
		return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path) throws IOException, InterruptedException {
		String token = auth.validAccessToken();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(API + path))
				.timeout(Duration.ofSeconds(8))
				.header("Authorization", "Bearer " + token)
				.header("Content-Length", "0")
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static void mapError(int status, String body) throws IOException {
		if (status == 204 || (status >= 200 && status < 300)) {
			return;
		}
		if (status == 401) {
			throw new IOException(SpotifyMessages.RECONNECTING);
		}
		if (status == 403) {
			throw new IOException(SpotifyMessages.PREMIUM_REQUIRED);
		}
		if (status == 404) {
			throw new IOException(SpotifyMessages.NO_DEVICE);
		}
		if (status == 429) {
			throw new IOException(SpotifyMessages.RATE_LIMITED);
		}
		if (body != null && body.toLowerCase().contains("premium")) {
			throw new IOException(SpotifyMessages.PREMIUM_REQUIRED);
		}
		throw new IOException(SpotifyMessages.NETWORK_ERROR);
	}

	private static SpotifyTrack parseTrack(JsonObject item) {
		String id = text(item, "id");
		String name = text(item, "name");
		String uri = text(item, "uri");
		long duration = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0L;
		StringBuilder artists = new StringBuilder();
		if (item.has("artists") && item.get("artists").isJsonArray()) {
			for (JsonElement el : item.getAsJsonArray("artists")) {
				if (!el.isJsonObject()) {
					continue;
				}
				if (!artists.isEmpty()) {
					artists.append(", ");
				}
				artists.append(text(el.getAsJsonObject(), "name"));
			}
		}
		String album = "";
		String cover = "";
		if (item.has("album") && item.get("album").isJsonObject()) {
			JsonObject albumObj = item.getAsJsonObject("album");
			album = text(albumObj, "name");
			if (albumObj.has("images") && albumObj.get("images").isJsonArray()) {
				JsonArray images = albumObj.getAsJsonArray("images");
				if (!images.isEmpty() && images.get(0).isJsonObject()) {
					cover = text(images.get(0).getAsJsonObject(), "url");
					// Prefer mid-size image when available
					for (JsonElement img : images) {
						if (!img.isJsonObject()) {
							continue;
						}
						JsonObject o = img.getAsJsonObject();
						int w = o.has("width") && !o.get("width").isJsonNull() ? o.get("width").getAsInt() : 0;
						if (w >= 200 && w <= 400) {
							cover = text(o, "url");
							break;
						}
					}
				}
			}
		}
		return new SpotifyTrack(id, name, artists.toString(), album, uri, cover, duration);
	}

	private static String text(JsonObject obj, String key) {
		return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
	}
}

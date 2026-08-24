package io.github.nitro.spotify;

public final class SpotifyMessages {

	public static final String NOT_CONNECTED = "Spotify is not connected.";
	public static final String NO_DEVICE = "No active Spotify device found.";
	public static final String PREMIUM_REQUIRED = "Spotify Premium is required for playback control.";
	public static final String AUTH_FAILED = "Spotify authorization failed.";
	public static final String AUTH_CANCELLED = "Spotify authorization was cancelled.";
	public static final String RECONNECTING = "Spotify connection expired. Reconnecting...";
	public static final String UNABLE_CONNECT = "Unable to connect to Spotify.";
	public static final String CLIENT_ID_MISSING = "Spotify Client ID is not configured.";
	public static final String RATE_LIMITED = "Spotify rate limit reached. Slowing down...";
	public static final String NO_MUSIC = "No music playing";
	public static final String NETWORK_ERROR = "Unable to reach Spotify. Check your network.";

	private SpotifyMessages() {
	}
}

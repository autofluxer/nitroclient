package io.github.nitro.spotify;

public record SpotifyPlaybackState(
		boolean connected,
		boolean playing,
		SpotifyTrack track,
		long progressMs,
		String deviceId,
		String deviceName,
		String statusMessage,
		long updatedAtMs
) {
	public static SpotifyPlaybackState disconnected(String message) {
		return new SpotifyPlaybackState(false, false, SpotifyTrack.EMPTY, 0L, "", "", message, System.currentTimeMillis());
	}

	public static SpotifyPlaybackState idle(String message) {
		return new SpotifyPlaybackState(true, false, SpotifyTrack.EMPTY, 0L, "", "", message, System.currentTimeMillis());
	}

	public boolean hasTrack() {
		return track != null && !track.isEmpty();
	}
}

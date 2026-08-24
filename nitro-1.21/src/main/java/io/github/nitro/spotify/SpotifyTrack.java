package io.github.nitro.spotify;

public record SpotifyTrack(
		String id,
		String name,
		String artists,
		String album,
		String uri,
		String coverUrl,
		long durationMs
) {
	public static final SpotifyTrack EMPTY = new SpotifyTrack("", "", "", "", "", "", 0L);

	public boolean isEmpty() {
		return id == null || id.isBlank();
	}
}

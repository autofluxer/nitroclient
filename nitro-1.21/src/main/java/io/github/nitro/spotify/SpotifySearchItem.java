package io.github.nitro.spotify;

/** Unified searchable / playable Spotify item (track, album, playlist, artist). */
public record SpotifySearchItem(
		String type,
		String id,
		String name,
		String subtitle,
		String uri,
		String coverUrl
) {
	public boolean isTrack() {
		return "track".equalsIgnoreCase(type);
	}

	public boolean isEmpty() {
		return id == null || id.isBlank();
	}
}

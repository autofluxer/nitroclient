package io.github.nitro.spotify;

public record SpotifyDevice(
		String id,
		String name,
		String type,
		boolean active,
		boolean restricted,
		int volumePercent
) {
}

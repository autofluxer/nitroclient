package io.github.nitro.config;

/** Non-secret Spotify HUD / UX preferences persisted in nitro-client.json. */
public final class SpotifyUiSettings {

	public boolean showHud = true;
	public boolean showAlbumCover = true;
	public boolean showProgressBar = true;
	public boolean showArtist = true;
	public boolean showSongName = true;
	public boolean showDevice = false;
	public boolean showControls = true;
	public boolean autoRefresh = true;
	public float hudScale = 1F;
	public float hudOpacity = 0.92F;
	/** Preferred Spotify Connect device id (empty = auto). */
	public String preferredDeviceId = "";
	/** Poll interval for currently-playing in milliseconds. */
	public int refreshIntervalMs = 2000;

	public void normalize() {
		hudScale = Math.max(0.75F, Math.min(2F, hudScale));
		hudOpacity = Math.max(0.25F, Math.min(1F, hudOpacity));
		if (preferredDeviceId == null) {
			preferredDeviceId = "";
		}
		if (refreshIntervalMs < 1000) {
			refreshIntervalMs = 1000;
		}
		if (refreshIntervalMs > 15_000) {
			refreshIntervalMs = 15_000;
		}
	}
}

package io.github.nitro.config;

public final class HudElementLayout {
	public int x = 4;
	public int y = 4;
	public float scale = 1F;
	public boolean visible = true;

	public HudElementLayout copy() {
		HudElementLayout copy = new HudElementLayout();
		copy.x = x;
		copy.y = y;
		copy.scale = scale;
		copy.visible = visible;
		return copy;
	}
}

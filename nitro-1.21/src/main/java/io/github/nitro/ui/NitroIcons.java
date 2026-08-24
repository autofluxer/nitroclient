package io.github.nitro.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class NitroIcons {

	private NitroIcons() {
	}

	public static void draw(DrawContext context, Id id, int x, int y, int size) {
		draw(context, id, x, y, size, 1F);
	}

	public static void draw(DrawContext context, Id id, int x, int y, int size, float alpha) {
		if (id == null || size <= 0 || alpha <= 0.01F) {
			return;
		}
		NitroDraw.blit(context, id.texture(), x, y, size, alpha);
	}

	public enum Id {
		USER("user"),
		USERS("users"),
		HANGER("hanger"),
		GRID("grid"),
		LAYOUT("layout"),
		BAG("bag"),
		SHOP("shop"),
		CHAT("chat"),
		CAMERA("camera"),
		GEAR("gear"),
		DIAMOND("diamond"),
		FOLDER("folder"),
		MONITOR("monitor"),
		MODS("mods"),
		SPEED("speed"),
		DISCORD("discord");

		private final Identifier texture;

		Id(String file) {
			this.texture = Identifier.of("nitro", "textures/gui/icons/" + file + ".png");
		}

		public Identifier texture() {
			return texture;
		}
	}
}

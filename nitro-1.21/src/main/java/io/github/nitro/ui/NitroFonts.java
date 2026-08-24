package io.github.nitro.ui;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Custom UI fonts (bold sans for brand mark). */
public final class NitroFonts {

	public static final Identifier BRAND_ID = Identifier.of("nitro", "brand");
	public static final StyleSpriteSource BRAND_FONT = new StyleSpriteSource.Font(BRAND_ID);

	private NitroFonts() {
	}

	public static Text brand(String text) {
		return Text.literal(text).setStyle(Style.EMPTY.withFont(BRAND_FONT));
	}
}

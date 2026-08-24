package io.github.nitro.mixin;

import io.github.nitro.client.NitroUsers;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the Nitro icon before the tab-list name (including rank prefixes like MEMBER).
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

	@Unique
	private static final Identifier TAB_ICON_FONT = Identifier.of("nitro", "tab_icon");

	@Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
	private void nitro$prefixTabIcon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
		if (entry == null) {
			return;
		}
		if (!NitroUsers.isNitro(entry.getProfile().id())) {
			return;
		}
		Text original = cir.getReturnValue();
		if (original == null) {
			return;
		}
		// Root must stay on the default font. If the icon is the root, Minecraft inherits its
		// custom font onto MEMBER/name and every letter becomes a missing-glyph box.
		MutableText icon = Text.literal("\uA100")
				.styled(style -> style.withFont(new StyleSpriteSource.Font(TAB_ICON_FONT)));
		MutableText name = original.copy()
				.styled(style -> style.withFont(StyleSpriteSource.DEFAULT));
		cir.setReturnValue(Text.empty().append(icon).append(Text.literal(" ")).append(name));
	}
}

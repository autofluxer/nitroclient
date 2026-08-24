package io.github.nitro.mixin;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.NitroDraw;
import io.github.nitro.ui.NitroPauseScreen;
import io.github.nitro.ui.NitroSettingsPopup;
import io.github.nitro.ui.NitroTitleScreen;
import io.github.nitro.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenBackgroundMixin {

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void nitro$themedBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!NitroConfig.INSTANCE.fancyMainMenu) {
			return;
		}
		Screen self = (Screen) (Object) this;
		if (self instanceof NitroTitleScreen || self instanceof ClickGuiScreen || self instanceof NitroPauseScreen
				|| self instanceof NitroSettingsPopup) {
			return;
		}
		if (!(self instanceof GameMenuScreen
				|| self instanceof OptionsScreen
				|| self instanceof MultiplayerScreen
				|| self instanceof SelectWorldScreen
				|| self instanceof TitleScreen)) {
			return;
		}
		int w = self.width;
		int h = self.height;
		NitroDraw.drawCoverBackground(context, NitroDraw.BG_MENU, w, h);
		context.fill(0, 0, w, h, 0x88000000);
		ci.cancel();
	}
}

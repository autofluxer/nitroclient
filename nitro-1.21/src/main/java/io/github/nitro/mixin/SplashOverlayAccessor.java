package io.github.nitro.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.screen.SplashOverlay;

@Mixin(SplashOverlay.class)
public interface SplashOverlayAccessor {

	@Accessor("reloadCompleteTime")
	long nitro$getReloadCompleteTime();

	@Accessor("reloadCompleteTime")
	void nitro$setReloadCompleteTime(long time);
}

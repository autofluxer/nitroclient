package io.github.nitro.mixin;

import io.github.nitro.module.impl.ZoomModule;
import io.github.nitro.render.ClientFov;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void nitro$zoomFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		float adjusted = ZoomModule.adjustFov(cir.getReturnValue());
		if (adjusted != cir.getReturnValue()) {
			cir.setReturnValue(adjusted);
		}
		ClientFov.degrees = cir.getReturnValue();
	}
}

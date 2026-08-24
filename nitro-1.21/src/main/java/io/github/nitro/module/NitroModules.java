package io.github.nitro.module;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.config.NitroConfig;
import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.module.impl.ArrayListHudModule;
import io.github.nitro.module.impl.ArmorHudModule;
import io.github.nitro.module.impl.BpsHudModule;
import io.github.nitro.module.impl.ClockHudModule;
import io.github.nitro.module.impl.ComboHudModule;
import io.github.nitro.module.impl.CoordinatesHudModule;
import io.github.nitro.module.impl.CpsHudModule;
import io.github.nitro.module.impl.DirectionHudModule;
import io.github.nitro.module.impl.FpsHudModule;
import io.github.nitro.module.impl.FullbrightModule;
import io.github.nitro.module.impl.KeystrokesHudModule;
import io.github.nitro.module.impl.PingHudModule;
import io.github.nitro.module.impl.PotionHudModule;
import io.github.nitro.module.impl.ReachHudModule;
import io.github.nitro.module.impl.SpotifyHudModule;
import io.github.nitro.module.impl.TargetHudModule;
import io.github.nitro.module.impl.ToggleSprintModule;
import io.github.nitro.module.impl.WatermarkHudModule;
import io.github.nitro.module.impl.ZoomModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NitroModules {

	private static final Map<String, NitroModule> BY_ID = new LinkedHashMap<>();
	private static final List<NitroModule> ALL = new ArrayList<>();

	private NitroModules() {
	}

	public static void init() {
		if (!ALL.isEmpty()) {
			return;
		}
		register(new WatermarkHudModule());
		register(new FpsHudModule());
		register(new CpsHudModule());
		register(new PingHudModule());
		register(new CoordinatesHudModule());
		register(new DirectionHudModule());
		register(new ClockHudModule());
		register(new BpsHudModule());
		register(new KeystrokesHudModule());
		register(new ArmorHudModule());
		register(new PotionHudModule());
		register(new ArrayListHudModule());
		register(new ComboHudModule());
		register(new ReachHudModule());
		register(new TargetHudModule());
		register(new SpotifyHudModule());
		register(new FullbrightModule());
		register(new ToggleSprintModule());
		register(ZoomModule.create());

		for (NitroModule module : ALL) {
			Boolean saved = NitroConfig.INSTANCE.moduleStates.get(module.getId());
			if (saved != null) {
				module.setEnabled(saved);
			}
		}
	}

	private static void register(NitroModule module) {
		ALL.add(module);
		BY_ID.put(module.getId(), module);
	}

	public static List<NitroModule> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static List<NitroModule> byCategory(NitroModuleCategory category) {
		return ALL.stream().filter(module -> module.getCategory() == category).toList();
	}

	public static NitroModule get(String id) {
		return BY_ID.get(id);
	}

	public static void toggle(String id) {
		NitroModule module = BY_ID.get(id);
		if (module != null) {
			module.toggle();
			NitroConfig.INSTANCE.moduleStates.put(id, module.isEnabled());
			syncHudVisibility(module);
			NitroConfig.save();
		}
	}

	public static void setEnabled(String id, boolean enabled) {
		NitroModule module = BY_ID.get(id);
		if (module != null && module.isEnabled() != enabled) {
			module.setEnabled(enabled);
			NitroConfig.INSTANCE.moduleStates.put(id, enabled);
			syncHudVisibility(module);
			NitroConfig.save();
		}
	}

	private static void syncHudVisibility(NitroModule module) {
		if (module instanceof PositionedHudModule hud) {
			HudElementLayout layout = HudLayoutStore.get(hud.hudId());
			layout.visible = module.isEnabled();
			NitroConfig.INSTANCE.hudLayouts.put(hud.hudId(), layout.copy());
		}
	}

	public static List<NitroModule> gameplayModules() {
		return Collections.unmodifiableList(ALL);
	}

	public static void clientTick() {
		// Skip module ticks while minimized / unfocused so overlays stay in sync with the game.
		if (io.github.nitro.config.NitroConfig.INSTANCE.pauseOverlaysWhenUnfocused
				&& !io.github.nitro.client.NitroClientActivity.isGameActive()) {
			return;
		}
		for (NitroModule module : ALL) {
			if (module.isEnabled() && module instanceof TickableModule tickable) {
				tickable.onClientTick();
			}
		}
	}

	public static void hudRender(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
		var client = net.minecraft.client.MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		if (io.github.nitro.config.NitroConfig.INSTANCE.pauseOverlaysWhenUnfocused
				&& !io.github.nitro.client.NitroClientActivity.shouldRenderOverlays()) {
			return;
		}
		for (NitroModule module : ALL) {
			if (module.isEnabled() && module instanceof HudModule hud) {
				hud.renderHud(context, tickCounter);
			}
		}
		io.github.nitro.hud.HudEditorState.drawOverlay(context,
				client.getWindow().getScaledWidth(),
				client.getWindow().getScaledHeight());
	}
}

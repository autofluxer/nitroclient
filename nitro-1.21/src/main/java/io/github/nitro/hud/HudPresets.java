package io.github.nitro.hud;

import io.github.nitro.config.HudElementLayout;
import io.github.nitro.config.NitroConfig;
import io.github.nitro.module.NitroModules;
import net.minecraft.client.MinecraftClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudPresets {

	private static final List<Preset> PRESETS = List.of(
			preset("pvp", "nitro.hud.preset.pvp",
					modules("watermark", true, "fps", true, "cps", true, "ping", true, "keystrokes", true,
							"combo", true, "reach", true, "target", true, "armor", true, "potions", true,
							"arraylist", true, "coordinates", false, "direction", false, "clock", false, "bps", true),
					layouts("watermark", spec(6, 6, 1.1f, true), "fps", spec(6, 20, 1.0f, true),
							"cps", spec(6, 34, 1.0f, true), "ping", spec(6, 48, 1.0f, true),
							"bps", spec(6, 62, 1.0f, true), "keystrokes", spec(-96, -78, 1.0f, true),
							"combo", spec(-90, -120, 1.0f, true), "reach", spec(6, 76, 1.0f, true),
							"target", spec(-70, 48, 1.0f, true), "armor", spec(-110, -28, 1.0f, true),
							"potions", spec(6, 100, 1.0f, true), "arraylist", spec(-8, 40, 1.0f, true))),
			preset("smp", "nitro.hud.preset.smp",
					modules("watermark", true, "fps", true, "cps", false, "ping", true, "coordinates", true,
							"direction", true, "clock", true, "bps", false, "keystrokes", false, "combo", false,
							"reach", false, "target", false, "armor", true, "potions", true, "arraylist", false),
					layouts("watermark", spec(6, 6, 1.05f, true), "fps", spec(6, 20, 1.0f, true),
							"ping", spec(6, 34, 1.0f, true), "coordinates", spec(6, -52, 1.0f, true),
							"direction", spec(-70, 6, 1.0f, true), "clock", spec(-90, 22, 1.0f, true),
							"armor", spec(-110, -28, 1.0f, true), "potions", spec(6, 70, 1.0f, true))),
			preset("streamer", "nitro.hud.preset.streamer",
					modules("watermark", true, "fps", true, "cps", true, "ping", false, "keystrokes", true,
							"coordinates", false, "direction", false, "clock", false, "bps", false, "combo", false,
							"reach", false, "target", false, "armor", false, "potions", false, "arraylist", false),
					layouts("watermark", spec(8, 8, 1.15f, true), "fps", spec(8, 24, 1.0f, true),
							"cps", spec(8, 38, 1.0f, true), "keystrokes", spec(-100, -80, 1.05f, true))),
			preset("minimal", "nitro.hud.preset.minimal",
					modules("watermark", true, "fps", true, "cps", false, "ping", false, "coordinates", false,
							"direction", false, "clock", false, "bps", false, "keystrokes", false, "combo", false,
							"reach", false, "target", false, "armor", false, "potions", false, "arraylist", false),
					layouts("watermark", spec(6, 6, 1.0f, true), "fps", spec(6, 20, 1.0f, true)))
	);

	private HudPresets() {
	}

	public static List<Preset> all() {
		return PRESETS;
	}

	public static void apply(String presetId) {
		Preset preset = PRESETS.stream().filter(p -> p.id().equals(presetId)).findFirst().orElse(null);
		if (preset == null) {
			return;
		}
		for (String id : HudElements.ALL) {
			boolean enabled = Boolean.TRUE.equals(preset.modules().get(id));
			NitroModules.setEnabled(id, enabled);
		}
		NitroConfig.INSTANCE.hudLayouts.clear();
		int screenW = screenW();
		int screenH = screenH();
		for (Map.Entry<String, LayoutSpec> entry : preset.layouts().entrySet()) {
			LayoutSpec spec = entry.getValue();
			HudElementLayout layout = new HudElementLayout();
			layout.x = spec.x() < 0 ? screenW + spec.x() : spec.x();
			layout.y = spec.y() < 0 ? screenH + spec.y() : spec.y();
			layout.scale = spec.scale();
			layout.visible = spec.visible();
			NitroConfig.INSTANCE.hudLayouts.put(entry.getKey(), layout);
		}
		for (String id : HudElements.ALL) {
			if (preset.layouts().containsKey(id)) {
				continue;
			}
			HudElementLayout layout = HudLayoutStore.get(id);
			layout.visible = false;
			NitroConfig.INSTANCE.hudLayouts.put(id, layout);
		}
		NitroConfig.INSTANCE.activeHudPreset = presetId;
		NitroConfig.save();
	}

	private static Preset preset(String id, String nameKey, Map<String, Boolean> modules, Map<String, LayoutSpec> layouts) {
		return new Preset(id, nameKey, Map.copyOf(modules), Map.copyOf(layouts));
	}

	private static Map<String, Boolean> modules(Object... pairs) {
		LinkedHashMap<String, Boolean> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put((String) pairs[i], (Boolean) pairs[i + 1]);
		}
		return map;
	}

	private static Map<String, LayoutSpec> layouts(Object... pairs) {
		LinkedHashMap<String, LayoutSpec> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put((String) pairs[i], (LayoutSpec) pairs[i + 1]);
		}
		return map;
	}

	private static LayoutSpec spec(int x, int y, float scale, boolean visible) {
		return new LayoutSpec(x, y, scale, visible);
	}

	private static int screenW() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client != null ? client.getWindow().getScaledWidth() : 854;
	}

	private static int screenH() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client != null ? client.getWindow().getScaledHeight() : 480;
	}

	public record Preset(String id, String nameKey, Map<String, Boolean> modules, Map<String, LayoutSpec> layouts) {
	}

	public record LayoutSpec(int x, int y, float scale, boolean visible) {
	}
}

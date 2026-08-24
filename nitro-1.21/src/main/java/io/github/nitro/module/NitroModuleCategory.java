package io.github.nitro.module;

public enum NitroModuleCategory {
	HUD("nitro.module.category.hud"),
	UTILITY("nitro.module.category.utility");

	private final String translationKey;

	NitroModuleCategory(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return translationKey;
	}
}

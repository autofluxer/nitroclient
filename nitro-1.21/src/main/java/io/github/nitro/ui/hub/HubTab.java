package io.github.nitro.ui.hub;

public enum HubTab {
	MODULES("nitro.hub.tab.modules");

	private final String translationKey;

	HubTab(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return translationKey;
	}
}

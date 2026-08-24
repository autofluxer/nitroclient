package io.github.nitro.module;

import net.minecraft.text.Text;

public abstract class NitroModule {

	private final String id;
	private final String nameKey;
	private final String descriptionKey;
	private final NitroModuleCategory category;
	private boolean enabled;

	protected NitroModule(String id, String nameKey, String descriptionKey, NitroModuleCategory category) {
		this.id = id;
		this.nameKey = nameKey;
		this.descriptionKey = descriptionKey;
		this.category = category;
	}

	public String getId() {
		return id;
	}

	public String getNameKey() {
		return nameKey;
	}

	public String getDescriptionKey() {
		return descriptionKey;
	}

	public NitroModuleCategory getCategory() {
		return category;
	}

	public Text getName() {
		return Text.translatable(nameKey);
	}

	public Text getDescription() {
		return Text.translatable(descriptionKey);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}
}

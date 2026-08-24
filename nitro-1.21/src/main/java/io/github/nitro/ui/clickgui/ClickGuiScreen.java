package io.github.nitro.ui.clickgui;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.hud.HudEditorState;
import io.github.nitro.hud.HudElements;
import io.github.nitro.hud.HudLayoutStore;
import io.github.nitro.module.NitroModule;
import io.github.nitro.module.NitroModuleCategory;
import io.github.nitro.module.NitroModules;
import io.github.nitro.ui.FeatherPalette;
import io.github.nitro.ui.NitroActionButton;
import io.github.nitro.ui.NitroDraw;
import io.github.nitro.ui.NitroIcons;
import io.github.nitro.ui.NitroLogoRenderer;
import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.TitleIconButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Feather Mod Menu: centered dark window, red tabs, category pills, card grid.
 */
public final class ClickGuiScreen extends Screen {

	private final Screen parent;
	private ClickGuiTab tab = ClickGuiTab.MODULES;
	private NitroModuleCategory category;
	private int scroll;
	private TextFieldWidget search;
	private String searchText = "";

	public ClickGuiScreen(Screen parent) {
		this(parent, ClickGuiTab.MODULES);
	}

	public ClickGuiScreen(Screen parent, ClickGuiTab initialTab) {
		super(Text.translatable("nitro.clickgui.title"));
		this.parent = parent;
		this.tab = initialTab;
	}

	public void reinit() {
		init();
	}

	@Override
	protected void init() {
		NitroConfig.reloadTheme();
		clearChildren();
		HudEditorState.active = tab == ClickGuiTab.HUD;
		HudEditorState.snapGrid = NitroConfig.INSTANCE.hudSnapGrid;
		if (tab == ClickGuiTab.THEMES) {
			tab = ClickGuiTab.MODULES;
		}

		int px = panelX();
		int py = panelY();
		int pw = panelW();

		addDrawableChild(new TitleIconButton(px + 10, py + 8, TitleIconButton.Kind.MODS, "Mods",
				tab == ClickGuiTab.MODULES, b -> switchTab(ClickGuiTab.MODULES)));
		addDrawableChild(new TitleIconButton(px + 38, py + 8, TitleIconButton.Kind.HUD, "HUD Editor",
				tab == ClickGuiTab.HUD, b -> switchTab(ClickGuiTab.HUD)));
		addDrawableChild(new NitroActionButton(px + pw - 28, py + 8, 18, 18, Text.literal("X"),
				NitroActionButton.Style.DANGER, b -> close()));

		if (tab == ClickGuiTab.HUD) {
			addDrawableChild(new NitroActionButton(px + 12, py + 40, 88, 18, Text.translatable("nitro.hud.reset"),
					NitroActionButton.Style.GHOST, b -> {
						HudLayoutStore.resetAll();
						init();
					}));
			addDrawableChild(new NitroActionButton(px + 104, py + 40, 100, 18,
					Text.translatable(HudEditorState.snapGrid ? "nitro.hud.snap_on" : "nitro.hud.snap_off"),
					NitroActionButton.Style.NAV, b -> {
						HudEditorState.snapGrid = !HudEditorState.snapGrid;
						NitroConfig.INSTANCE.hudSnapGrid = HudEditorState.snapGrid;
						NitroConfig.save();
						init();
					}));
			initHudToggles();
			return;
		}

		int pillY = py + 40;
		int pillX = px + 12;
		pillX += addPill(pillX, pillY, "All", category == null, b -> setCategory(null));
		pillX += addPill(pillX, pillY, "HUD", category == NitroModuleCategory.HUD,
				b -> setCategory(NitroModuleCategory.HUD));
		addPill(pillX, pillY, "Utility", category == NitroModuleCategory.UTILITY,
				b -> setCategory(NitroModuleCategory.UTILITY));

		int sw = 110;
		search = new TextFieldWidget(textRenderer, px + pw - sw - 14, pillY, sw, 16, Text.literal("Search"));
		search.setPlaceholder(Text.literal("Search"));
		search.setMaxLength(40);
		search.setText(searchText);
		search.setChangedListener(value -> {
			String next = value == null ? "" : value;
			if (next.equals(searchText)) {
				return;
			}
			searchText = next;
			scroll = 0;
			init();
		});
		addDrawableChild(search);

		initModuleCards();
	}

	private int addPill(int x, int y, String label, boolean active, NitroActionButton.PressAction action) {
		int w = textRenderer.getWidth(label) + 16;
		addDrawableChild(new NitroActionButton(x, y, w, 16, Text.literal(label),
				active ? NitroActionButton.Style.FEATURED : NitroActionButton.Style.NAV, action));
		return w + 4;
	}

	private void setCategory(NitroModuleCategory next) {
		category = next;
		scroll = 0;
		init();
	}

	private void switchTab(ClickGuiTab next) {
		tab = next;
		scroll = 0;
		init();
	}

	private void initModuleCards() {
		int px = panelX() + 12;
		int py = panelY() + 64;
		int pw = panelW() - 24;
		int cardW = LeafModCard.CARD_W;
		int cardH = LeafModCard.CARD_H;
		int gap = 8;
		int cols = Math.max(1, (pw + gap) / (cardW + gap));
		int used = cols * cardW + (cols - 1) * gap;
		int startX = px + Math.max(0, (pw - used) / 2);
		int bottom = panelY() + panelH() - 10;
		int i = 0;
		for (NitroModule module : filteredModules()) {
			int col = i % cols;
			int row = i / cols;
			int x = startX + col * (cardW + gap);
			int y = py + row * (cardH + gap) - scroll;
			if (y + cardH > panelY() + 58 && y < bottom) {
				addDrawableChild(new LeafModCard(x, y, cardW, cardH, module, i));
			}
			i++;
		}
	}

	private List<NitroModule> filteredModules() {
		List<NitroModule> out = new ArrayList<>();
		String q = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
		for (NitroModule module : NitroModules.gameplayModules()) {
			if (category != null && module.getCategory() != category) {
				continue;
			}
			if (!q.isEmpty()) {
				String name = module.getName().getString().toLowerCase(Locale.ROOT);
				if (!name.contains(q) && !module.getId().contains(q)) {
					continue;
				}
			}
			out.add(module);
		}
		return out;
	}

	private void initHudToggles() {
		int px = panelX() + 12;
		int py = panelY() + 66;
		int pw = panelW() - 24;
		int colW = (pw - 8) / 2;
		int i = 0;
		for (String id : HudElements.ALL) {
			int row = i / 2;
			int col = i % 2;
			var layout = HudLayoutStore.get(id);
			addDrawableChild(new NitroActionButton(px + col * (colW + 8), py + row * 22, colW, 18,
					Text.translatable("nitro.hud." + id),
					layout.visible ? NitroActionButton.Style.FEATURED : NitroActionButton.Style.NAV,
					b -> {
						layout.visible = !layout.visible;
						HudLayoutStore.save(id, layout);
						init();
					}));
			i++;
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		HudEditorState.active = false;
		HudEditorState.endDrag();
		if (parent != null) {
			client.setScreen(parent);
		} else {
			client.setScreen(null);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		if (client != null && client.world != null) {
			context.fill(0, 0, width, height, 0x99000000);
		} else {
			NitroDraw.drawCoverBackground(context, NitroDraw.BG_MENU, width, height, 1.08F);
			context.fill(0, 0, width, height, 0x88000000);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);

		NitroLogoRenderer.drawLogo(context, 12, 8, 16);
		context.drawText(textRenderer, "MOD MENU", 32, 12, 0xFFFFFFFF, false);

		int px = panelX();
		int py = panelY();
		int pw = panelW();
		int ph = panelH();
		NitroUiDraw.fillRoundRect(context, px, py, pw, ph, 8, FeatherPalette.PANEL);
		NitroUiDraw.strokeRoundRect(context, px, py, pw, ph, 8, 0x22FFFFFF);

		if (tab == ClickGuiTab.MODULES) {
			NitroUiDraw.fillRoundRect(context, px + 10, py + 8, 22, 22, 4, FeatherPalette.RED);
			NitroIcons.draw(context, NitroIcons.Id.MODS, px + 14, py + 12, 14);
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (tab == ClickGuiTab.HUD && click.button() == 0) {
			String hit = HudEditorState.hitScreen(click.x(), click.y());
			if (hit != null) {
				HudEditorState.beginDrag(hit, (int) click.x(), (int) click.y());
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (HudEditorState.draggingId != null) {
			HudEditorState.endDrag();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (HudEditorState.draggingId != null && tab == ClickGuiTab.HUD) {
			HudEditorState.drag((int) click.x(), (int) click.y());
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (tab == ClickGuiTab.MODULES) {
			scroll = Math.max(0, scroll - (int) (verticalAmount * 22));
			init();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private int panelX() {
		return Math.max(20, (width - panelW()) / 2);
	}

	private int panelY() {
		return Math.max(28, (height - panelH()) / 2);
	}

	private int panelW() {
		return Math.min(720, width - 40);
	}

	private int panelH() {
		return Math.min(400, height - 48);
	}
}

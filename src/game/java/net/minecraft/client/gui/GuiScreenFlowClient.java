package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.resources.I18n;

public class GuiScreenFlowClient extends GuiScreen {
	private static final int PAGE_SIZE = 12;

	private final GuiScreen parent;
	private final FlowModManager.Mod[] toggles = FlowModManager.getAllMods();
	private int infoIndex = -1;
	private int page = 0;

	public GuiScreenFlowClient(GuiScreen parent) {
		this.parent = parent;
	}

	public void initGui() {
		FlowModManager.initializeIfNeeded(this.mc.gameSettings);
		this.buttonList.clear();
		int y = this.height / 6 + 24;
		int start = this.page * PAGE_SIZE;
		int end = Math.min(this.toggles.length, start + PAGE_SIZE);
		for (int i = start; i < end; ++i) {
			int local = i - start;
			int row = local / 2;
			int col = i % 2;
			int x = this.width / 2 - 205 + col * 210;
			this.buttonList.add(new GuiButton(100 + i, x, y + row * 24, 200, 20, this.getToggleLabel(i)));
		}
		int footerY = y + ((PAGE_SIZE + 1) / 2) * 24 + 10;
		this.buttonList.add(new GuiButton(1, this.width / 2 - 154, footerY, 98, 20, "Apply Preset"));
		this.buttonList.add(new GuiButton(3, this.width / 2 - 52, footerY, 48, 20, "<"));
		this.buttonList.add(new GuiButton(4, this.width / 2 + 4, footerY, 48, 20, ">"));
		this.buttonList.add(new GuiButton(2, this.width / 2 + 56, footerY, 98, 20, I18n.format("gui.done")));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id >= 100 && button.id < 100 + this.toggles.length) {
			this.infoIndex = button.id - 100;
			FlowModManager.toggle(this.toggles[this.infoIndex]);
			button.displayString = this.getToggleLabel(this.infoIndex);
		} else if (button.id == 1) {
			FlowModManager.applyRecommendedPreset();
			this.initGui();
		} else if (button.id == 3) {
			int maxPage = (this.toggles.length - 1) / PAGE_SIZE;
			this.page = (this.page - 1 + maxPage + 1) % (maxPage + 1);
			this.initGui();
		} else if (button.id == 4) {
			int maxPage = (this.toggles.length - 1) / PAGE_SIZE;
			this.page = (this.page + 1) % (maxPage + 1);
			this.initGui();
		} else if (button.id == 2) {
			this.mc.displayGuiScreen(this.parent);
		}
	}

	private String getToggleLabel(int index) {
		FlowModManager.Mod feature = this.toggles[index];
		return feature.displayName + ": " + (FlowModManager.isEnabled(feature) ? "ON" : "OFF");
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawGradientRect(0, 0, this.width, this.height, 0xFF041225, 0xFF030B17);
		int panelLeft = this.width / 2 - 220;
		int panelTop = this.height / 6;
		int panelBottom = this.height - 36;
		this.drawRect(panelLeft, panelTop, panelLeft + 440, panelBottom, 0xAA071B34);
		this.drawRect(panelLeft, panelTop, panelLeft + 440, panelTop + 1, 0xFF5DB8FF);
		this.drawCenteredString(this.fontRendererObj, "FLOW CLIENT - MOD MENU", this.width / 2, panelTop + 10,
				0xFFBDE4FF);
		this.drawCenteredString(this.fontRendererObj, "Functional modules + HUD tools (" + this.toggles.length + " total)",
				this.width / 2, panelTop + 24, 0xFF7EAEDB);
		int maxPage = (this.toggles.length - 1) / PAGE_SIZE;
		this.drawCenteredString(this.fontRendererObj, "Page " + (this.page + 1) + " / " + (maxPage + 1),
				this.width / 2, panelTop + 36, 0xFF8CB4DA);

		if (this.infoIndex >= 0 && this.infoIndex < this.toggles.length) {
			FlowModManager.Mod feature = this.toggles[this.infoIndex];
			this.drawCenteredString(this.fontRendererObj, feature.description, this.width / 2, panelBottom - 14,
					0xFFA4C7EB);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}

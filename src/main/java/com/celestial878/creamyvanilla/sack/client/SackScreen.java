package com.celestial878.creamyvanilla.sack.client;

import com.celestial878.creamyvanilla.sack.SackMenu;
import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class SackScreen extends AbstractContainerScreen<SackMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "textures/gui/sack_gui.png");
    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "slot");

    public SackScreen(SackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // The background has no sack slots baked in: draw one for each unlocked slot.
        for (int i = 0; i < this.menu.unlockedSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            graphics.blitSprite(SLOT_SPRITE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 18, 18);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

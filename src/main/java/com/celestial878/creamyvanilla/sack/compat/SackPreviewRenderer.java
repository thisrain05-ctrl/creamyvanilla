package com.celestial878.creamyvanilla.sack.compat;

import java.util.List;
import java.util.function.Supplier;

import com.misterpemodder.shulkerboxtooltip.api.PreviewContext;
import com.misterpemodder.shulkerboxtooltip.api.PreviewType;
import com.misterpemodder.shulkerboxtooltip.api.provider.PreviewProvider;
import com.misterpemodder.shulkerboxtooltip.api.renderer.PreviewRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.celestial878.creamyvanilla.sack.SackMenu;
import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the sack preview with the sack GUI texture, laying slots out like the sack menu does.
 * Compact previews are handed to ShulkerBoxTooltip's own renderer.
 * Based on the renderer in Supplementaries' ShulkerBoxTooltip integration.
 */
public class SackPreviewRenderer implements PreviewRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "textures/gui/sack_gui.png");
    private static final ResourceLocation SLOT_SPRITE =
            ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "slot");

    private final PreviewRenderer delegate = PreviewRenderer.getModRendererInstance();
    private final Supplier<Integer> unlockedSlots;
    private List<ItemStack> items = List.of();
    private PreviewType previewType = PreviewType.FULL;
    private int[] dims;

    public SackPreviewRenderer(Supplier<Integer> unlockedSlots) {
        this.unlockedSlots = unlockedSlots;
        this.initDims();
    }

    private void initDims() {
        int size = this.unlockedSlots.get();
        this.dims = SackMenu.getRatio(size);
        if (this.dims[0] > 9) {
            this.dims[0] = 9;
            this.dims[1] = (int) Math.ceil(size / 9f);
        }
    }

    @Override
    public int getHeight() {
        if (this.previewType != PreviewType.FULL) {
            return this.delegate.getHeight();
        }
        return 7 + this.dims[1] * 18 + 7;
    }

    @Override
    public int getWidth() {
        if (this.previewType != PreviewType.FULL) {
            return this.delegate.getWidth();
        }
        return 7 + this.dims[0] * 18 + 7;
    }

    @Override
    public void setPreview(PreviewContext context, PreviewProvider provider) {
        this.items = provider.getInventory(context);
        this.initDims();
        this.delegate.setPreview(context, provider);
    }

    @Override
    public void setPreviewType(PreviewType type) {
        this.previewType = type;
        this.delegate.setPreviewType(type);
    }

    @Override
    public void draw(int x, int y, GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (this.previewType != PreviewType.FULL) {
            this.delegate.draw(x, y, graphics, font, mouseX, mouseY);
            return;
        }
        RenderSystem.enableDepthTest();
        this.renderBackground(x, y, graphics);
        this.renderSlots(x, y, graphics, font, mouseX, mouseY);
        this.renderInnerTooltip(x, y, graphics, font, mouseX, mouseY);
    }

    private void renderBackground(int x, int y, GuiGraphics graphics) {
        int w = this.dims[0] * 18;
        int h = this.dims[1] * 18;
        int rEdge = 7 + w;
        int bEdge = 7 + h;

        graphics.blit(TEXTURE, x, y, 0, 0, rEdge, 7);
        graphics.blit(TEXTURE, x + rEdge, y, 7 + 9 * 18, 0, 7, 7);

        graphics.blit(TEXTURE, x, y + 7, 0, 7, rEdge, h);
        graphics.blit(TEXTURE, x + rEdge, y + 7, 7 + 9 * 18, 7, 7, h);

        graphics.blit(TEXTURE, x, y + bEdge, 0, 159, rEdge, 7);
        graphics.blit(TEXTURE, x + rEdge, y + bEdge, 7 + 9 * 18, 159, 7, 7);
    }

    private void renderSlots(int x, int y, GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int slot = 0;
        int size = this.unlockedSlots.get();
        for (int row = 0; row < this.dims[1]; row++) {
            int inRow = Math.min(this.dims[0], size);
            int xp = 7 + (this.dims[0] * 18) / 2 - (inRow * 18) / 2;
            for (int col = 0; col < inRow; col++) {
                int slotX = xp + x + col * 18;
                int slotY = 7 + y + 18 * row;
                graphics.blitSprite(SLOT_SPRITE, slotX, slotY, 18, 18);

                if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                    AbstractContainerScreen.renderSlotHighlight(graphics, slotX + 1, slotY + 1, 0);
                }
                if (slot < this.items.size()) {
                    ItemStack stack = this.items.get(slot);
                    graphics.renderFakeItem(stack, slotX + 1, slotY + 1);
                    graphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);
                }
                slot++;
            }
            size -= this.dims[0];
        }
    }

    private ItemStack getStackAt(int x, int y) {
        int slot = -1;
        if (y >= 7) {
            int row = (y - 7) / 18;
            int size = this.unlockedSlots.get() - this.dims[0] * row;
            int inRow = Math.min(this.dims[0], size);
            int xp = 7 + (this.dims[0] * 18) / 2 - (inRow * 18) / 2;
            if (x >= xp) {
                int col = (x - xp) / 18;
                if (col < inRow) {
                    slot = col + row * this.dims[0];
                }
            }
        }
        if (slot >= 0 && slot < this.items.size()) {
            return this.items.get(slot);
        }
        return ItemStack.EMPTY;
    }

    private void renderInnerTooltip(int x, int y, GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        ItemStack stack = this.getStackAt(mouseX - x, mouseY - y);
        if (!stack.isEmpty()) {
            graphics.renderTooltip(font, stack, mouseX, mouseY);
        }
    }
}

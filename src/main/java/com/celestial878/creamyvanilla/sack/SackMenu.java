package com.celestial878.creamyvanilla.sack;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for a sack with a configurable number of slots (1 to 27).
 * The slots are laid out centred in the sack GUI; see {@link #getRatio(int)}.
 */
public class SackMenu extends AbstractContainerMenu {
    private static final int[][] TARGET_RATIOS = new int[][]{
            {1, 1},
            {2, 2},
            {3, 2},
            {3, 3},
            {4, 2},
            {5, 2},
            {6, 2},
            {7, 2},
            {5, 3},
            {8, 2},
            {6, 3},
            {7, 3},
            {8, 3},
            {9, 3}
    };

    private final Container sack;
    public final int unlockedSlots;

    /** Client side: contents are filled in by the server's slot sync. */
    public SackMenu(int containerId, Inventory playerInventory, int unlockedSlots) {
        this(containerId, playerInventory, new SimpleContainer(SackBlockEntity.MAX_SIZE), unlockedSlots);
    }

    public SackMenu(int containerId, Inventory playerInventory, Container sack, int unlockedSlots) {
        super(SackRegistry.SACK_MENU.get(), containerId);
        checkContainerSize(sack, unlockedSlots);
        this.sack = sack;
        this.unlockedSlots = unlockedSlots;
        sack.startOpen(playerInventory.player);

        int[] dims = getRatio(unlockedSlots);
        if (dims[0] > 9) {
            dims[0] = 9;
            dims[1] = (int) Math.ceil(unlockedSlots / 9f);
        }

        int y = 17 + (18 * 3) / 2 - 9 * dims[1];
        int remaining = unlockedSlots;
        int index = 0;
        for (int row = 0; row < dims[1]; row++) {
            int inRow = Math.min(dims[0], remaining);
            int x = 8 + (18 * 9) / 2 - (inRow * 18) / 2;
            for (int col = 0; col < inRow; col++) {
                this.addSlot(new SackSlot(sack, index++, x + col * 18, y + 18 * row));
            }
            remaining -= dims[0];
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /** Columns and rows used to lay out the given number of slots. */
    public static int[] getRatio(int size) {
        int[] dims = {Math.min(size, 23), Math.max(size / 23, 1)};
        for (int[] candidate : TARGET_RATIOS) {
            if (candidate[0] * candidate[1] == size) {
                dims = candidate.clone();
                break;
            }
        }
        return dims;
    }

    public Container getSackContainer() {
        return this.sack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.sack.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < this.unlockedSlots) {
                if (!this.moveItemStackTo(stack, this.unlockedSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.unlockedSlots, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.sack.stopOpen(player);
    }

    /** Slot that refuses items a sack can't hold (other sacks, shulker boxes). */
    private static class SackSlot extends Slot {
        SackSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SackBlockEntity.isAllowedInSack(stack);
        }
    }
}

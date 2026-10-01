package com.celestial878.creamyvanilla.sack;


import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SackBlockEntity extends RandomizableContainerBlockEntity {
    /** Storage is always allocated at the maximum so the slot count can be changed in the config without losing items. */
    public static final int MAX_SIZE = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(MAX_SIZE, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            SackBlockEntity.this.sackSound(0.95F);
            SackBlockEntity.this.sackSetOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            SackBlockEntity.this.sackSound(0.8F);
            SackBlockEntity.this.sackSetOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof SackMenu menu
                    && menu.getSackContainer() == SackBlockEntity.this;
        }
    };

    public SackBlockEntity(BlockPos pos, BlockState state) {
        super(SackRegistry.SACK_BE.get(), pos, state);
    }

    /** Sacks (and shulker boxes) can't be nested. */
    public static boolean isAllowedInSack(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }

    public static int getUnlockedSlots() {
        return SacksConfig.SACK_SLOTS.get();
    }

    @Override
    public int getContainerSize() {
        return getUnlockedSlots();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = NonNullList.withSize(MAX_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(items.size(), MAX_SIZE); i++) {
            this.items.set(i, items.get(i));
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index < this.getContainerSize() && isAllowedInSack(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.creamyvanilla.sack");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        if (inventory.player.isSpectator()) {
            return null;
        }
        return new SackMenu(containerId, inventory, this, this.getContainerSize());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(MAX_SIZE, ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items, registries);
        }
    }

    // Item -> block: restore contents from the item's container component when placed.
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    // Block -> item: write contents into the container component (used by the loot table).
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    // Deliberately no @Override: avoids a compile error if the base signature differs.
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
    }

    @Override
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    private void sackSetOpen(BlockState state, boolean open) {
        if (this.level != null) {
            this.level.setBlock(this.getBlockPos(), state.setValue(SackBlock.OPEN, open), 3);
        }
    }

    private void sackSound(float basePitch) {
        if (this.level != null) {
            SoundEvent sound = SackRegistry.SACK_OPEN.get();
            double x = this.worldPosition.getX() + 0.5D;
            double y = this.worldPosition.getY() + 1.0D;
            double z = this.worldPosition.getZ() + 0.5D;
            this.level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 1.0F,
                    this.level.random.nextFloat() * 0.1F + basePitch);
        }
    }
}

package com.celestial878.creamyvanilla.sack;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class SackItem extends BlockItem {
    private static final Component UNKNOWN_CONTENTS = Component.translatable("container.shulkerBox.unknownContents");

    public SackItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** 0 = nothing, 1 = a non-empty sack. In between for other overencumbering items that aren't containers. */
    public static float getEncumber(ItemStack stack) {
        if (stack.is(SackRegistry.OVERENCUMBERING)) {
            ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
            if (contents != null) {
                // Can't compare against EMPTY: an emptied sack that was reloaded gets a fresh instance.
                return contents.nonEmptyStream().findAny().isPresent() ? 1 : 0;
            }
            return stack.getCount() / (float) stack.getMaxStackSize();
        }
        return 0;
    }

    private static final ResourceLocation QUARK_BACKPACK = ResourceLocation.fromNamespaceAndPath("quark", "backpack");

    public static float getEncumbermentFromInventory(ServerPlayer player) {
        float amount = 0;
        // The entity item handler also covers slots added by other mods, unlike the plain inventory.
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                amount += getEncumber(handler.getStackInSlot(i));
            }
        } else {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                amount += getEncumber(inventory.getItem(i));
            }
        }
        amount += getEncumberFromQuarkBackpack(player.getItemBySlot(EquipmentSlot.CHEST));
        return amount;
    }

    /**
     * Quark's backpack keeps its items in the container component and exposes no item handler,
     * so sacks inside it have to be counted separately. Identified by id so Quark isn't a dependency.
     */
    public static float getEncumberFromQuarkBackpack(ItemStack stack) {
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(QUARK_BACKPACK)) return 0;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return 0;
        float amount = 0;
        for (ItemStack inside : contents.nonEmptyItems()) {
            amount += getEncumber(inside);
        }
        return amount;
    }

    public static void applyOverencumbered(ServerPlayer player) {
        if (!SacksConfig.SACK_PENALTY.get()) return;
        Level level = player.level();
        if ((level.getGameTime() + player.tickCount) % 27L == 0L && !player.isCreative() && !player.isSpectator()) {
            // Keep refreshing so it follows the inventory closely.
            float amount = getEncumbermentFromInventory(player);
            int increment = SacksConfig.SACK_INCREMENT.get();
            if (amount > increment) {
                player.addEffect(new MobEffectInstance(SackRegistry.OVERENCUMBERED,
                        20 * 10, ((((int) amount) - 1) / increment) - 1, false, false, true));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack.has(DataComponents.CONTAINER_LOOT)) {
            tooltip.add(UNKNOWN_CONTENTS);
        }
        int shown = 0;
        int total = 0;
        for (ItemStack content : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) {
            total++;
            if (shown <= 4) {
                shown++;
                tooltip.add(Component.translatable("container.shulkerBox.itemCount", content.getHoverName(), content.getCount()));
            }
        }
        if (total - shown > 0) {
            tooltip.add(Component.translatable("container.shulkerBox.more", total - shown).withStyle(ChatFormatting.ITALIC));
        }
    }

    // A sack never goes into other containers (shulker boxes, bundles, other sacks).
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    // NeoForge's stack-sensitive variant. Deliberately no @Override so this still compiles if NeoForge lacks it.
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return !stack.has(DataComponents.CONTAINER);
    }

    // Spill the contents when the item is destroyed (fire, lava, cactus...) instead of deleting them.
    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            stack.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            ItemUtils.onContainerDestroyed(itemEntity, contents.nonEmptyItemsCopy());
        }
    }
}

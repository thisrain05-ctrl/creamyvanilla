package com.celestial878.creamyvanilla.keephotbar;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KeepHotbarEvents {

    private static final String TAG = "keephotbar_kept";
    private static final int HOTBAR_SLOTS = 9;
    private static final int ARMOR_SLOTS = 4;
    private static final int OFFHAND_INDEX = HOTBAR_SLOTS + ARMOR_SLOTS;
    private static final int KEPT_SLOTS = OFFHAND_INDEX + 1;

    private final Map<UUID, ItemStack[]> pending = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (keepInventory(player)) {
            pending.remove(player.getUUID());
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack[] refs = new ItemStack[KEPT_SLOTS];
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            refs[i] = inventory.items.get(i);
        }
        for (int i = 0; i < ARMOR_SLOTS; i++) {
            refs[HOTBAR_SLOTS + i] = inventory.armor.get(i);
        }
        refs[OFFHAND_INDEX] = inventory.offhand.get(0);
        pending.put(player.getUUID(), refs);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack[] refs = pending.remove(player.getUUID());
        if (refs == null) {
            return;
        }

        Collection<ItemEntity> drops = event.getDrops();
        ItemEntity[] matched = new ItemEntity[KEPT_SLOTS];

        for (int i = 0; i < KEPT_SLOTS; i++) {
            ItemStack ref = refs[i];
            if (ref == null || ref.isEmpty()) {
                continue;
            }
            for (ItemEntity entity : drops) {
                if (entity.getItem() == ref && !isUsed(matched, entity)) {
                    matched[i] = entity;
                    break;
                }
            }
        }

        for (int i = 0; i < KEPT_SLOTS; i++) {
            ItemStack ref = refs[i];
            if (matched[i] != null || ref == null || ref.isEmpty()) {
                continue;
            }
            for (ItemEntity entity : drops) {
                if (!isUsed(matched, entity) && ItemStack.matches(entity.getItem(), ref)) {
                    matched[i] = entity;
                    break;
                }
            }
        }

        HolderLookup.Provider access = player.level().registryAccess();
        ListTag list = new ListTag();
        boolean any = false;
        for (int i = 0; i < KEPT_SLOTS; i++) {
            if (matched[i] == null) {
                list.add(new CompoundTag());
                continue;
            }
            drops.remove(matched[i]);
            list.add(matched[i].getItem().saveOptional(access));
            any = true;
        }

        if (any) {
            player.getPersistentData().put(TAG, list);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag data = event.getOriginal().getPersistentData();
        if (!data.contains(TAG, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = data.getList(TAG, Tag.TAG_COMPOUND);
        data.remove(TAG);

        Player player = event.getEntity();
        HolderLookup.Provider access = player.level().registryAccess();
        Inventory inventory = player.getInventory();

        for (int i = 0; i < list.size() && i < KEPT_SLOTS; i++) {
            ItemStack stack = ItemStack.parseOptional(access, list.getCompound(i));
            if (stack.isEmpty()) {
                continue;
            }
            restore(player, inventory, i, stack);
        }
    }

    private static void restore(Player player, Inventory inventory, int index, ItemStack stack) {
        NonNullList<ItemStack> target;
        int slot;
        if (index < HOTBAR_SLOTS) {
            target = inventory.items;
            slot = index;
        } else if (index < OFFHAND_INDEX) {
            target = inventory.armor;
            slot = index - HOTBAR_SLOTS;
        } else {
            target = inventory.offhand;
            slot = 0;
        }

        if (target.get(slot).isEmpty()) {
            target.set(slot, stack);
            return;
        }
        inventory.add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static boolean isUsed(ItemEntity[] matched, ItemEntity entity) {
        for (ItemEntity other : matched) {
            if (other == entity) {
                return true;
            }
        }
        return false;
    }

    private static boolean keepInventory(ServerPlayer player) {
        return player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }
}

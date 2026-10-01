package com.celestial878.creamyvanilla.sack;

import com.celestial878.creamyvanilla.CreamyVanilla;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SackRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreamyVanilla.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreamyVanilla.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreamyVanilla.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreamyVanilla.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreamyVanilla.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CreamyVanilla.MODID);

    public static final TagKey<Item> OVERENCUMBERING = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "overencumbering"));

    // sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> SACK_BREAK = registerSound("block.sack.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SACK_PLACE = registerSound("block.sack.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> SACK_OPEN = registerSound("block.sack.open");

    public static final SoundType SACK_SOUND = new DeferredSoundType(1.0F, 1.0F,
            SACK_BREAK,
            () -> SoundEvents.WOOL_STEP,
            SACK_PLACE,
            () -> SoundEvents.WOOL_HIT,
            () -> SoundEvents.WOOL_FALL);

    // effect
    public static final DeferredHolder<MobEffect, OverencumberedEffect> OVERENCUMBERED =
            EFFECTS.register("overencumbered", OverencumberedEffect::new);

    // block, item, block entity, menu
    public static final DeferredBlock<SackBlock> SACK = BLOCKS.register("sack",
            () -> new SackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
                    .mapColor(MapColor.WOOD)
                    .pushReaction(PushReaction.DESTROY)
                    .strength(0.8F)
                    .sound(SACK_SOUND)
                    .noOcclusion()));

    public static final DeferredItem<SackItem> SACK_ITEM = ITEMS.registerItem("sack",
            properties -> new SackItem(SACK.get(), properties.stacksTo(1)
                    .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)));

    public static final Supplier<BlockEntityType<SackBlockEntity>> SACK_BE = BLOCK_ENTITIES.register("sack",
            () -> BlockEntityType.Builder.of(SackBlockEntity::new, SACK.get()).build(null));

    public static final Supplier<MenuType<SackMenu>> SACK_MENU = MENUS.register("sack",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new SackMenu(id, inventory, buf.readInt())));

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, name)));
    }
}

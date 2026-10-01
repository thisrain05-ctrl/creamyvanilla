package com.celestial878.creamyvanilla.harvesting;

import com.celestial878.creamyvanilla.CreamyVanilla;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Collections;
import java.util.List;

/**
 * Right-click harvesting for hoes carrying the Harvesting enchantment.
 * The logic mirrors Harvest with Ease: a mature crop is harvested, drops its loot minus one seed,
 * and its age is reset instead of the block being broken.
 * <p>
 * Credit: the harvesting action is based on code from Harvest with Ease by Crystal Nest
 * (https://modrinth.com/mod/harvest-with-ease). The enchantment itself is original to Creamy Vanilla.
 */
@EventBusSubscriber(modid = CreamyVanilla.MODID)
public final class HarvestingHandler {
    /** The data-driven enchantment, see data/creamyvanilla/enchantment/harvesting.json. */
    public static final ResourceKey<Enchantment> HARVESTING =
        ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "harvesting"));

    /** Extra blocks (from any mod) that should be harvestable. They still need an integer "age" property. */
    public static final TagKey<Block> EXTRA_CROPS =
        TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "extra_crops"));

    /** Blocks that must never be harvested by the enchantment. */
    public static final TagKey<Block> BLACKLIST =
        TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "blacklist"));

    /**
     * Crops that stack vertically but where every block is its own crop (Farmer's Delight tomatoes and
     * tomatoes on rope). Harvesting one must not touch the blocks above or below it.
     */
    public static final TagKey<Block> SEPARATE_STACKED =
        TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CreamyVanilla.MODID, "separate_stacked"));

    /** Farmer's Delight plays its own sound when picking tomatoes; used when that mod is present. */
    private static final ResourceLocation FD_TOMATO_PICK_SOUND =
        ResourceLocation.fromNamespaceAndPath("farmersdelight", "block.tomatoes.pick_tomatoes");

    private HarvestingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.isSpectator() || player.isCrouching()) {
            return;
        }
        if (event.getUseBlock() == TriState.FALSE || event.getUseItem() == TriState.FALSE) {
            return;
        }

        InteractionHand hand = event.getHand();
        ItemStack tool = player.getItemInHand(hand);
        Level level = event.getLevel();
        if (!hasHarvesting(level, tool)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!isCrop(state)) {
            return;
        }

        IntegerProperty age = getAge(state);
        if (age == null || !isMature(state, age)) {
            return;
        }

        // Consume the click on both sides so the hand swings and nothing else (e.g. placing seeds) happens.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            harvest(serverLevel, serverPlayer, hand, tool, state, pos, age);
        }
    }

    private static void harvest(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack tool,
                                BlockState state, BlockPos pos, IntegerProperty age) {
        Block cropBlock = state.getBlock();
        BlockPos basePos = getBasePos(level, state, pos);
        BlockState baseState = level.getBlockState(basePos);

        // Normal loot for the crop, minus one seed so the crop can be "replanted" by resetting its age.
        List<ItemStack> drops = Block.getDrops(baseState, level, basePos,
            baseState.hasBlockEntity() ? level.getBlockEntity(basePos) : null, player, tool);
        ItemStack seed = cropBlock.getCloneItemStack(level, basePos, baseState);
        for (ItemStack stack : drops) {
            if (stack.is(seed.getItem())) {
                stack.shrink(1);
                break;
            }
        }
        for (ItemStack stack : drops) {
            Block.popResource(level, pos, stack);
        }

        if (!player.isCreative()) {
            tool.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        if (cropBlock == Blocks.PITCHER_CROP) {
            // Pitcher crops don't drop their pod, so take one from the inventory to replant, otherwise break it.
            int slot = player.getInventory().findSlotMatchingItem(seed);
            if (slot >= 0 || player.isCreative()) {
                level.setBlockAndUpdate(basePos, baseState.setValue(age, 0));
                if (!player.isCreative()) {
                    player.getInventory().getItem(slot).shrink(1);
                }
            } else {
                level.destroyBlock(basePos, false, player);
            }
        } else {
            level.setBlockAndUpdate(basePos, baseState.setValue(age, 0));
        }

        // Tall crops: remove the upper blocks (their loot was already dropped from the base above).
        if (!isStackedButSeparate(state) && level.getBlockState(basePos).is(BlockTags.CROPS)
            && level.getBlockState(basePos.above()).is(cropBlock)) {
            level.destroyBlock(basePos.above(), false, player);
        }

        SoundEvent fdSound = isStackedButSeparate(state)
            ? BuiltInRegistries.SOUND_EVENT.getOptional(FD_TOMATO_PICK_SOUND).orElse(null) : null;
        if (fdSound != null) {
            level.playSound(null, pos, fdSound, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
        } else {
            SoundType sound = state.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
        }
        player.causeFoodExhaustion(0.005F);
    }

    private static boolean hasHarvesting(Level level, ItemStack stack) {
        if (stack.isEmpty() || !stack.isEnchanted()) {
            return false;
        }
        return level.registryAccess().lookup(Registries.ENCHANTMENT)
            .flatMap(lookup -> lookup.get(HARVESTING))
            .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0)
            .orElse(false);
    }

    private static boolean isCrop(BlockState state) {
        Block block = state.getBlock();
        if (state.is(BLACKLIST) || block instanceof TorchflowerCropBlock) {
            return false;
        }
        return block instanceof CropBlock || block instanceof NetherWartBlock
            || block instanceof CocoaBlock || block instanceof PitcherCropBlock
            || state.is(EXTRA_CROPS);
    }

    private static boolean isStackedButSeparate(BlockState state) {
        return state.is(SEPARATE_STACKED);
    }

    private static IntegerProperty getAge(BlockState state) {
        for (var property : state.getProperties()) {
            if ("age".equals(property.getName()) && property instanceof IntegerProperty integerProperty) {
                return integerProperty;
            }
        }
        return null;
    }

    private static boolean isMature(BlockState state, IntegerProperty age) {
        return state.getValue(age) >= Collections.max(age.getPossibleValues());
    }

    private static BlockPos getBasePos(Level level, BlockState state, BlockPos pos) {
        Block crop = state.getBlock();
        BlockPos basePos = pos;
        if (isStackedButSeparate(state)) {
            return basePos;
        }
        while (level.getBlockState(basePos.below()).is(crop)) {
            basePos = basePos.below();
        }
        return basePos;
    }
}

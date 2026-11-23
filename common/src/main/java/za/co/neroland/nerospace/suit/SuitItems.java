package za.co.neroland.nerospace.suit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
//? if >=1.21.2 {
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
//?}
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Pathfinder pieces, plating, weaves and modules. Registered through the same item provider as {@code ModItems},
 * appended when {@link #init()} runs (after the legacy suit ids, which stay registered).
 */
public final class SuitItems {

    private static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, NerospaceCommon.MOD_ID);

    //? if >=1.21.2 {
    private static final Map<ArmorType, Integer> DEFENSE =
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 7, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3);

    private static final ArmorMaterial MATERIAL = new ArmorMaterial(28, DEFENSE, 12, SoundEvents.ARMOR_EQUIP_IRON,
            1.5F, 0.0F, TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/nerosteel")),
            net.minecraft.resources.ResourceKey.create(EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "suit")));
    //?} else {
    /*private static final RegistrationProvider<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS =
            RegistrationProvider.get(Registries.ARMOR_MATERIAL, NerospaceCommon.MOD_ID);
    private static final java.util.Map<net.minecraft.world.item.ArmorItem.Type, Integer> DEFENSE = java.util.Map.of(
            net.minecraft.world.item.ArmorItem.Type.HELMET, 3, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 7,
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 6, net.minecraft.world.item.ArmorItem.Type.BOOTS, 3);
    private static final RegistryEntry<net.minecraft.world.item.ArmorMaterial> LEGACY_MATERIAL = ARMOR_MATERIALS.register(
            "suit", key -> new net.minecraft.world.item.ArmorMaterial(DEFENSE, 12, SoundEvents.ARMOR_EQUIP_IRON,
                    () -> net.minecraft.world.item.crafting.Ingredient.of(TagKey.create(Registries.ITEM,
                            Identifier.fromNamespaceAndPath("c", "ingots/nerosteel"))),
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(
                            Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "suit"), "", true)),
                    1.5F, 0.0F));
    *///?}

    public static final RegistryEntry<Item> HELMET = piece("suit_helmet", SuitPiece.HELMET);
    public static final RegistryEntry<Item> CHESTPLATE = piece("suit_chestplate", SuitPiece.CHEST);
    public static final RegistryEntry<Item> LEGGINGS = piece("suit_leggings", SuitPiece.LEGS);
    public static final RegistryEntry<Item> BOOTS = piece("suit_boots", SuitPiece.BOOTS);

    public static final RegistryEntry<Item> PLATING_MK2 = item("frame_plating_mk2");
    public static final RegistryEntry<Item> PLATING_MK3 = item("frame_plating_mk3");
    public static final RegistryEntry<Item> PLATING_MK4 = item("frame_plating_mk4");
    public static final RegistryEntry<Item> THERMAL_WEAVE = item("thermal_weave");
    public static final RegistryEntry<Item> CRYO_WEAVE = item("cryo_weave");
    public static final RegistryEntry<Item> ADAPTIVE_WEAVE = item("adaptive_weave");
    public static final RegistryEntry<Item> LIFE_SUPPORT_TANK = item("life_support_tank");
    public static final RegistryEntry<Item> O2_RECLAIMER = item("o2_reclaimer");
    public static final RegistryEntry<Item> POWER_CELL = item("power_cell");
    public static final RegistryEntry<Item> SOLAR_SKIN = item("solar_skin");
    public static final RegistryEntry<Item> KINETIC_SHIELD = item("kinetic_shield");
    public static final RegistryEntry<Item> JUMP_JET = item("jump_jet");
    public static final RegistryEntry<Item> FLIGHT_HARNESS = item("flight_harness");
    public static final RegistryEntry<Item> NIGHT_VISOR = item("night_visor");
    public static final RegistryEntry<Item> SURVEY_SCANNER = item("survey_scanner");
    public static final RegistryEntry<Item> SPORE_FILTER = item("spore_filter");
    public static final RegistryEntry<Item> STRIDE_SERVOS = item("stride_servos");
    public static final RegistryEntry<Item> LEAP_SERVOS = item("leap_servos");
    public static final RegistryEntry<Item> SHOCK_ABSORBERS = item("shock_absorbers");
    public static final RegistryEntry<Item> GLIDE_FINS = item("glide_fins");
    /** Inert. Not in the creative tab and not craftable. */
    public static final RegistryEntry<Item> HARDPOINT = item("hardpoint");
    public static final RegistryEntry<Item> FABRICATOR = blockItem("suit_fabricator",
            za.co.neroland.nerospace.registry.ModBlocks.SUIT_FABRICATOR);
    public static final RegistryEntry<Item> DOCK = blockItem("suit_dock",
            za.co.neroland.nerospace.registry.ModBlocks.SUIT_DOCK);

    private SuitItems() {
    }

    public static void init() {
        SuitEnergy.init();
    }

    public static List<ItemLike> creative() {
        return List.of(HELMET.get(), CHESTPLATE.get(), LEGGINGS.get(), BOOTS.get(), PLATING_MK2.get(),
                PLATING_MK3.get(), PLATING_MK4.get(), THERMAL_WEAVE.get(), CRYO_WEAVE.get(), ADAPTIVE_WEAVE.get(),
                LIFE_SUPPORT_TANK.get(), O2_RECLAIMER.get(), POWER_CELL.get(), SOLAR_SKIN.get(), KINETIC_SHIELD.get(),
                JUMP_JET.get(), FLIGHT_HARNESS.get(), NIGHT_VISOR.get(), SURVEY_SCANNER.get(), SPORE_FILTER.get(),
                STRIDE_SERVOS.get(), LEAP_SERVOS.get(), SHOCK_ABSORBERS.get(), GLIDE_FINS.get(),
                FABRICATOR.get(), DOCK.get());
    }

    public static Item pieceItem(SuitPiece piece) {
        return switch (piece) {
            case HELMET -> HELMET.get();
            case CHEST -> CHESTPLATE.get();
            case LEGS -> LEGGINGS.get();
            case BOOTS -> BOOTS.get();
        };
    }

    private static RegistryEntry<Item> piece(String name, SuitPiece piece) {
        //? if >=1.21.5 {
        ArmorType type = switch (piece) {
            case HELMET -> ArmorType.HELMET;
            case CHEST -> ArmorType.CHESTPLATE;
            case LEGS -> ArmorType.LEGGINGS;
            case BOOTS -> ArmorType.BOOTS;
        };
        return ITEMS.register(name, key -> new SuitPieceItem(
                new Item.Properties().setId(key).humanoidArmor(MATERIAL, type).durability(SuitDurability.ITEM_MAX),
                piece));
        //?} else if >=1.21.2 {
        /*ArmorType type = switch (piece) {
            case HELMET -> ArmorType.HELMET;
            case CHEST -> ArmorType.CHESTPLATE;
            case LEGS -> ArmorType.LEGGINGS;
            case BOOTS -> ArmorType.BOOTS;
        };
        return ITEMS.register(name, key -> new SuitPieceItem(MATERIAL, type,
                new Item.Properties().setId(key).durability(SuitDurability.ITEM_MAX), piece));
        *///?} else {
        /*net.minecraft.world.item.ArmorItem.Type type = switch (piece) {
            case HELMET -> net.minecraft.world.item.ArmorItem.Type.HELMET;
            case CHEST -> net.minecraft.world.item.ArmorItem.Type.CHESTPLATE;
            case LEGS -> net.minecraft.world.item.ArmorItem.Type.LEGGINGS;
            case BOOTS -> net.minecraft.world.item.ArmorItem.Type.BOOTS;
        };
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.ArmorMaterial> materialKey =
                net.minecraft.resources.ResourceKey.create(Registries.ARMOR_MATERIAL, LEGACY_MATERIAL.id());
        return ITEMS.register(name, key -> new SuitPieceItem(
                ((net.minecraft.core.MappedRegistry<net.minecraft.world.item.ArmorMaterial>)
                        net.minecraft.core.registries.BuiltInRegistries.ARMOR_MATERIAL)
                        .createRegistrationLookup().getOrThrow(materialKey),
                type, new Item.Properties().durability(type.getDurability(28)), piece));
        *///?}
    }

    private static RegistryEntry<Item> item(String name) {
        return ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }

    private static RegistryEntry<Item> blockItem(String name,
            za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, key -> new net.minecraft.world.item.BlockItem(block.get(),
                new Item.Properties().setId(key)));
    }

    /** Kept so a future caller can see the inert id is registered. */
    public static List<Item> hidden() {
        List<Item> hidden = new ArrayList<>();
        hidden.add(HARDPOINT.get());
        return hidden;
    }
}

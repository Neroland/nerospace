package za.co.neroland.nerospace.datagen;

import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Entity loot for the terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5) — the mod's first entity
 * loot tables: each species drops its thematic item (no cooked variants in slice 1, design §13).
 * Only the livestock are "known" here; the predators deliberately keep no loot tables (unchanged).
 */
public class ModEntityLootSubProvider extends EntityLootSubProvider {

    public ModEntityLootSubProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    public void generate() {
        this.add(ModEntities.MEADOW_LOPER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.LOPER_HAUNCH.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))));

        this.add(ModEntities.EMBER_STRUTTER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.STRUTTER_DRUMSTICK.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))));

        this.add(ModEntities.WOOLLY_DRIFT.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.DRIFT_FLEECE.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(ModEntities.MEADOW_LOPER.get(), ModEntities.EMBER_STRUTTER.get(),
                ModEntities.WOOLLY_DRIFT.get());
    }
}

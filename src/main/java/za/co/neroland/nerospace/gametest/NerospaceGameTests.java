package za.co.neroland.nerospace.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.gas.GasCapability;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.LaunchPadMultiblock;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.world.GreenxertzAtmosphere;
import za.co.neroland.nerospace.world.OxygenField;

/**
 * The mod's gametests (26.1 data-driven framework, registered in code via
 * {@link RegisterGameTestsEvent}). Run with {@code gradlew runGameTestServer}; all tests share an
 * empty structure template ({@code nerospace:gametest/empty}, 7x12x7) and build their fixtures in
 * code. Covers the suit-and-station integration batch: launch-pad multiblock gating (deploy + Tier 3
 * Station Wall ring), the pad's item-capability proxy into a deployed rocket's fuel intake, and the
 * suit's airlock refill from a Gas Tank.
 *
 * <p>SYNC-SAFETY: the {@code TEST_INSTANCE} registry is a datapack registry that vanilla SYNCS to
 * joining clients, so every registered instance must round-trip through its
 * {@link GameTestInstance#codec()} — returning some other type's codec breaks world join with a
 * {@code ClassCastException} during {@code RegistrySynchronization}. {@link CodeTest} therefore has
 * its own {@code nerospace:code} type (registered into {@code TEST_INSTANCE_TYPE}) whose codec
 * stores the function by NAME and resolves it from the static {@link #FUNCTIONS} map on decode.</p>
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class NerospaceGameTests {

    /** Empty 7x12x7 test arena (generated NBT under {@code data/nerospace/structure/gametest/}). */
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.fromNamespaceAndPath(Nerospace.MODID, "gametest/empty");

    /** Test functions by name — the codec's decode side resolves against this map. */
    private static final Map<String, Consumer<GameTestHelper>> FUNCTIONS = buildFunctions();

    /** Max ticks per test (only the intake test waits on entity ticks; one budget fits all). */
    private static final int MAX_TICKS = 200;

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> INSTANCE_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, Nerospace.MODID);

    /** The {@code nerospace:code} instance type — lets {@link CodeTest} serialize legitimately. */
    public static final Supplier<MapCodec<CodeTest>> CODE_TEST_TYPE =
            INSTANCE_TYPES.register("code", () -> CodeTest.CODEC);

    private NerospaceGameTests() {
    }

    /** Called from the mod constructor (like every other DeferredRegister). */
    public static void register(IEventBus modEventBus) {
        INSTANCE_TYPES.register(modEventBus);
    }

    private static Map<String, Consumer<GameTestHelper>> buildFunctions() {
        Map<String, Consumer<GameTestHelper>> functions = new LinkedHashMap<>();
        functions.put("deploy_requires_full_pad", NerospaceGameTests::testDeployRequiresFullPad);
        functions.put("tier3_requires_station_wall_ring", NerospaceGameTests::testTier3RequiresRing);
        functions.put("pad_proxies_rocket_intake", NerospaceGameTests::testPadProxiesRocketIntake);
        functions.put("suit_airlock_refill", NerospaceGameTests::testSuitAirlockRefill);
        functions.put("suit_tier_detection", NerospaceGameTests::testSuitTierDetection);
        functions.put("test_instance_sync_roundtrip", NerospaceGameTests::testInstanceSyncRoundtrip);
        // Machine-automation audit: items inserted through Capabilities.Item.BLOCK must be visible
        // to (and consumed by) the machine itself — guards against the StacksResourceHandler
        // copies-the-list gotcha that silently severed handlers from machine Containers.
        functions.put("combustion_generator_cap_feed", NerospaceGameTests::testCombustionGeneratorCapFeed);
        functions.put("passive_generator_cap_feed", NerospaceGameTests::testPassiveGeneratorCapFeed);
        functions.put("grinder_cap_feed_and_extract", NerospaceGameTests::testGrinderCapFeedAndExtract);
        functions.put("item_store_cap_roundtrip", NerospaceGameTests::testItemStoreCapRoundtrip);
        functions.put("terraformer_cap_upgrade_feed", NerospaceGameTests::testTerraformerCapUpgradeFeed);
        // Deeper terraforming (DEEPER_TERRAFORM_DESIGN.md): stage engine + water cycle.
        functions.put("hydration_module_feeds_terraformer", NerospaceGameTests::testHydrationModuleFeedsTerraformer);
        functions.put("terraform_water_table_fill", NerospaceGameTests::testTerraformWaterTableFill);
        functions.put("terraform_stage_progression", NerospaceGameTests::testTerraformStageProgression);
        functions.put("terraform_legacy_save_compat", NerospaceGameTests::testTerraformLegacySaveCompat);
        functions.put("terraform_creature_breeding", NerospaceGameTests::testTerraformCreatureBreeding);
        functions.put("terraform_monitor_readout", NerospaceGameTests::testTerraformMonitorReadout);
        // Art overhaul (ART_OVERHAUL_DESIGN.md §3): machines face the placer.
        functions.put("machine_facing_placement", NerospaceGameTests::testMachineFacingPlacement);
        // Oxygen field boundary classification: doors/trapdoors seal when closed and flow when
        // open, glass seals (full collision cube fallback), panes/fences hold-but-leak.
        functions.put("oxygen_sealing_boundaries", NerospaceGameTests::testOxygenSealingBoundaries);
        // Heavy Launch Complex (LAUNCH_PAD_DESIGN.md).
        functions.put("heavy_complex_detection", NerospaceGameTests::testHeavyComplexDetection);
        functions.put("tier3_deploys_on_heavy_complex", NerospaceGameTests::testTier3DeploysOnHeavyComplex);
        functions.put("fuel_pump_rate_by_footprint", NerospaceGameTests::testFuelPumpRateByFootprint);
        functions.put("single_centered_rocket_per_pad", NerospaceGameTests::testSingleCenteredRocketPerPad);
        functions.put("pad_break_grounds_rocket", NerospaceGameTests::testPadBreakGroundsRocket);
        // Hazard suit variants (SUIT_HAZARD_DESIGN.md).
        functions.put("hazard_shield_detection", NerospaceGameTests::testHazardShieldDetection);
        functions.put("hazard_drain_multiplier", NerospaceGameTests::testHazardDrainMultiplier);
        // Glacira / Tier 4 (NEW_DESTINATION_DESIGN.md).
        functions.put("tier4_requires_heavy_complex", NerospaceGameTests::testTier4RequiresHeavyComplex);
        functions.put("tier_destinations_cumulative", NerospaceGameTests::testTierDestinationsCumulative);
        functions.put("glacira_dimension_loads", NerospaceGameTests::testGlaciraDimensionLoads);
        // Multiple stations (MULTI_STATION_DESIGN.md).
        functions.put("station_registry_roundtrip", NerospaceGameTests::testStationRegistryRoundtrip);
        functions.put("station_core_break_unregisters", NerospaceGameTests::testStationCoreBreakUnregisters);
        functions.put("rocket_station_selection", NerospaceGameTests::testRocketStationSelection);
        // Star Guide (progression block).
        functions.put("star_guide_install_return", NerospaceGameTests::testStarGuideInstallReturn);
        functions.put("star_guide_break_drops_book", NerospaceGameTests::testStarGuideBreakDropsBook);
        functions.put("star_guide_advancements_resolve", NerospaceGameTests::testStarGuideAdvancementsResolve);
        functions.put("star_guide_progress_and_seen", NerospaceGameTests::testStarGuideProgressAndSeen);
        return functions;
    }

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Nerospace.MODID, "default"),
                new TestEnvironmentDefinition.AllOf());

        FUNCTIONS.forEach((name, function) -> {
            TestData<Holder<TestEnvironmentDefinition<?>>> data =
                    new TestData<>(env, EMPTY_STRUCTURE, MAX_TICKS, 1, true);
            event.registerTest(
                    Identifier.fromNamespaceAndPath(Nerospace.MODID, "tests/" + name),
                    d -> new CodeTest(d, name),
                    data);
        });
    }

    // --- Fixtures -------------------------------------------------------------

    /** Lays a complete 3x3 launch pad with min-corner (1,1,1) in relative coords; returns centre. */
    private static BlockPos buildFullPad(GameTestHelper helper) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                helper.setBlock(new BlockPos(1 + dx, 1, 1 + dz), ModBlocks.ROCKET_LAUNCH_PAD.get());
            }
        }
        return new BlockPos(2, 1, 2);
    }

    /** Adds the 16-block 5x5 Station Wall border around the 3x3 pad laid by {@link #buildFullPad}. */
    private static void buildStationWallRing(GameTestHelper helper) {
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                if (dx == 0 || dx == 4 || dz == 0 || dz == 4) {
                    helper.setBlock(new BlockPos(dx, 1, dz), ModBlocks.STATION_WALL.get());
                }
            }
        }
    }

    /** Lays a complete 5x5 launch pad with min-corner (1,1,1) in relative coords; returns centre. */
    private static BlockPos buildHeavyPad(GameTestHelper helper) {
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                helper.setBlock(new BlockPos(1 + dx, 1, 1 + dz), ModBlocks.ROCKET_LAUNCH_PAD.get());
            }
        }
        return new BlockPos(3, 1, 3);
    }

    /** Uses {@code stack} on the relative position {@code pos} as a survival mock player. */
    private static void useItemOn(GameTestHelper helper, ItemStack stack, BlockPos pos) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void equipSuit(Player player, boolean tier2) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_HELMET.get() : ModItems.OXYGEN_SUIT_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get() : ModItems.OXYGEN_SUIT_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_LEGGINGS.get() : ModItems.OXYGEN_SUIT_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_BOOTS.get() : ModItems.OXYGEN_SUIT_BOOTS.get()));
    }

    /** Equips a full hazard-variant set: true = Thermal (heat), false = Cryo (cold). */
    private static void equipVariant(Player player, boolean heat) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                heat ? ModItems.OXYGEN_SUIT_HEAT_HELMET.get() : ModItems.OXYGEN_SUIT_COLD_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(
                heat ? ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get() : ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(
                heat ? ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get() : ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(
                heat ? ModItems.OXYGEN_SUIT_HEAT_BOOTS.get() : ModItems.OXYGEN_SUIT_COLD_BOOTS.get()));
    }

    // --- Tests ----------------------------------------------------------------

    /** An incomplete pad rejects deployment; completing the 3x3 accepts it. */
    private static void testDeployRequiresFullPad(GameTestHelper helper) {
        // A single pad block is not enough.
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.ROCKET_LAUNCH_PAD.get());
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_1.get()), new BlockPos(2, 1, 2));
        helper.assertEntityNotPresent(ModEntities.ROCKET.get());

        // The complete 3x3 deploys.
        BlockPos centre = buildFullPad(helper);
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_1.get()), centre);
        helper.assertEntityPresent(ModEntities.ROCKET.get());
        helper.succeed();
    }

    /** A Tier 3 rocket needs the 3x3 pad ringed with Station Wall. */
    private static void testTier3RequiresRing(GameTestHelper helper) {
        BlockPos centre = buildFullPad(helper);
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_3.get()), centre);
        helper.assertEntityNotPresent(ModEntities.ROCKET.get());
        helper.assertFalse(
                LaunchPadMultiblock.hasStationWallRing(helper.getLevel(),
                        LaunchPadMultiblock.connectedPads(helper.getLevel(), helper.absolutePos(centre))),
                "ring must not be detected before the Station Wall border exists");

        buildStationWallRing(helper);
        helper.assertTrue(
                LaunchPadMultiblock.hasStationWallRing(helper.getLevel(),
                        LaunchPadMultiblock.connectedPads(helper.getLevel(), helper.absolutePos(centre))),
                "ring must be detected once the Station Wall border is complete");
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_3.get()), centre);
        helper.assertEntityPresent(ModEntities.ROCKET.get());
        helper.succeed();
    }

    /** A fuel canister inserted through the pad's item capability ends up as rocket fuel. */
    private static void testPadProxiesRocketIntake(GameTestHelper helper) {
        BlockPos centre = buildFullPad(helper);
        BlockPos absCentre = helper.absolutePos(centre);
        RocketEntity rocket = new RocketEntity(helper.getLevel(),
                absCentre.getX() + 0.5D, absCentre.getY() + 1.0D, absCentre.getZ() + 0.5D,
                RocketTier.TIER_1);
        helper.getLevel().addFreshEntity(rocket);

        // The pad block proxies the rocket's intake slot into the block-capability graph.
        ResourceHandler<ItemResource> viaPad = Capabilities.Item.BLOCK.getCapability(
                helper.getLevel(), absCentre, null, null, Direction.UP);
        if (viaPad == null) {
            helper.fail("launch pad must expose the deployed rocket's intake");
            return;
        }
        int inserted;
        try (Transaction tx = Transaction.openRoot()) {
            inserted = viaPad.insert(0, ItemResource.of(ModItems.ROCKET_FUEL_CANISTER.get()), 1, tx);
            tx.commit();
        }
        helper.assertTrue(inserted == 1, "the intake must accept one fuel canister via the pad");
        helper.assertTrue(!rocket.getFuelInput().getItem(0).isEmpty(),
                "the committed insert must land in the rocket's intake container");

        // The rocket drains the canister into its tank on its server tick.
        helper.succeedWhen(() -> helper.assertTrue(
                rocket.getFuel() >= RocketEntity.CANISTER_MB,
                "rocket must consume the piped-in canister into fuel (fuel=" + rocket.getFuel()
                        + ", intake=" + rocket.getFuelInput().getItem(0) + ")"));
    }

    /** A worn suit refills from an oxygen-holding Gas Tank nearby, draining the gas. */
    private static void testSuitAirlockRefill(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(2, 1, 2);
        helper.setBlock(tankPos, ModBlocks.GAS_TANK.get());
        ResourceHandler<GasResource> tank = GasCapability.BLOCK.getCapability(
                helper.getLevel(), helper.absolutePos(tankPos), null, null, null);
        if (tank == null) {
            helper.fail("gas tank must expose the gas capability");
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int filled = tank.insert(0, GasResource.OXYGEN, 4_000, tx);
            helper.assertTrue(filled == 4_000, "test setup: tank accepts oxygen");
            tx.commit();
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equipSuit(player, false);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));

        int before = (int) tank.getAmountAsLong(0);
        int restored = GreenxertzAtmosphere.airlockRefill(helper.getLevel(), player,
                GreenxertzAtmosphere.SuitTier.TIER_1, 100);
        int after = (int) tank.getAmountAsLong(0);

        helper.assertTrue(restored > 0, "suit must refill next to an oxygen-holding gas tank");
        helper.assertTrue(after < before, "airlock refill must drain oxygen from the tank");

        // Out of range: no refill (and no gas drawn).
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 9, 2))));
        int farRestored = GreenxertzAtmosphere.airlockRefill(helper.getLevel(), player,
                GreenxertzAtmosphere.SuitTier.TIER_1, 100);
        helper.assertTrue(farRestored == 0, "suit must not refill outside the airlock radius");
        helper.succeed();
    }

    /** Full sets detect their tier; a mixed set counts as Tier 1; a partial set is no suit. */
    private static void testSuitTierDetection(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        equipSuit(player, false);
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_1,
                "full Tier 1 set must detect as TIER_1");

        equipSuit(player, true);
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_2,
                "full Tier 2 set must detect as TIER_2");

        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.OXYGEN_SUIT_BOOTS.get()));
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_1,
                "mixed T1/T2 set must count as TIER_1");

        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.NONE,
                "a partial set is not life support");
        helper.succeed();
    }

    // --- Machine-automation audit ----------------------------------------------

    /** Inserts {@code count} of {@code item} through the block's item capability; returns inserted. */
    private static int insertViaCap(GameTestHelper helper, BlockPos pos, Direction side,
            net.minecraft.world.item.Item item, int count) {
        ResourceHandler<ItemResource> handler = Capabilities.Item.BLOCK.getCapability(
                helper.getLevel(), helper.absolutePos(pos), null, null, side);
        if (handler == null) {
            helper.fail("block must expose an item capability at " + pos);
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(0, ItemResource.of(item), count, tx);
            tx.commit();
            return inserted;
        }
    }

    /** Pushes energy into a machine's energy capability (multiple calls to beat per-call caps). */
    private static void energiseViaCap(GameTestHelper helper, BlockPos pos, int calls, int perCall) {
        net.neoforged.neoforge.transfer.energy.EnergyHandler energy =
                Capabilities.Energy.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(pos),
                        null, null, Direction.NORTH);
        if (energy == null) {
            helper.fail("machine must expose an energy capability at " + pos);
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            for (int i = 0; i < calls; i++) {
                energy.insert(perCall, tx);
            }
            tx.commit();
        }
    }

    /** Coal inserted through the capability must actually burn (energy rises, slot empties). */
    private static void testCombustionGeneratorCapFeed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.COMBUSTION_GENERATOR.get());
        int inserted = insertViaCap(helper, pos, Direction.UP, net.minecraft.world.item.Items.COAL, 1);
        helper.assertTrue(inserted == 1, "the fuel slot must accept coal via the capability");

        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                instanceof za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity generator)) {
            helper.fail("expected a CombustionGeneratorBlockEntity");
            return;
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(generator.getEnergyHandler().getAmountAsLong() > 0,
                    "capability-fed coal must burn into energy");
            helper.assertTrue(generator.getItem(0).isEmpty(),
                    "the burned coal must vanish from the machine's own slot");
        });
    }

    /** A nerosium core inserted through the capability must power the passive generator. */
    private static void testPassiveGeneratorCapFeed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.PASSIVE_GENERATOR.get());
        int inserted = insertViaCap(helper, pos, Direction.UP, ModItems.NEROSIUM_DUST.get(), 1);
        helper.assertTrue(inserted == 1, "the core slot must accept a nerosium core via the capability");

        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                instanceof za.co.neroland.nerospace.machine.PassiveGeneratorBlockEntity generator)) {
            helper.fail("expected a PassiveGeneratorBlockEntity");
            return;
        }
        helper.succeedWhen(() -> helper.assertTrue(generator.getEnergyHandler().getAmountAsLong() > 0,
                "capability-fed core must trickle energy"));
    }

    /** Grindable input piped in from the top must be ground; dust must be extractable below. */
    private static void testGrinderCapFeedAndExtract(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.NEROSIUM_GRINDER.get());
        energiseViaCap(helper, pos, 10, 500); // 5,000 FE >= 100 ticks * 30 FE
        int inserted = insertViaCap(helper, pos, Direction.UP, ModItems.RAW_NEROSIUM.get(), 1);
        helper.assertTrue(inserted == 1, "the input slot must accept raw nerosium via the capability");

        helper.succeedWhen(() -> {
            ResourceHandler<ItemResource> below = Capabilities.Item.BLOCK.getCapability(
                    helper.getLevel(), helper.absolutePos(pos), null, null, Direction.DOWN);
            if (below == null) {
                helper.fail("grinder must expose the output side");
                return;
            }
            try (Transaction tx = Transaction.openRoot()) {
                ResourceStack<ItemResource> got = ResourceHandlerUtil.extractFirst(
                        below, r -> true, 64, tx);
                helper.assertTrue(got != null && got.amount() >= 2
                                && got.resource().toStack(1).is(ModItems.NEROSIUM_DUST.get()),
                        "capability-fed input must grind into extractable dust");
                // Aborted: leave the dust for the assertion to re-find (test ends on success).
            }
        });
    }

    /** Items piped into the Item Store must be visible in the GUI inventory and extractable again. */
    private static void testItemStoreCapRoundtrip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.ITEM_STORE.get());
        int inserted = insertViaCap(helper, pos, Direction.UP, ModItems.NEROSTEEL_INGOT.get(), 3);
        helper.assertTrue(inserted == 3, "the item store must accept items via the capability");

        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                instanceof za.co.neroland.nerospace.storage.ItemStoreBlockEntity store)) {
            helper.fail("expected an ItemStoreBlockEntity");
            return;
        }
        helper.assertTrue(store.getItem(0).is(ModItems.NEROSTEEL_INGOT.get())
                        && store.getItem(0).getCount() == 3,
                "capability-inserted items must be visible in the store's own inventory");

        ResourceHandler<ItemResource> handler = Capabilities.Item.BLOCK.getCapability(
                helper.getLevel(), helper.absolutePos(pos), null, null, Direction.NORTH);
        if (handler == null) {
            helper.fail("item store must expose an item capability");
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            ResourceStack<ItemResource> got = ResourceHandlerUtil.extractFirst(handler, r -> true, 64, tx);
            tx.commit();
            helper.assertTrue(got != null && got.amount() == 3,
                    "the same items must be extractable back out via the capability");
        }
        helper.assertTrue(store.getItem(0).isEmpty(),
                "extraction must empty the store's own inventory");
        helper.succeed();
    }

    /** Upgrade items piped into the Terraformer must land in its own upgrade slot. */
    private static void testTerraformerCapUpgradeFeed(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.TERRAFORMER.get());
        int inserted = insertViaCap(helper, pos, Direction.UP, ModItems.NEROSTEEL_INGOT.get(), 1);
        helper.assertTrue(inserted == 1, "the upgrade slot must accept a nerosteel ingot via the capability");

        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                instanceof za.co.neroland.nerospace.machine.TerraformerBlockEntity terraformer)) {
            helper.fail("expected a TerraformerBlockEntity");
            return;
        }
        helper.assertTrue(terraformer.getItem(0).is(ModItems.NEROSTEEL_INGOT.get()),
                "the capability-inserted upgrade must be visible in the machine's own slot");
        helper.succeed();
    }

    // --- Deeper terraforming (DEEPER_TERRAFORM_DESIGN.md) ------------------------

    /**
     * The Hydration Module melts glacite into a TOUCHING Terraformer's hydration buffer (§3.1):
     * one item per pulse, glacite = 16 units, and a module with a one-block gap feeds nothing
     * (sign-off: strict adjacency).
     */
    private static void testHydrationModuleFeedsTerraformer(GameTestHelper helper) {
        BlockPos terraformerPos = new BlockPos(2, 1, 2);
        BlockPos modulePos = new BlockPos(3, 1, 2);
        helper.setBlock(terraformerPos, ModBlocks.TERRAFORMER.get());
        helper.setBlock(modulePos, ModBlocks.HYDRATION_MODULE.get());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(terraformerPos))
                instanceof za.co.neroland.nerospace.machine.TerraformerBlockEntity terraformer)
                || !(helper.getLevel().getBlockEntity(helper.absolutePos(modulePos))
                instanceof za.co.neroland.nerospace.machine.HydrationModuleBlockEntity module)) {
            helper.fail("expected Terraformer + Hydration Module block entities");
            return;
        }

        module.setItem(0, new ItemStack(ModItems.GLACITE.get(), 2));
        module.meltPulse(helper.getLevel(), helper.absolutePos(modulePos));
        helper.assertTrue(terraformer.getHydration() == za.co.neroland.nerospace.Tuning.HYDRATION_PER_GLACITE,
                "one melt pulse must convert exactly one glacite into 16 hydration units");
        module.meltPulse(helper.getLevel(), helper.absolutePos(modulePos));
        helper.assertTrue(terraformer.getHydration() == 2 * za.co.neroland.nerospace.Tuning.HYDRATION_PER_GLACITE,
                "the second pulse must melt the second glacite");
        helper.assertTrue(module.getItem(0).isEmpty(), "both glacite must have been consumed");

        // A module that does not TOUCH the Terraformer (one-block gap) feeds nothing.
        BlockPos gappedPos = new BlockPos(5, 1, 2);
        helper.setBlock(gappedPos, ModBlocks.HYDRATION_MODULE.get());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(gappedPos))
                instanceof za.co.neroland.nerospace.machine.HydrationModuleBlockEntity gapped)) {
            helper.fail("expected the gapped Hydration Module block entity");
            return;
        }
        gapped.setItem(0, new ItemStack(ModItems.GLACITE.get()));
        gapped.meltPulse(helper.getLevel(), helper.absolutePos(gappedPos));
        helper.assertTrue(!gapped.getItem(0).isEmpty(),
                "a non-touching module must not melt (strict adjacency per sign-off)");
        helper.succeed();
    }

    /**
     * Stage-2 water-table fill (§3.2): a basin below the table fills (one hydration unit per
     * source), terrain at the table stays dry, the fill is idempotent (re-run costs nothing) and an
     * empty sink stalls the column instead of part-filling it for free.
     */
    private static void testTerraformWaterTableFill(GameTestHelper helper) {
        // A 5x5 two-layer stone plate; the water table sits at the plate's top surface.
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.AIR); // a 1-deep basin
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.AIR); // a second basin, for the stall case
        int tableY = helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        ServerLevel level = helper.getLevel();

        int[] units = {10};
        za.co.neroland.nerospace.machine.TerraformConversion.HydrationSink sink = want -> {
            int granted = Math.min(want, units[0]);
            units[0] -= granted;
            return granted;
        };

        BlockPos basin = helper.absolutePos(new BlockPos(3, 2, 3));
        boolean done = za.co.neroland.nerospace.machine.TerraformConversion.hydrateColumn(
                level, basin.getX(), basin.getZ(), tableY, sink);
        helper.assertTrue(done, "a funded basin column must hydrate fully");
        helper.assertTrue(level.getBlockState(basin).is(Blocks.WATER),
                "the basin cell below the table must fill with water");
        helper.assertTrue(units[0] == 9, "filling one cell must cost exactly one hydration unit");

        // Idempotent: re-running the same column costs nothing.
        done = za.co.neroland.nerospace.machine.TerraformConversion.hydrateColumn(
                level, basin.getX(), basin.getZ(), tableY, sink);
        helper.assertTrue(done && units[0] == 9, "re-hydrating a filled column must be a free no-op");

        // Terrain at the table stays dry (and costs nothing).
        BlockPos dry = helper.absolutePos(new BlockPos(2, 2, 2));
        done = za.co.neroland.nerospace.machine.TerraformConversion.hydrateColumn(
                level, dry.getX(), dry.getZ(), tableY, sink);
        helper.assertTrue(done && units[0] == 9, "a column at/above the table must stay dry for free");
        helper.assertTrue(level.getBlockState(dry).is(Blocks.STONE), "dry ground must be untouched");

        // An empty sink stalls the column: no water, not done.
        BlockPos stalled = helper.absolutePos(new BlockPos(4, 2, 4));
        done = za.co.neroland.nerospace.machine.TerraformConversion.hydrateColumn(
                level, stalled.getX(), stalled.getZ(), tableY, want -> 0);
        helper.assertFalse(done, "an unfunded basin column must report a stall");
        helper.assertTrue(level.getBlockState(stalled).isAir(),
                "a stalled column must not receive water");
        helper.succeed();
    }

    /**
     * Stage progression (§2.2): convert → hydrate → vivify walks the chunk's effective stage
     * 1 → 2 → 3 while the legacy {@code TERRAFORMED} breathability flag stays set throughout.
     */
    private static void testTerraformStageProgression(GameTestHelper helper) {
        // A free-standing stone column that owns the local heightmap.
        for (int y = 1; y <= 4; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.STONE);
        }
        ServerLevel level = helper.getLevel();
        BlockPos col = helper.absolutePos(new BlockPos(2, 4, 2));
        LevelChunk chunk = level.getChunkAt(col);
        // Own the chunk state outright — arenas can share chunks with other tests.
        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED, Boolean.FALSE);
        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORM_STAGE, 0);
        helper.assertTrue(
                za.co.neroland.nerospace.machine.TerraformConversion.effectiveStage(chunk) == 0,
                "a reset chunk must read stage 0");

        // The arena sits inside the framework's barrier shell, so the heightmap points at the
        // barrier lid — inject the real surface (one above our stone top) through the test seam.
        int surfaceY = col.getY() + 1;

        java.util.Set<LevelChunk> biomeChanged = new java.util.HashSet<>();
        za.co.neroland.nerospace.machine.TerraformConversion.convertColumn(
                level, col.getX(), col.getZ(), surfaceY, 1, biomeChanged);
        helper.assertTrue(level.getBlockState(new BlockPos(col.getX(), surfaceY - 1, col.getZ())).is(Blocks.GRASS_BLOCK),
                "stage 1 must grass the surface");
        helper.assertTrue(Boolean.TRUE.equals(chunk.getData(
                        za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED)),
                "stage 1 must set the breathability flag");
        helper.assertTrue(
                za.co.neroland.nerospace.machine.TerraformConversion.effectiveStage(chunk) == 1,
                "after conversion the chunk must read stage 1");
        helper.assertTrue(chunk.getNoiseBiome(col.getX() >> 2, col.getY() >> 2, col.getZ() >> 2).is(za.co.neroland.nerospace.world.ModBiomes.TERRAFORMED),
                "stage 1 must write the intermediate neon terraformed biome");

        za.co.neroland.nerospace.machine.TerraformConversion.hydrateColumn(
                level, col.getX(), col.getZ(), surfaceY - 1, null);
        helper.assertTrue(
                za.co.neroland.nerospace.machine.TerraformConversion.effectiveStage(chunk) == 2,
                "after hydration the chunk must read stage 2");

        za.co.neroland.nerospace.machine.TerraformConversion.vivifyColumn(
                level, col.getX(), col.getZ(), biomeChanged);
        helper.assertTrue(
                za.co.neroland.nerospace.machine.TerraformConversion.effectiveStage(chunk) == 3,
                "after vivification the chunk must read stage 3");
        helper.assertTrue(chunk.getNoiseBiome(col.getX() >> 2, col.getY() >> 2, col.getZ() >> 2).is(za.co.neroland.nerospace.world.ModBiomes.TERRAFORMED_MEADOW),
                "stage 3 must settle the mature biome (unknown dimensions default to meadow)");
        // Idempotent: a second vivify pass must leave the stage and biome settled.
        za.co.neroland.nerospace.machine.TerraformConversion.vivifyColumn(
                level, col.getX(), col.getZ(), biomeChanged);
        helper.assertTrue(
                za.co.neroland.nerospace.machine.TerraformConversion.effectiveStage(chunk) == 3
                        && chunk.getNoiseBiome(col.getX() >> 2, col.getY() >> 2, col.getZ() >> 2).is(za.co.neroland.nerospace.world.ModBiomes.TERRAFORMED_MEADOW),
                "re-running stage 3 must be a settled no-op");
        helper.assertTrue(Boolean.TRUE.equals(chunk.getData(
                        za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED)),
                "the legacy breathability flag must survive every stage");
        helper.succeed();
    }

    /**
     * The no-break contract (§9): a legacy chunk (flag set, no stage data) reads as stage 1 and
     * upgrades in place, and a pre-stage {@code TerraformManager} payload (no stage-radius lists)
     * decodes with the trailing frontiers at 0.
     */
    private static void testTerraformLegacySaveCompat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        LevelChunk chunk = level.getChunkAt(pos);

        // Legacy chunk state: TERRAFORMED true, stage attachment absent (default 0).
        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORM_STAGE, 0);
        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED, Boolean.TRUE);
        helper.assertTrue(
                za.co.neroland.nerospace.machine.TerraformConversion.effectiveStage(chunk) == 1,
                "a legacy chunk (flag only) must read as stage 1");

        // It upgrades in place without re-paying stage 1.
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        za.co.neroland.nerospace.machine.TerraformConversion.hydrateColumn(
                level, pos.getX(), pos.getZ(), pos.getY(), null);
        helper.assertTrue(
                za.co.neroland.nerospace.machine.TerraformConversion.effectiveStage(chunk) == 2,
                "a legacy chunk must upgrade straight to stage 2");
        helper.assertTrue(Boolean.TRUE.equals(chunk.getData(
                        za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED)),
                "upgrading must never clear the breathability flag");

        // A pre-stage SavedData payload (positions/radii/tiers only) decodes unchanged.
        net.minecraft.nbt.CompoundTag legacy = new net.minecraft.nbt.CompoundTag();
        BlockPos machine = new BlockPos(100, 64, -200);
        legacy.putLongArray("positions", new long[] {machine.asLong()});
        legacy.putIntArray("radii", new int[] {7});
        legacy.putIntArray("tiers", new int[] {2});
        za.co.neroland.nerospace.world.TerraformManager decoded =
                za.co.neroland.nerospace.world.TerraformManager.codec()
                        .parse(net.minecraft.nbt.NbtOps.INSTANCE, legacy)
                        .result().orElse(null);
        if (decoded == null) {
            helper.fail("a legacy terraformers.dat payload must decode");
            return;
        }
        helper.assertTrue(decoded.stageRadius(machine, 1) == 7,
                "the legacy stage-1 radius must survive the decode");
        helper.assertTrue(decoded.stageRadius(machine, 2) == 0 && decoded.stageRadius(machine, 3) == 0,
                "absent stage radii must default to 0 (frontiers start sweeping from the centre)");
        helper.succeed();
    }

    /**
     * Art overhaul (ART_OVERHAUL_DESIGN.md §3): FACING machines place with their front toward the
     * placer (visual only — old saved machines default north). Exercises the placement override on
     * the Terraformer and the Grinder (the same pattern all six FACING machines share).
     */
    private static void testMachineFacingPlacement(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Direction expected = player.getDirection().getOpposite();

        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        BlockPos floorAbs = helper.absolutePos(new BlockPos(2, 1, 2));
        ItemStack stack = new ItemStack(ModItems.TERRAFORMER_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floorAbs), Direction.UP, floorAbs, false)));
        BlockState placed = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(2, 2, 2)));
        helper.assertTrue(placed.is(ModBlocks.TERRAFORMER.get()), "the terraformer must place");
        helper.assertTrue(placed.getValue(
                        za.co.neroland.nerospace.machine.TerraformerBlock.FACING) == expected,
                "the terraformer's core lens must face the placer");

        helper.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        BlockPos floor2Abs = helper.absolutePos(new BlockPos(4, 1, 2));
        ItemStack grinder = new ItemStack(ModBlocks.NEROSIUM_GRINDER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, grinder);
        grinder.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor2Abs), Direction.UP, floor2Abs, false)));
        BlockState placedGrinder = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(4, 2, 2)));
        helper.assertTrue(placedGrinder.is(ModBlocks.NEROSIUM_GRINDER.get()), "the grinder must place");
        helper.assertTrue(placedGrinder.getValue(
                        za.co.neroland.nerospace.machine.NerosiumGrinderBlock.FACING) == expected,
                "the grinder's intake must face the placer");
        helper.succeed();
    }

    /**
     * The Terraform Monitor (§6) reports the LOCAL column's effective stage on its comparator
     * (0/5/10/15) and reads the nearest registered Terraformer's stage radii.
     */
    private static void testTerraformMonitorReadout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos abs = helper.absolutePos(rel);
        helper.setBlock(rel, ModBlocks.TERRAFORM_MONITOR.get());
        if (!(level.getBlockEntity(abs)
                instanceof za.co.neroland.nerospace.machine.TerraformMonitorBlockEntity monitor)) {
            helper.fail("expected a TerraformMonitorBlockEntity");
            return;
        }
        LevelChunk chunk = level.getChunkAt(abs);
        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORMED, Boolean.FALSE);

        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORM_STAGE, 2);
        monitor.refresh(level, abs);
        helper.assertTrue(monitor.comparatorSignal() == 10,
                "a Hydrated (stage 2) column must read comparator 10");
        helper.assertTrue(level.getBlockState(abs).getAnalogOutputSignal(level, abs, Direction.NORTH) == 10,
                "the block's analog output must surface the monitor's signal");

        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORM_STAGE, 3);
        monitor.refresh(level, abs);
        helper.assertTrue(monitor.comparatorSignal() == 15,
                "a Living (stage 3) column must read comparator 15");

        // Nearest-terraformer link: register a machine in the manager and re-read.
        BlockPos machine = abs.offset(5, 0, 0);
        za.co.neroland.nerospace.world.TerraformManager.get(level).update(machine, 9, 4, 2, 1);
        monitor.refresh(level, abs);
        helper.assertTrue(monitor.getDataAccess().get(0) == 1,
                "the monitor must link to a machine in range");
        helper.assertTrue(monitor.getDataAccess().get(1) == 9
                        && monitor.getDataAccess().get(2) == 4
                        && monitor.getDataAccess().get(3) == 2,
                "the monitor must surface the machine's stage radii");
        // Clean up the registry entry so no other arena links to this phantom machine.
        za.co.neroland.nerospace.world.TerraformManager.get(level).remove(machine);
        chunk.setData(za.co.neroland.nerospace.registry.ModAttachments.TERRAFORM_STAGE, 0);
        helper.succeed();
    }

    /**
     * The terraform livestock breed like vanilla animals (§5): species recognise their breed food
     * and {@code spawnChildFromBreeding} produces a baby of the same species.
     */
    private static void testTerraformCreatureBreeding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.GRASS_BLOCK);

        // Food recognition per species (wheat / seeds / wheat — design §5).
        za.co.neroland.nerospace.entity.MeadowLoper loperA =
                helper.spawn(ModEntities.MEADOW_LOPER.get(), new BlockPos(2, 2, 2));
        za.co.neroland.nerospace.entity.MeadowLoper loperB =
                helper.spawn(ModEntities.MEADOW_LOPER.get(), new BlockPos(3, 2, 2));
        helper.assertTrue(loperA.isFood(new ItemStack(net.minecraft.world.item.Items.WHEAT)),
                "the Meadow Loper must breed with wheat");
        za.co.neroland.nerospace.entity.EmberStrutter strutter =
                helper.spawn(ModEntities.EMBER_STRUTTER.get(), new BlockPos(4, 2, 2));
        helper.assertTrue(strutter.isFood(new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS))
                        && !strutter.isFood(new ItemStack(net.minecraft.world.item.Items.WHEAT)),
                "the Ember Strutter must breed with seeds, not wheat");
        za.co.neroland.nerospace.entity.WoollyDrift drift =
                helper.spawn(ModEntities.WOOLLY_DRIFT.get(), new BlockPos(5, 2, 2));
        helper.assertTrue(drift.isFood(new ItemStack(net.minecraft.world.item.Items.WHEAT)),
                "the Woolly Drift must breed with wheat");
        helper.assertTrue(!drift.canFreeze(), "the Woolly Drift's cold coat must block freezing");

        // Deterministic breeding: both in love, then breed directly (fires BredAnimalsTrigger too).
        loperA.setInLove(null);
        loperB.setInLove(null);
        loperA.spawnChildFromBreeding(level, loperB);
        long babies = level.getEntities(ModEntities.MEADOW_LOPER.get(),
                        new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(2, 2, 2))).inflate(8.0D),
                        e -> e.isAlive() && e.isBaby())
                .size();
        helper.assertTrue(babies >= 1, "breeding two Meadow Lopers must produce a baby Loper");
        helper.succeed();
    }

    // --- Oxygen field boundaries -------------------------------------------------

    /**
     * Doors/trapdoors/glass as sealed-room boundaries (terraform design §1.4): a closed door or
     * trapdoor is a wall, an open one lets oxygen flow (and leak), glass seals through the
     * full-collision-cube fallback (airtight windows), and a glass pane holds-but-leaks. Exercises
     * {@link OxygenField#canHold}/{@link OxygenField#isLeaky} — the exact predicates the field
     * simulation BFS uses to decide sealed vs leaky volumes.
     */
    private static void testOxygenSealingBoundaries(GameTestHelper helper) {
        BlockPos base = new BlockPos(2, 1, 2);
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(base, Blocks.STONE); // sturdy support so the door states are stable

        helper.setBlock(pos, Blocks.GLASS);
        assertHolds(helper, pos, false, "glass must seal (airtight window)");

        helper.setBlock(pos, Blocks.GLASS_PANE);
        assertHolds(helper, pos, true, "a glass pane is not airtight (holds but bleeds)");

        helper.setBlock(pos, Blocks.OAK_DOOR); // default state: closed
        assertHolds(helper, pos, false, "a closed door must seal the room");
        assertLeaky(helper, pos, false, "a closed door must not read as a leak");
        helper.setBlock(pos, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.OPEN, true));
        assertHolds(helper, pos, true, "an open door must let oxygen flow");
        assertLeaky(helper, pos, true, "an open door is a leak");

        helper.setBlock(pos, Blocks.OAK_TRAPDOOR); // default state: closed
        assertHolds(helper, pos, false, "a closed trapdoor must seal the room");
        assertLeaky(helper, pos, false,
                "a closed trapdoor must not read as a leak (OXYGEN_LEAKS tag ordering)");
        helper.setBlock(pos, Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.OPEN, true));
        assertHolds(helper, pos, true, "an open trapdoor must let oxygen flow");
        assertLeaky(helper, pos, true, "an open trapdoor is a leak");

        helper.setBlock(pos, ModBlocks.STATION_WALL.get());
        assertHolds(helper, pos, false, "station wall must seal (OXYGEN_SEALING tag)");

        helper.setBlock(pos, Blocks.AIR);
        assertHolds(helper, pos, true, "air must hold oxygen");
        helper.succeed();
    }

    private static void assertHolds(GameTestHelper helper, BlockPos pos, boolean expected, String message) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        helper.assertTrue(OxygenField.canHold(helper.getLevel(), abs, state) == expected, message);
    }

    private static void assertLeaky(GameTestHelper helper, BlockPos pos, boolean expected, String message) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(abs);
        helper.assertTrue(OxygenField.isLeaky(helper.getLevel(), abs, state) == expected, message);
    }

    // --- Heavy Launch Complex (LAUNCH_PAD_DESIGN.md) ---------------------------------------------

    /** A bare 5x5 is not a Heavy complex; adding a Launch Gantry on the border ring forms it. */
    private static void testHeavyComplexDetection(GameTestHelper helper) {
        BlockPos centre = buildHeavyPad(helper);
        java.util.Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(
                helper.getLevel(), helper.absolutePos(centre));
        helper.assertTrue(pads.size() == 25, "the 5x5 cluster must flood-fill completely (MAX_PADS)");
        helper.assertTrue(LaunchPadMultiblock.fullSquareCorner(pads, 5) != null,
                "the full 5x5 must be detected");
        helper.assertTrue(LaunchPadMultiblock.isFullThreeByThree(pads),
                "a 5x5 must still count as a basic 3x3 pad");
        helper.assertFalse(LaunchPadMultiblock.isHeavyComplex(helper.getLevel(), pads),
                "a bare 5x5 must NOT be a Heavy complex (gantry required — sign-off Q2)");

        helper.setBlock(new BlockPos(0, 1, 3), ModBlocks.LAUNCH_GANTRY.get());
        helper.assertTrue(LaunchPadMultiblock.isHeavyComplex(helper.getLevel(), pads),
                "5x5 + border gantry must form the Heavy complex");
        helper.succeed();
    }

    /** A Tier 3 rocket deploys on a Heavy complex without the Station-Wall ring (sign-off Q1). */
    private static void testTier3DeploysOnHeavyComplex(GameTestHelper helper) {
        BlockPos centre = buildHeavyPad(helper);
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_3.get()), centre);
        helper.assertEntityNotPresent(ModEntities.ROCKET.get());

        helper.setBlock(new BlockPos(0, 1, 3), ModBlocks.LAUNCH_GANTRY.get());
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_3.get()), centre);
        helper.assertEntityPresent(ModEntities.ROCKET.get());
        helper.succeed();
    }

    /** Fuel pump rate steps with the footprint: base → 4x on a 3x3 → 12x on the Heavy complex. */
    private static void testFuelPumpRateByFootprint(GameTestHelper helper) {
        // Partial cluster (2 pads): base rate.
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROCKET_LAUNCH_PAD.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ROCKET_LAUNCH_PAD.get());
        java.util.Set<BlockPos> partial = LaunchPadMultiblock.connectedPads(
                helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertTrue(za.co.neroland.nerospace.machine.FuelTankBlockEntity
                        .pumpRate(helper.getLevel(), partial) == za.co.neroland.nerospace.Tuning.fuelTankPumpRate(),
                "a partial cluster must pump at the base rate");

        // Full 5x5 without gantry: the 3x3 (full-pad) rate.
        BlockPos centre = buildHeavyPad(helper);
        java.util.Set<BlockPos> pads = LaunchPadMultiblock.connectedPads(
                helper.getLevel(), helper.absolutePos(centre));
        helper.assertTrue(za.co.neroland.nerospace.machine.FuelTankBlockEntity
                        .pumpRate(helper.getLevel(), pads) == za.co.neroland.nerospace.Tuning.fuelTankPumpRateFullPad(),
                "a bare 5x5 must pump at the full-pad (3x3) rate");

        // Heavy complex: 12x.
        helper.setBlock(new BlockPos(0, 1, 3), ModBlocks.LAUNCH_GANTRY.get());
        helper.assertTrue(za.co.neroland.nerospace.machine.FuelTankBlockEntity
                        .pumpRate(helper.getLevel(), pads) == za.co.neroland.nerospace.Tuning.fuelTankPumpRateHeavyPad(),
                "the Heavy complex must pump at the heavy rate");
        helper.succeed();
    }

    /** Deploys centre the rocket on the formed square, and an occupied pad rejects a second one. */
    private static void testSingleCenteredRocketPerPad(GameTestHelper helper) {
        BlockPos centre = buildFullPad(helper); // corner (1,1,1) → centre (2,1,2)
        // Click a CORNER pad: the rocket must still end up on the square's centre.
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_1.get()), new BlockPos(1, 1, 1));
        java.util.List<RocketEntity> rockets = helper.getLevel().getEntitiesOfClass(
                RocketEntity.class, helper.getBounds());
        helper.assertTrue(rockets.size() == 1, "the deploy must spawn exactly one rocket");
        RocketEntity rocket = rockets.get(0);
        BlockPos absCentre = helper.absolutePos(centre);
        helper.assertTrue(Math.abs(rocket.getX() - (absCentre.getX() + 0.5D)) < 0.01D
                        && Math.abs(rocket.getZ() - (absCentre.getZ() + 0.5D)) < 0.01D,
                "the rocket must be centred on the 3x3 (got " + rocket.position() + ")");

        // Second deploy on the same (occupied) pad must be rejected.
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_1.get()), centre);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        RocketEntity.class, helper.getBounds()).size() == 1,
                "an occupied pad must reject a second rocket");
        helper.succeed();
    }

    /** Breaking a block of the square the rocket stands on grounds it (launch re-check). */
    private static void testPadBreakGroundsRocket(GameTestHelper helper) {
        BlockPos centre = buildFullPad(helper);
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_1.get()), centre);
        java.util.List<RocketEntity> rockets = helper.getLevel().getEntitiesOfClass(
                RocketEntity.class, helper.getBounds());
        helper.assertTrue(rockets.size() == 1, "test setup: one deployed rocket");
        RocketEntity rocket = rockets.get(0);
        helper.assertTrue(rocket.isOnValidPad(), "the freshly deployed rocket must be launchable");

        helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR); // break a corner of ITS 3x3
        helper.assertFalse(rocket.isOnValidPad(),
                "breaking a block of the rocket's own square must ground it");
        helper.succeed();
    }

    // --- Hazard suit variants (SUIT_HAZARD_DESIGN.md) --------------------------------------------

    /**
     * Shield detection is orthogonal to the capacity tier: a full matching variant set grants its
     * shield AND Tier 2 capacity; mixing variants keeps Tier 2 capacity (all pieces are T2-class)
     * but voids the shield; a T1 piece demotes capacity AND voids the shield.
     */
    private static void testHazardShieldDetection(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        equipVariant(player, true);
        helper.assertTrue(GreenxertzAtmosphere.hazardShield(player) == GreenxertzAtmosphere.HazardShield.HEAT,
                "a full Thermal set must shield HEAT");
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_2,
                "variant pieces are Tier-2-class: full Thermal set keeps TIER_2 capacity");

        equipVariant(player, false);
        helper.assertTrue(GreenxertzAtmosphere.hazardShield(player) == GreenxertzAtmosphere.HazardShield.COLD,
                "a full Cryo set must shield COLD");
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_2,
                "variant pieces are Tier-2-class: full Cryo set keeps TIER_2 capacity");

        // Mixed variants: capacity stays Tier 2, but the shield is void (all-4-matching rule).
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.OXYGEN_SUIT_HEAT_HELMET.get()));
        helper.assertTrue(GreenxertzAtmosphere.hazardShield(player) == GreenxertzAtmosphere.HazardShield.NONE,
                "a heat helmet on a cryo suit must void the shield");
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_2,
                "mixed variants are still all Tier-2-class pieces (TIER_2 capacity)");

        // A plain T2 piece in a variant set: same — capacity holds, shield voids.
        equipVariant(player, true);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.OXYGEN_SUIT_T2_BOOTS.get()));
        helper.assertTrue(GreenxertzAtmosphere.hazardShield(player) == GreenxertzAtmosphere.HazardShield.NONE,
                "a plain T2 boot in a Thermal set must void the shield");
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_2,
                "T2 + variant mix keeps TIER_2 capacity");

        // A T1 piece demotes capacity and (trivially) the shield.
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.OXYGEN_SUIT_BOOTS.get()));
        helper.assertTrue(GreenxertzAtmosphere.suitTier(player) == GreenxertzAtmosphere.SuitTier.TIER_1,
                "a T1 piece in a variant set demotes capacity to TIER_1 (mixed-set rule)");
        helper.assertTrue(GreenxertzAtmosphere.hazardShield(player) == GreenxertzAtmosphere.HazardShield.NONE,
                "a T1 piece also voids the shield");
        helper.succeed();
    }

    /** Hazard map + effective drain: ×4 unprotected on hazard worlds, ×1 with the matching shield. */
    private static void testHazardDrainMultiplier(GameTestHelper helper) {
        helper.assertTrue(GreenxertzAtmosphere.hazardFor(
                        za.co.neroland.nerospace.registry.ModDimensions.CINDARA_LEVEL)
                        == GreenxertzAtmosphere.Hazard.HEAT,
                "Cindara must carry the HEAT hazard");
        helper.assertTrue(GreenxertzAtmosphere.hazardFor(
                        za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL)
                        == GreenxertzAtmosphere.Hazard.COLD,
                "Glacira must carry the COLD hazard");
        helper.assertTrue(GreenxertzAtmosphere.hazardFor(
                        za.co.neroland.nerospace.registry.ModDimensions.GREENXERTZ_LEVEL)
                        == GreenxertzAtmosphere.Hazard.NONE
                        && GreenxertzAtmosphere.hazardFor(
                                za.co.neroland.nerospace.registry.ModDimensions.STATION_LEVEL)
                        == GreenxertzAtmosphere.Hazard.NONE,
                "Greenxertz and the Station must stay hazard-free");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int x = za.co.neroland.nerospace.Tuning.BASE_HAZARD_DRAIN_MULTIPLIER;

        // Unprotected (T2 suit): hazard worlds drain x4.
        equipSuit(player, true);
        helper.assertTrue(GreenxertzAtmosphere.hazardDrainMultiplier(
                        za.co.neroland.nerospace.registry.ModDimensions.CINDARA_LEVEL, player) == x,
                "a T2 suit on Cindara must drain x" + x);
        helper.assertTrue(GreenxertzAtmosphere.hazardDrainMultiplier(
                        za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL, player) == x,
                "a T2 suit on Glacira must drain x" + x);
        helper.assertTrue(GreenxertzAtmosphere.hazardDrainMultiplier(
                        za.co.neroland.nerospace.registry.ModDimensions.GREENXERTZ_LEVEL, player) == 1,
                "Greenxertz never hazard-drains");

        // The matching shield counters its own hazard — and ONLY its own.
        equipVariant(player, true);
        helper.assertTrue(GreenxertzAtmosphere.hazardDrainMultiplier(
                        za.co.neroland.nerospace.registry.ModDimensions.CINDARA_LEVEL, player) == 1,
                "a Thermal Suit must counter Cindara's heat");
        helper.assertTrue(GreenxertzAtmosphere.hazardDrainMultiplier(
                        za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL, player) == x,
                "a Thermal Suit must NOT counter Glacira's cold");

        equipVariant(player, false);
        helper.assertTrue(GreenxertzAtmosphere.hazardDrainMultiplier(
                        za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL, player) == 1,
                "a Cryo Suit must counter Glacira's cold");
        helper.assertTrue(GreenxertzAtmosphere.hazardDrainMultiplier(
                        za.co.neroland.nerospace.registry.ModDimensions.CINDARA_LEVEL, player) == x,
                "a Cryo Suit must NOT counter Cindara's heat");
        helper.succeed();
    }

    // --- Glacira / Tier 4 (NEW_DESTINATION_DESIGN.md) --------------------------------------------

    /**
     * A Tier 4 rocket deploys ONLY on the Heavy Launch Complex: the Station-Wall ring that
     * satisfies Tier 3 must NOT satisfy Tier 4 (no ring shortcut — design doc §5), and breaking
     * the gantry afterwards grounds the deployed rocket (launch re-check).
     */
    private static void testTier4RequiresHeavyComplex(GameTestHelper helper) {
        // Ringed 3x3 (the Tier 3 path) must reject a Tier 4 deploy.
        BlockPos centre3 = buildFullPad(helper);
        buildStationWallRing(helper);
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_4.get()), centre3);
        helper.assertEntityNotPresent(ModEntities.ROCKET.get());

        // Clear and build the 5x5; still no gantry => still rejected.
        for (int dx = 0; dx <= 4; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                helper.setBlock(new BlockPos(dx, 1, dz), Blocks.AIR);
            }
        }
        BlockPos centre5 = buildHeavyPad(helper);
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_4.get()), centre5);
        helper.assertEntityNotPresent(ModEntities.ROCKET.get());

        // Completing the Heavy complex (border gantry) accepts the deploy.
        helper.setBlock(new BlockPos(0, 1, 3), ModBlocks.LAUNCH_GANTRY.get());
        useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_4.get()), centre5);
        java.util.List<RocketEntity> rockets = helper.getLevel().getEntitiesOfClass(
                RocketEntity.class, helper.getBounds());
        helper.assertTrue(rockets.size() == 1, "the Heavy complex must accept a Tier 4 deploy");
        RocketEntity rocket = rockets.get(0);
        helper.assertTrue(rocket.isOnValidPad(), "the deployed Tier 4 must be launchable");

        // Breaking the gantry demotes the complex — the Tier 4 is grounded.
        helper.setBlock(new BlockPos(0, 1, 3), Blocks.AIR);
        helper.assertFalse(rocket.isOnValidPad(),
                "breaking the gantry must ground a deployed Tier 4 (heavy-only gating)");
        helper.succeed();
    }

    /** Tier destination lists stay cumulative and each tier's signature target is correct. */
    private static void testTierDestinationsCumulative(GameTestHelper helper) {
        helper.assertTrue(RocketTier.TIER_1.destinations().size() == 1
                        && RocketTier.TIER_2.destinations().size() == 2
                        && RocketTier.TIER_3.destinations().size() == 3
                        && RocketTier.TIER_4.destinations().size() == 4,
                "tier destination lists must grow by exactly one per tier");
        for (RocketTier lower : RocketTier.values()) {
            helper.assertTrue(RocketTier.TIER_4.destinations().containsAll(lower.destinations()),
                    "Tier 4 must reach every lower tier's destinations (cumulative): " + lower);
        }
        helper.assertTrue(RocketTier.TIER_4.destination(RocketTier.TIER_4.defaultDestinationIndex())
                        .equals(za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL),
                "Tier 4's signature (default) destination must be Glacira");
        helper.assertTrue("Glacira".equals(za.co.neroland.nerospace.rocket.Destinations.name(
                        za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL)),
                "the destination selector must know Glacira's display name");
        helper.succeed();
    }

    /**
     * Travel parity guard: WHEREVER the proven destinations' level stems are visible, Glacira's
     * must be too. The gametest server composes only the vanilla dimensions (it neither
     * instantiates nor registers datapack level stems — Cindara/Greenxertz are absent here as
     * well), so this asserts strict parity with Cindara rather than absolute presence; in a real
     * server both load from the same generated {@code data/nerospace/dimension/*.json}.
     */
    private static void testGlaciraDimensionLoads(GameTestHelper helper) {
        var stems = helper.getLevel().registryAccess().lookupOrThrow(Registries.LEVEL_STEM);
        boolean cindara = stems.get(za.co.neroland.nerospace.registry.ModDimensions.CINDARA_STEM).isPresent();
        boolean glacira = stems.get(za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_STEM).isPresent();
        helper.assertTrue(cindara == glacira,
                "Glacira's level stem must register exactly like Cindara's (cindara=" + cindara
                        + ", glacira=" + glacira + ")");

        ServerLevel cindaraLevel = helper.getLevel().getServer()
                .getLevel(za.co.neroland.nerospace.registry.ModDimensions.CINDARA_LEVEL);
        ServerLevel glaciraLevel = helper.getLevel().getServer()
                .getLevel(za.co.neroland.nerospace.registry.ModDimensions.GLACIRA_LEVEL);
        helper.assertTrue((cindaraLevel != null) == (glaciraLevel != null),
                "Glacira must load as a server level exactly like Cindara");
        helper.succeed();
    }

    // --- Multiple stations (MULTI_STATION_DESIGN.md) ---------------------------------------------

    /**
     * Registry behaviour: founding allocates unique, never-reused slots at well-separated centres;
     * names come from the charter (blank = auto "Station N"); entries codec-round-trip (the shape
     * the SavedData persists). Registers clean up so tests stay order-independent.
     */
    private static void testStationRegistryRoundtrip(GameTestHelper helper) {
        za.co.neroland.nerospace.rocket.StationRegistry registry =
                za.co.neroland.nerospace.rocket.StationRegistry.get(helper.getLevel().getServer());
        int before = registry.count();

        // Null-narrowed locals (the @Nullable founder returns; ECJ-friendly explicit checks).
        za.co.neroland.nerospace.rocket.StationRegistry.StationEntry named = registry.found("Port Dario");
        za.co.neroland.nerospace.rocket.StationRegistry.StationEntry auto = registry.found("  ");
        if (named == null || auto == null) {
            helper.fail("founding must register below the cap");
            return;
        }
        try {
            helper.assertTrue("Port Dario".equals(named.name()),
                    "the charter name must become the station name");
            helper.assertTrue(auto.name().startsWith("Station "),
                    "a blank charter must auto-name (got '" + auto.name() + "')");
            helper.assertTrue(named.slot() != auto.slot(), "slots must be unique");
            helper.assertTrue(Math.abs(named.center().getX() - auto.center().getX())
                            >= za.co.neroland.nerospace.rocket.StationRegistry.SLOT_SPACING,
                    "station centres must be separated by at least one slot spacing");
            helper.assertTrue(registry.count() == before + 2, "both stations must be registered");

            // Entry codec round-trip (what the SavedData persists).
            var encoded = za.co.neroland.nerospace.rocket.StationRegistry.StationEntry.CODEC
                    .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, named);
            helper.assertTrue(encoded.result().isPresent(), "the station entry must encode");
            var decoded = za.co.neroland.nerospace.rocket.StationRegistry.StationEntry.CODEC
                    .parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded.result().orElseThrow());
            helper.assertTrue(decoded.result().isPresent() && named.equals(decoded.result().orElseThrow()),
                    "the station entry must decode back to an equal record");

            // Unregistering frees the entry but never the slot number.
            int freedSlot = auto.slot();
            helper.assertTrue(registry.unregister(freedSlot) != null, "unregister must remove the entry");
            helper.assertTrue(registry.get(freedSlot) == null, "the slot must be gone after unregister");
            za.co.neroland.nerospace.rocket.StationRegistry.StationEntry third = registry.found(null);
            if (third == null) {
                helper.fail("re-founding after an unregister must succeed");
                return;
            }
            helper.assertTrue(third.slot() > freedSlot,
                    "slot numbers must never be reused (no founding inside abandoned hulls)");
            registry.unregister(third.slot());
        } finally {
            registry.unregister(named.slot());
            registry.unregister(auto.slot());
        }
        helper.assertTrue(registry.count() == before, "the test must leave the registry as it found it");
        helper.succeed();
    }

    /** Breaking a bound Station Core unregisters its station and pops a charter named after it. */
    private static void testStationCoreBreakUnregisters(GameTestHelper helper) {
        za.co.neroland.nerospace.rocket.StationRegistry registry =
                za.co.neroland.nerospace.rocket.StationRegistry.get(helper.getLevel().getServer());
        za.co.neroland.nerospace.rocket.StationRegistry.StationEntry entry = registry.found("Testopolis");
        if (entry == null) {
            helper.fail("test setup: a founded station");
            return;
        }

        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.STATION_CORE.get());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                instanceof za.co.neroland.nerospace.rocket.StationCoreBlockEntity core)) {
            helper.fail("expected a StationCoreBlockEntity");
            return;
        }
        core.bindStation(entry.slot(), entry.name());
        helper.assertTrue(core.comparatorSignal() == 15, "a bound core must read comparator 15");

        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.assertTrue(registry.get(entry.slot()) == null,
                "breaking the core must unregister its station");
        helper.succeedWhen(() -> helper.assertItemEntityPresent(
                ModItems.STATION_CHARTER.get(), pos, 3.0D));
    }

    /** Station selection on the rocket: slot-stable, registry-validated, cleared by planet picks. */
    private static void testRocketStationSelection(GameTestHelper helper) {
        za.co.neroland.nerospace.rocket.StationRegistry registry =
                za.co.neroland.nerospace.rocket.StationRegistry.get(helper.getLevel().getServer());
        za.co.neroland.nerospace.rocket.StationRegistry.StationEntry entry = registry.found("Waypoint Alpha");
        if (entry == null) {
            helper.fail("test setup: a founded station");
            return;
        }
        try {
            BlockPos centre = buildFullPad(helper);
            useItemOn(helper, new ItemStack(ModItems.ROCKET_TIER_1.get()), centre);
            java.util.List<RocketEntity> rockets = helper.getLevel().getEntitiesOfClass(
                    RocketEntity.class, helper.getBounds());
            helper.assertTrue(rockets.size() == 1, "test setup: one deployed rocket");
            RocketEntity rocket = rockets.get(0);

            // Select the founded station: the destination resolves to the station dimension.
            rocket.selectStation(entry.slot());
            helper.assertTrue(rocket.stationSelection() == entry.slot(),
                    "selecting a registered station must stick");
            helper.assertTrue(za.co.neroland.nerospace.registry.ModDimensions.STATION_LEVEL
                            .equals(rocket.selectedDestination()),
                    "a station selection must target the station dimension");

            // A planet pick clears the station selection.
            rocket.setDestinationIndex(0);
            helper.assertTrue(rocket.stationSelection() == RocketEntity.STATION_NONE,
                    "picking a planet must clear the station selection");

            // Cycling enters the station list; an unregistered slot is rejected.
            rocket.cycleStation();
            helper.assertTrue(rocket.stationSelection() >= 0,
                    "cycling must select a registered station");
            rocket.setDestinationIndex(0);
            rocket.selectStation(9_999);
            helper.assertTrue(rocket.stationSelection() == RocketEntity.STATION_NONE,
                    "selecting an unregistered slot must be rejected");

            // FOUND requires a charter in the rider's inventory.
            Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
            rider.startRiding(rocket);
            rocket.selectFound();
            helper.assertTrue(rocket.stationSelection() == RocketEntity.STATION_NONE,
                    "FOUND must be rejected without a Station Charter");
            rider.getInventory().add(new ItemStack(ModItems.STATION_CHARTER.get()));
            rocket.selectFound();
            helper.assertTrue(rocket.stationSelection() == RocketEntity.STATION_FOUND,
                    "FOUND must be selectable with a charter aboard");
            rider.stopRiding();
        } finally {
            registry.unregister(entry.slot());
        }
        helper.succeed();
    }

    // --- Star Guide (progression block) ---------------------------------------------------------

    /** Installing the book loads the pedestal; sneak-use returns it and the pedestal goes bare. */
    private static void testStarGuideInstallReturn(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, za.co.neroland.nerospace.registry.ModBlocks.STAR_GUIDE.get());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                instanceof za.co.neroland.nerospace.progression.StarGuideBlockEntity guide)) {
            helper.fail("expected a StarGuideBlockEntity");
            return;
        }
        helper.assertFalse(guide.hasBook(), "a fresh pedestal must be bare");
        helper.assertTrue(guide.comparatorSignal() == 0, "bare pedestal comparator must read 0");

        ItemStack book = new ItemStack(ModItems.STAR_GUIDE_BOOK.get());
        helper.assertTrue(guide.installBook(book), "the pedestal must accept a Star Guide Book");
        helper.assertTrue(book.isEmpty(), "installing must consume the held book");
        helper.assertTrue(guide.hasBook(), "the pedestal must hold the installed book");
        helper.assertTrue(guide.comparatorSignal() == 15, "loaded pedestal comparator must read 15");
        helper.assertFalse(guide.installBook(new ItemStack(ModItems.STAR_GUIDE_BOOK.get())),
                "a loaded pedestal must reject a second book");

        ItemStack returned = guide.removeBook();
        helper.assertTrue(returned.is(ModItems.STAR_GUIDE_BOOK.get()),
                "removing must return the installed book");
        helper.assertFalse(guide.hasBook(), "the pedestal must be bare after removal");
        helper.succeed();
    }

    /** Breaking a loaded pedestal pops the book (plus the block via loot). */
    private static void testStarGuideBreakDropsBook(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, za.co.neroland.nerospace.registry.ModBlocks.STAR_GUIDE.get());
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos))
                instanceof za.co.neroland.nerospace.progression.StarGuideBlockEntity guide)) {
            helper.fail("expected a StarGuideBlockEntity");
            return;
        }
        guide.installBook(new ItemStack(ModItems.STAR_GUIDE_BOOK.get()));
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(
                ModItems.STAR_GUIDE_BOOK.get(), pos, 3.0D));
    }

    /** Every Star Guide step's advancement id must resolve (guards table ↔ datagen drift). */
    private static void testStarGuideAdvancementsResolve(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getAdvancements();
        for (za.co.neroland.nerospace.progression.StarGuide.Chapter chapter
                : za.co.neroland.nerospace.progression.StarGuide.CHAPTERS) {
            for (za.co.neroland.nerospace.progression.StarGuide.Step step : chapter.steps()) {
                helper.assertTrue(manager.get(step.advancement()) != null,
                        "step '" + step.id() + "' names a missing advancement: " + step.advancement());
            }
        }
        helper.succeed();
    }

    /** Awarding a step's advancement flips its completion bit; a menu click marks it seen. */
    private static void testStarGuideProgressAndSeen(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var manager = helper.getLevel().getServer().getAdvancements();

        // Chapter 0 ("nerosium"), step 1 = the root advancement (nerosium ingot).
        helper.assertTrue(za.co.neroland.nerospace.progression.StarGuideProgress
                .chapterMask(player, 0) == 0, "a fresh player has no completed steps");

        var root = manager.get(Identifier.fromNamespaceAndPath(Nerospace.MODID, "root"));
        if (root == null) {
            helper.fail("nerospace:root advancement missing");
            return;
        }
        root.value().criteria().keySet().forEach(c -> player.getAdvancements().award(root, c));
        helper.assertTrue((za.co.neroland.nerospace.progression.StarGuideProgress
                        .chapterMask(player, 0) & 0b010) != 0,
                "completing nerospace:root must flip chapter 0 / step 1");
        helper.assertTrue(za.co.neroland.nerospace.progression.StarGuideProgress
                        .nextStepIcon(player).is(ModItems.RAW_NEROSIUM.get()),
                "the next-step hologram icon must be the first incomplete step (raw nerosium)");

        // Seen-state: the menu click writes the attachment bit.
        var menu = new za.co.neroland.nerospace.progression.StarGuideMenu(1, player.getInventory(), player);
        helper.assertTrue(menu.clickMenuButton(player, 0 * 16 + 1), "the seen click must be accepted");
        java.util.List<Integer> seen = player.getData(
                za.co.neroland.nerospace.registry.ModAttachments.STAR_GUIDE_SEEN);
        helper.assertTrue(!seen.isEmpty() && (seen.get(0) & 0b010) != 0,
                "the click must set the seen bit in the attachment");
        player.disconnect();
        helper.succeed();
    }

    /**
     * Regression guard for the world-join crash: vanilla SYNCS the {@code TEST_INSTANCE} registry to
     * joining clients, encoding every entry through {@link GameTestInstance#DIRECT_CODEC}. A test
     * instance whose {@code codec()} can't round-trip it kills the connection — so round-trip one of
     * our own entries through the exact same codec here.
     */
    private static void testInstanceSyncRoundtrip(GameTestHelper helper) {
        net.minecraft.core.RegistryAccess access = helper.getLevel().registryAccess();
        GameTestInstance self = access.lookupOrThrow(Registries.TEST_INSTANCE)
                .getValue(Identifier.fromNamespaceAndPath(Nerospace.MODID, "tests/suit_tier_detection"));
        helper.assertTrue(self != null, "our test instances must be in the TEST_INSTANCE registry");

        net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops =
                net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, access);
        var encoded = GameTestInstance.DIRECT_CODEC.encodeStart(ops, self);
        helper.assertTrue(encoded.result().isPresent(),
                "test instance must ENCODE for registry sync: " + encoded.error().map(Object::toString).orElse(""));
        var decoded = GameTestInstance.DIRECT_CODEC.parse(ops, encoded.result().orElseThrow());
        helper.assertTrue(decoded.result().isPresent(),
                "test instance must DECODE after registry sync: " + decoded.error().map(Object::toString).orElse(""));
        helper.assertTrue(decoded.result().orElseThrow() instanceof CodeTest,
                "decoded instance must be a nerospace CodeTest");
        helper.succeed();
    }

    /**
     * A code-backed test instance with a REAL codec: serializes as {@code {type: nerospace:code,
     * function: <name>, ...TestData}} so vanilla's registry sync round-trips it cleanly (see the
     * class javadoc); decode resolves the function from {@link #FUNCTIONS} by name.
     */
    public static final class CodeTest extends GameTestInstance {

        public static final MapCodec<CodeTest> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("function").forGetter(test -> test.name),
                TestData.CODEC.forGetter(test -> test.info()))
                .apply(instance, CodeTest::new));

        private final String name;
        private final Consumer<GameTestHelper> function;

        CodeTest(String name, TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
            this.name = name;
            this.function = FUNCTIONS.getOrDefault(name,
                    helper -> helper.fail("unknown nerospace test function: " + name));
        }

        CodeTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, String name) {
            this(name, data);
        }

        @Override
        public void run(GameTestHelper helper) {
            this.function.accept(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("nerospace code test: " + this.name);
        }
    }
}

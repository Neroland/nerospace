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
        // Oxygen field boundary classification: doors/trapdoors seal when closed and flow when
        // open, glass seals (full collision cube fallback), panes/fences hold-but-leak.
        functions.put("oxygen_sealing_boundaries", NerospaceGameTests::testOxygenSealingBoundaries);
        // Heavy Launch Complex (LAUNCH_PAD_DESIGN.md).
        functions.put("heavy_complex_detection", NerospaceGameTests::testHeavyComplexDetection);
        functions.put("tier3_deploys_on_heavy_complex", NerospaceGameTests::testTier3DeploysOnHeavyComplex);
        functions.put("fuel_pump_rate_by_footprint", NerospaceGameTests::testFuelPumpRateByFootprint);
        functions.put("single_centered_rocket_per_pad", NerospaceGameTests::testSingleCenteredRocketPerPad);
        functions.put("pad_break_grounds_rocket", NerospaceGameTests::testPadBreakGroundsRocket);
        // Glacira / Tier 4 (NEW_DESTINATION_DESIGN.md).
        functions.put("tier4_requires_heavy_complex", NerospaceGameTests::testTier4RequiresHeavyComplex);
        functions.put("tier_destinations_cumulative", NerospaceGameTests::testTierDestinationsCumulative);
        functions.put("glacira_dimension_loads", NerospaceGameTests::testGlaciraDimensionLoads);
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

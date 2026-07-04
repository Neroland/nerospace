package za.co.neroland.nerospace.pipe;

import java.util.Comparator;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * A pipe face's item filter — the multi-entry model behind the Advanced Pipe Filter
 * (issue #25: multiple items per pipe filter). Immutable; stored per face on the
 * {@link UniversalPipeBlockEntity} and as the {@code ADVANCED_FILTER} data component on the
 * Advanced Pipe Filter item (so a configured filter is a shareable template).
 *
 * <ul>
 *   <li>Up to {@link #MAX_ENTRIES} {@link FilterEntry} ghosts; an entry matches by exact item or
 *       by one of the ghost item's tags (see {@link FilterEntry#tagIndex}).</li>
 *   <li>{@code blacklist} flips the sense: pass everything <b>except</b> the entries.</li>
 *   <li>{@code matchComponents} controls whether item-mode entries require identical data
 *       components (the pre-existing single-item filter semantics) or item-only matching.</li>
 *   <li>A filter with no non-empty entries passes everything — in <b>both</b> whitelist and
 *       blacklist mode (symmetric pass-all), matching the old "no filter" behaviour.</li>
 * </ul>
 *
 * <p>The basic Pipe Filter's single-item behaviour is expressed as {@link #ofItem}: a one-entry
 * whitelist with component matching — bit-for-bit the legacy semantics, so
 * {@link UniversalPipeBlockEntity} migrates old {@code Filter0..5} NBT through it.</p>
 */
public record FaceFilter(List<FilterEntry> entries, boolean blacklist, boolean matchComponents) {

    public static final int MAX_ENTRIES = 9;

    /** The unfiltered face: no entries, whitelist, exact components — passes everything. */
    public static final FaceFilter EMPTY = new FaceFilter(List.of(), false, true);

    public static final Codec<FaceFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FilterEntry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(FaceFilter::entries),
            Codec.BOOL.optionalFieldOf("blacklist", false).forGetter(FaceFilter::blacklist),
            Codec.BOOL.optionalFieldOf("match_components", true).forGetter(FaceFilter::matchComponents)
    ).apply(instance, FaceFilter::new));

    /** The legacy single-item filter: one exact-match whitelist entry (EMPTY stack → pass-all). */
    public static FaceFilter ofItem(ItemStack single) {
        if (single == null || single.isEmpty()) {
            return EMPTY;
        }
        return new FaceFilter(List.of(new FilterEntry(single.copyWithCount(1), FilterEntry.MODE_ITEM)),
                false, true);
    }

    /**
     * The effective filter of a physically contained filter item (Configurator GUI face slots):
     * an Advanced Pipe Filter contributes its configured {@code ADVANCED_FILTER} component, a basic
     * Pipe Filter its single {@code FILTER_ITEM} entry; anything else (or EMPTY) filters nothing.
     */
    public static FaceFilter fromFilterItem(ItemStack filterItem) {
        if (filterItem.isEmpty()) {
            return EMPTY;
        }
        if (filterItem.getItem() instanceof za.co.neroland.nerospace.item.AdvancedPipeFilterItem) {
            return za.co.neroland.nerospace.item.AdvancedPipeFilterItem.configured(filterItem);
        }
        if (filterItem.getItem() instanceof za.co.neroland.nerospace.item.PipeFilterItem) {
            return ofItem(za.co.neroland.nerospace.item.PipeFilterItem.configured(filterItem));
        }
        return EMPTY;
    }

    /**
     * Synthesize the physical filter item equivalent to this config (world migration from the
     * config-only formats): a single exact whitelist entry becomes a configured basic Pipe Filter,
     * anything richer an Advanced Pipe Filter carrying the whole config.
     */
    public ItemStack toFilterItem() {
        if (isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!this.blacklist && this.matchComponents && activeEntryCount() == 1) {
            for (FilterEntry entry : this.entries) {
                if (!entry.ghost().isEmpty() && entry.tagIndex() == FilterEntry.MODE_ITEM) {
                    ItemStack basic = new ItemStack(
                            za.co.neroland.nerospace.registry.ModItems.PIPE_FILTER.get());
                    basic.set(za.co.neroland.nerospace.registry.ModDataComponents.FILTER_ITEM.get(),
                            entry.ghost().copyWithCount(1));
                    return basic;
                }
            }
        }
        ItemStack advanced = new ItemStack(
                za.co.neroland.nerospace.registry.ModItems.ADVANCED_PIPE_FILTER.get());
        advanced.set(za.co.neroland.nerospace.registry.ModDataComponents.ADVANCED_FILTER.get(), this);
        return advanced;
    }

    /** True when no entry restricts anything (the filter passes every stack). */
    public boolean isEmpty() {
        for (FilterEntry entry : this.entries) {
            if (!entry.ghost().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** How many active (non-empty) entries are configured — for chat messages + tooltips. */
    public int activeEntryCount() {
        int n = 0;
        for (FilterEntry entry : this.entries) {
            if (!entry.ghost().isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Whether {@code stack} may pass this face. Empty filters pass everything (both senses). */
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isEmpty()) {
            return true;
        }
        boolean matched = false;
        for (FilterEntry entry : this.entries) {
            if (entry.matches(stack, this.matchComponents)) {
                matched = true;
                break;
            }
        }
        return this.blacklist != matched;
    }

    /**
     * The ghost item's tags in a deterministic order (sorted by tag id), so a synced
     * {@link FilterEntry#tagIndex} resolves to the same tag on the server and the client.
     */
    public static List<TagKey<Item>> sortedTags(ItemStack ghost) {
        if (ghost.isEmpty()) {
            return List.of();
        }
        return ghost.typeHolder().tags()
                .sorted(Comparator.comparing((TagKey<Item> tag) -> tag.location().toString()))
                .toList();
    }

    /**
     * One filter entry: a ghost stack plus a match mode. {@code tagIndex}
     * {@link #MODE_ITEM} (-1) matches the ghost item itself; {@code 0..n} matches the ghost
     * item's n-th tag in {@link FaceFilter#sortedTags} order (e.g. {@code #c:ores} covers a
     * whole family with one entry). An out-of-range index falls back to exact-item matching
     * rather than matching nothing, so datapack tag changes degrade gracefully.
     */
    public record FilterEntry(ItemStack ghost, int tagIndex) {

        /** {@link #tagIndex} value for plain item matching (no tag). */
        public static final int MODE_ITEM = -1;

        public static final FilterEntry EMPTY = new FilterEntry(ItemStack.EMPTY, MODE_ITEM);

        public static final Codec<FilterEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("ghost", ItemStack.EMPTY).forGetter(FilterEntry::ghost),
                Codec.INT.optionalFieldOf("tag_index", MODE_ITEM).forGetter(FilterEntry::tagIndex)
        ).apply(instance, FilterEntry::new));

        /** The tag this entry matches by, or {@code null} for exact-item mode. */
        @Nullable
        public TagKey<Item> resolveTag() {
            if (this.tagIndex < 0 || this.ghost.isEmpty()) {
                return null;
            }
            List<TagKey<Item>> tags = sortedTags(this.ghost);
            return this.tagIndex < tags.size() ? tags.get(this.tagIndex) : null;
        }

        boolean matches(ItemStack stack, boolean matchComponents) {
            if (this.ghost.isEmpty()) {
                return false;
            }
            TagKey<Item> tag = resolveTag();
            if (tag != null) {
                return stack.is(tag);
            }
            return matchComponents
                    ? ItemStack.isSameItemSameComponents(this.ghost, stack)
                    : stack.is(this.ghost.getItem());
        }
    }
}

package za.co.neroland.nerospace.village;

import java.util.ArrayList;
import java.util.List;

/**
 * One culture's trade table as plain data: item ids and counts, no registry objects, so the tables can be
 * compared in unit tests without a game bootstrap. {@link AlienTrades} resolves the rows into vanilla
 * {@code MerchantOffers}. Offers are cumulative: a tier-N trader offers every row whose tier is 1..N.
 *
 * @param rows the offers in display order
 */
public record TradeTable(List<Row> rows) {

    public TradeTable {
        rows = List.copyOf(rows);
    }

    /**
     * One offer: the player pays {@code costA}×{@code countA} (plus {@code costB}×{@code countB} when
     * {@code costB} is not null) and receives {@code result}×{@code resultCount}.
     */
    public record Row(int tier, String costA, int countA, String costB, int countB, String result,
            int resultCount, int maxUses, int xp) {
    }

    /** The rows a tier-{@code tier} trader offers (every row at or below that tier, in order). */
    public List<Row> rowsUpTo(int tier) {
        List<Row> out = new ArrayList<>();
        for (Row row : this.rows) {
            if (row.tier() <= tier) {
                out.add(row);
            }
        }
        return out;
    }

    /** Builder that reads like the 1.6.0 table it replaced. */
    public static final class Builder {
        private final List<Row> rows = new ArrayList<>();
        private int tier = 1;

        /** Rows added after this call unlock at {@code t}. */
        public Builder tier(int t) {
            this.tier = t;
            return this;
        }

        /** Player pays {@code n}×{@code cost}, receives {@code rc}×{@code result}. */
        public Builder buy(String cost, int n, String result, int rc, int maxUses, int xp) {
            this.rows.add(new Row(this.tier, cost, n, null, 0, result, rc, maxUses, xp));
            return this;
        }

        /** Player pays {@code a}×{@code costA} and {@code b}×{@code costB}, receives {@code rc}×{@code result}. */
        public Builder buy2(String costA, int a, String costB, int b, String result, int rc, int maxUses, int xp) {
            this.rows.add(new Row(this.tier, costA, a, costB, b, result, rc, maxUses, xp));
            return this;
        }

        public TradeTable build() {
            return new TradeTable(this.rows);
        }
    }
}

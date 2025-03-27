package com.wabradshaw.bannerbeasts.balancer.results;

import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitMetadata;

public class RankResult {

    public static final String[] CSV_HEADERS = new String[] {
            "Unit", "FullUnit", "Faction", "Class", "CompClass", "Tier", "Cost", "TCR",
            "Average Ratio", "Effective Rank", "Rank Diff"
    };

    private final UnitDescription unit;
    private final double averageRatio;
    private final double effectiveRank;
    private final double rankDiff;

    public RankResult(UnitDescription unit, double averageRatio, double effectiveRank) {
        this.unit = unit;
        this.averageRatio = averageRatio;
        this.effectiveRank = effectiveRank;
        this.rankDiff = effectiveRank - unit.getUnitMetadata().getTargetCombatRanking();
    }

    public UnitDescription getUnit() {
        return unit;
    }

    public double getAverageRatio() {
        return averageRatio;
    }

    public double getEffectiveRank() {
        return effectiveRank;
    }

    public double getRankDiff() {
        return rankDiff;
    }

    public String[] toCsvRow() {
        UnitMetadata meta = unit.getUnitMetadata();

        return new String[] {
            meta.getUnit(), meta.toString(), meta.getFaction(), meta.getUnitClass(), meta.getComparisonClass(),
                String.valueOf(meta.getTier()), String.valueOf(meta.getCost()), String.valueOf(meta.getTargetCombatRanking()),

                String.format("%.2f", this.averageRatio),
                String.format("%.2f", this.effectiveRank),
                String.format("%.2f", this.getRankDiff())
        };
    }
}

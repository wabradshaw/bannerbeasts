package com.wabradshaw.bannerbeasts.balancer.results;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitMetadata;

public class BattleSummary {

    public static final String[] CSV_HEADERS = new String[] {
            "Unit1", "Faction1", "Class1", "CompClass1", "Tier1", "Cost1",
            "Unit2", "Faction2", "Class2", "CompClass2", "Tier2", "Cost2",
            "Any Wins", "Outright Wins", "Win Through Flee", "Ties", "Lose Through Flee", "Outright Losses", "Any Losses", "Total Destructions",
            "AvgRounds", "AvgUnit1Models", "AvgUnit1Hp", "AvgUnit2Models", "AvgUnit2Hp"
    };

    private final int unit1Id;
    private final int unit2Id;
    private final UnitDescription unit1;
    private final UnitDescription unit2;

    private final Map<Outcome, Integer> outcomeCounts;
    private final double avgRounds;
    private final double avgUnit1Hp;
    private final double avgUnit1Models;
    private final double avgUnit2Hp;
    private final double avgUnit2Models;

    public BattleSummary(int unit1Id, int unit2Id, List<BattleResult> results) {
        this.unit1Id = unit1Id;
        this.unit2Id = unit2Id;
        this.unit1 = results.get(0).getUnit1();
        this.unit2 = results.get(0).getUnit2();

        this.outcomeCounts = new EnumMap<>(Outcome.class);

        int total1Hp = 0, total1Models = 0, total2Hp = 0, total2Models = 0, totalRounds = 0;

        for (BattleResult result : results) {
            outcomeCounts.merge(result.getOutcome(), 1, Integer::sum);
            total1Hp += result.getUnit1RemainingHp();
            total1Models += result.getUnit1RemainingModels();
            total2Hp += result.getUnit2RemainingHp();
            total2Models += result.getUnit2RemainingModels();
            totalRounds += result.getRounds();
        }

        int size = results.size();
        this.avgRounds = size > 0 ? (double) totalRounds / size : 0;
        this.avgUnit1Hp = size > 0 ? (double) total1Hp / size : 0;
        this.avgUnit1Models = size > 0 ? (double) total1Models / size : 0;
        this.avgUnit2Hp = size > 0 ? (double) total2Hp / size : 0;
        this.avgUnit2Models = size > 0 ? (double) total2Models / size : 0;        
    }

    public int getUnit1Id() {
        return unit1Id;
    }

    public int getUnit2Id() {
        return unit2Id;
    }

    public Map<Outcome, Integer> getOutcomeCounts() {
        return outcomeCounts;
    }

    public double getAvgUnit1Hp() {
        return avgUnit1Hp;
    }

    public double getAvgUnit1Models() {
        return avgUnit1Models;
    }

    public double getAvgUnit2Hp() {
        return avgUnit2Hp;
    }

    public double getAvgUnit2Models() {
        return avgUnit2Models;
    }

    public String[] toCsvRow() {
        UnitMetadata meta1 = unit1.getUnitMetadata();
        UnitMetadata meta2 = unit2.getUnitMetadata();

        return new String[] {
                meta1.getUnit(), meta1.getFaction(), meta1.getUnitClass(), meta1.getComparisonClass(),
                String.valueOf(meta1.getTier()), String.valueOf(meta1.getCost()),

                meta2.getUnit(), meta2.getFaction(), meta2.getUnitClass(), meta2.getComparisonClass(),
                String.valueOf(meta2.getTier()), String.valueOf(meta2.getCost()),

                String.valueOf(outcomeCounts.getOrDefault(Outcome.WON_WIPEOUT, 0) + outcomeCounts.getOrDefault(Outcome.WON_FLED, 0)),
                String.valueOf(outcomeCounts.getOrDefault(Outcome.WON_WIPEOUT, 0)),
                String.valueOf(outcomeCounts.getOrDefault(Outcome.WON_FLED, 0)),
                String.valueOf(outcomeCounts.getOrDefault(Outcome.TIED, 0)),
                String.valueOf(outcomeCounts.getOrDefault(Outcome.LOST_FLED, 0)),
                String.valueOf(outcomeCounts.getOrDefault(Outcome.LOST_WIPEOUT, 0)),
                String.valueOf(outcomeCounts.getOrDefault(Outcome.LOST_FLED, 0) + outcomeCounts.getOrDefault(Outcome.LOST_WIPEOUT, 0)),
                String.valueOf(outcomeCounts.getOrDefault(Outcome.TOTAL_WIPEOUT, 0)),
                String.format("%.2f", avgRounds),
                String.format("%.2f", avgUnit1Models),
                String.format("%.2f", avgUnit1Hp),
                String.format("%.2f", avgUnit2Models),
                String.format("%.2f", avgUnit2Hp)
        };
    }
}

package com.wabradshaw.bannerbeasts.balancer.results;

import com.wabradshaw.bannerbeasts.balancer.unit.Unit;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitMetadata;

public class BattleResult {
    public static final String[] CSV_HEADERS = new String[]{
        "Unit1", "Full Unit1", "Faction1", "Class1", "CompClass1", "Tier1", "Cost1", "TCR1",
        "Unit2", "Full Unit2", "Faction2", "Class2", "CompClass2", "Tier2", "Cost2", "TCR2",
        "Outcome", "Rounds",
        "Unit1Models", "Unit1Hp", "Unit2Models", "Unit2Hp"
    };

    private final UnitDescription unit1;
    private final UnitDescription unit2;
    private final Outcome outcome;
    private final int rounds;
    private final int unit1RemainingModels;
    private final int unit1RemainingHp;
    private final int unit2RemainingModels;
    private final int unit2RemainingHp;    

    public BattleResult(Unit unit1, Unit unit2, Outcome outcome, int rounds) {
        this.unit1 = unit1.getDescription();
        this.unit2 = unit2.getDescription();        
        this.outcome = outcome;
        this.rounds = rounds;
        this.unit1RemainingModels = unit1.getCurrentModels();
        this.unit1RemainingHp = unit1.getCurrentTotalHp();
        this.unit2RemainingModels = unit2.getCurrentModels();
        this.unit2RemainingHp = unit2.getCurrentTotalHp();
    }

    @Override
    public String toString() {
        return String.format("%s vs %s -> %s in %d rounds | %d models, %d HP to %d models, %d HP",
                unit1.getUnitMetadata().getUnit(), unit2.getUnitMetadata().getUnit(), outcome.name(), rounds,
                unit1RemainingModels, unit1RemainingHp,
                unit2RemainingModels, unit2RemainingHp);
    }

    public String[] toCsvRow() {
        UnitMetadata meta1 = unit1.getUnitMetadata();
        UnitMetadata meta2 = unit2.getUnitMetadata();

        return new String[]{
                meta1.getUnit(), meta1.toString(), meta1.getFaction(), meta1.getUnitClass(), meta1.getComparisonClass(),
                String.valueOf(meta1.getTier()), String.valueOf(meta1.getCost()), String.valueOf(meta1.getTargetCombatRanking()),
                meta2.getUnit(), meta2.toString(), meta2.getFaction(), meta2.getUnitClass(), meta2.getComparisonClass(),
                String.valueOf(meta2.getTier()), String.valueOf(meta2.getCost()), String.valueOf(meta2.getTargetCombatRanking()),
                outcome.name(), String.valueOf(rounds),
                String.valueOf(unit1RemainingModels), String.valueOf(unit1RemainingHp),
                String.valueOf(unit2RemainingModels), String.valueOf(unit2RemainingHp)
        };
    }

    public static String[] getCsvHeaders() {
        return CSV_HEADERS;
    }

    public UnitDescription getUnit1() {
        return unit1;
    }

    public UnitDescription getUnit2() {
        return unit2;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public int getRounds() {
        return rounds;
    }

    public int getUnit1RemainingModels() {
        return unit1RemainingModels;
    }

    public int getUnit1RemainingHp() {
        return unit1RemainingHp;
    }

    public int getUnit2RemainingModels() {
        return unit2RemainingModels;
    }

    public int getUnit2RemainingHp() {
        return unit2RemainingHp;
    }
       
}
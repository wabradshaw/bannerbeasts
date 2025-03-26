package com.wabradshaw.bannerbeasts.balancer.unit;

public class UnitMetadata {
    private final String id;
    private final String name;
    private final String unit;
    private final String faction;
    private final String unitClass;
    private final String comparisonClass;
    private final int tier;
    private final Integer cost;
    private final Double targetCombatRanking;

    public UnitMetadata(String id, String name, String unit, String faction, String unitClass, String comparisonClass,
            int tier, Integer cost, Double targetCombatRanking) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.faction = faction;
        this.unitClass = unitClass;
        this.comparisonClass = comparisonClass;
        this.tier = tier;
        this.cost = cost;
        this.targetCombatRanking = targetCombatRanking;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public String getFaction() {
        return faction;
    }

    public String getUnitClass() {
        return unitClass;
    }

    public String getComparisonClass() {
        return comparisonClass;
    }

    public int getTier() {
        return tier;
    }

    public Integer getCost() {
        return cost;
    }

    public Double getTargetCombatRanking() {
        return targetCombatRanking;
    }

    @Override
    public String toString() {
        return String.format("%s %s - T%d %s %s", id, unit, tier, (cost != null && cost >= 0 ? "[" + cost + "]" : ""), targetCombatRanking);
    }
}
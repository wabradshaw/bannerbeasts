package com.wabradshaw.bannerbeasts.balancer.harness;

import java.util.ArrayList;
import java.util.List;

import com.wabradshaw.bannerbeasts.balancer.battler.AutoBattler;
import com.wabradshaw.bannerbeasts.balancer.results.BattleResult;
import com.wabradshaw.bannerbeasts.balancer.unit.Unit;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;

public class FullHarness implements Harness {

    private final AutoBattler battler = new AutoBattler();
    private final int iterations;

    public FullHarness(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public List<BattleResult> runSimulation(List<UnitDescription> possibleUnits) {
        List<BattleResult> results = new ArrayList<>();

        for (UnitDescription attacker : possibleUnits) {
            for (UnitDescription defender : possibleUnits) {
                if (attacker != defender) {
                    for (int i = 0; i < this.iterations; i++) {
                        Unit unitA = new Unit(attacker);
                        Unit unitB = new Unit(defender);
                        BattleResult result = battler.battle(unitA, unitB);
                        results.add(result);
                    }
                }
            }
        }
        return results;
    }

}

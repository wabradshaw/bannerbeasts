package com.wabradshaw.bannerbeasts.balancer.harness;

import java.util.ArrayList;
import java.util.List;

import com.wabradshaw.bannerbeasts.balancer.battler.AutoBattler;
import com.wabradshaw.bannerbeasts.balancer.results.BattleResult;
import com.wabradshaw.bannerbeasts.balancer.unit.Unit;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;

public class TestHarness implements Harness {

    private final AutoBattler battler = new AutoBattler();

    private final int[] fighters;
    
        private final int iterations;
    
        public TestHarness(int[] fighters, int iterations) {
            this.fighters = fighters;
            this.iterations = iterations;
        }
    
        @Override
        public List<BattleResult> runSimulation(List<UnitDescription> possibleUnits) {
            List<BattleResult> results = new ArrayList<>();
    
            for (int attacker : fighters) {
                for (int defender : fighters) {
                    if (attacker != defender) {
                        for(int i = 0; i < iterations; i++){
                        Unit unitA = new Unit(possibleUnits.get(attacker));
                        Unit unitB = new Unit(possibleUnits.get(defender));
                        BattleResult result = battler.battle(unitA, unitB);
                        results.add(result);
                    }
                }
            }
        }
        return results;
    }
    
    
}

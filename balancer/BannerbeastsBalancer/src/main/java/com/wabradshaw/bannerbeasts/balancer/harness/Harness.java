package com.wabradshaw.bannerbeasts.balancer.harness;

import java.util.List;

import com.wabradshaw.bannerbeasts.balancer.results.BattleResult;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;

public interface Harness {
    
    List<BattleResult> runSimulation(List<UnitDescription> possibleUnits);
    
}

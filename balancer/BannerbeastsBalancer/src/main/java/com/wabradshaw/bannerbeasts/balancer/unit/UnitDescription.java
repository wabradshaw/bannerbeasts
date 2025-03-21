package com.wabradshaw.bannerbeasts.balancer.unit;

import com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues.DynamicInt;

public class UnitDescription {
    
    private static int nextId = 0;
    private final int id;

    private final UnitMetadata unitMetadata;
    private final int startingNumber;
    private final int startingHp;

    private final UnitPowers powers;
 
    // Possibly Dynamic Values
    private final DynamicInt movement;
    private final DynamicInt meleeHit;
    private final DynamicInt rangedHit;
    private final DynamicInt attacks;
    private final DynamicInt block;
    
    public UnitDescription(UnitMetadata unitMetadata, int startingNumber, int startingHp, String powers,
            DynamicInt movement, DynamicInt meleeHit, DynamicInt rangedHit, DynamicInt attacks, DynamicInt block) {
        this.id = nextId;
        nextId ++;
        this.unitMetadata = unitMetadata;
        this.startingNumber = startingNumber;
        this.startingHp = startingHp;
        this.powers = new UnitPowers(powers);
        this.movement = movement;
        this.meleeHit = meleeHit;
        this.rangedHit = rangedHit;
        this.attacks = attacks;
        this.block = block;
    }

    @Override
    public String toString() {
        return String.format("%s : M %s | N %s | HP %s | A %s | H %s | R %s | B %s : %s", unitMetadata.toString(), movement, startingNumber, startingHp, attacks, meleeHit, rangedHit, block, powers);
    }

    public int getId() {
        return this.id;
    }

    public UnitMetadata getUnitMetadata() {
        return unitMetadata;
    }

    public int getStartingNumber() {
        return startingNumber;
    }

    public int getStartingHp() {
        return startingHp;
    }

    public UnitPowers getPowers() {
        return powers;
    }

    public DynamicInt getMovement() {
        return movement;
    }

    public DynamicInt getMeleeHit() {
        return meleeHit;
    }

    public DynamicInt getRangedHit() {
        return rangedHit;
    }

    public DynamicInt getAttacks() {
        return attacks;
    }

    public DynamicInt getBlock() {
        return block;
    }
}

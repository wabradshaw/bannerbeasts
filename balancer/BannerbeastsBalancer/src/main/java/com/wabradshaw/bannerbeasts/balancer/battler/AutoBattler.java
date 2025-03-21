package com.wabradshaw.bannerbeasts.balancer.battler;

import com.wabradshaw.bannerbeasts.balancer.results.BattleResult;
import com.wabradshaw.bannerbeasts.balancer.results.Outcome;
import com.wabradshaw.bannerbeasts.balancer.unit.Unit;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitPowers;

import java.util.Random;

public class AutoBattler {
    private static final int MAX_ROUNDS = 12;
    private static final Random random = new Random();

    public BattleResult battle(Unit unit1, Unit unit2) {
        int rounds = 0;

        UnitPowers unit1Powers = unit1.getDescription().getPowers();
        UnitPowers unit2Powers = unit2.getDescription().getPowers();

        while (unit1.isAlive() && unit2.isAlive() && rounds < MAX_ROUNDS) {
            rounds++;

            // Terrifying pre-checks
            if (unit1Powers.isTerrifying()) {
                if (checkFlee(unit2, 1, 0)) {
                    return new BattleResult(unit1, unit2, Outcome.WON_FLED, rounds);
                }
            }
            if (unit2Powers.isTerrifying()) {
                if (checkFlee(unit1, 1, 0)) {
                    return new BattleResult(unit1, unit2, Outcome.LOST_FLED, rounds);
                }
            }

            // Combat
            int unit1Attacks = unit1.getCurrentModels() * unit1.getDescription().getAttacks().get();
            int unit2Attacks = unit2.getCurrentModels() * unit2.getDescription().getAttacks().get();

            int woundsToUnit1 = 0;
            int woundsToUnit2 = 0;

            int unit1ModelsBefore = unit1.getCurrentModels();
            int unit2ModelsBefore = unit2.getCurrentModels();
            int unit1HpBefore = unit1.getCurrentTotalHp();
            int unit2HpBefore = unit2.getCurrentTotalHp();

            for (int i = 0; i < unit1Attacks; i++) {
                int roll = random.nextInt(6) + 1;
                int wounds = resolveWoundFromHit(unit1, unit2, roll);
                woundsToUnit2 += wounds;
            }

            for (int i = 0; i < unit2Attacks; i++) {
                int roll = random.nextInt(6) + 1;
                int wounds = resolveWoundFromHit(unit2, unit1, roll);
                woundsToUnit1 += wounds;
            }

            // Post Combat - counted towards combat res
            int unit1ModelsLost = unit1ModelsBefore - unit1.getCurrentModels();
            int unit2ModelsLost = unit2ModelsBefore - unit2.getCurrentModels();

            if (unit1Powers.hasBurst() && unit1ModelsLost > 0) {
                woundsToUnit2 += resolveBurst(unit1, unit2, unit1ModelsLost);
            }
            if (unit2Powers.hasBurst() && unit2ModelsLost > 0) {
                woundsToUnit1 += resolveBurst(unit2, unit1, unit2ModelsLost);
            }

            if (unit1Powers.isExplosive()) {
                woundsToUnit1 += unit1.takeWounds(999, 1);
            }
            if (unit2Powers.isExplosive()) {
                woundsToUnit2 += unit2.takeWounds(999, 1);
            }

            if (unit1.isAlive()) {
                int reduction = Math.min(woundsToUnit1, unit1Powers.getSpongeyValue().get());
                unit1.healWounds(reduction, true);
                woundsToUnit1 -= reduction;
            }
            if (unit2.isAlive()) {
                int reduction = Math.min(woundsToUnit2, unit2Powers.getSpongeyValue().get());
                unit2.healWounds(reduction, true);
                woundsToUnit2 -= reduction;
            }

            // Post Combat - Excluded from combat res

            if (unit1.isAlive() && unit1Powers.hasRevenants() && unit2ModelsLost > 0) {
                unit1.healWounds(unit2ModelsLost, true);
            }
            if (unit2.isAlive() && unit2Powers.hasRevenants() && unit1ModelsLost > 0) {
                unit2.healWounds(unit1ModelsLost, true);
            }

            if (unit1.isAlive() && unit1Powers.hasFreshRevenants() && unit2ModelsLost > 0) {
                unit1.healWounds(unit2ModelsLost * unit1.getDescription().getStartingHp(), true);
            }
            
            if (unit2.isAlive() && unit2Powers.hasFreshRevenants() && unit1ModelsLost > 0) {
                unit2.healWounds(unit1ModelsLost * unit2.getDescription().getStartingHp(), true);
            }

            if (unit1Powers.hasBloodRites() && woundsToUnit2 > 0 && unit2.isAlive()) {
                applyBloodRites(unit1, unit2, woundsToUnit2);
                int newModelsLost = unit2ModelsBefore - (unit2ModelsLost + unit2.getCurrentModels());
                if (unit2Powers.hasBurst() && newModelsLost > 0) {
                    resolveBurst(unit2, unit1, newModelsLost);
                }
            }
            if (unit2Powers.hasBloodRites() && woundsToUnit1 > 0 && unit1.isAlive()) {
                applyBloodRites(unit2, unit1, woundsToUnit1);
                int newModelsLost = unit1ModelsBefore - (unit1ModelsLost + unit1.getCurrentModels());
                if (unit1Powers.hasBurst() && newModelsLost > 0) {
                    resolveBurst(unit1, unit2, newModelsLost);
                }
            }

            // Post Combat - Spongey is after all wounds, and can reduce the number of wounds felt in combat
            if (unit1.isAlive()) {
                int reduction = Math.min(woundsToUnit1, unit1Powers.getSpongeyValue().get());
                int maxHealable = unit1HpBefore - unit1.getCurrentTotalHp();
                int actualHeal = Math.min(reduction, maxHealable);
            
                if (actualHeal > 0) {
                    unit1.healWounds(actualHeal, true);
                    woundsToUnit1 = Math.max(0, woundsToUnit1 - actualHeal);
                }
            }
            
            if (unit2.isAlive()) {
                int reduction = Math.min(woundsToUnit2, unit2Powers.getSpongeyValue().get());
                int maxHealable = unit2HpBefore - unit2.getCurrentTotalHp();
                int actualHeal = Math.min(reduction, maxHealable);
            
                if (actualHeal > 0) {
                    unit2.healWounds(actualHeal, true);
                    woundsToUnit2 = Math.max(0, woundsToUnit2 - actualHeal);
                }
            }

            // Combat res flee checks
            if (unit1.isAlive() && unit2.isAlive()) {
                boolean unit1LostCombat = woundsToUnit1 > woundsToUnit2 || unit1Powers.isCowardly();
                boolean unit2LostCombat = woundsToUnit2 > woundsToUnit1 || unit2Powers.isCowardly();
                
                if (unit1LostCombat) {
                    if (unit1Powers.isUnstable()) {
                        unit1.takeWounds(1);
                    } else if (checkFlee(unit1, woundsToUnit1, woundsToUnit2)) {
                        return new BattleResult(unit1, unit2, Outcome.LOST_FLED, rounds);
                    }
                }
                
                if (unit2LostCombat) {
                    if (unit2Powers.isUnstable()) {
                        unit2.takeWounds(1);
                    } else if (checkFlee(unit2, woundsToUnit2, woundsToUnit1)) {
                        return new BattleResult(unit1, unit2, Outcome.WON_FLED, rounds);
                    }
                }
            }

            // Post Combat res stuff
            if (unit1.isAlive() && unit1Powers.hasPlague() && !unit2.getDescription().getUnitMetadata().getFaction().equalsIgnoreCase("Boneborn")) {
                if (random.nextInt(6) + 1 >= 4) {
                    unit2.takeWounds(1);
                }
            }
            
            if (unit2.isAlive() && unit2Powers.hasPlague() && !unit1.getDescription().getUnitMetadata().getFaction().equalsIgnoreCase("Boneborn")) {
                if (random.nextInt(6) + 1 >= 4) {
                    unit1.takeWounds(1);
                }
            }
        }

        return resolveResult(unit1, unit2, rounds);
    }

    private int applyBloodRites(Unit attacker, Unit defender, int attacks) {
        int bonusWoundsDealt = 0;
        for (int i = 0; i < attacks; i++) {
            int roll = random.nextInt(6) + 1;
            int wounds = resolveWoundFromHit(attacker, defender, roll);
            bonusWoundsDealt += wounds;            
        }
        return bonusWoundsDealt;
    }

    private int resolveWoundFromHit(Unit attacker, Unit defender, int roll) {
        UnitPowers attackerPowers = attacker.getDescription().getPowers();
        UnitPowers defenderPowers = defender.getDescription().getPowers();

        int effectiveRoll = roll - defenderPowers.getElusivePenalty().get();

        int hitTarget = attacker.getDescription().getMeleeHit().get();
        int blockTarget = defender.getDescription().getBlock().get() + attackerPowers.getApValue().get();
        int multiwound = attackerPowers.getMultiwound().get();

        if (roll == 1) {
            return 0;
        } else if (attackerPowers.hasPoison() && roll == 6) {
            return defender.takeWounds(multiwound, multiwound); // poison auto-wound
        } else if (roll == 6 || effectiveRoll >= hitTarget) {
            int blockRoll = random.nextInt(6) + 1;
            if (blockRoll >= blockTarget && blockRoll != 1) {
                return 0;
            } else {
                if(roll == 6 && attackerPowers.isLethal()){
                    return defender.takeWounds(multiwound, Math.max(2, multiwound));
                } else {
                    return defender.takeWounds(multiwound, multiwound);
                }
            }
        } else {
            return 0;
        }
    }

    private int resolveBurst(Unit attacker, Unit target, int lostModels) {
        int wounds = 0;
        int blockTarget = target.getDescription().getBlock().get() + 1; // AP1
        int multiwound = attacker.getDescription().getPowers().getMultiwound().get();

        for (int i = 0; i < lostModels; i++) {
            int hitRoll = random.nextInt(6) + 1;
            if (hitRoll >= 4) {
                int blockRoll = random.nextInt(6) + 1;
                if (blockRoll < blockTarget || blockRoll == 1) {
                    wounds += target.takeWounds(1, multiwound);
                }
            }
        }

        return wounds;
    }

    private boolean checkFlee(Unit unit, int woundsTaken, int woundsDealt) {
        UnitPowers powers = unit.getDescription().getPowers();
        if (powers.isUnbreakable())
            return false;
        if (woundsTaken <= woundsDealt)
            return false;

        int currentModels = unit.getCurrentModels();
        int startingModels = unit.getDescription().getStartingNumber();
        int roll = random.nextInt(6) + 1;

        // Apply modifiers (unless it's a natural 6 or 1)
        if (roll != 6 && powers.isChaff()) {
            roll--;
        }
        if (roll != 1 && powers.isStalwart()) {
            roll++;
        }

        if (currentModels <= startingModels / 4) {
            return roll < 6;
        } else if (currentModels <= startingModels / 2) {
            return roll < 4;
        } else {
            return roll < 2;
        }
    }

    private BattleResult resolveResult(Unit unit1, Unit unit2, int rounds) {
        boolean u1Alive = unit1.isAlive();
        boolean u2Alive = unit2.isAlive();

        Outcome outcome;
        if (!u1Alive && !u2Alive) {
            outcome = Outcome.TOTAL_WIPEOUT;
        } else if (u1Alive && !u2Alive) {
            outcome = Outcome.WON_WIPEOUT;
        } else if (!u1Alive && u2Alive) {
            outcome = Outcome.LOST_WIPEOUT;
        } else {
            outcome = Outcome.TIED;
        }

        return new BattleResult(unit1, unit2, outcome, rounds);
    }
}

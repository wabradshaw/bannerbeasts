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

        applySilences(unit1, unit2);

        UnitPowers unit1Powers = unit1.powers();
        UnitPowers unit2Powers = unit2.powers();

        applyAdaptive(unit1, 2 + random.nextInt(4));
        applyAdaptive(unit2, 2 + random.nextInt(4));

        while (unit1.isAlive() && unit2.isAlive() && rounds < MAX_ROUNDS) {
            rounds++;

            if (resolveTerrifying(unit1, unit2))
                return new BattleResult(unit1, unit2, Outcome.WON_FLED, rounds);
            if (resolveTerrifying(unit2, unit1))
                return new BattleResult(unit1, unit2, Outcome.LOST_FLED, rounds);

            applyAdaptive(unit1, 1);
            applyAdaptive(unit2, 1);

            int unit1HpBefore = unit1.getCurrentTotalHp();
            int unit2HpBefore = unit2.getCurrentTotalHp();
            int unit1ModelsBefore = unit1.getCurrentModels();
            int unit2ModelsBefore = unit2.getCurrentModels();

            int unit1AttacksFirst = calculateFirstAttack(unit1, unit2);

            int woundsToUnit1 = 0;
            int woundsToUnit2 = 0;

            int unit1Attacks;
            int unit2Attacks;

            if (unit1AttacksFirst == 0) {
                unit1Attacks = calculateTotalAttacks(unit1, unit2);
                unit2Attacks = calculateTotalAttacks(unit2, unit1);
                woundsToUnit1 = performAttacks(unit2, unit1, unit2Attacks);
                woundsToUnit2 = performAttacks(unit1, unit2, unit1Attacks);
            } else if (unit1AttacksFirst > 0) {
                unit1Attacks = calculateTotalAttacks(unit1, unit2);
                woundsToUnit2 = performAttacks(unit1, unit2, unit1Attacks);

                unit2Attacks = calculateTotalAttacks(unit2, unit1);
                woundsToUnit1 = performAttacks(unit2, unit1, unit2Attacks);
            } else {
                unit2Attacks = calculateTotalAttacks(unit2, unit1);
                woundsToUnit1 = performAttacks(unit2, unit1, unit2Attacks);

                unit1Attacks = calculateTotalAttacks(unit1, unit2);
                woundsToUnit2 = performAttacks(unit1, unit2, unit1Attacks);
            }

            if (rounds % 2 == 0) {
                applyVirulent(unit1Powers, unit2, woundsToUnit2);
            }
            if (rounds % 2 == 1) {
                applyVirulent(unit2Powers, unit1, woundsToUnit1);
            }

            woundsToUnit1 += applyPetrify(unit1, unit2, unit2Attacks);
            woundsToUnit2 += applyPetrify(unit2, unit1, unit1Attacks);

            woundsToUnit2 += applyBurst(unit1, unit2, unit1ModelsBefore);
            woundsToUnit1 += applyBurst(unit2, unit1, unit2ModelsBefore);

            woundsToUnit1 += applyExplosive(unit1);
            woundsToUnit2 += applyExplosive(unit2);

            applyRevenants(unit1, unit2, unit2ModelsBefore);
            applyRevenants(unit2, unit1, unit1ModelsBefore);
            applyArchrevenants(unit1, unit2, woundsToUnit2);
            applyArchrevenants(unit2, unit1, woundsToUnit1);

            applyLitFuses(unit1, unit2);
            applyLitFuses(unit2, unit1);

            woundsToUnit2 += applyBloodRitesAndPostBurst(unit1, unit2, woundsToUnit2, unit2ModelsBefore);
            woundsToUnit1 += applyBloodRitesAndPostBurst(unit2, unit1, woundsToUnit1, unit1ModelsBefore);

            woundsToUnit1 = applySpongey(unit1, unit1Powers, unit1HpBefore, woundsToUnit1);
            woundsToUnit2 = applySpongey(unit2, unit2Powers, unit2HpBefore, woundsToUnit2);

            if (unit1.isAlive() && unit2.isAlive()) {
                if (wretchedPoisonFlight(unit1, unit2Powers)) {
                    return new BattleResult(unit1, unit2, Outcome.LOST_FLED, rounds);
                }
                if (wretchedPoisonFlight(unit2, unit1Powers)) {
                    return new BattleResult(unit2, unit1, Outcome.LOST_FLED, rounds);
                }

                if (resolveFlee(unit1, unit1Powers, unit2Powers, woundsToUnit1, woundsToUnit2)) {
                    return new BattleResult(unit1, unit2, Outcome.LOST_FLED, rounds);
                }
                if (resolveFlee(unit2, unit2Powers, unit1Powers, woundsToUnit2, woundsToUnit1)) {
                    return new BattleResult(unit1, unit2, Outcome.WON_FLED, rounds);
                }
            }

            applyPlague(unit1, unit2);
            applyPlague(unit2, unit1);

            applyVirulentDamage(unit1);
            applyVirulentDamage(unit2);

            applyRegenerativeClay(unit1);
            applyRegenerativeClay(unit2);

            applyRegeneration(unit1);
            applyRegeneration(unit2);
        }

        return

        resolveResult(unit1, unit2, rounds);
    }

    private void applyAdaptive(Unit unit, int times) {
        if (unit.powers().isAdaptive()) {
            for (int i = 0; i < times; i++) {
                int roll = d6();
                switch (roll) {
                    case 2:
                        unit.grantMovement(1);
                        break;
                    case 3:
                        unit.grantMeleeHit(1);
                        break;
                    case 4:
                        unit.grantBlock(1);
                        break;
                    case 5:
                        unit.grantAttacks(1);
                        break;
                    case 6:
                        unit.grantPoisonous();
                        break;
                }
            }
        }
    }

    private void applySilences(Unit unit1, Unit unit2) {
        boolean silence1 = unit2.powers().isSilencer();
        boolean silence2 = unit1.powers().isSilencer();
        if (silence1) {
            unit1.silence();
        }
        if (silence2) {
            unit2.silence();
        }
    }

    private int calculateFirstAttack(Unit unit1, Unit unit2) {
        UnitPowers unit1Powers = unit1.powers();
        UnitPowers unit2Powers = unit2.powers();

        int result = 0;

        if (unit1Powers.isStrikesFirst()) {
            result++;
        }
        if (unit1Powers.isStrikesLast()) {
            result--;
        }
        if (unit1Powers.hasSpitWebs()) {
            result++;
        }
        if (unit1Powers.hasFlankingAttack() && unit1.getCurrentModels() == unit1.getDescription().getStartingNumber()) {
            result++;
        }
        if (unit2Powers.isStrikesFirst()) {
            result--;
        }
        if (unit2Powers.isStrikesLast()) {
            result++;
        }
        if (unit2Powers.hasSpitWebs()) {
            result--;
        }
        if (unit2Powers.hasFlankingAttack() && unit2.getCurrentModels() == unit2.getDescription().getStartingNumber()) {
            result--;
        }

        return result;
    }

    private int calculateTotalAttacks(Unit unit, Unit enemy) {
        int baseAttacks = unit.getAttacks();
        UnitPowers powers = unit.powers();

        int maxHp = unit.getDescription().getStartingHp() * unit.getCurrentModels();
        boolean isWounded = unit.getCurrentTotalHp() < maxHp;

        if (powers.hasBloodlust() && isWounded) {
            baseAttacks += 1;
        }

        if (powers.hasKillingBlow()) {
            if (enemy.getDescription().getStartingHp() > 2 && enemy.getCurrentTotalHp() > 2) {
                baseAttacks -= 2;
            } else if (enemy.getDescription().getStartingHp() > 1 && enemy.getCurrentTotalHp() > 1) {
                baseAttacks -= 1;
            }
        }

        if (powers.hasOffhand()) {
            baseAttacks *= 2;
        }
        return unit.getCurrentModels() * baseAttacks;
    }

    private int performAttacks(Unit attacker, Unit defender, int attacks) {
        int wounds = 0;
        for (int i = 0; i < attacks; i++) {
            int roll = d6();
            wounds += resolveWoundFromHit(attacker, defender, roll);
        }
        return wounds;
    }

    private boolean isCavalrybaneEffective(Unit source, Unit target) {
        return source.powers().hasCavalrybane()
                && target.getMovement() >= 2;
    }

    private int applyBurst(Unit source, Unit target, int modelsBefore) {
        int modelsLost = modelsBefore - source.getCurrentModels();
        if (source.powers().hasBurst() && modelsLost > 0) {
            return resolveBurst(source, target, modelsLost);
        }
        return 0;
    }

    private int applyExplosive(Unit unit) {
        if (unit.powers().isExplosive()) {
            return unit.takeWounds(999, 1);
        }
        return 0;
    }

    private void applyRevenants(Unit self, Unit enemy, int enemyModelsBefore) {
        UnitPowers powers = self.powers();
        int modelsLost = enemyModelsBefore - enemy.getCurrentModels();

        if (!self.isAlive())
            return;

        if (powers.hasRevenants() && modelsLost > 0) {
            self.healWounds(modelsLost, true);
        } else if (powers.hasFreshRevenants() && modelsLost > 0) {
            int hp = modelsLost * self.getDescription().getStartingHp();
            self.healWounds(hp, true);
        }
    }

    private void applyArchrevenants(Unit self, Unit enemy, int woundsDealt) {
        UnitPowers powers = self.powers();

        if (!self.isAlive())
            return;

        if (powers.hasArchrevenant()) {
            self.healWounds(woundsDealt, true);
        }
    }

    private void applyLitFuses(Unit source, Unit target) {
        if (source.isAlive() || !source.powers().hasLitFuses() || !target.isAlive())
            return;

        int attacks = d6();

        for (int i = 0; i < attacks; i++) {
            int hitRoll = d6();
            if (hitRoll >= 4) {
                resolveWoundFromHit(source, target, hitRoll, 2);
            }
        }
    }

    private int applyBloodRitesAndPostBurst(Unit attacker, Unit defender, int woundsDealt, int defenderModelsBefore) {
        if (!attacker.isAlive() || !defender.isAlive())
            return 0;

        if (attacker.powers().hasBloodRites()) {
            int additionalWounds = applyBloodRites(attacker, defender, woundsDealt);

            int newModelsLost = defenderModelsBefore - (defender.getCurrentModels() + additionalWounds);
            if (defender.powers().hasBurst() && newModelsLost > 0) {
                resolveBurst(defender, attacker, newModelsLost);
            }
        }
        return 0;
    }

    private int applySpongey(Unit unit, UnitPowers powers, int hpBefore, int woundsTaken) {
        if (!unit.isAlive())
            return woundsTaken;

        int reduction = Math.min(woundsTaken, powers.getSpongeyValue().get());
        int maxHealable = hpBefore - unit.getCurrentTotalHp();
        int actualHeal = Math.min(reduction, maxHealable);

        if (actualHeal > 0) {
            unit.healWounds(actualHeal, true);
            woundsTaken = Math.max(0, woundsTaken - actualHeal);
        }

        return woundsTaken;
    }

    private boolean wretchedPoisonFlight(Unit fleeCandidate, UnitPowers fearCauser) {
        if (fleeCandidate.hasPoisonWound()) {
            fleeCandidate.resetPoisonWound();
            if (fearCauser.hasWretchedPoisons()) {
                return checkFlee(fleeCandidate, fearCauser);
            }
        }
        return false;
    }

    private boolean resolveFlee(Unit unit, UnitPowers powers, UnitPowers attackerPowers, int woundsTaken,
            int woundsDealt) {
        if (!unit.isAlive())
            return false;

        int bonus = powers.hasGeneralsBanner() ? 1 : 0;
        int penalty = attackerPowers.hasGeneralsBanner() ? 1 : 0;
        int score = (woundsDealt + bonus) - (woundsTaken + penalty);

        if (score >= 0 && !powers.isCowardly())
            return false;

        if (powers.isUnstable()) {
            if (powers.hasPracticedNecromancy() && score == 1) {
                return false;
            }
            unit.takeWounds(1);
            return false;
        }

        return checkFlee(unit, attackerPowers);
    }

    private void applyPlague(Unit source, Unit target) {
        if (!source.isAlive() || !target.isAlive())
            return;
        if (!source.powers().hasPlague())
            return;
        if (target.getDescription().getUnitMetadata().getFaction().equalsIgnoreCase("Boneborn"))
            return;

        if (d6() >= 4) {
            target.takeWounds(1);
        }
    }

    private void applyVirulent(UnitPowers attacker, Unit defender, int wounds) {
        if (wounds > 0) {
            defender.applyPoison(attacker.getVirulentThreshold().get());
        }
    }

    private void applyVirulentDamage(Unit unit) {
        int poisonThreshold = unit.getPoisoned();
        if (d6() >= poisonThreshold) {
            unit.takeWounds(1);
        }
    }

    private int applyPetrify(Unit defender, Unit attacker, int attacks) {
        int wounds = 0;
        if (defender.powers().isPetrifying()) {
            for (int i = 0; i < attacks; i++) {
                if (d6() == 6) {
                    wounds += resolveWoundFromHit(defender, attacker, 6, 99);
                }
            }
        }
        return wounds;
    }

    private void applyRegenerativeClay(Unit unit) {
        if (!unit.isAlive())
            return;
        if (!unit.powers().hasRegenerativeClay())
            return;

        unit.healWounds(999, false); // Heals living models only
    }

    private void applyRegeneration(Unit unit) {
        if (!unit.isAlive())
            return;
        unit.healWounds(unit.powers().getRegeneration().get(), true);
    }

    private boolean resolveTerrifying(Unit source, Unit target) {
        return source.powers().isTerrifying()
                && checkFlee(target, source.powers());
    }

    private int applyBloodRites(Unit attacker, Unit defender, int attacks) {
        int bonusWoundsDealt = 0;
        for (int i = 0; i < attacks; i++) {
            bonusWoundsDealt += resolveWoundFromHit(attacker, defender, d6());
        }
        return bonusWoundsDealt;
    }

    private int d6() {
        return random.nextInt(6) + 1;
    }

    private int resolveWoundFromHit(Unit attacker, Unit defender, int roll) {
        return resolveWoundFromHit(attacker, defender, roll, attacker.powers().getApValue().get());
    }

    private int resolveWoundFromHit(Unit attacker, Unit defender, int roll, int ap) {
        UnitPowers attackerPowers = attacker.powers();
        UnitPowers defenderPowers = defender.powers();

        int effectiveRoll = roll - defenderPowers.getElusivePenalty().get();
        int hitTarget = attacker.getMeleeHit();
        int rawBlock = defender.getBlock() + attackerPowers.getApValue().get() + ap
                - defenderPowers.getBonusBlock().get();
        int blockTarget = rawBlock;

        if (defenderPowers.isAirborne()) {
            hitTarget++;
        }
        if (attackerPowers.isUnwieldy()) {
            hitTarget++;
        }
        if (isCavalrybaneEffective(attacker, defender)) {
            hitTarget--;
            blockTarget++; // Effectively +1 AP.
        }
        if (isCavalrybaneEffective(defender, attacker)) {
            blockTarget--;
        }

        if (attackerPowers.hasArmourbane() && rawBlock >= 5) {
            hitTarget--;
        }

        if (defenderPowers.hasUncannyProtection()) {
            blockTarget = Math.min(5, blockTarget);
        }

        if (defenderPowers.isEthereal()) {
            blockTarget = rawBlock;
        }

        int multiwound = attackerPowers.getMultiwound().get();
        if (roll == 6 && attackerPowers.isLethal()) {
            multiwound = Math.max(2, multiwound);
        }
        if (attackerPowers.hasKillingBlow()) {
            multiwound += 2;
        }

        if (roll == 1)
            return 0;

        if (attackerPowers.hasPoison() && roll == 6) {
            defender.setPoisonWound();
            return defender.takeWounds(multiwound, multiwound);
        }

        if (defenderPowers.isEvasive() && roll != 6) {
            return 0;
        }

        if (attackerPowers.hasAggressivePoison() && roll >= 5 && effectiveRoll >= hitTarget) {
            defender.setPoisonWound();
            return defender.takeWounds(multiwound, multiwound);
        }

        if (roll == 6 || effectiveRoll >= hitTarget) {
            int blockRoll = d6();
            boolean wasBlocked = blockRoll >= blockTarget && blockRoll != 1;

            if (wasBlocked) {
                if (defenderPowers.hasVengeance()) {
                    int change = -attacker.takeWounds(1);
                    if (defenderPowers.hasRetribution()) {
                        change = 2 * change;
                    }
                    ;
                    if (!defenderPowers.hasProtectiveMarkings()) {
                        change += defender.takeWounds(1);
                    }

                    return change;
                } else {
                    return 0;
                }
            }

            int damage = attackerPowers.hasCleave() ? 2 : 1;
            return defender.takeWounds(damage, multiwound);

        }

        return 0;
    }

    private int resolveBurst(Unit attacker, Unit target, int lostModels) {
        int wounds = 0;

        for (int i = 0; i < lostModels; i++) {
            int hitRoll = d6();
            if (hitRoll >= 4) {
                resolveWoundFromHit(attacker, target, hitRoll, 1);
            }
        }
        return wounds;
    }

    private boolean checkFlee(Unit unit, UnitPowers attackerPowers) {
        UnitPowers powers = unit.powers();
        if (powers.isUnbreakable())
            return false;

        if (powers.isDisorganised()) {
            int current = unit.getCurrentModels();
            int starting = unit.getDescription().getStartingNumber();
            if (current <= starting / 2) {
                return true; // auto-fails fear check
            }
        }

        int currentModels = unit.getCurrentModels();
        int startingModels = unit.getDescription().getStartingNumber();
        int roll = d6();

        if (roll != 6 && powers.isChaff())
            roll--;
        if (roll != 1 && powers.isStalwart())
            roll++;
        if (roll != 6 && attackerPowers.hasFearfulPresence()) {
            roll--;
        }

        if (currentModels <= startingModels / 4)
            return roll < 6;
        else if (currentModels <= startingModels / 2)
            return roll < 4;
        else
            return roll < 2;
    }

    private BattleResult resolveResult(Unit unit1, Unit unit2, int rounds) {
        boolean u1Alive = unit1.isAlive();
        boolean u2Alive = unit2.isAlive();

        Outcome outcome;
        if (!u1Alive && !u2Alive)
            outcome = Outcome.TOTAL_WIPEOUT;
        else if (u1Alive && !u2Alive)
            outcome = Outcome.WON_WIPEOUT;
        else if (!u1Alive && u2Alive)
            outcome = Outcome.LOST_WIPEOUT;
        else
            outcome = Outcome.TIED;

        return new BattleResult(unit1, unit2, outcome, rounds);
    }
}

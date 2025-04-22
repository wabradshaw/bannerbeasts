package com.wabradshaw.bannerbeasts.balancer.unit;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues.DynamicInt;
import com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues.DynamicIntFactory;
import com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues.StaticInt;

public class UnitPowers {

    public static Set<String> unhandledPowers = new LinkedHashSet<>();

    public static Set<String> ignoredPowers = Stream.of(
            "Random mover", "Eager to Fight", "Eager to Fight.", "Manoeuvrable", "Chariot", "Giant Powder Keg",
            "Unprotected", "Fire on the Move", "Multishot", "War Machine", "The Big Guns", "Fast", "Razorstar",
            "Friendly Fire", "Rabid", "Slow", "Flamethrower", "\"Jetpacks\"", "Smoke Bombs", "Sacrifice",
            "Call the Swarm", "Aura of Power", "Trojan Rats", "Push", "Thieves", "Mighty Cleave", "Well Prepared",
            "Rock Lobber", "Raise the Dead", "Stabalizer", "Bone Spurs", "Web Traps", "Ambush", "Tactical Retreat",
            "Jumping", "Shifting", "Flexible", "Cunning Disguises", "Cocoon Cannon", "Spitter Cannon", "Web Layer",
            "Cloaked", "Fragile Spikes", "Relentless", "Circle of Spikes", "Blood Dance", "About Face", "Quill Spray",
            "Flanking Ambush", "Battlefield Salves", "Bloodspells", "Revels", "Prepared Spell", "Mechanics", "Entrench",
            "Meatshields", "Prepared Equipment", "Prepared Weapons", "Equippable", "Long Range", "Construct",
            "Four Arms", "Trample", "Overwatch", "Prepared Ranged Weapon", "Lift and Shift", "Primed",
            "Reloading", "Bombard", "Hookshot", "Surpression Cannon", "Detachment", "Self Sufficient Bows",
            "Shieldwall", "Pike Square", "Cannons", "Foe Piercing", "Storm Mortar", "Organ Gun", "Regiment", "Flying",
            "Breath Weapons", "Master of Munitions", "Powder Keg", "Chaoscaller", "Miscasts", "Mage", "Broadside",
            "Dose Up", "Retreat", "Ring of Rats", "Tricksie Wizards", "Sacrificial Altar", "Glorious Sacrifice",
            "Occult Sacrifice", "Terrifying Sacrifice", "Cheese it", "Packrat", "Filthy Looter", "Corpsefinder",
            "Deceptive Shots", "Volley Fire", "Pull Strings", "Another's Bidding", "Center of the Web", "Cauldron",
            "Aggressive Flanking", "Master of Movement", "Buck Wildly", "Battlefield Prep", "Sensibly Entrenched",
            "Landbreaker", "Coppersmith's Upgrade", "Custom Tools", "Riflemaker", "Bomber", "Firing platform",
            "Officer", "General's Spirit", "Old Knowledge", "Natural Breath Weapon", "Spotter", "Flap", "Infuse",
            "Silencing Shots", "Permanent Grafts", "Innovator", "Appeal to the Masses", "Crack the Whip",
            "Widespread Carnage", "Beasts Unleashed")
            .collect(Collectors.toCollection(HashSet::new));

    private final Set<String> mechanicalPowers;
    private final Set<String> unhandledUnitPowers;

    private boolean strikesFirst = false;
    private boolean strikesLast = false;

    private boolean poison = false;
    private boolean lethal = false;
    private boolean uncannyProtection = false;
    private boolean cleave = false;

    private boolean chaff = false;
    private boolean stalwart = false;
    private boolean unbreakable = false;
    private boolean cowards = false;

    private boolean explosive = false;
    private boolean burst = false;
    private boolean litFuses = false;

    private boolean flankingAttack = false;

    private boolean unstable = false;
    private boolean revenants = false;
    private boolean freshRevenants = false;
    private boolean terrifying = false;
    private boolean bloodRites = false;
    private boolean plague = false;
    private boolean practicedNecromancy = false;
    private boolean ethereal = false;
    private boolean archrevenant = false;

    private boolean spitWebs = false;
    private boolean aggressivePoison = false;
    private boolean evasive = false;
    private boolean wretchedPoisons = false;

    private boolean vengeance = false;
    private boolean bloodlust = false;
    private boolean retribution = false;
    private boolean protectiveMarkings = false;
    private boolean killingBlow = false;

    private boolean regenerativeClay = false;
    private boolean airborne = false;

    private boolean disorganised = false;
    private boolean cavalrybane = false;
    private boolean fearfulPresence = false;
    private boolean generalsBanner = false;

    private boolean petrifying = false;
    private boolean silencer = false;
    private boolean adaptive = false;

    private DynamicInt apValue = new StaticInt(0);
    private DynamicInt impactHits = new StaticInt(0);
    private DynamicInt multiwound = new StaticInt(1);
    private DynamicInt elusive = new StaticInt(0);
    private DynamicInt spongey = new StaticInt(0);
    private DynamicInt virulentThreshold = new StaticInt(7);
    private DynamicInt regeneration = new StaticInt(0);

    private boolean offhand = false;
    private boolean unwieldy = false;
    private boolean armourbane = false;
    private DynamicInt bonusBlock = new StaticInt(0);

    public UnitPowers(String powers) {
        this.mechanicalPowers = new LinkedHashSet<>();
        this.unhandledUnitPowers = new LinkedHashSet<>();

        if (powers != null && !powers.isBlank()) {
            Pattern pattern = Pattern.compile("<b>(.*?)</b>");
            Matcher matcher = pattern.matcher(powers);

            while (matcher.find()) {
                String power = matcher.group(1).strip();
                String base = power;
                String value = null;
                DynamicInt parsed = null;

                // Power 3
                // Power 3+
                // Power d3
                // Power 2d3
                // Power 2d3+1
                Matcher varMatcher = Pattern.compile("(.+?)\\s+((\\d*d\\d+(?:[+-]\\d+)?|\\d+))\\+?$").matcher(power);

                if (varMatcher.find()) {
                    base = varMatcher.group(1).trim();
                    value = varMatcher.group(2).trim();
                    try {
                        parsed = DynamicIntFactory.parse(value, 0);
                    } catch (Exception e) {
                        parsed = null;
                    }
                }

                switch (base.toLowerCase()) {
                    case "strikes first":
                        strikesFirst = true;
                        mechanicalPowers.add("Strikes First");
                        break;
                    case "strikes last":
                        strikesLast = true;
                        mechanicalPowers.add("Strikes Last");
                        break;
                    case "poison":
                        poison = true;
                        mechanicalPowers.add("Poison");
                        break;
                    case "lethal":
                        lethal = true;
                        mechanicalPowers.add("Lethal");
                        break;
                    case "uncanny protection":
                        uncannyProtection = true;
                        mechanicalPowers.add("Uncanny Protection");
                        break;
                    case "cleave":
                        cleave = true;
                        mechanicalPowers.add("Cleave");
                        break;
                    case "chaff":
                        chaff = true;
                        mechanicalPowers.add("Chaff");
                        break;
                    case "stalwart":
                        stalwart = true;
                        mechanicalPowers.add("Stalwart");
                        break;
                    case "unbreakable":
                        unbreakable = true;
                        mechanicalPowers.add("Unbreakable");
                        break;
                    case "cowards":
                        cowards = true;
                        mechanicalPowers.add("Cowards");
                        break;
                    case "explosive":
                        explosive = true;
                        mechanicalPowers.add("Explosive");
                        break;
                    case "burst":
                        burst = true;
                        mechanicalPowers.add("Burst");
                        break;
                    case "lit fuses":
                        litFuses = true;
                        mechanicalPowers.add("Lit Fuses");
                        break;
                    case "flanking attack":
                        flankingAttack = true;
                        mechanicalPowers.add("Flanking Attack");
                        break;
                    case "unstable":
                        unstable = true;
                        mechanicalPowers.add("Unstable");
                        break;
                    case "revenants":
                        revenants = true;
                        mechanicalPowers.add("Revenants");
                        break;
                    case "fresh revenants":
                        freshRevenants = true;
                        mechanicalPowers.add("Fresh Revenants");
                        break;
                    case "terrifying":
                        terrifying = true;
                        mechanicalPowers.add("Terrifying");
                        break;
                    case "blood rites":
                        bloodRites = true;
                        mechanicalPowers.add("Blood Rites");
                        break;
                    case "plague":
                        plague = true;
                        mechanicalPowers.add("Plague");
                        break;
                    case "practiced necromancy":
                        practicedNecromancy = true;
                        mechanicalPowers.add("Practiced Necromancy");
                        break;
                    case "ethereal":
                        ethereal = true;
                        mechanicalPowers.add("Ethereal");
                        break;
                    case "archrevenant":
                        archrevenant = true;
                        mechanicalPowers.add("Archrevenant");
                        break;
                    case "spit webs":
                        spitWebs = true;
                        mechanicalPowers.add("Spit Webs");
                        break;
                    case "aggressive poison":
                        aggressivePoison = true;
                        mechanicalPowers.add("Aggressive Poison");
                        break;
                    case "evasive":
                        evasive = true;
                        mechanicalPowers.add("Evasive");
                        break;
                    case "wretched poisons":
                        wretchedPoisons = true;
                        mechanicalPowers.add("Wretched Poisons");
                        break;
                    case "vengence":
                        vengeance = true;
                        mechanicalPowers.add("Vengeance");
                        break;
                    case "bloodlust":
                        bloodlust = true;
                        mechanicalPowers.add("Bloodlust");
                        break;
                    case "retribution":
                        retribution = true;
                        mechanicalPowers.add("Retribution");
                        break;
                    case "protective markings":
                        protectiveMarkings = true;
                        mechanicalPowers.add("Protective Markings");
                        break;
                    case "killing blow":
                        killingBlow = true;
                        mechanicalPowers.add("Killing Blow");
                        break;
                    case "regenerative clay":
                        regenerativeClay = true;
                        mechanicalPowers.add("Regenerative Clay");
                        break;
                    case "airborne":
                        airborne = true;
                        mechanicalPowers.add("Airborne");
                        break;
                    case "disorganised":
                        disorganised = true;
                        mechanicalPowers.add("Disorganised");
                        break;
                    case "cavalrybane":
                        cavalrybane = true;
                        mechanicalPowers.add("Cavalrybane");
                        break;
                    case "fearful presence":
                        fearfulPresence = true;
                        mechanicalPowers.add("Fearful Presence");
                        break;
                    case "general's banner":
                    case "generals banner":
                        generalsBanner = true;
                        mechanicalPowers.add("General's Banner");
                        break;
                    case "petrifying gaze":
                        petrifying = true;
                        mechanicalPowers.add("Petrifying Gaze");
                        break;
                    case "silence":
                        silencer = true;
                        mechanicalPowers.add("Silence");
                        break;
                    case "adaptive":
                        adaptive = true;
                        mechanicalPowers.add("Adaptive");
                        break;
                    case "ap":
                        if (parsed != null) {
                            apValue = parsed;
                            mechanicalPowers.add("AP " + parsed);
                        }
                        break;
                    case "impact hits":
                        if (parsed != null) {
                            impactHits = parsed;
                            mechanicalPowers.add("Impact Hits " + parsed);
                        }
                        break;
                    case "multiwound":
                        if (parsed != null) {
                            multiwound = parsed;
                            mechanicalPowers.add("Multiwound " + parsed);
                        }
                        break;
                    case "elusive":
                        if (parsed != null) {
                            elusive = parsed;
                            mechanicalPowers.add("Elusive " + parsed);
                        }
                        break;
                    case "spongey":
                        if (parsed != null) {
                            spongey = parsed;
                            mechanicalPowers.add("Spongey " + parsed);
                        }
                        break;
                    case "virulent":
                        if (parsed != null) {
                            spongey = parsed;
                            mechanicalPowers.add("Virulent " + parsed);
                        }
                        break;
                    case "regeneration":
                        if (parsed != null) {
                            regeneration = parsed;
                            mechanicalPowers.add("Regeneration " + parsed);
                        }
                        break;
                    case "offhand":
                        offhand = true;
                        mechanicalPowers.add("Offhand");
                        break;
                    case "unwieldy":
                        unwieldy = true;
                        mechanicalPowers.add("Unwieldy");
                        break;
                    case "armourbane":
                        armourbane = true;
                        mechanicalPowers.add("Armourbane");
                        break;
                    case "block":
                        if (parsed != null) {
                            bonusBlock = parsed;
                            mechanicalPowers.add("Block " + bonusBlock);
                        }
                        break;
                    default:
                        if (!ignoredPowers.contains(base)) {
                            unhandledPowers.add(power);
                        }
                        unhandledUnitPowers.add(power);
                        break;
                }
            }
        }
    }

    public boolean isStrikesFirst() {
        return strikesFirst;
    }

    public boolean isStrikesLast() {
        return strikesLast;
    }

    public boolean hasPoison() {
        return poison;
    }

    public boolean isLethal() {
        return lethal;
    }

    public boolean hasUncannyProtection() {
        return uncannyProtection;
    }

    public boolean hasCleave() {
        return cleave;
    }

    public boolean isChaff() {
        return chaff;
    }

    public boolean isStalwart() {
        return stalwart;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public boolean isCowardly() {
        return cowards;
    }

    public boolean isExplosive() {
        return explosive;
    }

    public boolean hasBurst() {
        return burst;
    }

    public boolean hasLitFuses() {
        return litFuses;
    }

    public boolean hasFlankingAttack() {
        return flankingAttack;
    }

    public boolean isUnstable() {
        return unstable;
    }

    public boolean hasRevenants() {
        return revenants;
    }

    public boolean hasFreshRevenants() {
        return freshRevenants;
    }

    public boolean isTerrifying() {
        return terrifying;
    }

    public boolean hasBloodRites() {
        return bloodRites;
    }

    public boolean hasPlague() {
        return plague;
    }

    public boolean hasPracticedNecromancy() {
        return practicedNecromancy;
    }

    public boolean isEthereal() {
        return ethereal;
    }

    public boolean hasArchrevenant() {
        return archrevenant;
    }

    public boolean hasSpitWebs() {
        return spitWebs;
    }

    public boolean hasAggressivePoison() {
        return aggressivePoison;
    }

    public boolean isEvasive() {
        return evasive;
    }

    public boolean hasWretchedPoisons() {
        return wretchedPoisons;
    }

    public boolean hasVengeance() {
        return vengeance;
    }

    public boolean hasBloodlust() {
        return bloodlust;
    }

    public boolean hasRetribution() {
        return retribution;
    }

    public boolean hasProtectiveMarkings() {
        return protectiveMarkings;
    }

    public boolean hasKillingBlow() {
        return killingBlow;
    }

    public boolean hasRegenerativeClay() {
        return regenerativeClay;
    }

    public boolean isAirborne() {
        return airborne;
    }

    public boolean isDisorganised() {
        return disorganised;
    }

    public boolean hasCavalrybane() {
        return cavalrybane;
    }

    public boolean hasFearfulPresence() {
        return fearfulPresence;
    }

    public boolean hasGeneralsBanner() {
        return generalsBanner;
    }

    public boolean isPetrifying() {
        return petrifying;
    }

    public boolean isSilencer() {
        return silencer;
    }

    public boolean isAdaptive() {
        return adaptive;
    }

    public DynamicInt getApValue() {
        return apValue;
    }

    public DynamicInt getImpactHits() {
        return impactHits;
    }

    public DynamicInt getMultiwound() {
        return multiwound;
    }

    public DynamicInt getElusivePenalty() {
        return elusive;
    }

    public DynamicInt getSpongeyValue() {
        return spongey;
    }

    public DynamicInt getVirulentThreshold() {
        return virulentThreshold;
    }

    public DynamicInt getRegeneration() {
        return regeneration;
    }

    public boolean hasOffhand() {
        return offhand;
    }

    public boolean isUnwieldy() {
        return unwieldy;
    }

    public boolean hasArmourbane() {
        return armourbane;
    }

    public DynamicInt getBonusBlock() {
        return bonusBlock;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", mechanicalPowers, unhandledUnitPowers);
    }
}
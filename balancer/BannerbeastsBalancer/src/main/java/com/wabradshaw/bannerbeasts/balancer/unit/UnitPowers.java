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
            "Rock Lobber", "Raise the Dead", "Stabalizer", "Bone Spurs")
            .collect(Collectors.toCollection(HashSet::new));

    private final Set<String> mechanicalPowers;
    private final Set<String> unhandledUnitPowers;

    private boolean strikesFirst = false;
    private boolean strikesLast = false;

    private boolean poison = false;
    private boolean lethal = false;

    private boolean chaff = false;
    private boolean stalwart = false;
    private boolean unbreakable = false;
    private boolean cowards = false;

    private boolean explosive = false;
    private boolean burst = false;

    private boolean unstable = false;
    private boolean revenants = false;
    private boolean freshRevenants = false;
    private boolean terrifying = false;
    private boolean bloodRites = false;
    private boolean plague = false;

    private DynamicInt apValue = new StaticInt(0);
    private DynamicInt impactHits = new StaticInt(0);
    private DynamicInt multiwound = new StaticInt(1);
    private DynamicInt elusive = new StaticInt(0);
    private DynamicInt spongey = new StaticInt(0);

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

                Matcher varMatcher = Pattern.compile("(.+?)\\s+(\\d+|\\d*d\\d+([+-]\\d+)?)$").matcher(power);

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
                    case "ap":
                        if (parsed != null) {
                            apValue = parsed;
                            mechanicalPowers.add("AP");
                        }
                        break;
                    case "impact hits":
                        if (parsed != null) {
                            impactHits = parsed;
                            mechanicalPowers.add("Impact Hits");
                        }
                        break;
                    case "multiwound":
                        if (parsed != null) {
                            multiwound = parsed;
                            mechanicalPowers.add("Multiwound");
                        }
                        break;
                    case "elusive":
                        if (parsed != null) {
                            elusive = parsed;
                            mechanicalPowers.add("Elusive");
                        }
                        break;
                    case "spongey":
                        if (parsed != null) {
                            spongey = parsed;
                            mechanicalPowers.add("Spongey");
                        }
                        break;
                    default:
                        if (!ignoredPowers.contains(base)) {
                            unhandledPowers.add(power);
                            unhandledUnitPowers.add(power);
                        }
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

    @Override
    public String toString() {
        return String.format("%s : %s", mechanicalPowers, unhandledUnitPowers);
    }
}
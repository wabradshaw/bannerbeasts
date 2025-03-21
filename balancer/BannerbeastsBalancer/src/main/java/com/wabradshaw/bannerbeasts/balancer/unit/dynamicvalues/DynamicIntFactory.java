package com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicIntFactory {
    public static final String DICE_ROLL_REGEX = "(\\d*)d(\\d+)([+-]\\d+)?";

    public static DynamicInt parse(String input, int base) {
        if (input == null || input.equals("-") || input.isBlank()) {
            return new StaticInt(base);
        } else if (input.matches("\\d+")) {
            return new StaticInt(Integer.parseInt(input));
        } else if (input.matches(DICE_ROLL_REGEX)) {
            // Matches: d6, 2d6, d3+1, 2d6+2
            Matcher matcher = Pattern.compile(DICE_ROLL_REGEX).matcher(input);
            if (matcher.matches()) {
                int count = matcher.group(1).isEmpty() ? 1 : Integer.parseInt(matcher.group(1));
                int sides = Integer.parseInt(matcher.group(2));
                int modifier = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                return new DiceRollInt(count, sides, modifier);
            }
        } else if (input.matches("\\d+x\\d+")) {
            String[] parts = input.split("x");
            return new StaticInt(Integer.parseInt(parts[0]) * Integer.parseInt(parts[1]));
        }
        throw new IllegalArgumentException("Invalid DynamicInt format: " + input);
    }
}

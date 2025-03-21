package com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues;

public class DiceRollInt implements DynamicInt {
    private final int count;
    private final int sides;
    private final int bonus; 

    public DiceRollInt(int sides) {
        this(1, sides, 0);
    }

    public DiceRollInt(int count, int sides, int bonus) {
        this.count = count;
        this.sides = sides;
        this.bonus = bonus;
    }

    @Override
    public int get() {
        int result = 0;
        for(int i = 0; i < count; i++){
            result += (int) (Math.random() * sides) + 1;
        }
        return result + bonus;
    }

    @Override
    public String toString() {
        return count + "d" + sides + (bonus > 0 ? "+" + bonus : "");
    }
}
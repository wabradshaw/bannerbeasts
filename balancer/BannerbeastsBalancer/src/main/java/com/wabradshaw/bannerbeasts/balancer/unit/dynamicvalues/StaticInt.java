package com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues;

public class StaticInt implements DynamicInt {
    private final int value;

    public StaticInt(int value) {
        this.value = value;
    }

    @Override
    public int get() {
        return value;
    }

    @Override
    public String toString() {
        return value > 6 ? "-" : Integer.toString(value);
    }
}
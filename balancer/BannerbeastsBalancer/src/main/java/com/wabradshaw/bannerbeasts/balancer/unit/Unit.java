package com.wabradshaw.bannerbeasts.balancer.unit;

public class Unit {

    private final UnitDescription description;
    private int currentModels;
    private int maxTotalHp;
    private int currentTotalHp;
    private boolean alive;

    private int poisoned = 0;
    private boolean poisonWound = false;

    public Unit(UnitDescription description) {
        this.description = description;
        this.currentModels = description.getStartingNumber();
        this.maxTotalHp = description.getStartingHp() * this.currentModels;
        this.currentTotalHp = this.maxTotalHp;
        this.alive = true;
    }

    public int takeWounds(int count) {
        return this.takeWounds(count, 1);
    }

    public int takeWounds(int count, int multiwound) {
        if (alive) {
            if (this.getDescription().getStartingHp() == 1) {
                multiwound = 1;
            }

            int wounds = Math.min(count * multiwound, this.currentTotalHp);

            this.currentTotalHp -= wounds;
            refreshModelCount();
            this.alive = this.currentModels > 0;

            return wounds;
        } else {
            return 0;
        }
    }

    public void setPoisonWound() {
        this.poisonWound = true;
    }

    public void resetPoisonWound() {
        this.poisonWound = false;
    }

    public void healWounds(int wounds, boolean ressurectsModels){
        if(alive) {
            if(ressurectsModels){
                this.currentTotalHp = Math.min(this.currentTotalHp + wounds, this.maxTotalHp);
                refreshModelCount(); 
            } else {
                int maxHpForModels = this.currentModels * this.description.getStartingHp();
                this.currentTotalHp = Math.min(this.currentTotalHp + wounds, maxHpForModels);
            }     
        }
    }

    public void applyPoison(int threshold){
        this.poisoned = Math.min(this.poisoned, threshold);
    }

    private void refreshModelCount(){
        this.currentModels = (int) Math.ceil((double) this.currentTotalHp / this.description.getStartingHp());
    }

    public UnitDescription getDescription() {
        return description;
    }

    public int getCurrentTotalHp() {
        return currentTotalHp;
    }

    public int getCurrentModels() {
        return currentModels;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public UnitPowers powers() {
        return this.getDescription().getPowers();
    }

    public int getPoisoned() {
        return poisoned;
    }

    public boolean hasPoisonWound() {
        return this.poisonWound;
    }
    
    @Override
    public String toString() {
        return this.description.toString();
    }
}
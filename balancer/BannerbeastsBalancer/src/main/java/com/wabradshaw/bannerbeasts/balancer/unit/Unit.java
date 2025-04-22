package com.wabradshaw.bannerbeasts.balancer.unit;

public class Unit {

    private final UnitDescription description;
    private int currentModels;
    private int maxTotalHp;
    private int currentTotalHp;
    private boolean alive;
    private boolean silenced;

    private int poisoned = 0;
    private boolean poisonWound = false;

    private int movementMod = 0;
    private int meleeHitMod = 0;
    private int attacksMod = 0;
    private int blockMod = 0;
    private boolean poisonMod = false;

    public Unit(UnitDescription description) {
        this.description = description;
        this.currentModels = description.getStartingNumber();
        this.maxTotalHp = description.getStartingHp() * this.currentModels;
        this.currentTotalHp = this.maxTotalHp;
        this.alive = true;
        this.silenced = false;
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
        if(this.silenced){
            return new UnitPowers("");
        } else {
            return this.getDescription().getPowers();
        }
    }

    public int getPoisoned() {
        return poisoned;
    }

    public boolean hasPoisonWound() {
        return this.poisonWound;
    }

    public void silence(){
        this.silenced = true;
    }
    
    public void grantMovement(int amount){
        this.movementMod += amount;
    }

    public void grantAttacks(int amount){
        this.attacksMod += amount;
    }

    public void grantMeleeHit(int amount){
        this.meleeHitMod += amount;
    }

    public void grantBlock(int amount){
        this.blockMod += amount;
    }

    public void grantPoisonous(){
        this.poisonMod = true;
    }

    public int getMovement(){
        return this.description.getMovement().get() + movementMod;
    }

    public int getAttacks(){
        return this.description.getAttacks().get() + attacksMod;
    }

    public int getMeleeHit(){
        return this.description.getMeleeHit().get() - meleeHitMod;
    }

    public int getBlock(){
        return this.description.getBlock().get() - blockMod;
    }

    public boolean isTemporarilyPoisonous(){
        return this.poisonMod;
    }
    
    @Override
    public String toString() {
        return this.description.toString();
    }
}
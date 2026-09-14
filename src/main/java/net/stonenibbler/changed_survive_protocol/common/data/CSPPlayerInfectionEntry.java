package net.stonenibbler.changed_survive_protocol.common.data;

import net.minecraft.nbt.CompoundTag;

public class CSPPlayerInfectionEntry {
    private double infection = 0D;
    private double coverage = 0D;

    private static double clampPercent(double rawPercent) {
        return Math.min(Math.max(rawPercent, 0.0D), 100.0D);
    }

    public CSPPlayerInfectionEntry() {}

    // Convinience constructor used to load directly from a save
    public CSPPlayerInfectionEntry(CompoundTag tag) {
        this.load(tag);
    }
    
    public void setInfectionPercent(double infection) {
        this.infection = clampPercent(infection);
    }

    public void setCoveragePercent(double coverage) {
        this.coverage = clampPercent(coverage);
    }

    // value is current value, in percent
    // added is amount added, in percent
    // total is total WITHOUT added, in percent
    // isAdded is a bool
    private static double percentCalc(double value, double added, double total, boolean isAdded) {
        System.out.println("DEBUG: v=" + value + ", a=" + added + ", t=" + total + ", ia?=" + (isAdded ? "yes" : "no"));
        double currentPrescense = total * (value / 100);
        if (isAdded) {
            currentPrescense += added;
        }
        currentPrescense /= total + added;

        return currentPrescense * 100.0D; // Convert back to percentage
    }

    // Convinience function to update infection percentage
    // totalInfection is the total infection BEFORE adding added infection
    public void updateInfectionPercentage(double addedInfection, double totalInfection, boolean isAddedStrain) {
        infection = percentCalc(infection, addedInfection, totalInfection, isAddedStrain);
    }

    public void updateCoveragePercentage(double addedCoverage, double totalCoverage, boolean isAddedStrain) {
        coverage = percentCalc(coverage, addedCoverage, totalCoverage, isAddedStrain);
    }

    public double getInfectionScore(double coverageBonus, boolean infected) {
        return infected ? infection + (coverage * coverageBonus) : coverage; // Make sure those infecting during coverage stage get at least some of the credit
    }

    public double getInfectionPercent() {
        return infection;
    }
    
    public double getCoveragePercent() {
        return coverage;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("coverage", coverage);
        tag.putDouble("infection", infection);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.coverage = tag.getDouble("coverage");
        this.infection = tag.getDouble("infection");
    }
}

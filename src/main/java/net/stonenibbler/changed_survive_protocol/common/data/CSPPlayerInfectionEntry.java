package net.stonenibbler.changed_survive_protocol.common.data;

import net.minecraft.nbt.CompoundTag;

public class CSPPlayerInfectionEntry {
    private double infection = 0D;
    private double coverage = 0D;

    public CSPPlayerInfectionEntry() {}

    // Convinience constructor used to load directly from a save
    public CSPPlayerInfectionEntry(CompoundTag tag) {
        this.load(tag);
    }
    
    public void setInfectionPercent(double infection) {
        this.infection = infection;
    }

    public void setCoveragePercent(double coverage) {
        this.coverage = coverage;
    }

    // Convinience function to update infection percentage
    // totalInfection is the total infection BEFORE adding added infection
    public void updateInfectionPercentage(double addedInfection, double totalInfection, boolean isAddedStrain) {

    }

    public void updateCoveragePercentage(double addedCoverage, double totalCoverage, boolean isAddedStrain) {

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

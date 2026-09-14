package net.stonenibbler.changed_survive_protocol.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.minecraft.core.BlockPos;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class CSPPlayerData {
    private double infectionPercent;
    private double coverage;
    private boolean infected;
    private Map<String, CSPPlayerInfectionEntry> strainIds = new HashMap<>();
    private int suppressantTicks;
    private double lucidity = 100.0D;
    private boolean lucidityActive;
    private boolean unstableLatex;
    private boolean stabilizedLatex;
    private int unstableLatexTicks;
    private double lucidityDrainMultiplier = 1.0D;
    private String settledStrainId = "";
    private String totemFormId = "";
    private UUID feralSelfUuid;
    private String feralSelfDimension = "";
    private BlockPos feralSelfPos;
    private int collapseCount;

    // Helper function to get absolute strain id
    // Either to convert to the new format, or to set strain
    private CSPPlayerInfectionEntry getAbsoluteStrain() {
        CSPPlayerInfectionEntry entry = new CSPPlayerInfectionEntry();
        entry.setCoveragePercent(100D);
        if (infected) {
            entry.setInfectionPercent(100D);
        }

        return entry;
    }

    public void copyFrom(CSPPlayerData other) {
        infectionPercent = other.infectionPercent;
        coverage = other.coverage;
        infected = other.infected;
        strainIds = new HashMap<>();
        strainIds.putAll(other.strainIds); // Clone now doesn't exist. huh.
        suppressantTicks = other.suppressantTicks;
        lucidity = other.lucidity;
        lucidityActive = other.lucidityActive;
        unstableLatex = other.unstableLatex;
        stabilizedLatex = other.stabilizedLatex;
        unstableLatexTicks = other.unstableLatexTicks;
        lucidityDrainMultiplier = other.lucidityDrainMultiplier;
        settledStrainId = other.settledStrainId;
        totemFormId = other.totemFormId;
        feralSelfUuid = other.feralSelfUuid;
        feralSelfDimension = other.feralSelfDimension;
        feralSelfPos = other.feralSelfPos;
        collapseCount = other.collapseCount;
    }

    public void reset() {
        infectionPercent = 0.0D;
        coverage = 0.0D;
        infected = false;
        strainIds.clear();
        suppressantTicks = 0;
        lucidity = 100.0D;
        lucidityActive = false;
        unstableLatex = false;
        stabilizedLatex = false;
        unstableLatexTicks = 0;
        lucidityDrainMultiplier = 1.0D;
        settledStrainId = "";
        totemFormId = "";
        feralSelfUuid = null;
        feralSelfDimension = "";
        feralSelfPos = null;
        collapseCount = 0;
    }

    private ListTag saveStrainIds() {
        ListTag tag = new ListTag();
        for (HashMap.Entry<String, CSPPlayerInfectionEntry> entry : strainIds.entrySet()) {
            CompoundTag ctag = entry.getValue().save();
            ctag.putString("strainId", entry.getKey());
            
            tag.add(ctag);
        }

        return tag;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("infectionPercent", infectionPercent);
        tag.putDouble("coverage", coverage);
        tag.putBoolean("infected", infected);
        tag.put("strainIds", saveStrainIds());
        tag.putString("strainId", getActiveStrainId()); // For backwards-compatibility, we just place the active strain id here, too.
        tag.putInt("suppressantTicks", suppressantTicks);
        tag.putDouble("lucidity", lucidity);
        tag.putBoolean("lucidityActive", lucidityActive);
        tag.putBoolean("unstableLatex", unstableLatex);
        tag.putBoolean("stabilizedLatex", stabilizedLatex);
        tag.putInt("unstableLatexTicks", unstableLatexTicks);
        tag.putDouble("lucidityDrainMultiplier", lucidityDrainMultiplier);
        tag.putString("settledStrainId", settledStrainId);

        if (!totemFormId.isBlank()) {
            tag.putString("totemFormId", totemFormId);
        }
        if (feralSelfUuid != null) {
            tag.putUUID("feralSelfUuid", feralSelfUuid);
        }
        if (!feralSelfDimension.isBlank()) {
            tag.putString("feralSelfDimension", feralSelfDimension);
        }
        if (feralSelfPos != null) {
            tag.putLong("feralSelfPos", feralSelfPos.asLong());
        }
        tag.putInt("collapseCount", collapseCount);
        return tag;
    }

    private Map<String, CSPPlayerInfectionEntry> convertToNewStrainId(String activeStrainId) {
        Map<String, CSPPlayerInfectionEntry> newMap = new HashMap<>();
        if (activeStrainId == "") {
            return newMap;
        }

        CSPPlayerInfectionEntry entry = getAbsoluteStrain();

        newMap.put(activeStrainId, entry);
        return newMap;
    }

    private Map<String, CSPPlayerInfectionEntry> loadStrainIds(ListTag strainIdTag) {
        Map<String, CSPPlayerInfectionEntry> newMap = new HashMap<>();
        if (strainIdTag.isEmpty()) {
            return newMap;
        }
        
        Iterator<Tag> iter = strainIdTag.iterator();
        while (iter.hasNext()) {
            CompoundTag tag = (CompoundTag) iter.next();
            String strain = tag.getString("strainId");
            CSPPlayerInfectionEntry entry = new CSPPlayerInfectionEntry(tag);

            newMap.put(strain, entry);
        }

        return newMap;
    }

    public void load(CompoundTag tag) {
        infectionPercent = clampPercent(tag.getDouble("infectionPercent"));
        coverage = clampPercent(tag.getDouble("coverage"));
        infected = infectionPercent > 0.0D;
        strainIds = tag.contains("strainIds") ? loadStrainIds(tag.getList("strainIds", CompoundTag.TAG_COMPOUND)) : convertToNewStrainId(tag.getString("strainId"));
        suppressantTicks = Math.max(0, tag.getInt("suppressantTicks"));
        lucidity = tag.contains("lucidity") ? clampPercent(tag.getDouble("lucidity")) : 100.0D;
        lucidityActive = tag.getBoolean("lucidityActive");
        unstableLatex = tag.getBoolean("unstableLatex");
        stabilizedLatex = tag.getBoolean("stabilizedLatex");
        unstableLatexTicks = Math.max(0, tag.getInt("unstableLatexTicks"));
        lucidityDrainMultiplier = tag.contains("lucidityDrainMultiplier") ? Math.max(0.0D, tag.getDouble("lucidityDrainMultiplier")) : 1.0D;
        settledStrainId = tag.getString("settledStrainId");
        totemFormId = tag.getString("totemFormId");
        feralSelfUuid = tag.hasUUID("feralSelfUuid") ? tag.getUUID("feralSelfUuid") : null;
        feralSelfDimension = tag.getString("feralSelfDimension");
        feralSelfPos = tag.contains("feralSelfPos") ? BlockPos.of(tag.getLong("feralSelfPos")) : null;
        collapseCount = Math.max(0, tag.getInt("collapseCount"));
        
    }

    public double getInfectionPercent() {
        return infectionPercent;
    }

    public void setInfectionPercent(double value) {
        infectionPercent = clampPercent(value);
        infected = infectionPercent > 0.0D;
    }

    public void clearInfection() {
        infectionPercent = 0.0D;
        infected = false;
        strainIds.clear();
        suppressantTicks = 0;
    }

    @Deprecated
    public void addInfection(double amount) {
        setInfectionPercent(infectionPercent + amount);
    }

    public void addInfection(double amount, String strainId) {
        double oldTotal = infectionPercent;
        setInfectionPercent(infectionPercent + amount);
        if (strainId == null || strainId.isBlank() || amount == 0) return;

        if (!strainIds.containsKey(strainId)) {
            strainIds.put(strainId, new CSPPlayerInfectionEntry());     
        }

        for (Map.Entry<String, CSPPlayerInfectionEntry> mapEntry : strainIds.entrySet()) {
            CSPPlayerInfectionEntry curEntry = mapEntry.getValue();
            curEntry.updateInfectionPercentage(amount, oldTotal, mapEntry.getKey() == strainId);
            strainIds.put(strainId, curEntry);
        }
    }

    public double getCoverage() {
        return coverage;
    }

    public void setCoverage(double value) {
        coverage = clampPercent(value);
    }

    public void clearCoverage() {
        coverage = 0.0D;
    }

    @Deprecated 
    public void addCoverage(double amount) {
        setCoverage(coverage + amount);
    }

    public void addCoverage(double amount, String strainId) {
        double oldTotal = coverage;
        setCoverage(coverage + amount);
        if (strainId == null || strainId.isBlank() || amount == 0) return;
        
        if (!strainIds.containsKey(strainId)) {
            strainIds.put(strainId, new CSPPlayerInfectionEntry());     
        }

        for (Map.Entry<String, CSPPlayerInfectionEntry> mapEntry : strainIds.entrySet()) {
            CSPPlayerInfectionEntry curEntry = mapEntry.getValue();
            curEntry.updateCoveragePercentage(amount, oldTotal, mapEntry.getKey() == strainId);
            strainIds.put(strainId, curEntry);
        }
    }

    public boolean isInfected() {
        return infected;
    }

    public void setInfected(boolean infected) {
        this.infected = infected;
        if (!infected) {
            infectionPercent = 0.0D;
        } else if (infectionPercent <= 0.0D) {
            infectionPercent = 1.0D;
        }
    }

    public String getActiveStrainId() {
        // Get strain id now gets the active strain id
        String activeStrain = "";
        double highestScore = 0D;
        if (strainIds.isEmpty()) 
            return "";

        for (Map.Entry<String, CSPPlayerInfectionEntry> entry : strainIds.entrySet()) {
            CSPPlayerInfectionEntry infectionEntry = entry.getValue();
            double infectionScore = infectionEntry.getInfectionScore(CSPConfig.COMMON.coverageBonusMultiplier.get(), infected);
            if (infectionScore > highestScore) {
                highestScore = infectionScore;
                activeStrain = entry.getKey();
            }
        }

        return activeStrain;
    }

    @Deprecated 
    public String getStrainId() {
        return this.getActiveStrainId();
    }

    public void setActiveStrainId(String strainId) {
        // Replace the whole tf table with the active strain
        strainIds.clear();
        if (strainId == null)
            return;

        strainIds.put(strainId, getAbsoluteStrain());
    }

    @Deprecated
    public void setStrainId(String strainId) {
        this.setActiveStrainId(strainId);
    }

    public int getSuppressantTicks() {
        return suppressantTicks;
    }

    public void setSuppressantTicks(int suppressantTicks) {
        this.suppressantTicks = Math.max(0, suppressantTicks);
    }

    public boolean tickSuppressant() {
        if (suppressantTicks <= 0) {
            return false;
        }
        suppressantTicks--;
        return true;
    }

    public double getLucidity() {
        return lucidity;
    }

    public void setLucidity(double value) {
        lucidity = clampPercent(value);
    }

    public void addLucidity(double amount) {
        setLucidity(lucidity + amount);
    }

    public boolean isLucidityActive() {
        return lucidityActive;
    }

    public void setLucidityActive(boolean lucidityActive) {
        this.lucidityActive = lucidityActive;
    }

    public boolean isUnstableLatex() {
        return unstableLatex;
    }

    public void setUnstableLatex(boolean unstableLatex) {
        this.unstableLatex = unstableLatex;
    }

    public boolean isStabilizedLatex() {
        return stabilizedLatex;
    }

    public void setStabilizedLatex(boolean stabilizedLatex) {
        this.stabilizedLatex = stabilizedLatex;
    }

    public int getUnstableLatexTicks() {
        return unstableLatexTicks;
    }

    public void setUnstableLatexTicks(int unstableLatexTicks) {
        this.unstableLatexTicks = Math.max(0, unstableLatexTicks);
    }

    public void addUnstableLatexTick() {
        unstableLatexTicks++;
    }

    public double getLucidityDrainMultiplier() {
        return lucidityDrainMultiplier;
    }

    public void setLucidityDrainMultiplier(double lucidityDrainMultiplier) {
        this.lucidityDrainMultiplier = Math.max(0.0D, lucidityDrainMultiplier);
    }

    public String getSettledStrainId() {
        return settledStrainId;
    }

    public void setSettledStrainId(String settledStrainId) {
        this.settledStrainId = settledStrainId == null ? "" : settledStrainId;
    }

    public boolean hasSettledStrain() {
        return !settledStrainId.isBlank();
    }

    public String getTotemFormId() {
        return totemFormId;
    }

    public void setTotemFormId(String totemFormId) {
        this.totemFormId = totemFormId == null ? "" : totemFormId;
    }

    public boolean hasTotemForm() {
        return !totemFormId.isBlank();
    }

    public void clearTotemForm() {
        totemFormId = "";
    }

    public UUID getFeralSelfUuid() {
        return feralSelfUuid;
    }

    public void setFeralSelfUuid(UUID feralSelfUuid) {
        this.feralSelfUuid = feralSelfUuid;
    }

    public String getFeralSelfDimension() {
        return feralSelfDimension;
    }

    public BlockPos getFeralSelfPos() {
        return feralSelfPos;
    }

    public void setFeralSelfLocation(String dimension, BlockPos pos) {
        feralSelfDimension = dimension == null ? "" : dimension;
        feralSelfPos = pos == null ? null : pos.immutable();
    }

    public void clearFeralSelf() {
        feralSelfUuid = null;
        feralSelfDimension = "";
        feralSelfPos = null;
    }

    public int getCollapseCount() {
        return collapseCount;
    }

    public void setCollapseCount(int collapseCount) {
        this.collapseCount = Math.max(0, collapseCount);
    }

    public void incrementCollapseCount() {
        collapseCount++;
    }

    private static double clampPercent(double value) {
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}

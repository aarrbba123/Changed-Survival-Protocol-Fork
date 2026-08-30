package net.stonenibbler.changed_survive_protocol.common.config;

import net.ltxprogrammer.changed.data.RegistryElementPredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Stream;

public final class CSPConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private CSPConfig() {
    }

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static final class Common {
        public final ForgeConfigSpec.DoubleValue waterCoverageWashPerTick;
        public final ForgeConfigSpec.DoubleValue rainCoverageWashPerTick;
        public final ForgeConfigSpec.IntValue passiveCoverageDecayIntervalTicks;
        public final ForgeConfigSpec.DoubleValue passiveCoverageDecayAmount;
        public final ForgeConfigSpec.DoubleValue coverageInfectionThreshold;
        public final ForgeConfigSpec.DoubleValue infectionStartPercent;
        public final ForgeConfigSpec.IntValue infectionGrowthIntervalTicks;
        public final ForgeConfigSpec.DoubleValue infectionGrowthPerInterval;
        public final ForgeConfigSpec.DoubleValue infectionFromExtraCoverageMultiplier;

        public final ForgeConfigSpec.DoubleValue disinfectantWipeCoverageRemoval;
        public final ForgeConfigSpec.DoubleValue disinfectantSprayCoverageRemoval;
        public final ForgeConfigSpec.IntValue inhibitorTicks;
        public final ForgeConfigSpec.DoubleValue inhibitorInfectionRemoval;
        public final ForgeConfigSpec.DoubleValue sampleExtractionDamage;
        public final ForgeConfigSpec.DoubleValue sampleExtractionMinHealth;
        public final ForgeConfigSpec.DoubleValue sampleExtractionInfectionRemoval;
        public final ForgeConfigSpec.DoubleValue cureDoseInfectionRemoval;
        public final ForgeConfigSpec.IntValue cureDoseSuppressantTicks;

        public final ForgeConfigSpec.IntValue microscopeProcessTimeTicks;

        public final ForgeConfigSpec.DoubleValue immediateHazardCoverage;

        public final ForgeConfigSpec.BooleanValue lucidityMechanicsEnabled;
        public final ForgeConfigSpec.IntValue latexNeedIntervalTicks;
        public final ForgeConfigSpec.DoubleValue unstableLucidityDrain;
        public final ForgeConfigSpec.DoubleValue maxUnstableLucidityMultiplier;
        public final ForgeConfigSpec.IntValue unstableTicksForMaxMultiplier;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryPerFoodNutrition;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryPerFoodSaturation;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearLatexSmall;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearLatexMedium;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearLatexLarge;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryAquaticUnderwater;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryFromLatexNestSleep;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryFromAssimilation;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearTransfurredSmall;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearTransfurredMedium;
        public final ForgeConfigSpec.DoubleValue lucidityRecoveryNearTransfurredLarge;
        public final ForgeConfigSpec.IntValue transfurredCheckRadius;
        public final ForgeConfigSpec.DoubleValue stabilizationRequiredLucidity;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> lucidityBlacklistEntityTypes;

        public final ForgeConfigSpec.DoubleValue culturedStrandNestAttunement;
        public final ForgeConfigSpec.DoubleValue culturedStrandAssimilationAttunement;
        public final ForgeConfigSpec.DoubleValue culturedStrandPassiveAttunement;

        public final ForgeConfigSpec.BooleanValue latexMobsAttackDifferentLatexPlayers;
        public final ForgeConfigSpec.BooleanValue changedLatexMobsIgnoreNaturalSpawnLight;
        public final ForgeConfigSpec.DoubleValue changedLatexMobsDaylightSpawnChance;
        public final ForgeConfigSpec.BooleanValue changedLatexMobsDaylightSpawnIncludesAddons;
        public final ForgeConfigSpec.IntValue changedLatexMobsNaturalSpawnLocalCap;
        public final ForgeConfigSpec.IntValue changedLatexMobsNaturalSpawnLocalCapRadius;
        public final ForgeConfigSpec.BooleanValue debugLatexHeartSpawnMessages;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("coverage");
            waterCoverageWashPerTick = builder.comment("Coverage removed per tick while the player is in water.").defineInRange("waterCoverageWashPerTick", 0.15D, 0.0D, 100.0D);
            rainCoverageWashPerTick = builder.comment("Coverage removed per tick while the player is in rain or bubbles.").defineInRange("rainCoverageWashPerTick", 0.1D, 0.0D, 100.0D);
            passiveCoverageDecayIntervalTicks = builder.comment("How often passive coverage decay runs. 20 ticks = 1 second.").defineInRange("passiveCoverageDecayIntervalTicks", 40, 1, 20 * 60 * 10);
            passiveCoverageDecayAmount = builder.comment("Coverage removed on each passive decay tick.").defineInRange("passiveCoverageDecayAmount", 0.05D, 0.0D, 100.0D);
            coverageInfectionThreshold = builder.comment("Coverage required to begin infection. Default is 100: fully coated means infected.").defineInRange("coverageInfectionThreshold", 100.0D, 1.0D, 100.0D);
            infectionStartPercent = builder.comment("Infection percent assigned when coverage reaches the infection threshold.").defineInRange("infectionStartPercent", 1.0D, 1.0D, 100.0D);
            builder.pop();

            builder.push("infection");
            infectionGrowthIntervalTicks = builder.comment("How often infection grows after it has started. 20 ticks = 1 second.").defineInRange("infectionGrowthIntervalTicks", 40, 1, 20 * 60 * 10);
            infectionGrowthPerInterval = builder.comment("Infection gained each growth interval while not suppressed.").defineInRange("infectionGrowthPerInterval", 0.2D, 0.0D, 100.0D);
            infectionFromExtraCoverageMultiplier = builder.comment("When already infected, extra exposure is converted directly into infection using this multiplier.").defineInRange("infectionFromExtraCoverageMultiplier", 0.45D, 0.0D, 100.0D);
            builder.pop();

            builder.push("treatment");
            disinfectantWipeCoverageRemoval = builder.comment("Coverage removed by one disinfectant wipe.").defineInRange("disinfectantWipeCoverageRemoval", 5.0D, 0.0D, 100.0D);
            disinfectantSprayCoverageRemoval = builder.comment("Coverage removed by one disinfectant spray.").defineInRange("disinfectantSprayCoverageRemoval", 10.0D, 0.0D, 100.0D);
            inhibitorTicks = builder.comment("Ticks of infection suppression granted by latex inhibitor. 20 ticks = 1 second.").defineInRange("inhibitorTicks", 6000, 0, 20 * 60 * 60);
            inhibitorInfectionRemoval = builder.comment("Infection removed by latex inhibitor.").defineInRange("inhibitorInfectionRemoval", 3.0D, 0.0D, 100.0D);
            sampleExtractionDamage = builder.comment("Damage dealt by extracting an infected human sample with the sampling scalpel.").defineInRange("sampleExtractionDamage", 6.0D, 0.0D, 100.0D);
            sampleExtractionMinHealth = builder.comment("Sample extraction refuses to reduce the player below this health.").defineInRange("sampleExtractionMinHealth", 2.0D, 0.0D, 100.0D);
            sampleExtractionInfectionRemoval = builder.comment("Infection removed by cutting out a raw latex sample.").defineInRange("sampleExtractionInfectionRemoval", 2.0D, 0.0D, 100.0D);
            cureDoseInfectionRemoval = builder.comment("Infection removed by consuming one matching strand cure dose.").defineInRange("cureDoseInfectionRemoval", 100.0D, 0.0D, 100.0D);
            cureDoseSuppressantTicks = builder.comment("Ticks of infection suppression granted by one matching strand cure dose.").defineInRange("cureDoseSuppressantTicks", 1200, 0, 20 * 60 * 60);
            builder.pop();

            builder.push("machines");
            microscopeProcessTimeTicks = builder.comment("Ticks needed for a microscope to identify one latex sample. 20 ticks = 1 second.").defineInRange("microscopeProcessTimeTicks", 900, 1, 20 * 60 * 60);
            builder.pop();

            builder.push("exposure");
            immediateHazardCoverage = builder.comment("Coverage added by immediate Changed hazards such as syringes, flasks, puddles, and latex food.").defineInRange("immediateHazardCoverage", 15.0D, 0.0D, 100.0D);
            builder.pop();

            builder.push("lucidity");
            lucidityMechanicsEnabled = builder.comment("If false, lucidity mechanics are disabled for all forms.").define("lucidityMechanicsEnabled", true);
            latexNeedIntervalTicks = builder.comment("How often lucidity checks tick. 20 ticks = 1 second.").defineInRange("latexNeedIntervalTicks", 40, 1, 20 * 60 * 10);
            unstableLucidityDrain = builder.comment("Base lucidity drain per interval while unstable.").defineInRange("unstableLucidityDrain", 0.1D, 0.0D, 100.0D);
            maxUnstableLucidityMultiplier = builder.comment("Lucidity drains faster as instability increases. This is the maximum multiplier.").defineInRange("maxUnstableLucidityMultiplier", 7.5D, 1.0D, 100.0D);
            unstableTicksForMaxMultiplier = builder.comment("How many ticks does it take for lucidity to reach the maximum drain multiplier.").defineInRange("unstableTicksForMaxMultiplier", 24000, 1, 20 * 60 * 60 * 24);
            lucidityRecoveryPerFoodNutrition = builder.comment("Lucidity restored when a latex player finishes eating food, per nutrition point.").defineInRange("lucidityRecoveryPerFoodNutrition", 0.4D, 0.0D, 100.0D);
            lucidityRecoveryPerFoodSaturation = builder.comment("Lucidity restored when a latex player finishes eating food, per saturation modifier point.").defineInRange("lucidityRecoveryPerFoodSaturation", 3.0D, 0.0D, 100.0D);
            lucidityRecoveryNearLatexSmall = builder.comment("Lucidity restored per latex need interval near a small amount of friendly latex.").defineInRange("lucidityRecoveryNearLatexSmall", 0.3D, 0.0D, 100.0D);
            lucidityRecoveryNearLatexMedium = builder.comment("Lucidity restored per latex need interval near a moderate amount of friendly latex.").defineInRange("lucidityRecoveryNearLatexMedium", 0.5D, 0.0D, 100.0D);
            lucidityRecoveryNearLatexLarge = builder.comment("Lucidity restored per latex need interval near a dense friendly latex nest.").defineInRange("lucidityRecoveryNearLatexLarge", 0.75D, 0.0D, 100.0D);
            lucidityRecoveryAquaticUnderwater = builder.comment("Lucidity restored per latex need interval while an aquatic latex form is underwater.").defineInRange("lucidityRecoveryAquaticUnderwater", 0.5D, 0.0D, 100.0D);
            lucidityRecoveryFromLatexNestSleep = builder.comment("Lucidity restored when waking after sleeping long enough in a friendly latex nest.").defineInRange("lucidityRecoveryFromLatexNestSleep", 25.0D, 0.0D, 100.0D);
            lucidityRecoveryFromAssimilation = builder.comment("Lucidity restored when a latex player successfully assimilates/transfurs another entity.").defineInRange("lucidityRecoveryFromAssimilation", 20.0D, 0.0D, 100.0D);
            lucidityRecoveryNearTransfurredSmall = builder.comment("Lucidity restored per latex need interval near a small number of friendly transfurred entities").defineInRange("lucidityRecoveryNearTransfurredSmall", 0.3D, 0.0D, 100.0D);
            lucidityRecoveryNearTransfurredMedium = builder.comment("Lucidity restored per latex need interval near a medium number of friendly transfurred entities").defineInRange("lucidityRecoveryNearTransfurredMedium", 0.5D, 0.0D, 100.0D);
            lucidityRecoveryNearTransfurredLarge = builder.comment("Lucidity restored per latex need interval near a large number of friendly transfurred entities").defineInRange("lucidityRecoveryNearTransfurredLarge", 0.75D, 0.0D, 100.0D);
            transfurredCheckRadius = builder.comment("Radius checked for lucidityRecoveryNearTransfurred").defineInRange("transfurredCheckRadius", 20, 1, 256);
            culturedStrandNestAttunement = builder.comment("Attunement added to a carried matching cultured strand when sleeping in a friendly latex nest.").defineInRange("culturedStrandNestAttunement", 40.0D, 0.0D, 100.0D);
            culturedStrandAssimilationAttunement = builder.comment("Attunement added to a carried matching cultured strand after successful assimilation.").defineInRange("culturedStrandAssimilationAttunement", 25.0D, 0.0D, 100.0D);
            culturedStrandPassiveAttunement = builder.comment("Attunement added once per minute to a carried matching cultured strand while lucid near friendly latex or transfurred entities.").defineInRange("culturedStrandPassiveAttunement", 1.0D, 0.0D, 100.0D);
            stabilizationRequiredLucidity = builder.comment("Minimum lucidity required to consume a stabilization dose.").defineInRange("stabilizationRequiredLucidity", 80.0D, 0.0D, 100.0D);
            builder.comment("Blacklist lucidity. Acceptable formats: \"@modid\", \"#tag\", \"modid:entity_id\"");
            lucidityBlacklistEntityTypes = builder.defineList("lucidityBlacklistEntityTypes", List.of("#changed_survive_protocol:lucidity_blacklist"), RegistryElementPredicate::isValidSyntax);
            builder.pop();

            builder.push("misc");
            latexMobsAttackDifferentLatexPlayers = builder.comment("If true, Changed latex mobs can target transfurred players unless they share/friend the same latex type.").define("latexMobsAttackDifferentLatexPlayers", true);
            changedLatexMobsIgnoreNaturalSpawnLight = builder.comment("If true, natural Changed latex mob spawns can ignore the vanilla monster light check, allowing limited daytime or lit-area spawns while keeping biome, ground, difficulty, collision, and local cap rules.").define("changedLatexMobsIgnoreNaturalSpawnLight", false);
            changedLatexMobsDaylightSpawnChance = builder.comment("Chance for a failed natural Changed latex spawn to be retried while ignoring light. Lower values keep daytime spawns rare.").defineInRange("changedLatexMobsDaylightSpawnChance", 0.25D, 0.0D, 1.0D);
            changedLatexMobsDaylightSpawnIncludesAddons = builder.comment("If true, the daylight spawn bypass also affects addon latex entities. Keep false if addon mobs overpopulate broad biome tags.").define("changedLatexMobsDaylightSpawnIncludesAddons", false);
            changedLatexMobsNaturalSpawnLocalCap = builder.comment("Maximum nearby latex mobs before new natural latex spawns are denied. Set to 0 to disable this cap.").defineInRange("changedLatexMobsNaturalSpawnLocalCap", 8, 0, 200);
            changedLatexMobsNaturalSpawnLocalCapRadius = builder.comment("Radius checked by changedLatexMobsNaturalSpawnLocalCap.").defineInRange("changedLatexMobsNaturalSpawnLocalCapRadius", 48, 1, 256);
            builder.pop();

            builder.push("debug");
            debugLatexHeartSpawnMessages = builder.comment("If true, sends a clickable chat message whenever a latex heart successfully spawns.").define("latexHeartSpawnMessages", false);
            builder.pop();
        }

        public Stream<RegistryElementPredicate<EntityType<?>>> getLucidityBlacklistedEntityTypes() {
            return lucidityBlacklistEntityTypes.get().stream().map(s -> RegistryElementPredicate.parseString(ForgeRegistries.ENTITY_TYPES, s));
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue infectionOverlayEnabled;
        public final ForgeConfigSpec.DoubleValue infectionOverlayLowEndPercent;
        public final ForgeConfigSpec.DoubleValue infectionOverlayCriticalStartPercent;
        public final ForgeConfigSpec.DoubleValue infectionOverlayLowMaxAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayMidMaxAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayCriticalAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayPulseAlpha;
        public final ForgeConfigSpec.DoubleValue infectionOverlayFinalWashAlpha;

        public final ForgeConfigSpec.BooleanValue sharedStatusMeterEnabled;
        public final ForgeConfigSpec.DoubleValue coverageVignetteMaxAlpha;
        public final ForgeConfigSpec.DoubleValue lowLucidityVignetteMaxAlpha;

        public final ForgeConfigSpec.BooleanValue darkLatexMaskOverlayEnabled;
        public final ForgeConfigSpec.DoubleValue darkLatexMaskOverlayAlpha;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("infectionOverlay");
            infectionOverlayEnabled = builder.define("infectionOverlayEnabled", true);
            infectionOverlayLowEndPercent = builder.comment("At this infection percent, the goo outline becomes clearly visible.").defineInRange("visibleAtPercent", 25.0D, 0.0D, 100.0D);
            infectionOverlayCriticalStartPercent = builder.comment("At this infection percent, the goo outline is nearly full and the final color wash begins.").defineInRange("finalWashStartPercent", 90.0D, 0.0D, 100.0D);
            infectionOverlayLowMaxAlpha = builder.comment("Goo outline alpha at visibleAtPercent.").defineInRange("visibleAlpha", 0.20D, 0.0D, 1.0D);
            infectionOverlayMidMaxAlpha = builder.comment("Goo outline alpha just before finalWashStartPercent.").defineInRange("midAlpha", 0.82D, 0.0D, 1.0D);
            infectionOverlayCriticalAlpha = builder.comment("Goo outline alpha from finalWashStartPercent to full infection.").defineInRange("criticalAlpha", 0.95D, 0.0D, 1.0D);
            infectionOverlayPulseAlpha = builder.comment("Extra pulsing alpha added as infection becomes severe.").defineInRange("pulseAlpha", 0.04D, 0.0D, 1.0D);
            infectionOverlayFinalWashAlpha = builder.comment("Maximum screen wash during the final infection surge.").defineInRange("finalWhiteWashAlpha", 0.88D, 0.0D, 1.0D);
            builder.pop();

            builder.push("meters");
            sharedStatusMeterEnabled = builder.comment("If true, one Changed-style meter shows coverage before infection, infection while infected, and lucidity while latex.").define("sharedStatusMeterEnabled", true);
            coverageVignetteMaxAlpha = builder.comment("Maximum coverage vignette alpha while human. Keep this low; high values are rough on the eyes.").defineInRange("coverageVignetteMaxAlpha", 0.16D, 0.0D, 1.0D);
            lowLucidityVignetteMaxAlpha = builder.comment("Maximum vignette alpha as latex lucidity gets dangerously low.").defineInRange("lowLucidityVignetteMaxAlpha", 0.24D, 0.0D, 1.0D);
            builder.pop();

            builder.push("darkLatexMaskOverlay");
            darkLatexMaskOverlayEnabled = builder.comment("If true, dark-latex players may show the built-in latex mask overlay.").define("enabled", false);
            darkLatexMaskOverlayAlpha = builder.comment("Mask overlay alpha.").defineInRange("alpha", 0.65D, 0.0D, 1.0D);
            builder.pop();
        }

    }
}

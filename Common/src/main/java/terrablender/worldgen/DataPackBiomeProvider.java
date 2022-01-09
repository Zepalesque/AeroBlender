/*******************************************************************************
 * Copyright 2022, the Glitchfiend Team.
 * Creative Commons Attribution-NonCommercial-NoDerivatives 4.0.
 ******************************************************************************/
package terrablender.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import terrablender.api.BiomeProvider;
import terrablender.core.TerraBlender;
import terrablender.worldgen.BiomeProviderUtils;
import terrablender.worldgen.TBClimate;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataPackBiomeProvider extends BiomeProvider
{
    private WorldGenSettings settings;
    private ChunkGenerator overworldGenerator;
    private Optional<SurfaceRules.RuleSource> overworldSurfaceRules = Optional.empty();
    private Optional<SurfaceRules.RuleSource> netherSurfaceRules = Optional.empty();

    public DataPackBiomeProvider(ResourceLocation name, int weight, WorldGenSettings settings)
    {
        super(name, weight);
        this.settings = settings;
        this.overworldGenerator = settings.overworld();

        if (this.overworldGenerator instanceof NoiseBasedChunkGenerator)
        {
            NoiseGeneratorSettings generatorSettings = ((NoiseBasedChunkGenerator)this.overworldGenerator).settings.get();
            this.overworldSurfaceRules = Optional.of(generatorSettings.surfaceRule());
        }

        LevelStem netherStem = settings.dimensions().get(LevelStem.NETHER);
        ChunkGenerator netherGenerator = netherStem != null ? netherStem.generator() : null;

        if (netherGenerator != null && netherGenerator instanceof NoiseBasedChunkGenerator)
        {
            NoiseGeneratorSettings generatorSettings = ((NoiseBasedChunkGenerator)netherGenerator).settings.get();
            this.netherSurfaceRules = Optional.of(generatorSettings.surfaceRule());
        }
    }

    @Override
    public void addOverworldBiomes(Registry<Biome> registry, Consumer<Pair<TBClimate.ParameterPoint, ResourceKey<Biome>>> mapper)
    {
        if (!(this.overworldGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource))
            return;

        MultiNoiseBiomeSource biomeSource = (MultiNoiseBiomeSource)this.overworldGenerator.getBiomeSource();
        Climate.Parameter uniquenessParameter = this.getUniquenessParameter();
        TerraBlender.LOGGER.info("Adding overworld biomes for datapack " + this.getName() + " with uniqueness " + uniquenessParameter);

        biomeSource.parameters.values().stream().forEach(pair -> {
            TBClimate.ParameterPoint parameters = BiomeProviderUtils.convertParameterPoint(pair.getFirst(), uniquenessParameter);
            Optional<ResourceKey<Biome>> key = registry.getResourceKey(pair.getSecond().get());

            if (key.isPresent())
                this.addBiome(mapper, parameters, key.get());
        });
    }

    @Override
    public Optional<SurfaceRules.RuleSource> getOverworldSurfaceRules()
    {
        return this.overworldSurfaceRules;
    }

    @Override
    public Optional<SurfaceRules.RuleSource> getNetherSurfaceRules()
    {
        return this.netherSurfaceRules;
    }
}
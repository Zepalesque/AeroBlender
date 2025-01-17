/**
 * Copyright (C) Glitchfiend
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package aeroblender.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import aeroblender.api.Region;
import aeroblender.api.RegionType;
import aeroblender.api.Regions;
import aeroblender.worldgen.IExtendedParameterList;
import aeroblender.worldgen.noise.Area;
import aeroblender.worldgen.noise.LayeredNoiseUtil;

import java.util.List;

@Mixin(Climate.ParameterList.class)
public abstract class MixinParameterList<T> implements IExtendedParameterList<T>
{
    @Shadow
    @Final
    private List<Pair<Climate.ParameterPoint, T>> values;

    @Shadow
    public abstract T findValue(Climate.TargetPoint target);

    private boolean initialized = false;
    private boolean treesPopulated = false;
    private Area uniqueness;
    private Climate.RTree[] uniqueTrees;

    @Override
    public void initializeForAeroBlender(RegistryAccess registryAccess, RegionType regionType, long seed)
    {
        // We don't want to initialize multiple times
        if (this.initialized)
            return;

        this.uniqueness = LayeredNoiseUtil.uniqueness(registryAccess, regionType, seed);
        this.uniqueTrees = new Climate.RTree[Regions.getCount(regionType)];

        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

        for (Region region : Regions.get(regionType))
        {
            int regionIndex = Regions.getIndex(regionType, region.getName());

            // Use the existing values for index 0, rather than those from the region. This is for datapack support.
            if (regionIndex == 0)
            {
                this.uniqueTrees[0] = Climate.RTree.create(this.values);
            }
            else
            {
                ImmutableList.Builder<Pair<Climate.ParameterPoint, Holder<Biome>>> builder = ImmutableList.builder();
                region.addBiomes(biomeRegistry, pair -> builder.add(pair.mapSecond(biomeRegistry::getHolderOrThrow)));
                ImmutableList<Pair<Climate.ParameterPoint, Holder<Biome>>> uniqueValues = builder.build();

                // We can't create an RTree if there are no values present.
                if (!uniqueValues.isEmpty())
                    this.uniqueTrees[regionIndex] = Climate.RTree.create(uniqueValues);
            }
        }

        this.treesPopulated = true;

        // Mark as initialized
        this.initialized = true;
    }

    @Override
    public int getUniqueness(int x, int y, int z)
    {
        return this.uniqueness.get(x, z);
    }

    @Override
    public Climate.RTree getTree(int uniqueness)
    {
        return this.uniqueTrees[uniqueness];
    }

    @Override
    public int getTreeCount()
    {
        return this.uniqueTrees.length;
    }

    @Override
    public boolean isInitialized()
    {
        return this.initialized && this.treesPopulated;
    }

    @Override
    public T findValuePositional(Climate.TargetPoint target, int x, int y, int z)
    {
        // Fallback on findValue if we are uninitialized (may be the case for non-TerraBlender dimensions)
        if (!this.initialized)
            return this.findValue(target);

        if (!this.treesPopulated)
            throw new RuntimeException("Attempted to call findValuePositional whilst trees remain unpopulated!");

        int uniqueness = this.getUniqueness(x, y, z);
        Holder<Biome> biome = (Holder<Biome>)this.getTree(uniqueness).search(target, Climate.RTree.Node::distance);

        if (biome.is(Region.DEFERRED_PLACEHOLDER))
            return (T)this.uniqueTrees[0].search(target, Climate.RTree.Node::distance);
        else
            return (T)biome;
    }
}

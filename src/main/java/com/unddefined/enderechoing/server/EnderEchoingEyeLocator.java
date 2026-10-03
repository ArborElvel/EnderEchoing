package com.unddefined.enderechoing.server;

import com.unddefined.enderechoing.api.event.EnderEchoStructureEvent;
import com.unddefined.enderechoing.server.DataComponents.VisitedStructures.VisitedStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Set;

import static com.unddefined.enderechoing.server.registry.DataRegistry.VISITED_STRUCTURES;

public final class EnderEchoingEyeLocator {
    public static final TagKey<Structure> EYE_LOCATED_STRUCTURES = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("enderechoing", "ender_echoing_eye_located"));
    // 单位是 placement 的 spacing 圈数，不是 block 或 chunk
    private static final int SEARCH_RING_RADIUS = 100;

    private EnderEchoingEyeLocator() {
    }

    public static BlockPos findNearestUnvisited(ServerLevel level, BlockPos origin, Set<VisitedStructure> visited) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structures = registry.getTag(EYE_LOCATED_STRUCTURES);
        if (structures.isEmpty()) return null;

        var generatorState = level.getChunkSource().getGeneratorState();
        StructureManager structureManager = level.structureManager();
        int originChunkX = SectionPos.blockToSectionCoord(origin.getX());
        int originChunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        long seed = generatorState.getLevelSeed();

        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        for (Holder<Structure> holder : structures.get()) {
            var structureKey = registry.getResourceKey(holder.value());
            if (structureKey.isEmpty()) continue;

            for (StructurePlacement placement : generatorState.getPlacementsForStructure(holder)) {
                BlockPos candidate;
                if (placement instanceof RandomSpreadStructurePlacement spreadPlacement) {
                    candidate = nearestInSpread(level, structureManager, holder.value(), structureKey.get(),
                            spreadPlacement, seed, origin, originChunkX, originChunkZ, visited);
                } else if (placement instanceof ConcentricRingsStructurePlacement ringsPlacement) {
                    candidate = nearestInRings(level, structureManager, holder.value(), structureKey.get(),
                            placement, generatorState.getRingPositionsFor(ringsPlacement), origin, visited);
                } else continue;

                if (candidate != null) {
                    double distSqr = origin.distSqr(candidate);
                    if (distSqr < nearestDistSqr) {
                        nearestDistSqr = distSqr;
                        nearest = candidate;
                    }
                }
            }
        }
        return nearest;
    }

    private static BlockPos nearestInSpread(ServerLevel level, StructureManager structureManager, Structure structure,
                                            ResourceKey<Structure> structureKey, RandomSpreadStructurePlacement placement,
                                            long seed, BlockPos origin, int originChunkX, int originChunkZ,
                                            Set<VisitedStructure> visited) {
        int spacing = placement.spacing();
        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;

        for (int ring = 0; ring <= SEARCH_RING_RADIUS; ring++) {
            if (nearest != null && ring > 1) {
                double minPossible = (double) (ring - 1) * spacing * 16.0D;
                if (nearestDistSqr <= minPossible * minPossible) break;
            }
            for (int j = -ring; j <= ring; j++) {
                boolean onJEdge = j == -ring || j == ring;
                for (int k = -ring; k <= ring; k++) {
                    if (!(onJEdge || k == -ring || k == ring)) continue;
                    var chunkPos = placement.getPotentialStructureChunk(seed,
                            originChunkX + spacing * j, originChunkZ + spacing * k);
                    BlockPos structurePos = locateAt(structureManager, structure, placement, chunkPos);
                    if (structurePos == null || visited.contains(new VisitedStructure(structureKey, chunkPos))) continue;

                    double distSqr = origin.distSqr(structurePos);
                    if (distSqr < nearestDistSqr) {
                        nearestDistSqr = distSqr;
                        nearest = structurePos;
                    }
                }
            }
        }
        return nearest;
    }

    private static BlockPos nearestInRings(ServerLevel level, StructureManager structureManager, Structure structure,
                                           ResourceKey<Structure> structureKey, StructurePlacement placement,
                                           List<ChunkPos> ringChunks, BlockPos origin, Set<VisitedStructure> visited) {
        if (ringChunks == null) return null;
        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        for (ChunkPos chunkPos : ringChunks) {
            BlockPos structurePos = locateAt(structureManager, structure, placement, chunkPos);
            if (structurePos == null || visited.contains(new VisitedStructure(structureKey, chunkPos))) continue;

            double distSqr = origin.distSqr(structurePos);
            if (distSqr < nearestDistSqr) {
                nearestDistSqr = distSqr;
                nearest = structurePos;
            }
        }
        return nearest;
    }

    private static BlockPos locateAt(StructureManager structureManager, Structure structure,
                                     StructurePlacement placement, ChunkPos chunkPos) {
        var result = structureManager.checkStructurePresence(chunkPos, structure, placement, false);
        if (result == StructureCheckResult.START_NOT_PRESENT) return null;
        // START_PRESENT 或 CHUNK_LOAD_NEEDED：前者是已存在，后者已通过结构检查的
        // biome/随机校验，可视为该 chunk 的理论可生成位置，无需主动加载区块确认。
        return placement.getLocatePos(chunkPos);
    }

    public static void markVisitedIfInside(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structures = registry.getTag(EYE_LOCATED_STRUCTURES);
        if (structures.isEmpty()) return;

        var structureManager = level.structureManager();
        for (Holder<Structure> holder : structures.get()) {
            var structureKey = registry.getResourceKey(holder.value());
            if (structureKey.isEmpty()) continue;

            var start = structureManager.getStructureAt(player.blockPosition(), holder.value());
            if (!start.isValid()) continue;

            var chunkPos = start.getChunkPos();
            if (player.getData(VISITED_STRUCTURES.get()).add(new VisitedStructure(structureKey.get(), chunkPos)))
                NeoForge.EVENT_BUS.post(new EnderEchoStructureEvent.Visited(player, structureKey.get(), chunkPos));
        }
    }
}

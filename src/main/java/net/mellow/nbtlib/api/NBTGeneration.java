package net.mellow.nbtlib.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import net.mellow.nbtlib.Config;
import net.mellow.nbtlib.Registry;
import net.mellow.nbtlib.api.JigsawPiece.WeightedJigsawPiece;
import net.mellow.nbtlib.api.NBTStructure.JigsawConnection;
import net.mellow.nbtlib.api.SpawnCondition.WorldCoordinate;
import net.mellow.nbtlib.block.BlockPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Handles generating structures in the world, based on current dimension and biome
 */
public class NBTGeneration {

    private static Map<String, SpawnCondition> namedMap = new HashMap<>();

    private static Map<Integer, List<SpawnCondition>> spawnMap = new HashMap<>();
    private static Map<Integer, List<SpawnCondition>> customSpawnMap = new HashMap<>();

    private static Map<Integer, Map<Integer, WeightedSpawnList>> validBiomeCache = new HashMap<>();

    public static void register() {
        MapGenStructureIO.registerStructure(Start.class, "NBTStructures");
        MapGenStructureIO.func_143031_a(Component.class, "NBTComponents");
    }

    // Register a new structure for a given dimension
    public static void registerStructure(int dimensionId, SpawnCondition spawn) {
        if (namedMap.containsKey(spawn.name) && namedMap.get(spawn.name) != spawn)
            throw new IllegalStateException("A severe error has occurred in NBTStructure! A SpawnCondition has been registered with the same name as another: " + spawn.name);

        namedMap.put(spawn.name, spawn);

        if (spawn.checkCoordinates != null) {
            List<SpawnCondition> spawnList = customSpawnMap.computeIfAbsent(dimensionId, integer -> new ArrayList<SpawnCondition>());
            spawnList.add(spawn);
            return;
        }

        List<SpawnCondition> spawnList = spawnMap.computeIfAbsent(dimensionId, integer -> new ArrayList<SpawnCondition>());
        spawnList.add(spawn);
    }

    // Register a structure for multiple dimensions simultaneously
    public static void registerStructure(int[] dimensionIds, SpawnCondition spawn) {
        for (int dimensionId : dimensionIds) {
            registerStructure(dimensionId, spawn);
        }
    }

    // Add a chance for nothing to spawn at a given valid spawn location
    public static void registerNullWeight(int dimensionId, int weight) {
        registerNullWeight(dimensionId, weight, null);
    }

    // Add a chance for nothing to spawn at a given valid spawn location, for specific biomes
    public static void registerNullWeight(int dimensionId, int weight, Predicate<BiomeGenBase> predicate) {
        SpawnCondition spawn = new SpawnCondition() {
            {
                spawnWeight = weight;
                canSpawn = predicate;
            }
        };

        List<SpawnCondition> spawnList = spawnMap.computeIfAbsent(dimensionId, integer -> new ArrayList<SpawnCondition>());
        spawnList.add(spawn);
    }

    // Presents a list of all structures registered (so far)
    public static List<String> listStructures() {
        List<String> names = new ArrayList<>(namedMap.keySet());
        names.sort((a, b) -> a.compareTo(b));
        return names;
    }

    // Fetches a registered structure by mod prefix and name.
    // If one is not found, will simply return null.
    public static SpawnCondition getStructure(String prefix, String name) {
        return namedMap.get(prefix + ":" + name);
    }

    // Fetches a registered structure by name, you must add the mod ID prefix you wish to fetch from, eg.
    //   `mod:example_structure`
    // If one is not found, will simply return null.
    public static SpawnCondition getStructure(String name) {
        return namedMap.get(name);
    }

    public static class Component extends StructureComponent {

        JigsawPiece piece;

        int minHeight = 1;
        int maxHeight = 128;

        boolean heightUpdated = false;

        int priority;

        // this is fucking hacky but we need a way to update ALL component bounds once a Y-level is determined
        private Start parent;

        private JigsawConnection connectedFrom;

        public Component() {}

        public Component(SpawnCondition spawn, JigsawPiece piece, Random rand, int x, int z) {
            this(spawn, piece, rand, x, 0, z, rand.nextInt(4));
        }

        public Component(SpawnCondition spawn, JigsawPiece piece, Random rand, int x, int y, int z, int coordBaseMode) {
            super(0);
            this.coordBaseMode = coordBaseMode;
            this.piece = piece;
            this.minHeight = spawn.minHeight;
            this.maxHeight = spawn.maxHeight;

            switch (this.coordBaseMode) {
            case 1:
            case 3:
                this.boundingBox = new StructureBoundingBox(x, y, z, x + piece.structure.size.z - 1, y + piece.structure.size.y - 1, z + piece.structure.size.x - 1);
                break;
            default:
                this.boundingBox = new StructureBoundingBox(x, y, z, x + piece.structure.size.x - 1, y + piece.structure.size.y - 1, z + piece.structure.size.z - 1);
                break;
            }
        }

        public Component connectedFrom(JigsawConnection connection) {
            this.connectedFrom = connection;
            return this;
        }

        // Save to NBT
        @Override
        protected void func_143012_a(NBTTagCompound nbt) {
            nbt.setString("piece", piece != null ? piece.name : "NULL");
            nbt.setInteger("min", minHeight);
            nbt.setInteger("max", maxHeight);
            nbt.setBoolean("hasHeight", heightUpdated);
        }

        // Load from NBT
        @Override
        protected void func_143011_b(NBTTagCompound nbt) {
            piece = JigsawPiece.jigsawMap.get(nbt.getString("piece"));
            minHeight = nbt.getInteger("min");
            maxHeight = nbt.getInteger("max");
            heightUpdated = nbt.getBoolean("hasHeight");
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            if (piece == null)
                return false;

            // now we're in the world, update minY/maxY
            if (!piece.conformToTerrain && !heightUpdated) {
                int y = MathHelper.clamp_int(getAverageHeight(world, box) + piece.heightOffset, minHeight, maxHeight);

                if (!piece.alignToTerrain && parent != null) {
                    parent.offsetYHeight(y);
                } else {
                    offsetYHeight(y);
                }
            }

            return piece.structure.build(world, piece, boundingBox, box, coordBaseMode);
        }

        public void offsetYHeight(int y) {
            boundingBox.minY += y;
            boundingBox.maxY += y;

            heightUpdated = true;
        }

        // Overrides to fix Mojang's fucked rotations which FLIP instead of rotating in two instances
        // vaer being in the mines doing this the hard way for years was absolutely not for naught
        @Override
        protected int getXWithOffset(int x, int z) {
            return boundingBox.minX + piece.structure.rotateX(x, z, coordBaseMode);
        }

        @Override
        protected int getYWithOffset(int y) {
            return boundingBox.minY + y;
        }

        @Override
        protected int getZWithOffset(int x, int z) {
            return boundingBox.minZ + piece.structure.rotateZ(x, z, coordBaseMode);
        }

        private ForgeDirection rotateDir(ForgeDirection dir) {
            if (dir == ForgeDirection.UP || dir == ForgeDirection.DOWN) return dir;

            switch (coordBaseMode) {
            default: return dir;
            case 1: return dir.getRotation(ForgeDirection.UP);
            case 2: return dir.getOpposite();
            case 3: return dir.getRotation(ForgeDirection.DOWN);
            }
        }

        private int getAverageHeight(World world, StructureBoundingBox box) {
            int total = 0;
            int iterations = 0;

            for (int z = box.minZ; z <= box.maxZ; z++) {
                for (int x = box.minX; x <= box.maxX; x++) {
                    total += world.getTopSolidOrLiquidBlock(x, z);
                    iterations++;
                }
            }

            if (iterations == 0)
                return 64;

            return total / iterations;
        }

        private int getNextCoordBase(JigsawConnection fromConnection, JigsawConnection toConnection, Random rand) {
            if (fromConnection.dir == ForgeDirection.DOWN || fromConnection.dir == ForgeDirection.UP) {
                if (fromConnection.isRollable) return rand.nextInt(4);
                return coordBaseMode;
            }

            return directionOffsetToCoordBase(fromConnection.dir.getOpposite(), toConnection.dir);
        }

        private int directionOffsetToCoordBase(ForgeDirection from, ForgeDirection to) {
            for (int i = 0; i < 4; i++) {
                if (from == to) return (i + coordBaseMode) % 4;
                from = from.getRotation(ForgeDirection.DOWN);
            }
            return coordBaseMode;
        }

        protected boolean hasIntersectionIgnoringSelf(LinkedList<StructureComponent> components, StructureBoundingBox box) {
            for (StructureComponent component : components) {
                if (component == this) continue;
                if (component.getBoundingBox() == null) continue;

                if (component.getBoundingBox().intersectsWith(box)) return true;
            }

            return false;
        }

        protected boolean isInsideIgnoringSelf(LinkedList<StructureComponent> components, int x, int y, int z) {
            for (StructureComponent component : components) {
                if (component == this) continue;
                if (component.getBoundingBox() == null) continue;

                if (component.getBoundingBox().isVecInside(x, y, z)) return true;
            }

            return false;
        }

    }

    public static class Start extends StructureStart {

        public Start() {}

        public Start(World world, Random rand, SpawnCondition spawn, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);

            int x = chunkX << 4;
            int z = chunkZ << 4;

            JigsawPiece startPiece = spawn.structure != null ? spawn.structure : spawn.pools.get(spawn.startPool).get(rand);

            Component startComponent = new Component(spawn, startPiece, rand, x, z);
            startComponent.parent = this;

            components.add(startComponent);

            List<Component> queuedComponents = new ArrayList<>();
            if (spawn.structure == null)
                queuedComponents.add(startComponent);

            Set<JigsawPiece> requiredPieces = findRequiredPieces(spawn);

            // Iterate through and build out all the components we intend to spawn
            while (!queuedComponents.isEmpty()) {
                queuedComponents.sort((a, b) -> b.priority - a.priority); // sort by placement priority descending
                int matchPriority = queuedComponents.get(0).priority;
                int max = 1;
                while (max < queuedComponents.size()) {
                    if (queuedComponents.get(max).priority != matchPriority) break;
                    max++;
                }

                final int i = rand.nextInt(max);
                Component fromComponent = queuedComponents.remove(i);

                if (fromComponent.piece.structure.fromConnections == null)
                    continue;

                int distance = getDistanceTo(fromComponent.getBoundingBox());

                // Only generate fallback pieces once we hit our size limit, unless we have a required component
                // Note that there is a HARD limit of 1024 pieces to prevent infinite generation
                boolean fallbacksOnly = requiredPieces.size() == 0 && (components.size() >= spawn.sizeLimit || distance >= spawn.rangeLimit) || components.size() > 1024;

                for (List<JigsawConnection> unshuffledList : fromComponent.piece.structure.fromConnections) {
                    List<JigsawConnection> connectionList = new ArrayList<>(unshuffledList);
                    Collections.shuffle(connectionList, rand);

                    for (JigsawConnection fromConnection : connectionList) {
                        if (fromComponent.connectedFrom == fromConnection)
                            continue; // if we already connected to this piece, don't process

                        if (fallbacksOnly) {
                            String fallback = spawn.pools.get(fromConnection.poolName).fallback;

                            if (fallback != null) {
                                Component fallbackComponent = buildNextComponent(rand, spawn, spawn.pools.get(fallback), fromComponent, fromConnection);
                                addComponent(fallbackComponent, fromConnection.placementPriority);
                            }

                            continue;
                        }

                        JigsawPool nextPool = spawn.getPool(fromConnection.poolName);
						if (nextPool == null) {
							Registry.LOG.warn("[Jigsaw] Jigsaw block points to invalid pool: " + fromConnection.poolName);
							continue;
						}

                        Component nextComponent = null;

                        // Iterate randomly through the pool, attempting each piece until one fits
                        while (nextPool.totalWeight > 0) {
                            nextComponent = buildNextComponent(rand, spawn, nextPool, fromComponent, fromConnection);
                            if (nextComponent != null && !fromComponent.hasIntersectionIgnoringSelf(components, nextComponent.getBoundingBox())) break;
                            nextComponent = null;
                        }

                        if (nextComponent != null) {
                            addComponent(nextComponent, fromConnection.placementPriority);
                            queuedComponents.add(nextComponent);

                            requiredPieces.remove(nextComponent.piece);
                        } else {
                            // If we failed to fit anything in, grab something from the fallback pool,
                            // ignoring bounds check
                            // unless we are perfectly abutting another piece, so grid layouts can work!
                            if (nextPool.fallback != null) {
                                BlockPos checkPos = getConnectionTargetPosition(fromComponent, fromConnection);

                                if (!fromComponent.isInsideIgnoringSelf(components, checkPos.x, checkPos.y, checkPos.z)) {
                                    nextComponent = buildNextComponent(rand, spawn, spawn.pools.get(nextPool.fallback), fromComponent, fromConnection);

                                    // don't add to  queued list, we don't want to try continue from fallback
                                    addComponent(nextComponent, fromConnection.placementPriority);
                                }
                            }
                        }
                    }
                }
            }

            if (Config.debugSpawning) {
                Registry.LOG.info("[Debug] Spawning NBT structure with " + components.size() + " piece(s) at: " + chunkX * 16 + ", " + chunkZ * 16);
                String componentList = "[Debug] Components: ";
                for (Object component : this.components) {
                    componentList += ((Component) component).piece.structure.name + " ";
                }
                Registry.LOG.info(componentList);
            }

            updateBoundingBox();
        }

        private void addComponent(Component component, int placementPriority) {
            if (component == null) return;
            components.add(component);

            component.parent = this;
            component.priority = placementPriority;
        }

        private BlockPos getConnectionTargetPosition(Component component, JigsawConnection connection) {
            // The direction this component is extending towards in ABSOLUTE direction
            ForgeDirection extendDir = component.rotateDir(connection.dir);

            // Set the starting point for the next structure to the location of the connector block
            int x = component.getXWithOffset(connection.pos.x, connection.pos.z) + extendDir.offsetX;
            int y = component.getYWithOffset(connection.pos.y) + extendDir.offsetY;
            int z = component.getZWithOffset(connection.pos.x, connection.pos.z) + extendDir.offsetZ;

            return new BlockPos(x, y, z);
        }

        private Component buildNextComponent(Random rand, SpawnCondition spawn, JigsawPool pool, Component fromComponent, JigsawConnection fromConnection) {
            JigsawPiece nextPiece = pool.get(rand);
            if (nextPiece == null) {
                Registry.LOG.warn("[Jigsaw] Pool returned null piece: " + fromConnection.poolName);
                return null;
            }

            if (nextPiece.instanceLimit > 0) {
                int instances = 0;
                for (StructureComponent component : components) {
                    if (component instanceof Component && ((Component) component).piece == nextPiece) {
                        instances++;

                        if (instances >= nextPiece.instanceLimit) return null;
                    }
                }
            }

            List<JigsawConnection> connectionPool = getConnectionPool(nextPiece, fromConnection);
            if (connectionPool == null || connectionPool.isEmpty()) {
                Registry.LOG.warn("[Jigsaw] No valid connections for: " + fromConnection.targetName + " - in piece: " + nextPiece.name);
                return null;
            }

            JigsawConnection toConnection = connectionPool.get(rand.nextInt(connectionPool.size()));

            // Rotate our incoming piece to plug it in
            int nextCoordBase = fromComponent.getNextCoordBase(fromConnection, toConnection, rand);

            BlockPos pos = getConnectionTargetPosition(fromComponent, fromConnection);

            // offset the starting point to the connecting point
            int ox = nextPiece.structure.rotateX(toConnection.pos.x, toConnection.pos.z, nextCoordBase);
            int oy = toConnection.pos.y;
            int oz = nextPiece.structure.rotateZ(toConnection.pos.x, toConnection.pos.z, nextCoordBase);

            return new Component(spawn, nextPiece, rand, pos.x - ox, pos.y - oy, pos.z - oz, nextCoordBase).connectedFrom(toConnection);
        }

        private Set<JigsawPiece> findRequiredPieces(SpawnCondition spawn) {
            Set<JigsawPiece> requiredPieces = new HashSet<>();

            if (spawn.pools == null) return requiredPieces;

            for (JigsawPool pool : spawn.pools.values()) {
                for (WeightedJigsawPiece weight : pool.pieces) {
                    if (weight.piece.required) {
                        requiredPieces.add(weight.piece);
                    }
                }
            }

            return requiredPieces;
        }

        private List<JigsawConnection> getConnectionPool(JigsawPiece nextPiece, JigsawConnection fromConnection) {
            if (fromConnection.dir == ForgeDirection.DOWN) {
                return nextPiece.structure.toTopConnections.get(fromConnection.targetName);
            } else if (fromConnection.dir == ForgeDirection.UP) {
                return nextPiece.structure.toBottomConnections.get(fromConnection.targetName);
            }

            return nextPiece.structure.toHorizontalConnections.get(fromConnection.targetName);
        }

        private int getDistanceTo(StructureBoundingBox box) {
            int x = box.getCenterX();
            int z = box.getCenterZ();

            return Math.max(Math.abs(x - (func_143019_e() << 4)), Math.abs(z - (func_143018_f() << 4)));
        }

        // post loading, update parent reference for loaded components
        @Override
        public void func_143017_b(NBTTagCompound nbt) {
            for (Object o : components) {
                ((Component) o).parent = this;
            }
        }

        public void offsetYHeight(int y) {
            for (Object o : components) {
                Component component = (Component) o;
                if (component.heightUpdated || component.piece.conformToTerrain || component.piece.alignToTerrain)
                    continue;
                component.offsetYHeight(y);
            }
        }

    }

    public static class GenStructure extends MapGenStructure {

        private SpawnCondition nextSpawn;

        public void generateStructures(World world, Random rand, IChunkProvider chunkProvider, int chunkX, int chunkZ) {
            func_151539_a(chunkProvider, world, chunkX, chunkZ, null);
            generateStructuresInChunk(world, rand, chunkX, chunkZ);
        }

        @Override
        public String func_143025_a() {
            return "NBTStructures";
        }

        @Override
        protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
            nextSpawn = getSpawnAtCoords(chunkX, chunkZ);
            return nextSpawn != null;
        }

        public SpawnCondition getStructureAt(World world, int chunkX, int chunkZ) {
            // make sure the random is in the correct state
            this.worldObj = world;
            this.rand.setSeed(world.getSeed());
            long l = this.rand.nextLong();
            long i1 = this.rand.nextLong();

            long l1 = (long)chunkX * l;
            long i2 = (long)chunkZ * i1;
            this.rand.setSeed(l1 ^ i2 ^ world.getSeed());

            // random nextInt call just before `canSpawnStructureAtCoords`, no, I don't know why Mojang added that
            this.rand.nextInt();

            return getSpawnAtCoords(chunkX, chunkZ);
        }

        private SpawnCondition getSpawnAtCoords(int chunkX, int chunkZ) {
            // attempt to spawn with custom chunk coordinate rules
            if (customSpawnMap.containsKey(worldObj.provider.dimensionId)) {
                WorldCoordinate coords = new WorldCoordinate(worldObj, new ChunkCoordIntPair(chunkX, chunkZ), rand);

                List<SpawnCondition> spawnList = customSpawnMap.get(worldObj.provider.dimensionId);
                for (SpawnCondition spawn : spawnList) {
                    if ((spawn.pools != null || spawn.structure != null) && spawn.checkCoordinates.test(coords)) {
                        return spawn;
                    }
                }
            }

            if (!spawnMap.containsKey(worldObj.provider.dimensionId))
                return null;

            int x = chunkX;
            int z = chunkZ;

            if (x < 0) x -= Config.structureMaxChunks - 1;
            if (z < 0) z -= Config.structureMaxChunks - 1;

            x /= Config.structureMaxChunks;
            z /= Config.structureMaxChunks;
            rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L + this.worldObj.getWorldInfo().getSeed() + (long) 994994994 - worldObj.provider.dimensionId);
            x *= Config.structureMaxChunks;
            z *= Config.structureMaxChunks;
            x += rand.nextInt(Config.structureMaxChunks - Config.structureMinChunks);
            z += rand.nextInt(Config.structureMaxChunks - Config.structureMinChunks);

            if (chunkX == x && chunkZ == z) {
                BiomeGenBase biome = this.worldObj.getWorldChunkManager().getBiomeGenAt(chunkX * 16 + 8, chunkZ * 16 + 8);

                SpawnCondition spawn = findSpawn(biome);

                if (spawn != null && (spawn.pools != null || spawn.structure != null))
                    return spawn;
            }

            return null;
        }

        @Override
        protected StructureStart getStructureStart(int chunkX, int chunkZ) {
            return new Start(this.worldObj, this.rand, nextSpawn, chunkX, chunkZ);
        }

        private SpawnCondition findSpawn(BiomeGenBase biome) {
            Map<Integer, WeightedSpawnList> dimensionCache = validBiomeCache.computeIfAbsent(worldObj.provider.dimensionId, integer -> new HashMap<>());

            WeightedSpawnList filteredList;
            if (!dimensionCache.containsKey(biome.biomeID)) {
                List<SpawnCondition> spawnList = spawnMap.get(worldObj.provider.dimensionId);

                filteredList = new WeightedSpawnList();
                for (SpawnCondition spawn : spawnList) {
                    if (spawn.isValid(biome)) {
                        filteredList.add(spawn);
                        filteredList.totalWeight += spawn.spawnWeight;
                    }
                }

                dimensionCache.put(biome.biomeID, filteredList);
            } else {
                filteredList = dimensionCache.get(biome.biomeID);
            }

            if (filteredList.totalWeight == 0) return null;

            int weight = rand.nextInt(filteredList.totalWeight);

            for (SpawnCondition spawn : filteredList) {
                weight -= spawn.spawnWeight;

                if (weight < 0) {
                    return spawn;
                }
            }

            return null;
        }

    }

    private static class WeightedSpawnList extends ArrayList<SpawnCondition> {

        public int totalWeight = 0;

    }

}

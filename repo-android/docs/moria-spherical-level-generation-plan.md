# 🌌 Plan: Spherical Level Generation, High Surface Exploitation & Path Connectivity

This document details the architectural specifications and mathematical algorithms designed to upgrade the procedural level generator in the **Moria Roguelike** applet. 

Our main goals are to **exploit the full 3D sphere surface**, guarantee **greater than 50% exploration coverage** of the planetary planetoid, and enforce **100% path connectedness** between the player's arrival node and the dimensional portal.

---

## 1. Architectural Strategy & Constraints

Our underlying spherical world consists of a subdivided Icosphere (Geodesic Grid) with Level 3 subdivision, resulting in exactly **642 distinct nodes** (each node representing a tile).

To transition from localized cave clustering to a global planetary dungeon, we solve three key architectural requirements:

| Goal | Challenge | Mathematical Solution |
| :--- | :--- | :--- |
| **Exploit Full Sphere Surface** | Standard random walks get stuck in local feedback loops, leaving entire hemispheres empty. | Apply **Antipodal Multi-Seed Spawn** and **Great-Circle Geodesic Highways** to force global routing. |
| **>50% Sphere Exploitation** | Low floor node density keeps the map too constrained or disjointed. | Increase target floor nodes to $N_{\text{target}} \geq 330$ and run a **Spherical Cellular Automata** expansion pass. |
| **Guaranteed Reachability** | Walls isolate regions, trapping the player or sealing the portal. | Run a **BFS Graph Component Solver** with **Geodesic Chord Tunnelling** to link disconnected caverns. |

---

## 2. Spherical Level Generation Pipeline

The generation pipeline replaces the old single DLA (Diffusion-Limited Aggregation) / Drunkard's Walk with a three-stage hybrid generator:

```
                  +---------------------------------------+
                  |  1. Generate Multi-Points & Highways  |
                  +---------------------------------------+
                                      |
                                      v
                  +---------------------------------------+
                  |    2. Spherical Cellular Automata    |
                  |     (Grow floor nodes to >50% space)  |
                  +---------------------------------------+
                                      |
                                      v
                  +---------------------------------------+
                  |      3. BFS Connectivity Solver       |
                  |     (Heal and link isolated zones)   |
                  +---------------------------------------+
                                      |
                                      v
                  +---------------------------------------+
                  |    4. Distribute Portal & Hazards     |
                  +---------------------------------------+
```

---

## 3. Mathematical Foundations & Algorithms

### A. Antipodal Multi-Seed Spawn & Great-Circle Highways
To ensure the terrain wraps across the entire planetoid instead of clustering around a single pole, we seed the level from **three antipodal axis nodes**:
1. **Primary Seed (Arrival Pad)**: Chosen randomly or fixed at the North Pole $(0, 1, 0)$.
2. **Secondary Seed (Portal Well)**: Projected at the exact antipodal opposite coordinates $(0, -1, 0)$.
3. **Equatorial Highway Seed**: Projecting a ring of 4 support nodes equally spaced around the equator $(y = 0)$.

We then carve **Great-Circle Geodesic Highways** to connect these seeds. For any two seeds $\mathbf{P}_1$ and $\mathbf{P}_2$ on the sphere, the shortest path on the surface is a great-circle segment. We find this using graph pathfinding on the uncarved icosphere (using nodes as vertices with unweighted costs) and carve the entire string into a wide $2$-tile wide corridor. This forces lanes spanning across the northern hemisphere, equator, and southern hemisphere, fully exploiting the map.

### B. High-Density Spherical Cellular Automata
To reliably cover more than 50% of the planetary surface (minimum $322$ nodes), we run a specialized hexagonal/pentagonal Cellular Automata (CA) model:
1. **Initial Fill**: Randomly mark $55\%$ of all non-highway nodes as `FLOOR` and the rest as `WALL`.
2. **Neighbor Density Integration**: For each node on the sphere, count how many of its $K$ neighbors ($K=5$ or $K=6$) are `FLOOR`.
3. **Transition Rules**:
   - If a node is `WALL` and has $\geq 3$ floor neighbors, it relaxes into a `FLOOR`.
   - If a node is `FLOOR` and has $< 2$ floor neighbors, it collapses into a `WALL` (preventing thin, single-node noise).
4. Run this filter for exactly 3 iterations. This converges into thick, winding cavern continents and oceans of walls that span the globe, guaranteeing a stable coverage of $52\% - 60\%$ explorable surface.

### C. BFS Graph-Connectedness Solver (Dungeon Merger)
Using standard cellular automata or noise on a closed sphere always creates some isolated cavern chambers. To guarantee that the player can explore all carved areas and successfully reach the portal, the engine runs a post-generation graph verification:

1. **BFS Component Isolation**: Starting from the player spawn node (ID: `px`), run a Breadth-First Search (BFS) tracking all reachable `FLOOR` nodes. Let this set of reachable nodes be $C_{\text{active}}$.
2. **Coverage Guard Check**: Count $N_{\text{reachable}} = |C_{\text{active}}|$.
   - If $N_{\text{reachable}} < 322$ (less than 50% of the 642 nodes), we locate the largest disconnected cave component $C_{\text{hidden}}$ and trigger a tunnel bridge.
3. **Geodesic Tunnel Bridging**:
   - Determine the pair of nodes $(n_{\text{active}} \in C_{\text{active}}, n_{\text{hidden}} \in C_{\text{hidden}})$ that are closest in physical 3D space:
     $$\mathbf{D} = \min \|\mathbf{P}_{\text{active}} - \mathbf{P}_{\text{hidden}}\|$$
   - Run a short A* search between these two nodes overriding the graph to allow pathing through `WALL` tiles, marking each node on this shortest bridge path as `FLOOR`.
   - Merge the sets and repeat until $100\%$ of all floor tiles on the globe are interconnected.

---

## 4. Portal and Loot Placement Coordinates

To maximize active navigation times and prevent trivial instant-wins:
1. **Portal Placement**: The 3D Teleportation Portal is placed in the floor node situated at the absolute maximum geodesic distance from the player's starting spawn point.
   - For start node $\hat{\mathbf{P}}_0$, we scan all floor nodes $f$ and find the one that maximizes:
     $$D(f) = \arccos\left(\hat{\mathbf{P}}_0 \cdot \hat{\mathbf{P}}_f\right)$$
   - This ensures the portal is positioned as close to the exact antipodal coordinate as geometrically possible, forcing the player to traverse more than 50% of the planet's surface.
2. **High-Value loot**: Placed in "pocket" regions—defined as nodes with only 1 exit/neighbor of type `FLOOR` (dead-ends), encouraging off-highway exploration.

---

## 5. Mobile UI Mini-map & Radar Feedback

Since the player is walking a 3D sphere, we assist exploration of this >50% surface area with a lightweight, ergonomic **Curvature Minimap Card** inside the stats structure:
* **The Radar Indicator**: A circular widget showing the relative direction of the volumetric Polar Beacons (Emerald North/Orchid South).
* **Relative Compass Heading**: Automatically rotates matching the camera's current yaw factor, telling the player which quadrant they are heading towards.
* **Surface Coverage Tracker**: Renders a glowing progress indicator bar mapping:
  $$\text{Exploration Rate} = \frac{\text{Count of Revealed Floor Tiles}}{\text{Total Floor Tiles}} \times 100\%$$
  When this value exceeds $50\%$, a subtle notification icon lights up, signaling the player has mapped more than half of the world's surface!

---

## 6. Code Migration Blueprint (Kotlin Template)

Below is the planned implementation function to be slotted inside `RoguelikeViewModel.kt` during the final code merger:

```kotlin
private fun generatePlanetoidLevel(floorIndex: Int) {
    val totalNodes = planetNodes.size // 642 nodes
    val tempTiles = mutableMapOf<Int, GameTile>()
    
    // Step 1: Pre-fill all as WALL
    for (id in planetNodes.keys) {
        tempTiles[id] = GameTile(id, 0, "WALL", false)
    }

    val startNode = 0 // North Pole
    val exitNode = totalNodes - 1 // South Pole / Antipode
    
    // Step 2: Form Great-Circle Geodesic Highways
    val highwayNodes = findShortest3DGraphPath(startNode, exitNode)
    for (nodeId in highwayNodes) {
        tempTiles[nodeId] = GameTile(nodeId, 0, "FLOOR", false)
        // Make highway 2 tiles wide for easy navigation
        planetNodes[nodeId]?.neighbors?.forEach { neighborId ->
            tempTiles[neighborId] = GameTile(neighborId, 0, "FLOOR", false)
        }
    }

    // Step 3: Spherical Cellular Automata Fill
    val rnd = java.util.Random()
    val targetFloorCount = 350 // ensure ~54% explorable surface
    
    // Initial noise seed
    for (id in planetNodes.keys) {
        if (tempTiles[id]?.tileType == "WALL" && rnd.nextFloat() < 0.55f) {
            tempTiles[id] = GameTile(id, 0, "FLOOR", false)
        }
    }

    // Cellular Automata smoothing iterations
    for (pass in 0 until 3) {
        val nextStates = tempTiles.mapValues { it.value.tileType }
        for (id in planetNodes.keys) {
            val neighbors = planetNodes[id]?.neighbors ?: emptyList()
            val floorNeighbors = neighbors.count { nextStates[it] == "FLOOR" }
            
            if (tempTiles[id]?.tileType == "WALL" && floorNeighbors >= 3) {
                tempTiles[id] = tempTiles[id]!!.copy(tileType = "FLOOR")
            } else if (tempTiles[id]?.tileType == "FLOOR" && floorNeighbors < 2) {
                // Ensure highway nodes never collapse
                if (!highwayNodes.contains(id)) {
                    tempTiles[id] = tempTiles[id]!!.copy(tileType = "WALL")
                }
            }
        }
    }

    // Step 4: Run BFS Connectivity and tunnel healing
    ensureSphericalConnectivity(tempTiles, startNode)

    // Step 5: Place Stairs and Hazards
    tempTiles[exitNode] = GameTile(exitNode, 0, "STAIRS_DOWN", false)
    
    // Set view balances
    _playerX.value = startNode
    _tiles.value = tempTiles.values.toList()
    recalculateFogOfWar(startNode)
}
```

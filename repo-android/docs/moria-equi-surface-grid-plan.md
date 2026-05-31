# Moria Roguelike: Equi-Surface Spherical Grid Implementation Plan

## 1. Architectural Motivation

Currently, the Moria applet simulates a spherical planet by wrapping a traditional flat 2D `map[y][x]` array around a sphere using a standard UV projection (cylindrical or equirectangular projection). This approach introduces critical flaws:
* **Severe Distortion:** Tiles at the equator are square, but tiles near the poles become highly stretched and pinched.
* **Polar Singularities:** Approaching the "North" or "South" pole causes unpredictable movement mapping and visual tearing.
* **Unequal Area:** Game mechanics (like splash damage or movement distance) become mathematically inconsistent depending on latitude.

To fix this, a **Geodesic Grid (Equi-Surface Grid)** will be implemented. A true spherical grid is formed by subdividing a regular polyhedron (such as an Icosahedron), yielding a planet covered in discrete tiles (hexagons and pentagons) that all possess nearly identical surface areas, completely eliminating poles and coordinate pinching.

---

## 2. Mathematical Foundation: The Icosphere / Hexasphere

We will transition the grid data structure from a 2D Array to a **3D Graph-Based Icosphere**.

### Subdivision of an Icosahedron
1. **Base Mesh:** Start with a regular Icosahedron (12 vertices, 20 equilateral triangular faces).
2. **Subdivision:** Subdivide each triangle into 4 smaller triangles by adding a new vertex at the midpoint of each edge.
3. **Normalization:** Normalize all new vertices so their distance from the origin $(0,0,0)$ equals the sphere's radius $R$.
4. **Iterative Detail:** Repeat this process to increase the grid resolution (e.g., Level 3 subdivision yields 642 vertices/tiles).

### Dual Graph Representation (The Gameplay Grid)
While the geometry is composed of triangles, the gameplay tile grid functions perfectly on the **Vertices**.
* Every vertex on the subdivided icosphere becomes a distinct tile.
* Vertices inherently possess **6 neighbors** (forming a hexagonal logic grid), except for the original 12 vertices of the base icosahedron, which possess **5 neighbors** (forming pentagons).
* This provides a true, uniform Hexagonal/Pentagonal roguelike grid wrapped perfectly around a sphere, analogous to a soccer ball (Fullerene).

---

## 3. Data Structures: Graph vs. Array

Traditional 2D arrays cannot represent a closed geometric sphere. The map must be managed as a mapped Graph memory structure.

```kotlin
enum class TileType { FLOOR, WALL, LAVA, PORTAL }

class SphereNode(
    val id: Int,
    val position: Vector3, // Normalized 3D coordinate of the tile on the sphere
    val neighbors: MutableList<SphereNode>, // Collection of 5 or 6 adjacent nodes
    var type: TileType,
    var entity: Entity? // Player, enemy, item, etc.
)

class GeodesicGrid {
    val nodes: Map<Int, SphereNode>
    // Functions to retrieve neighbors, calculate A* shortest paths, 
    // and retrieve nodes via 3D raycasting from camera.
}
```

---

## 4. Procedural Generation on a Sphere

Traditional flat map generation algorithms (like BSP Trees) rely on Euclidean X/Y coordinates. We adapt core algorithms to operate on graph traversal:

### A. 3D Perlin Noise Mapping (Caves & Terrain)
Instead of 2D noise, sample a **3D Simplex or Perlin Noise** generator using the exact `(x, y, z)` spatial coordinate of each `SphereNode`. If the sampled noise is greater than a threshold, mark it as `WALL`, otherwise `FLOOR`. This naturally wraps around the sphere creating continents and cave chambers.

### B. Drunkard's Walk (Graph-Based)
For organic sprawling dungeons:
1. Start at a random node.
2. Select a random neighbor in the graph.
3. Carve it into a `FLOOR`.
4. Iterate 1,000 times, creating a contiguous winding cavern network on the planet.

### C. Voronoi Tectonic Regions
Choose $N$ random nodes as cluster seeds (e.g., lava biomes, obsidian crystal biomes). Flood-fill outward uniformly across neighbors to partition the globe into distinctive regions.

---

## 5. Overhauled Movement & UI Constraints

A hex/pent grid spanning a sphere lacks an absolute "North" or "South". "Up" on the screen depends entirely on the camera's orientation.

### Camera Orbiting Model
* The camera strictly acts as an orbital satellite looking down.
* The player is consistently locked to the center-screen viewport.
* The camera's "Up" vector aligns with the player's local tangent vector heading forward. As the player traverses the planet, the camera smoothly applies a Quaternion rotation mathematically rotating the planet mesh underneath the player.

### User Input Adaptation
Traditional 4-way D-pads map poorly to a 6-neighbor hexasphere. 
1. **Turn-Based Tap Controls:** The player taps on any adjacent hexagonal/pentagonal tile. A raycast translates the screen 2D $(x,y)$ touch to a 3D intersection on the sphere, identifying the target `SphereNode` ID.
2. **Analog Vector Snapping:** If a virtual joystick is used, the 2D joystick angle is projected onto the 3D local tangent plane relative to the player. The game calculates the Dot Product between the joystick vector and the vectors of all 5 or 6 neighbors, and automatically steps the player into the neighbor yielding the highest alignment.

---

## 6. Rendering Implementation in Jetpack Canvas / OpenGL

To render the Geodesic Grid without relying on an external game engine or complex mapping:

1. **Polygon Construction:** Each `SphereNode` implicitly defines a Voronoi cell on the sphere (a hexagon or pentagon) defined by the centroids of its adjoining triangular faces.
2. **Painter's Algorithm or Z-Buffer Depth Sorting:** 
   * Filter out nodes mathematically situated on the back-face of the sphere (Dot product of node normal and camera view vector < 0).
   * Sort visible nodes by Z-depth away from the camera.
   * Render discrete polygons filling out the sphere.
3. **Tile Shading:** Apply deep-space ambient lighting. Calculate shading based on the dot product of a simulated directional sun vector and the normal vector of the tile, casting shadows smoothly around the curvature of the spherical dungeon.

---

## 7. Migration Steps

* **Phase 1 (Data Engine):** Implement the `IcosphereGenerator` class. It manages the mathematical subdivision loop and constructs the interconnected node graph geometry offline.
* **Phase 2 (Rendering Refactor):** Strip out the existing lat/long projection math in `DungeonCanvas3D`. Render the new graph vertices connecting discrete lines and polygons.
* **Phase 3 (Roguelike Logic):** Update pathfinding logic. `A* Search` calculates heuristic costs using the 3D Euclidean distance $d = \sqrt{(\Delta x)^2 + (\Delta y)^2 + (\Delta z)^2}$ between nodes. Update the combat logic to utilize neighbor index arrays instead of array indices.
* **Phase 4 (Input Handling):** Introduce Raycasting touch input in Compose pointer events or localized vector snapping to resolve spherical click-to-move mapping.

By adapting this graph-based Geodesic structural map, Moria transforms into a deeply impressive, seamless, mathematically exact spherical-world roguelike natively crafted entirely.

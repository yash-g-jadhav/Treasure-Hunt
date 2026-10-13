# The Lost Vault — Expedition Edition

A single-player 2D treasure-hunt adventure in Java. Explore a mysterious island through three connected regions, investigate the fate of Captain Elias Vale, speak with survivors, follow evidence in your journal, search for hidden chambers, collect useful items, solve symbol dials, unlock the ancient gate and Vault, then escape by rescue boat. The world is presented as an orthogonal grid of paths with intersections, loops, branches, landmarks, and a gradually revealed map.

## Requirements

- JDK 17 or newer (JDK 21 also works)
- A graphical desktop session to play the game
- No third-party runtime libraries are required

> **Rendering-stack limitation:** The original specification asks for LWJGL 3, GLFW, OpenGL 3.3, and GLSL 330. The workspace has no downloadable dependencies and this environment is offline, so the runnable implementation uses Java Swing/Java2D instead. It includes animated primitive characters and environment art, but does not claim to satisfy the OpenGL-specific requirement.

## Build, test, and run

The included launch scripts use an installed Gradle distribution if available. If Gradle is not installed, they fall back to the local JDK so the game remains buildable offline.

### Windows PowerShell

From the project directory:

```powershell
.\gradlew.bat clean
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat runGame
```

The Windows script is an offline JDK fallback, not the official Gradle Wrapper distribution. It supports `clean`, `build`, `test`/`logicTest`, and `runGame` without installing Gradle. JDK 17+ must be on `PATH` or configured through `JAVA_HOME`.

### Linux / macOS

```sh
./gradlew clean
./gradlew build
./gradlew test
./gradlew runGame
```

If Gradle is installed, these scripts delegate to Gradle. The equivalent installed-Gradle tasks are `gradle build`, `gradle test`, and `gradle runGame`.

## Controls

| Action | Control |
|---|---|
| Move | W/A/S/D or Arrow Keys |
| Interact / inspect / operate | Space or Enter; click to interact with the nearest nearby object |
| Choose dialogue option | 1, 2, or 3; mouse click also works |
| Solve symbol dials | Number keys in the sequence shown by clues/NPCs |
| Expedition journal | J |
| Inventory / expedition pack | I |
| Cartographer's map | M |
| Mute / enable sound effects | V |
| Pause / resume | Escape |
| Navigate menus | Up/Down or W/S; Enter/Space to select |

## Story

Captain Elias Vale sailed into a supernatural storm while chasing a crown hidden by sailors who defied a tyrant. Mara, a shipwreck survivor, knows why the storm was summoned. A lost cartographer explains the ruins' symbol language. The Keeper reveals that the Vault tests the explorer's intent as much as their ability to find treasure. Clues and conversations are recorded in the expedition journal; dialogue choices can provide supplies, reveal hints, and affect the ending.

## Expedition objectives

1. **The Landing Shore:** Recover at least 3 gems and Clue A, investigate the shore's branching trails, and reach the inland path. Mara can provide a rusty key; use it on the suspicious vine-covered wall to find a hidden cache.
2. **The Overgrown Interior:** Recover at least 5 gems and Clues B and C. Discover the stone dial and enter **2 → 3** (Thorn → Sun), then interact with the ancient gate and reach the exit. The cracked ruin wall hides an optional cache after Clue B is found.
3. **Vault Island:** Recover at least 4 gems. Use the clues to solve the crown dial with **1 → 2 → 3** (Wave → Thorn → Sun). Interact with the Vault, claim the treasure, then interact with the rescue boat. The island collapses after the crown is taken, starting a 45-second escape countdown; running out of time costs a life and leaves a shorter escape window. Promising the Keeper to return the seal and recovering it enables the true ending.

Each level begins with three lives. Hazards cost a life and respawn at the latest checkpoint. Gems and clues remain after an ordinary hit; losing all lives restarts the current level attempt. The gate, Vault, treasure, and boat are gated by explicit state checks.

## Interactive systems

- **Dialogue choices:** NPC conversations offer three selectable questions or promises. Choices add evidence, give items, or influence the ending.
- **Inventory:** Collect the rusty key, Captain's log, cartographer's compass, stone dial token, Keeper's Promise, and Keeper's Seal.
- **Clue-driven puzzles:** The Thorn/Sun gate dial and Wave/Thorn/Sun crown dial accept ordered input; incorrect sequences reset.
- **Secrets and switches:** Hidden walls reveal caches. Mechanisms remove blocked geometry and record discoveries in the journal.
- **Exploration map:** A minimap reveals nearby grid cells as the player explores. Press M for the larger cartographer's map; discovered cells remain visible for the current level.
- **Journal:** Clues, NPC conversations, inscriptions, secrets, and puzzle solutions become expedition entries.
- **Audio feedback:** Small sound effects are synthesized locally for pickups, clues, hazards, secrets, puzzles, and treasure. Audio device failures are ignored safely; press V to mute. Background music is not included.
- **Persistence:** The highest unlocked level and discovered clue IDs are stored in `~/.the-lost-vault/save.properties` (on Windows, under the user's home directory). A Continue Expedition option appears after progress has been saved. A new expedition starts from Level 1.

## Architecture

- `GameModel`: explicit game states, hand-authored level layouts, movement/collision, clues, inventory, dialogue choices, puzzles, secrets, checkpoints, progression, and persistence.
- `Rect` / `UnlockCondition`: small deterministic, testable geometry and progression primitives.
- `Main.GamePanel`: Swing window lifecycle, keyboard/mouse input, camera, grid-path rendering, minimap, HUD, menus, dialogue, journal, inventory, map, puzzle screens, and primitive animation.
- `AudioManager`: generated sound effects with graceful fallback when no audio line is available.
- `GameLogicTest`: dependency-free headless regression harness.

World coordinates use `(0,0)` at the top-left; the world is 1800 × 1200 units. Level data is hand-authored in `GameModel.createLevel` because the referenced `Design.md` layout source was not supplied.

## Verification

Run:

```sh
./gradlew clean
./gradlew build
./gradlew test
```

The dependency-free regression suite covers movement normalization, AABB overlap, unlock conditions, collectible de-duplication, hazards/lives/checkpoints, level order, clue-driven gate and Vault puzzles, treasure uniqueness, boat gating, NPC choices, inventory rewards, hidden caches, map/inventory state, and end-to-end progression to Victory.

## Known limitations

- Rendering is Java2D/Swing, not the requested LWJGL/OpenGL/GLSL pipeline. No official Gradle Wrapper JAR/distribution is bundled; `gradlew` and `gradlew.bat` are offline fallback scripts.
- There is no background music, controller support, full sprite-sheet animation, or full save-state restoration (save stores highest unlocked level and clue IDs, not player position, gems, or puzzle state).
- Grid paths are drawn as an orthogonal network, while navigation is free-roaming with AABB obstacles rather than tile-by-tile movement. Level geometry still needs a complete human-controlled playthrough for route readability and balance.
- Automated model tests are not a substitute for a full manual playthrough. See `docs/VerificationReport.md` for checks actually performed.

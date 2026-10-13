# The Lost Vault

A single-player 2D treasure-hunt adventure in Java. Explore three hand-authored islands, collect gems and clue scrolls, avoid hazards, use checkpoints, unlock the ancient gate and Lost Vault, recover the treasure, and interact with the rescue boat to win.

## Requirements

- JDK 17 or newer
- A graphical desktop session to play the game
- No third-party runtime libraries are required

> **Rendering-stack note:** The supplied build specification recommends LWJGL 3 / GLFW / OpenGL 3.3. This workspace did not contain an existing project, Gradle was not installed, and network access to fetch Gradle/LWJGL artifacts was unavailable. To deliver a runnable and testable game rather than unbuildable dependency-bound source, this implementation uses a single Java Swing `JPanel` with Java2D drawing and keyboard/mouse input. Gameplay rules are independent of rendering. This is a documented deviation: it does not satisfy the specification's LWJGL/OpenGL-specific rendering requirement.

## Build and run

On a machine with Gradle installed, use the standard Gradle tasks:

```sh
gradle build
gradle logicTest
gradle runGame
```

The included `gradlew` script detects installed Gradle and delegates to it. If Gradle is not installed, it falls back to the local JDK for dependency-free compilation and test execution:

```sh
./gradlew clean
./gradlew build
./gradlew test
./gradlew runGame
```

On Windows, `gradlew.bat` delegates to an installed Gradle distribution; the JDK fallback is currently provided by the Unix `gradlew` script.

## Controls

| Action | Control |
|---|---|
| Move | W/A/S/D or Arrow Keys |
| Interact / collect / open | Space or Enter |
| Pause / resume | Escape |
| Navigate menus | Up/Down or W/S; Enter/Space to select |
| Mouse | Click menu choices; click in-world to interact |

## Objective

1. **The Landing Shore:** Collect at least 3 gems, recover Clue A, and reach the inland path.
2. **The Overgrown Interior:** Collect at least 5 gems and Clues B and C. Approach and interact with the ancient gate after finding both clues, then reach the exit.
3. **Vault Island:** Collect at least 4 gems. The clues from all previous levels unlock the Vault; approach and interact with it to reveal the treasure, collect the treasure, then approach and interact with the rescue boat.

Each level starts with three lives. Hazards cost a life and respawn at the most recent checkpoint. Gems and clues survive an ordinary life loss; exhausting all lives restarts the current level attempt. Escape pauses simulation.

## Architecture

- `GameModel`: explicit game states, level progression, collectible state, unlock conditions, lives, checkpoints, hazards, and victory rules.
- `Rect` / `UnlockCondition`: small deterministic, testable geometry and progression primitives.
- `Main.GamePanel`: window lifecycle, keyboard/mouse input, camera, world rendering, HUD, menus, and simple hierarchical player animation.
- `GameLogicTest`: dependency-free headless regression checks for core rules.

Level layouts are hand-authored in `GameModel.createLevel`. Coordinates use world units with `(0,0)` at the top-left. The world is 1800 × 1200 units; the camera follows the player and clamps to those bounds.

## Tests

```sh
./gradlew test
# or, with Gradle installed:
gradle logicTest
```

The test suite covers movement normalization, AABB overlap, unlock conditions, new-run state, gem and clue deduplication, hazard life loss/checkpoint respawn, exhausted-life restart, locked-level rejection, Vault/treasure uniqueness, boat gating, and pause/resume.

## Known limitations

- The requested LWJGL 3 / OpenGL 3.3 renderer and GLSL shader pipeline are not implemented; Java2D is used for reliable offline buildability in this environment.
- Audio, persistence/save files, minimap, and level select are optional features and are not implemented.
- The game was launched under Xvfb and the Main Menu → Level 1 gameplay UI was smoke-tested. A complete human-controlled graphical playthrough was not performed; end-to-end progression is covered by the automated model integration test.

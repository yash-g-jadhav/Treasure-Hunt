# Implementation Notes

## Source specification boundaries

The provided one-shot build specification says `Rules.md`, `Architecture.md`, and `Design.md` were referenced by the source roadmap but not supplied. Their contents and exact object coordinates therefore could not be reproduced. The three levels are hand-authored in `GameModel.createLevel` with coordinates and object counts chosen for the stated completion requirements.

## Environment-driven rendering choice

No existing game repository was mounted in the workspace. The environment has JDK 21 but no installed Gradle and cannot resolve external hosts, so Gradle Wrapper distribution files and LWJGL dependencies could not be fetched. To make the game and headless regression suite runnable without network access, the delivered implementation uses Java2D in one Swing panel. It is a deliberate and documented deviation from the requested LWJGL 3 / GLFW / OpenGL 3.3 stack, not a claim that those graphics requirements were met.

The `gradlew` shell script delegates to Gradle if installed; otherwise it provides an offline JDK-only fallback for build/test/run tasks. It is not the official Gradle Wrapper JAR/distribution. The graphical app was launched under Xvfb for a Main Menu → Level 1 smoke test, but no complete human-controlled playthrough was performed.

## Gameplay choices

- Coordinates are world units with top-left origin; the world is 1800 × 1200 units.
- Level exits require their listed gem/clue/gate objectives.
- Level 3's Vault requires Clues A, B, and C gathered during the current expedition. The gem threshold is zero beyond the level's own four-gem completion requirement.
- The rescue boat requires treasure collection and an explicit interaction.
- Gems and clues are retained on ordinary respawn; exhausting all lives restarts the current level and clears that level's clue/gem attempt progress.
- Checkpoints are represented as one-use touch triggers, with the initial spawn as checkpoint 0.
- Audio and save persistence are omitted because they are optional in the supplied specification.

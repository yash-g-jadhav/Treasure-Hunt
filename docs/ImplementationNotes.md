# Implementation Notes

## Decisions made where source design files were unavailable

- The build specification references `Rules.md`, `Architecture.md`, and `Design.md`, but those files were not present. The game therefore uses hand-authored coordinates in `GameModel.createLevel`, with three orthogonal grid-trail networks and optional branches.
- The required language is Java 17+. The implementation has no third-party runtime dependencies so it can compile and run in an offline environment.
- The renderer uses Swing/Java2D rather than LWJGL/GLFW/OpenGL 3.3/GLSL. This is a known specification deviation, not an equivalent implementation of the required OpenGL pipeline.
- The game uses a lightweight `GameModel` plus a Java2D `GamePanel`, not a full ECS. Interactable objects are handled once per interaction and the nearest eligible object is selected to avoid activating multiple nearby objects at once.
- The grid map reveals cells around the player. Level navigation remains free-roaming with AABB collision rather than tile-step movement; grid paths are the visual exploration structure.
- NPC dialogue choices use number keys 1–3 or mouse clicks. The cartographer teaches the Thorn → Sun sequence for the Level 2 gate. The crown constellation uses Wave → Thorn → Sun. Wrong inputs reset the sequence.
- Mara's supply choice grants a rusty key that opens the Level 1 hidden wall. Optional caches provide evidence/items; the Keeper's Promise plus the Keeper's Seal enables the true ending.
- Local progress is stored as a small properties file at `~/.the-lost-vault/save.properties`. It stores the highest unlocked level and known clue IDs. It does not serialize exact player position, collected gems, inventory, or puzzle state.
- Sound effects are synthesized locally using Java Sound and fail silently if no output line is available. Background music is omitted.

## Reliability details

- `Rect` and `UnlockCondition` remain small deterministic value/rule objects.
- Movement uses normalized vectors, delta time, and axis-separated obstacle resolution.
- Level progression is explicitly ordered. A gate needs its clue condition and puzzle solution; the Vault needs all clues and its constellation solution; the boat requires treasure.
- Collectible IDs are stable and duplicate collection is ignored.
- The test harness is dependency-free so it can run with only a JDK in offline environments.

## Known limitations

- OpenGL shader pipeline and native-resource lifecycle from the original spec are not implemented because this version uses Java2D.
- There is no background music or controller support.
- The full game has been tested through deterministic model integration tests; manual graphical playthrough and level-balance review remain necessary.

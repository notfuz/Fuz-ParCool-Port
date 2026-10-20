# Building ParCool (Architectury, Minecraft 1.21.4)

Requires JDK 21 or newer. The toolchain is declared through
`java { toolchain { languageVersion = 21 } }` in `build.gradle`, and `org.gradle.java.home` is not
set: there is no machine-specific path anywhere in the build.

**Run Gradle with a JDK the wrapper supports.** Gradle 8.10.2 caps at Java 23 and Gradle 9.4.1 at
Java 24, so a machine whose default `java` is 25 has to set `JAVA_HOME` to a 21 installation for
every invocation:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build
```

Versions (all in `gradle.properties` / `settings.gradle`): Minecraft 1.21.4, Architectury API 15.0.3,
NeoForge 21.4.158, Fabric Loader 0.16.14, Fabric API 0.119.4+1.21.4, Architectury Loom 1.17.493,
ModDevGradle 1.0.9, Gradle 9.4.1. The Architectury API choice is not the obvious one — 16.1.4 is a
1.21.5 build wearing a `~1.21.4-` label and hard-crashes on 1.21.4 from Architectury's own mixin; the
full comparison is in the comment block above `architectury_api_version` in `gradle.properties`.

### Why Loom 1.17 / Gradle 9.4 for a 1.21.4 target

The three are coupled, and the coupling is not about Minecraft:

* Architectury Loom refuses to set up a mod built with a newer Loom than its own, and
  `architectury-fabric 16.1.4` was built with Loom 1.10.1 — that is the **floor**.
* The newest published `architectury-plugin` (3.5.170) calls
  `LoomGradleExtension#disableObfuscation()`, which exists only in Loom 1.17.x — that is the
  **ceiling**.
* Loom 1.17.x needs Gradle 8.11 or newer.

So the only combination that configures at all is Loom 1.17.493 on Gradle 9.4.1. Loom 1.7.435 fails
with *"Mod was built with a newer version of Loom (1.10.1)"*, and Loom 1.10.455 fails with
*"'boolean net.fabricmc.loom.LoomGradleExtension.disableObfuscation()'"*.

One more consequence: **ModDevGradle 1.0.x's internal `RepositoriesPlugin` does not run on Gradle
9**, so `neoforge/build.gradle` writes `https://libraries.minecraft.net/` and
`https://maven.neoforged.net/mojang-meta/` out itself. Without them, `:neoforge` fails with
"Could not find com.mojang:jtracy:…".

| Module      | Toolchain | Contents |
|-------------|-----------|----------|
| `common`    | `dev.architectury.loom` | The whole mod, loader agnostic: registries (Architectury `DeferredRegister`), the `ParCoolData` player-data subsystem, the payload plumbing, the JSON `ConfigSpec`, the Architectury event bridge, the vanilla mixins and the `platform` seam. |
| `fabric`    | `dev.architectury.loom` | `ParCoolFabric` / `ParCoolFabricClient` entrypoints, `FabricParCoolPlatform`, `FabricParCoolNetwork`. |
| `neoforge`  | `net.neoforged.moddev` | `ParCoolNeoForge` / `ParCoolNeoForgeClient` entrypoints, `NeoForgeParCoolPlatform`, `NeoForgeParCoolNetwork`, and the Paraglider / EpicFight / BetterThirdPerson integrations. |

## Building

```bash
./gradlew :common:build   # on a clean checkout, once
./gradlew build           # from then on
```

**Two steps on a clean checkout, and that is not a bug in the wiring.** Architectury Loom resolves
the `:common` project dependency while it *configures* `:fabric`, i.e. before any task runs, so
`:common`'s jar has to exist. The root `build` task depends on `bootstrap` (an alias of
`:common:build`), but that dependency is only evaluated *after* configuration, so a truly fresh
checkout has to be primed once by hand. The root `build` covers every case except that first
invocation, and the same is true of the other ParCool branches in this repository.

**If you add a new class to `:common` and a loader module then reports
`cannot find symbol` for it, Loom's cached remap of `:common` is stale.** Clear it once:

```bash
rm -rf .gradle/loom-cache common/build/devlibs common/build/loom-cache fabric/build/loom-cache
```

Do **not** reach for `./gradlew --stop` to "fix" a Loom problem: it stops every Gradle daemon on the
machine, including ones serving unrelated projects. The cache directories are per-project.

`.gradle/loom-cache/remapped_mods` holds the per-consumer remapped copy of `:common` that the loader
modules actually load; if a freshly added mixin class is present in the built jar but reported as
"not found" at runtime, this directory is the stale one.

## Distributables

```bash
./gradlew build
# -> fabric/build/libs/parcool-1.21.4-3.4.3.3-fabric.jar
# -> neoforge/build/libs/parcool-1.21.4-3.4.3.3-neoforge.jar
```

Each contains the `:common` code and assets plus the loader module's own classes, metadata
(`fabric.mod.json` / `META-INF/neoforge.mods.toml`), the shared `parcool.accesswidener` and
`parcool-common.mixins.json`, and the `ServiceLoader` file that binds `ParCoolPlatform`.

The Fabric one is assembled from the two `remapJar` outputs (this module's first, so its remapped
classes and its `v2 intermediary` access widener win the `EXCLUDE`), and the NeoForge one from
`:common`'s `transformProductionNeoForge` artifact plus this module's own output. The reasoning
behind both is in the comments in `fabric/build.gradle` and `neoforge/build.gradle`.

## Running

```bash
./gradlew :fabric:runClient      :fabric:runServer
./gradlew :neoforge:runclient    :neoforge:runserver
```

The dev runs pass `-Dmixin.debug=true -Dmixin.debug.verbose=true`, so the log lists every applied
ParCool mixin. A dedicated
server refuses to start until it is acknowledged, so before the first `:fabric:runServer` /
`:neoforge:runserver` create `<module>/run/eula.txt` containing:

```
eula=true
```

Those `run/` directories are generated by the dev tasks and are not checked in.

`mixin.debug` can be switched for `mixin.debug.export` (writes `<module>/run/.mixin.out/`, one
transformed class per file) when a specific mixin needs inspecting.

### Booting straight into a world / a server

The cross-loader check ("does a client of one loader actually join a server of the other one?") and
the in-world render pass are driven by the game's own quick-play flags, so they can be repeated
without a mouse:

```bash
# client of one loader -> dedicated server of the other one
./gradlew :neoforge:runServer                                   # waits for "Done (!)"
./gradlew :fabric:runClient -PparcoolQuickPlay=127.0.0.1:25566

# straight into a saved world, i.e. the render path with the mod's blocks in it
./gradlew :fabric:runClient  -PparcoolQuickPlayWorld="New World"
./gradlew :neoforge:runClient -PparcoolQuickPlayWorld="New World"
```

The dedicated servers need `online-mode=false` in `<module>/run*/server.properties` (offline dev
login) and a free `server-port`.

## Loader independence check

`./gradlew :common:checkCommonLoaderIndependence` fails the build if `common/src/main` ever imports
`net.fabricmc.*` or `net.neoforged.*`. It is wired into `:common:check`, so plain `./gradlew build`
runs it.

## Access widening

Some vanilla members are private or protected and are used by ParCool, so they are widened on both
loaders:

* `common/src/main/resources/parcool.accesswidener` — Fabric. Loom applies it to the dev Minecraft
  jar, validates it with `./gradlew :common:validateAccessWidener`, and remaps it to
  `v2 intermediary` for the published jar.
* `neoforge/src/main/resources/META-INF/accesstransformer.cfg` — NeoForge, in **mojmap** names,
  because the NeoForge production runtime loads mojmap-named vanilla members.

The widener holds eleven entries: `Entity#onGround`, `LivingEntity#swimAmount` / `#swimAmountO`,
`ArgumentTypeInfos#register`, `Player#canPlayerFitWithinBlocksAndEntitiesWhen`,
`LevelResource#<init>(String)`, and the six members `client/renderer/RenderTypes` needs
(`RenderType#create` plus the five `RenderStateShard` fields).

The two files are **not** the same list, and should not be made into one. The widener is a hard
requirement: a missing entry is a `private`/`protected` compile error, and a wrong descriptor there is
worse than none. The access transformer is not — NeoForge widens some of these through its own patches,
and an entry for an already-public member is harmless. So the NeoForge file carries the five members
that are non-public in the 21.4.158 runtime — `Entity#onGround`, `LivingEntity#swimAmount` /
`#swimAmountO`, `DamageSources#*()`, `Player#canPlayerFitWithinBlocksAndEntitiesWhen` — and leaves out
the render ones, which 21.4.158 already exposes. When you widen a new member on Fabric, decide on the
NeoForge side from the runtime, not from the widener.

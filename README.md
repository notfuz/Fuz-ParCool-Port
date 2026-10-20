# ParCool — Architectury API port

A multiloader port of the Minecraft mod **ParCool** by **alRex_U** (LGPL-3.0), originally built for
NeoForge, reworked onto the [Architectury API](https://github.com/architectury/architectury-api) so
that one codebase ships both a **Fabric** and a **NeoForge** artifact.

Minecraft **1.21.4**, Java **21**, Gradle **9.4.1**.

The mod itself is unchanged: 26 actions, the zipline and hook blocks, the stamina system, the full
animation set and the settings screens are all there, and the four optional NeoForge integrations are
wired. Both loaders have been run in a world and in a real Prism instance — see
[Status](#status) for what that covered.

### Packaging

The Fabric distributable is built from `:common`'s `remapJar` output, so it ships intermediary
bytecode with the mixin targets statically remapped into it, and there is deliberately **no
refmap**: Loom 1.17 defaults to `mixinRemapType = static` and fabric-api ships none either. The
access widener in the Fabric jar is in `v2 intermediary`, which is what a production Fabric client
requires.

The NeoForge distributable ships mojmap-named bytecode, because that is what the NeoForge production
runtime loads: NeoForge 21.4 has no SRG step in front of a mod jar. `accesstransformer.cfg` is
written in the same naming. architectury-plugin's `neoForge()` transform is therefore not used; it
needs a Loom-based NeoForge setup, and the assembly is done explicitly instead.

**Known limitation:** the ShoulderSurfing integration is NeoForge-only in practice. If ShoulderSurfing
ships a `PluginLoader` for NeoForge and none for Fabric, the `shouldersurfing_plugin.json` in the
Fabric jar is never read, and the decoupled-camera hook is absent on Fabric rather than broken there.

> This repository contains a *port*, not alRex_U's original sources. The original project lives at
> <https://github.com/alRex-U/ParCool> and on CurseForge at
> <https://www.curseforge.com/minecraft/mc-mods/parcool>. All credit for the mod's design and assets
> goes to alRex_U; this repository only carries the loader-abstraction work.

## Modules

| Module     | Contents                                                                                                                                                        |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `common`   | The whole mod: registries, `ParCoolData` (replacement for NeoForge's attachment API), the network layer, the JSON config backend, the vanilla mixins, and the platform seam. |
| `fabric`   | Fabric entrypoints, `FabricParCoolPlatform` / `FabricParCoolNetwork`, and the ServiceLoader binding.                                                                |
| `neoforge` | NeoForge entrypoints, `NeoForgeParCoolPlatform` / `NeoForgeParCoolNetwork`, plus the optional Paraglider, EpicFight and BetterThirdPerson integrations.           |

There is intentionally **no source-set split**: ParCool is client-authoritative, and NeoForge ships a
merged jar, so `common` stays a single tree. The two loader modules differ only in their entrypoints
and their platform implementation. `./gradlew :common:checkCommonLoaderIndependence` fails the build
if `common/src/main` ever imports `net.fabricmc.*` or `net.neoforged.*`.

## Requirements

* JDK 21 or newer (pinned through the Gradle toolchain in `build.gradle`, no machine-specific path
  needed)

## Building

```bash
./gradlew :common:build   # on a clean checkout, once
./gradlew build           # from then on
```

See [BUILDING.md](BUILDING.md) for the module layout, the dev run tasks
(`:fabric:runClient`, `:neoforge:runclient`, `:fabric:runServer`, `:neoforge:runserver`) and the
loader-independence check.

## Installing

Drop the matching jar into `mods/`:

* Fabric — `fabric/build/libs/parcool-1.21.4-3.4.3.3-fabric.jar`
* NeoForge — `neoforge/build/libs/parcool-1.21.4-3.4.3.3-neoforge.jar`

## Dependencies

**Required**

* Architectury API 15.0.3 or newer. 16.1.4 declares `~1.21.4-` but is a 1.21.5 build: it pulls a
  1.21.5 fabric-api and its own mixin targets a post-1.21.4 signature, which hard-crashes the boot.
  The comparison is in `gradle.properties`.
* Fabric: [Fabric API](https://modrinth.com/mod/fabric-api) 0.119.4+1.21.4, Fabric Loader 0.16.14+
* NeoForge 21.4.0 or newer
* MixinExtras 0.4+ — bundled by both loaders, declared as an *optional* dependency in both mod
  descriptors because `common/…/mixin/common/PlayerMixin` uses `@WrapWithCondition` from it. It stays
  optional on purpose: declaring it required makes NeoForge refuse to start, because the loaders
  bundle it rather than listing it as a mod.

**Optional**

* [Patchouli](https://modrinth.com/mod/patchouli) — in-game guide
* [ShoulderSurfing](https://www.curseforge.com/minecraft/mc-mods/shouldersurfing) — compiled against
  the 1.21.4 build; a version for a different Minecraft version reports itself absent and ParCool
  falls back to its own camera handling
* [EpicFight](https://www.curseforge.com/minecraft/mc-mods/epicfight) — NeoForge only. **EpicFight has
  no 1.21.4 build on CurseForge**, so the integration is compiled against its 1.21.1 API but reports
  itself absent at runtime (`EpicFightManager#isEpicFightUsable` checks the class is loadable) and
  ParCool falls back to its own stamina system.
* BetterThirdPerson — NeoForge only, compiled against its 1.21.4 build and reported absent on a
  client that does not have it
* [Paraglider](https://www.curseforge.com/minecraft/mc-mods/paraglider) — NeoForge only. **Paraglider
  publishes no 1.21.4 build**, so the integration is compiled against its 1.21.1 API and reports
  itself absent otherwise.

## Status

`./gradlew build` succeeds and `checkCommonLoaderIndependence` passes.

Checked in a running game, on both Fabric and NeoForge: the loaders boot with the mod loaded, the two
ParCool attributes resolve on the first `Player#createAttributes`, parkour and the zipline (including
the rope render) work, the stamina HUD and the settings screen behave, the rope takes its dye colour,
key bindings can be rebound, and progress survives saving and reloading a world. Both jars were also
installed into a real Prism instance.

Not covered: a dedicated NeoForge server, a cross-loader join (NeoForge client against Fabric server)
and two players in one world.

## Attribution and license

ParCool was created by **alRex_U** and is licensed under the
**GNU Lesser General Public License v3.0** — the full text is in [LICENSE](LICENSE).

The Architectury port keeps the same license, mod id, version and project links. Contributions to
the port itself are welcome under the same terms.

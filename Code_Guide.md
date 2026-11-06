# FBX Player Models Code Guide

## Release variants and loader support

This release maintains the full mod on Fabric 1.21.1 and 26.3 and adds client-only Fabric 1.21.1, Fabric 26.3, Forge 1.21.1, and Babric/StationAPI Beta 1.7.3 variants. Each target produces a separate jar; there is no universal jar. NeoForge, Quilt, and Architectury are not used.

The full Fabric variants support single-player and Open to LAN model sharing. Fabric 1.21.1 Client Only, Fabric 26.3 Client Only, Forge, and Beta retain local-player rendering only and register no model-sharing packets, server stores, or FBX entities. The client-only ports retain their configuration formats, paths, and mod identifiers; their display names describe the client-only variant.

`common` serves the full modern Fabric mod, both client-only Fabric variants, and Forge, including their shared Java 21 shape/voice/model/config code. Forge compiles the common sources directly into its own source set to avoid split Java packages. `client-common` retains the separate Java 17 Lite model/config/platform code for Beta, which includes its compiled output. No jar includes both common variants, avoiding duplicate model/config classes.

`session-policy/src/main/java` is a small shared source directory, compiled into `common`, `client-common`, and Forge. It has no Minecraft, loader, or rendering dependencies.

## Supported targets

The repository maintains two full-mod Fabric targets, plus the four client-only ports described below. Each loader module has its own version-sensitive source, resources, metadata, and production jar. Full Fabric, both client-only Fabric variants, and Forge use `common`; Beta uses `client-common`. Neither shared module is a standalone Minecraft distribution.

### Target and toolchain versions

| Module | Minecraft | Game namespace | Java | Fabric Loom | Fabric Loader | Fabric API |
| --- | --- | --- | --- | --- | --- | --- |
| `fabric-1.21.1` | `1.21.1` | Yarn `1.21.1+build.3` | 21 | `1.16.2` | `0.16.14` | `0.116.6+1.21.1` |
| `fabric-1.21.1-client` | `1.21.1` | Yarn `1.21.1+build.3` | 21 | `1.16.2` | `0.16.14` | `0.116.6+1.21.1` |
| `fabric-26.3` | `26.3` | unobfuscated game names; no Yarn | 25 | `1.17.21` | `0.20.1` | `0.165.2+26.3` |
| `fabric-26.3-client` | `26.3` | unobfuscated game names; no Yarn | 25 | `1.17.21` | `0.20.1` | `0.165.2+26.3` |

### Mod and production-build versions

| Module | Mod version | Mod Menu | LWJGL Assimp/NFD | Build task | Production jar |
| --- | --- | --- | --- | --- | --- |
| `fabric-1.21.1` | `2.0.0+1.21.1` | `11.0.3` | `3.3.3` | `:fabric-1.21.1:remapJar` | `fbx-player-models-v2.0.0+1.21.1+mc1.21.1.jar` |
| `fabric-1.21.1-client` | `2.0.0+1.21.1` | `11.0.3` | `3.3.3` | `:fabric-1.21.1-client:remapJar` | `fbx-player-models-client-v2.0.0+1.21.1+mc1.21.1.jar` |
| `fabric-26.3` | `2.0.0+26.3` | `21.0.0` | `3.4.3` | `:fabric-26.3:jar` | `fbx-player-models-v2.0.0+26.3+mc26.3.jar` |
| `fabric-26.3-client` | `2.0.0+26.3` | `21.0.0` | `3.4.3` | `:fabric-26.3-client:jar` | `fbx-player-models-client-v2.0.0+26.3+mc26.3.jar` |

`fabric-1.21.1` uses Yarn and produces its production artifact through Loom's `remapJar` task. Both Minecraft 26.3 variants expose unobfuscated game names, so those modules deliberately omit Yarn and produce their production artifacts through `jar`.

Production jars are written to `<module>/build/libs/`. Modern Fabric jars include the `common` output and bundle jgltf `2.0.4`, Native File Dialog, and Assimp natives for Windows, Linux, and macOS on x64 and arm64. The Gradle wrapper is `9.6.0`. DevAuth is development-only: Fabric 1.21.1 uses `1.2.1`, while both Fabric 26.3 variants use `1.2.2`. The imported ports have separate dependencies and toolchains described below.

## Manual build commands

Build and publication are user-triggered. After retiring Fabric 26.2, the prescribed compilation command covers the full and client-only variants for Minecraft 1.21.1 and 26.3:

```bash
bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.3:compileJava :fabric-26.3:compileClientJava :fabric-1.21.1-client:compileJava :fabric-1.21.1-client:compileClientJava :fabric-26.3-client:compileJava :fabric-26.3-client:compileClientJava
```

Use Java 25 or newer to launch the combined compilation. Source release levels remain Java 21 for 1.21.1 and Java 25 for 26.3. Dated validation entries below preserve earlier results; references to 26.2 in those entries are historical and their old commands no longer apply to the active build.

Production packaging commands for the user to trigger individually:

| Variant | Gradle runtime / compiler | Packaging command | Production filename under `<module>/build/libs/` |
| --- | --- | --- | --- |
| Fabric 1.21.1 | Java 21+ / release 21 | `bash ./gradlew :fabric-1.21.1:remapJar` | `fbx-player-models-v2.0.0+1.21.1+mc1.21.1.jar` |
| Fabric 1.21.1 Client Only | Java 21+ / release 21 | `bash ./gradlew :fabric-1.21.1-client:remapJar` | `fbx-player-models-client-v2.0.0+1.21.1+mc1.21.1.jar` |
| Fabric 26.3 | Java 25+ / release 25 | `bash ./gradlew :fabric-26.3:jar` | `fbx-player-models-v2.0.0+26.3+mc26.3.jar` |
| Fabric 26.3 Client Only | Java 25+ / release 25 | `bash ./gradlew :fabric-26.3-client:jar` | `fbx-player-models-client-v2.0.0+26.3+mc26.3.jar` |
| Forge 1.21.1 | Java 21+ / Java 21 toolchain | `bash ./gradlew :forge-1.21.1:jarJar` | `fbx-player-models-forge-v2.0.0+1.21.1+mc1.21.1.jar` |
| Babric Beta 1.7.3 | Java 21+ / Java 17 toolchain | `bash ./gradlew :fabric-b1.7.3:remapJar` | `fbx-player-models-v2.0.0+b1.7.3+mcb1.7.3.jar` |

The imported Forge port uses Forge `52.1.16`, ForgeGradle `7.0.40`, JarJar `0.2.3`, official Mojang mappings, and Java 21. Its ordinary `jar` has the `slim` classifier; publish only the `jarJar` production artifact containing jgltf/Jackson dependencies and the existing NFD/Assimp native resources.

Beta uses BINY `b1.7.3+e1fe071`, Fabric Loader `0.19.3`, StationAPI `2.0.0-alpha.6.4`, Loom `1.16.3`, and Babric Loom Extension `1.17.2`. Its Gradle daemon runs on Java 21 or newer, while Beta and `client-common` use a Java 17 compiler and bytecode. The manual workflow explicitly installs that compiler. The imported Foojay resolver supports Gradle toolchain discovery/downloads.

`settings.gradle` recognizes both `fabric-*` and `forge-*` explicit task paths and configures only requested loader targets. Prefer explicit target tasks so unrelated loader plugins and toolchains do not need to resolve together.

## Fabric 1.21.1 client-only features and comparison

`fabric-1.21.1-client` originated from the user-specified `../FBXPlayerModels-lite/fabric-1.21.1` port and now includes the full version's requested local Default shape keys, Voice Shape, and UpAxis-aware FBX pipeline. The source repository was read only. The module retains client-only metadata/entrypoints/mixins and registers no main/server initializer, FBX model-sharing payloads/receivers, or entity types.

The Gradle `localModelFeatures` source-directory set compiles selected files directly from `fabric-1.21.1/src/client/java`: FBX/GLB/OBJ importers, model/animation math, normalization, shape-aware cache/geometry classes, Shape Key Settings, Voice Shape Settings, and the searchable Voice Target picker. The prior Lite copies of these files were removed from the client-only source tree. Both variants therefore compile the same importer, deformation, animation, and feature-editor implementations. Full-mod networking, remote-player state, server classes, and entity registration are outside the source selection.

| Feature | Full `fabric-1.21.1` | `fabric-1.21.1-client` |
| --- | --- | --- |
| Local FBX selection/reset, saved model settings and menu preview | Present | Present |
| Auto/manual rig binding, clip mappings, animation toggle | Present | Present; same model/animation code |
| First-person model, camera offsets, held-item integration | Present | Present; local cache/render adapters |
| Default shape keys, sliders/reset and centered preview | Present | Present; same feature editor and deformation code |
| Voice Shape bone/key mapping, input selection and live/manual preview | Present | Present; same editors, local-only pose source |
| Local microphone / optional Simple Voice Chat input | Present | Present; same audio input/envelope classes |
| UpAxis-aware import and binary shape recovery | Present | Present; same importers and `FbxCoordinateSpace` |
| Rendering remote custom player models and LAN model/settings transfers | Present in supported integrated LAN sessions | No; local player only |
| FBX view entity and mob variants | Present | No |
| Connection eligibility | Integrated single-player or approved LAN host | Single-player or resolved local-network address |

The client-only target now includes `common`, not `client-common`, so `ClientSkin` retains Default shape profiles and Voice Shape settings alongside its existing rig/clip choices. Missing legacy fields initialize to zero shape weights and a disabled Voice Shape mapping. Its settings toggle uses the shared saved enabled preference through getters/setters. Forge now uses the same common data classes through its feature port below; Beta keeps its existing Lite data/models.

`SkinManager` owns a single hash-bound local cache. Selecting a model loads the shared importer/normalizer and reapplies saved rig, animation, and Default shape settings. A shape slider reshapes the cached model from immutable base geometry without reparsing or accumulating deformation, then saves locally. Voice settings save locally and the local `VoiceShapeClient` produces a frame pose only for the cached selected model and an existing imported target. No upload, download, remote lookup, settings sync, or voice-level relay is implemented.

Local renderer/first-person/preview adapters use this cache and the shared shape-aware geometry/voice pose APIs. The settings screen exposes the shared shape and voice editors. Voice Shape composes with Default weights, bone animation and head/limb poses as in the full model pipeline. Editor manual preview and Test Pulse stay preview-local. World sampling requires an eligible world, visible FBX models and a loaded local model; explicit Voice Shape editing can sample without a world. Input stops on model selection/reset, disconnect and client shutdown. The optional `voicechat` entrypoint uses the existing compile-only API and plugin; the FBX feature never sends PCM or animation packets. Simple Voice Chat retains its own independent audio behavior.

The client target's `test` task now depends on `common:test`, which covers the shared model feature contracts and address policy on Java 21-compatible bytecode. Its production build no longer needs a Java 17 shared compiler; the workflow's explicit Java 17 compiler setup remains only for Beta. The eight headless model tests cover signed UpAxis conversion/handedness, unchanged Y-up/invalid-axis behavior, shape-delta scaling without translation or mutation, Default weight/reset rules, legacy save defaults, voice bone interpolation/clamping, and reversed shape-key mappings.

The requested release note remains at `release-notes/fabric-1.21.1-client.md`, including the purpose of future-proofing [Aksu's Peaceful Gameplay Pack](https://modrinth.com/modpack/aksus-peaceful-gameplay-pack). Both release destinations append it automatically for this target, retaining custom notes. It now describes the added local shape/voice/import features. The full and client-only Fabric jars share the `fbx-player-models` mod ID; install only one per game instance. Their filenames and Modrinth versions remain distinct: `fbx-player-models-client-v2.0.0+1.21.1+mc1.21.1.jar` and `2.0.0+fabric-client.1.21.1` for the client-only variant.

Validation for this feature addition includes the four prescribed full Fabric compile tasks plus `:fabric-1.21.1-client:compileJava`, `:fabric-1.21.1-client:compileClientJava`, and `:fabric-1.21.1-client:test`, using JDK 26 and the existing Gradle cache. The first run found a missing `FileUtil` import in the adapted local settings screen; adding it resolved the error, and the same command passed. `compileJava` has no sources for this client-only module; `compileClientJava` successfully compiles the shared feature sources and local adapters. The shared `common:test` suite passed 16 tests (eight local model feature contracts plus eight LAN address-policy cases), and all 20 release-helper tests also passed. Inspection of the compiled client output confirmed the new editors/importers/voice adapter and no FBX protocol/server classes. No microphone or OpenGL display was opened by those tests. Production jar packaging, in-game UI/deformation/audio, and modpack behavior have not been exercised; no release was dispatched or published.

## Fabric 26.3 client-only variant (2026-11-06)

`fabric-26.3-client` ports the local-player behavior of `fabric-1.21.1-client` onto the existing full 26.3 renderer and game APIs. Its metadata declares environment `client`, client/Mod Menu/optional voice-chat entrypoints, and only client mixins. `FBXPlayerModelsMod` is a client-side constants/logger holder, not a main/server initializer. The module registers `/skin` locally and no upload-permission command, model-sharing payload, server store, or FBX entity. Its main Java source set is empty.

The module compiles a selected `localModelFeatures` set directly from `fabric-26.3/src/client/java`: importers, model/animation math, normalization, shape-aware geometry/cache classes, deferred `CustomSkinRenderData`, Shape Key Settings, Voice Shape Settings, and the searchable Voice Target picker. This shares the 26.3 UpAxis-aware importer, binary shape recovery, deformation, live limb/swing poses, and editor behavior. The Gradle project does not depend on the full Fabric module or include its main source set.

The local `SkinManager` keeps one cache bound to the selected model hash, reads/writes the existing `.fbxplayermodels/.config` and `skins` paths, and reapplies saved rig bindings, clip mappings, Default shape profiles, and Voice Shape settings. Local adapters provide title/config/editor previews, first-person body/hidden-head rendering, camera offsets, animation controls, and microphone/optional Simple Voice Chat input. Selection/reset, disconnect, and shutdown stop local audio capture. The config screen contains Select, Reset, ON/OFF, Settings, and Done controls; uploads and sharing are outside this variant.

The living-entity mixin clears custom data before each player extraction and populates it only for the current local player in an eligible connection. Remote players retain vanilla rendering. Its deferred submission consumes that prepared snapshot and honors final inventory pose overrides. Explicit previews construct their own selected-model snapshots, so they remain usable when world rendering is inactive. First-person hands use the complete 26.3 `FirstPersonHandsAndItemsRenderer` descriptor; body transforms use `PoseStack.rotateDegrees`; mouse controls use SDL-aware `InputConstants.MOUSE_BUTTON_LEFT`.

The connection policy matches the 1.21.1 client-only variant: an in-memory connection requires an integrated server, while remote connections must resolve to an allowed local-network address. It exchanges no custom approval packets and requires no server-side installation. The address policy cannot distinguish a dedicated server or proxy behind a private address; it remains a support policy. Unsupported connections retain vanilla gameplay rendering and receive one local status notice per connection.

Build dependencies reuse the full 26.3 properties: Minecraft 26.3, Java 25, Loom 1.17.21, Fabric Loader 0.20.1, Fabric API 0.165.2+26.3, Mod Menu 21.0.0, LWJGL Assimp/NFD 3.4.3, jgltf 2.0.4, and development-only DevAuth 1.2.2. The separate `fabric_263_client_mod_version` is `2.0.0+26.3`. The `jar` task packages common output with the client implementation as `fbx-player-models-client-v2.0.0+26.3+mc26.3.jar`. Full and client-only jars have the same Fabric mod ID and must be installed separately.

Manual build, GitHub release, and CurseForge release selectors include this sixth target and select it through `all`. Central metadata gives both client-only Fabric versions the `-client` filename and `fabric-client` version suffix, keeping full/client artifacts distinct. The new Modrinth-style version is `2.0.0+fabric-client.26.3`. `release-notes/fabric-26.3-client.md` is appended for this target without applying the 1.21.1-only modpack note. Existing release tests cover both variants' filenames, packaging tasks, provenance verification, version labels, and appended notes.

Initial client-only validation, before 26.2 retirement, used the existing test approval: the four then-prescribed Fabric 1.21.1/26.2 compile tasks, the full 26.3 compile tasks, both client-only Fabric compile task pairs, and `:fabric-26.3-client:test` passed together under JDK 26. The initial run found one remaining Yarn `world` field in the local connection-policy adapter; changing it to `level` fixed compilation. Both client-only `compileJava` tasks report `NO-SOURCE`; their `compileClientJava` tasks compile successfully. The new client's test task depends on the 16 shared `common:test` cases; those were already up to date with passing results. All 20 release-helper tests passed again with extended coverage for 26.3 Client Only. Inspection of the 84 compiled client classes found no mod server, protocol, remote-cache, or FBX entity classes. No production packaging, in-game rendering/input/audio, or release/publication was performed.

## Forge 1.21.1 local shape, voice and importer features

The initial audit found that Forge's Lite implementation lacked Default shape keys, Voice Shape and the updated FBX importer. The requested feature port now includes all three. Forge remains a client-only local-player variant for single-player and eligible LAN connections.

| Local feature | Forge 1.21.1 |
| --- | --- |
| Own-player model, menu/editor previews | Present |
| Rig bindings, animation clip mappings/toggle | Present |
| First-person self model and camera controls | Present |
| Default shape-key profiles, sliders, reset and deformation | Present |
| Voice Shape bone/key mapping, input selection and live/manual preview | Present |
| Local microphone / optional Simple Voice Chat input | Present |
| Updated UpAxis-aware importer and binary shape-key recovery | Present |
| Model-sharing protocol and FBX entities | Absent by client-only design |

`forge-1.21.1/build.gradle` now compiles `common/src/main/java` and resources directly, replacing the Lite `client-common` input. This supplies the same `ClientSkin`, shape profiles, signed-axis conversion, voice settings, audio envelope/input and platform contracts as Fabric. The Forge mod ID remains `fbx_player_models_lite`; `.fbxplayermodels/.config` and cached skin paths are retained. Legacy selections, enabled preference, rig/animation mappings and first-person settings remain readable; missing shape/voice fields initialize safely. Fallback/dynamic textures use the common asset namespace, and the legacy fallback namespace remains packaged as an alias.

The full Fabric 1.21.1 importer/model/normalizer sources were ported to Forge's Mojang names. Assimp and binary fallback use `FbxCoordinateSpace`, apply scene/node transforms consistently to geometry and skeletons, and recover shape deltas when needed. Mesh shape keys survive material recovery and normalization. Forge retains its loader-specific texture upload and player-pose adapters. `SkinnedModel` composes Default profiles, authored/procedural animation and local voice, including descendant bone motion, unchanged base geometry and hidden-head first-person rendering.

`SkinManager` binds one local cache to the selected model hash, saves Default profiles without reparsing, and persists Voice Shape mappings without networking. The binding editor now links the ported Shape Key Settings, Voice Shape Settings and searchable target picker; the preview renderer supports centered independent previews with rotation/zoom. The saved model-enabled preference uses the common getter/setter and persists changes.

`VoiceShapeClient` samples input on Forge client ticks for an eligible world with an enabled, loaded model, or while the explicit voice editor is open. Both ordinary and first-person render paths use its local pose after confirming the selected model and imported target. Input stops on model replacement/reset, disconnect and `GameShuttingDownEvent`. Forge platform events now implement join/disconnect callbacks and reload the selected cache on join. The microphone is not opened by tests or by disabled mappings.

Simple Voice Chat remains optional: the API is a compile-only 2.6.0 dependency and an optional version-constrained `voicechat_api` metadata dependency. The Forge-specific plugin uses the documented [`@ForgeVoicechatPlugin` registration](https://modrepo.de/minecraft/voicechat/api/getting_started) and delegates to the shared local PCM/envelope adapter. The API and voice chat implementation are not bundled. FBX registers no model, appearance or voice-level packets, remote state or server/entity code; Simple Voice Chat retains its independent audio behavior.

Validation:

- `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 bash ./gradlew :forge-1.21.1:jarJar :forge-1.21.1:test` passed. The feature port compiled and packaged successfully; the final test run executed 22 tests with zero failures/errors/skips: eight address-policy cases, eight common feature contracts, and six Forge production-model regressions. The new tests exercise non-accumulating Default edits/reset, voice replacing only its target and restoring Default weights, bone rotation reaching descendants while animations are disabled, first-person head hiding under voice motion, normalization preserving scaled shape deltas exactly once, and legacy JSON selections/preferences.
- The four prescribed Fabric compilation tasks passed using JDK 26; their outputs were up to date.
- The production jar passed ZIP integrity inspection. It contains the new feature editors, coordinate conversion, local audio/voice adapters, expanded client-only metadata, translated labels/fallback texture, six JarJar libraries and 20 native files. Voice chat API and test classes are absent. The mod's network packages still contain only LAN address/session policy classes.
- Artifact: `forge-1.21.1/build/libs/fbx-player-models-forge-v2.0.0+1.21.1+mc1.21.1.jar`, 23,522,374 bytes; SHA-256 `64f2029036ff87cf1465506ff88a9a42561ca9d49b4971552a97fa6ea8446e9f`.

The existing deprecated Forge `RenderLevelStageEvent.getPoseStack()` and Gradle compatibility warnings remain. No in-game UI/rendering, real FBX import/native execution, microphone capture, Simple Voice Chat session or LAN gameplay was exercised. No release was dispatched or published. Before release, check a Y-up and Z-up model, an exported shape-key model, slider reset, bone and shape-key voice mappings, manual/Test Pulse preview, first-person head hiding, disconnect cleanup and public-address inactivity in a game instance.

## Beta 1.7.3 stationary sneaking fix (2026-11-06)

`fabric-b1.7.3` uses `BetaPlayerPoseAdapter` for both third-person and hidden-head first-person player geometry. Its walking/sneaking limb calculation previously replaced a near-zero native walking amplitude with a nonzero amplitude and a phase based on player age. Holding sneak while standing still therefore generated a repeating walk cycle, and stopping a sneak walk restarted that artificial cycle as the real walking amplitude faded.

The adapter now always uses `player.walkAnimationProgress` and the interpolated `lastWalkAnimationSpeed`/`walkAnimationSpeed` values. Zero native amplitude leaves the crouched limbs still, while movement continues to animate them and deceleration follows the game's interpolation. The generated crouching posture, head look, held-item/action poses, and animation selection retain their existing paths.

`BetaPlayerPoseAdapterTest` adds three headless regressions using a real mapped Beta client player and a synthetic rig with weighted arms, legs, and head. They check stationary crouching across tick ages/partial ticks in both render paths, sneak walking driven by native gait progress, and interpolation back to the stationary pose after stopping. The stationary/stopping cases failed against the original adapter and passed after removing the fallback. No OpenGL context or external FBX asset is needed.

Validation: `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 bash ./gradlew :fabric-b1.7.3:compileJava :fabric-b1.7.3:test -Dorg.gradle.java.installations.paths=/usr/lib/jvm/temurin-17-jdk-amd64` passed. All four Beta pose tests passed, and the dependent `client-common:test` suite retained its eight passing address-policy cases. The eight modern Fabric compilation tasks required by `AGENTS.md` also passed under JDK 26, with outputs up to date. No production packaging, game launch, or publication was performed; the visual result still needs in-game verification.

## Beta 1.7.3 FBX mesh deformation and normals (2026-11-06)

Analysis of a model exposed two problems in the Beta mesh pipeline. Its mesh contains 5,778 control points and 46 weighted bones, with up to nine influences per control point. The importer previously retained only the first four influences and normalized that subset. This discarded authored weights at 220 control points, in some cases dropping about 85% of their original total weight. `FBXParser.BoneWeights` now retains every finite positive influence and normalizes the complete set, matching the CPU skinning loop's variable-length arrays. The supplied FBX is read only and does not need re-exporting for this correction.

The vanilla Beta `Tessellator.normal` also uses signed-byte packing that lets a negative component overwrite later components and maps +X through 128, wrapping its direction. `LegacyModelRenderer` now supplies independently masked signed components scaled to 127 through the registered `TessellatorAccessor`. It still invokes the ordinary normal method first so existing hooks run. Smooth shading is enabled for FBX surfaces within the renderer's existing push/pop attribute scope, restoring Minecraft's previous state afterward. The correction applies only to the mod's model submissions.

`FBXParserSkinningTest` checks a six-influence synthetic mesh whose dominant head weight is last, including its expected posed vertex position. It also reads the supplied file through the production binary reader/importer when that optional local asset exists. The asset test bypasses GPU texture allocation with cached placeholder IDs, then verifies bind-pose geometry, all nine influences, and decoded GPU-normal directions for static, animated-bind, and head-turned vertices. The imported mesh has 43,752 expanded vertices; its maximum animated bind-pose position error is approximately `5.14e-7` model units. The asset-specific case skips on machines without the local FBX; the synthetic import and `LegacyModelRendererTest` normal-direction cases always run. The normal tests include all six axis directions and mixed-sign vectors, catching byte sign leakage and +X reversal without an OpenGL display.

The sample's filename is anonymized as "a model" in documentation and test diagnostics. The optional fixture defaults to `.temp/a model.fbx`; set `FBX_TEST_MODEL` to use another local path. Gradle tracks the selected fixture as a test input, and the original supplied file is retained separately.

Validation: `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 bash ./gradlew :fabric-b1.7.3:compileJava :fabric-b1.7.3:test -Dorg.gradle.java.installations.paths=/usr/lib/jvm/temurin-17-jdk-amd64` passed with eight Beta tests and no failures/errors/skips, including the supplied-mesh test and existing sneaking/head-pose regressions. The dependent Java 17 address-policy suite retained its eight passing cases. All eight modern Fabric compilation tasks prescribed by `AGENTS.md` passed under JDK 26 with outputs up to date. No production packaging, OpenGL/game launch, or publication was performed; the complete visual result still needs in-game verification.

## LAN session behavior

`LanAddressPolicy` permits resolved loopback, link-local, IPv4 RFC1918, and IPv6 unique-local peer addresses. Wildcard, multicast, unresolved, and public addresses are rejected. Checks never resolve a hostname or trust a server-list LAN flag.

Modern Fabric adds a server-to-client `lan_session` payload containing protocol version `1`. The integrated host announces it on player join only for the local owner or a permitted LAN peer. Dedicated servers never issue approval. Every model, mob-model, shape-setting, and voice handler checks the server/session policy before touching a store, and every outgoing response/broadcast also checks the recipient's supported channels and address policy.

Client `LanSession` binds approval to the receiving play handler. The local in-memory integrated connection is allowed directly; remote connections require both the resolved LAN peer address and the matching approval protocol. On the first ready client tick, the selected model is uploaded and visible remote players are looked up again, covering entities loaded before approval. Unknown, old, dedicated, and public servers leave FBX gameplay features inactive. Minecraft connections are not cancelled. A chat notice appears after 60 waiting ticks; configuration also displays the session status and disables LAN sharing when unavailable.

The session gate covers ordinary player rendering, deferred 26.3 submissions, first-person body rendering, camera offsets, entity-model cache access, appearance synchronization, and world-driven microphone sampling. Explicit editor previews remain usable. The existing enabled preference is never rewritten to implement this restriction.

INIT/disconnect resets clear approval, pending shape snapshots, lookup/model waiters, remote skin/settings state, remote voice envelopes, and entity-model caches. Queued responses retain their originating play handler; asynchronous player parsing retains its existing world generation guard. Entity-model requests also carry a cache generation so callbacks from a departed session cannot populate the next world's cache. Server stop events remove the integrated world's store and voice-rate state from the server maps. The selected local model/cache remains available for menus.

Client-only Fabric 1.21.1, Forge, and Beta use their resolved connection address without any server handshake. Client-only Fabric 1.21.1 and Forge recognize their local in-memory single-player connection; Beta recognizes local worlds and reads the multiplayer address through a registered `ClientNetworkHandlerAccessor`. Neither variant registers networking payloads or uploads a model. The same decision gates their player render, first-person body/hand behavior, and camera offsets; menu/editor geometry remains available. Each displays one unsupported-connection notice per play connection.

A client-only port cannot identify an integrated host versus a dedicated/private/VPN/proxy endpoint from its address alone. Such connections may pass the local-address check. The modern Fabric protocol provides a cooperating host check, not authenticated proof against modified peers or tunneled networks. These are explicit support-policy limits, not network-isolation guarantees.

## Manual release workflows

There are two separate release entrypoints, both triggered only by `workflow_dispatch` and the GitHub **Run workflow** button:

| Workflow | File | User inputs | Result |
| --- | --- | --- | --- |
| **GitHub release** | `.github/workflows/github-release.yml` | Selected branch, target/all, new release tag, channel, notes | One GitHub release containing the selected production jars |
| **Modrinth release** | `.github/workflows/modrinth-release.yml` | Selected branch, target/all, channel, notes | A separate Modrinth version for each selected loader/game target |

The default channel is `alpha` for both workflows. GitHub maps alpha/beta to prereleases; selecting `release` creates a stable release. The client-only Fabric modpack note is added to the verified changelog automatically. Modrinth receives the selected channel directly through `cloudnode-pro/modrinth-publish@v2` and requires repository secret `MODRINTH_TOKEN` and variable `MODRINTH_ID`. GitHub uses the built-in `GITHUB_TOKEN`; only its publication job receives `contents: write`.

The observed [manual build run 41423139303](https://github.com/aksulightning/FBXPlayerModels/actions/runs/41423139303) was dispatched on `main` but skipped because its prepare job required `modrinth-last-update`. That hard-coded branch condition has been removed. Every job now checks out the exact `${{ github.sha }}` selected by Run workflow, and artifact creation/verification receives `${{ github.ref_name }}` explicitly as `BUILD_SOURCE_BRANCH`. The workflows support `main` and other selected source branches without silently skipping them.

`.github/workflows/nightly.yml` retains the **Manual release build** entrypoint for building artifacts without publishing and adds `workflow_call` so release workflows reuse that implementation from the same source commit. There is no push or scheduled trigger. All active selectors include both client-only Fabric variants and the full `fabric-26.3`, for six targets total. Target metadata remains centralized in `.github/scripts/release_metadata.py`. Explicit target tasks isolate the selected loader/toolchain configurations; Beta uses a Java 17 compiler and Java 21 Gradle runtime, Forge and both Fabric 1.21.1 variants use Java 21, and both Fabric 26.3 variants use Java 25. Beta, Forge, both client-only Fabric variants, and full Fabric 26.3 run their configured test tasks alongside production packaging.

Both release workflows wait for all selected builds/tests to succeed before publishing. Each build uploads its production jar and `release-manifest.json` in artifact `fbx-player-models-<module>`. The manifest records repository, selected source branch/ref, commit, run ID, target, loader/game, mod version, filename, and SHA-256. Publication downloads artifacts from its own workflow run and validates every selected artifact before the first external write. No previous run ID or separate version input is needed.

`.github/scripts/release_publish.py` handles artifact directory normalization, selected-target validation, Modrinth duplicate preflight, and GitHub release creation. Modrinth version numbers come from the built target's version plus loader/game suffix, such as `2.0.0+forge.1.21.1`, and retain the 32-character limit. Existing versions are skipped only when listed with the selected channel, exact loader/game pair, and identical SHA-512 file content. Conflicts stop publication without replacing or deleting anything; retries may skip versions that were already published successfully.

GitHub release creation takes a new, valid tag, checks that it does not already exist, and creates one release at the verified build commit with all selected production jars. Release notes are written through a body file and include artifact checksums. Alpha/beta use `--prerelease`. Existing tags/releases are never deleted, overwritten, or force-moved.

The older combined `publish-release.yml` and one-time `scheduled-alpha-release.yml` workflows have been removed, along with the date-gated scheduling helper/tests. There is no remaining time-of-day restriction or automatic publication trigger. The manual GitHub and Modrinth runs are serialized independently to avoid overlapping same-destination publication runs.

To use the release buttons, commit/push the workflow files to the repository's default branch, then open **Actions → GitHub release** or **Actions → Modrinth release → Run workflow**. Select the intended source branch and targets. GitHub additionally requires a fresh tag; both workflows accept channel/notes. This implementation does not commit, push, dispatch a release, or publish remotely.

Focused helper tests run with `python3 -m unittest discover -s .github/scripts/tests -v`. They cover artifact checksum/version/branch/commit/run protections, target selection, matching/conflicting Modrinth versions and channels, GitHub tag collisions, multi-jar prereleases, exact build-commit targeting, and literal/multiline release-note preservation. All publication/network calls are mocked in these tests. The build reuse follows the [official reusable-workflow syntax](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows).

Validation on 2026-11-02: all 17 release-helper tests passed, and the required four modern Fabric compilation tasks passed with JDK 26 and the existing Gradle cache. The new release workflows have not been dispatched on GitHub; no commits, pushes, tags, releases, or Modrinth versions were created by this change.

## User-run gameplay checklist

1. On each variant, select/edit a model in the main menu, enter single-player, and inspect player rendering, first-person behavior, and saved settings with models toggled on/off.
2. On each modern Fabric target, open a world to LAN and join from a second matching modded client. Grant the guest upload permission, replace/reset both players' models, and verify both clients see the updates.
3. On modern Fabric LAN, adjust Default shape weights and Voice Shape bone/key mappings, then verify remote appearance and voice motion. Check an FBX entity model and a LAN client without the mod.
4. Join a public-address server and verify vanilla rendering, no model transfers, no FBX camera/hand changes, and the inactive notice. Modern Fabric should also remain inactive on a private dedicated server and on an old server without the new approval payload.
5. On client-only Fabric 1.21.1, Forge, and Beta, join a local-network server without installing this mod on the server. Verify only the local player changes and no models/settings are shared. Confirm their documented address-based limitation.
6. Move between two LAN worlds with different entity files using the same filename, then between LAN and public multiplayer. Confirm models, shape/voice state, and pending responses do not leak between sessions; menu previews and the local selection should survive.
7. Use Manual release build for a build-only review, or run the separate GitHub/Modrinth release workflow when ready to build and publish. Check each published loader/game pair, including Babric's StationAPI requirement.

Modern Fabric validation on 2026-11-02: `bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.2:compileJava :fabric-26.2:compileClientJava` succeeded with `JAVA_HOME=/usr/lib/jvm/temurin-26-jdk-amd64` and access to the existing Gradle cache outside the workspace sandbox. The default Java 8 is unsuitable. The first compilation found a removed 26.2 `displayClientMessage` call in `LanSession`; replacing it with the existing target's `sendSystemMessage` API resolved the error. The compiler still reports an existing deprecated API in `FBXPlayerModelsClient`.

### Client-only build and test validation

The user subsequently authorized Beta and Forge production builds and tests on 2026-11-02. Both commands succeeded with `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64` and access to the existing Gradle cache:

```bash
bash ./gradlew :fabric-b1.7.3:remapJar :fabric-b1.7.3:test
bash ./gradlew :forge-1.21.1:jarJar :forge-1.21.1:test
```

The initial test tasks had no test sources. Added JUnit `4.13.2` as a test-only dependency and an eight-case `LanAddressPolicyTest` suite in `session-policy/src/test/java`. `client-common:test` runs it with Java 17, and `forge-1.21.1:test` runs the same suite against Forge's compiled policy with Java 21. Coverage includes private IPv4 boundary addresses, public neighboring ranges and carrier space, IPv4/IPv6 loopback and link-local peers, IPv6 unique-local space, global/deprecated site-local IPv6, wildcard/multicast destinations, unresolved hosts/literals, missing addresses, and non-IP connections.

Imported the existing `SkinnedModelHeadPoseRegression` fixture from the Lite Beta source and wrapped it in `SkinnedModelHeadPoseTest`. Beta's `test` task depends on `client-common:test` and runs the pose fixture without an OpenGL display. It checks head/neck pivots, yaw/pitch behavior, descendants and ancestor motion, authored bind bases and normalization, logical bindings, material preservation, and player pose tracking. The external-FBX/OpenGL regression fixture was not imported or run.

Results: eight policy tests passed on Java 17, one comprehensive Beta pose regression passed, and eight policy tests passed on Java 21: **17 test executions, zero failures/errors/skips**. XML reports are under each tested module's `build/test-results/test/`; HTML reports are under `build/reports/tests/test/` within each module. The observed Beta build resolved Loom `1.17.21` through Babric's dependency graph while retaining the declared plugin versions above.

Production jar inspection passed ZIP CRC validation and confirmed expanded client-only metadata, the shared policy and port client classes, correct Java 17/21 classfile targets, and no server/model-sharing or test classes. Beta includes its StationAPI requirement, registered LAN accessor, and nested JOML jar. Forge includes six nested jgltf/Jackson runtime jars plus NFD/Assimp/Draco natives for Windows, Linux, and macOS on x64/arm64.

| Artifact | Bytes | SHA-256 |
| --- | --- | --- |
| `fabric-b1.7.3/build/libs/fbx-player-models-v2.0.0+b1.7.3+mcb1.7.3.jar` | 951,297 | `0d3a7ad1c2dda2d25551364216246ce17e7bf45f73ad18f8c68b4a2ac781cdc4` |
| `forge-1.21.1/build/libs/fbx-player-models-forge-v2.0.0+1.21.1+mc1.21.1.jar` | 23,441,141 | `7d8a306a33f56b87a557255cd35cb62396e01e6e94e884cddf0f21ab1872557e` |

Existing warnings remain: deprecated Beta APIs/unchecked operations, Forge's deprecated `RenderLevelStageEvent.getPoseStack()`, and Gradle features scheduled for removal in Gradle 10. These did not fail the builds. Actual game launch, graphical rendering, connection behavior, GitHub workflow execution, and publication remain unverified; nothing was published.

## Project layout

```text
root/
  settings.gradle
  build.gradle
  gradle.properties
  common/
    build.gradle
    src/main/java/...
    src/main/resources/...
  client-common/
    build.gradle
    src/main/java/...
    src/main/resources/...
  session-policy/
    src/main/java/...
  forge-1.21.1/
    build.gradle
    src/main/java/...
    src/main/resources/META-INF/mods.toml
  fabric-b1.7.3/
    build.gradle
    src/main/java/...
    src/main/resources/fabric.mod.json
  fabric-1.21.1-client/
    build.gradle
    src/client/java/...
    src/main/resources/fabric.mod.json
  fabric-26.3-client/
    build.gradle
    src/client/java/...
    src/main/resources/fabric.mod.json
  release-notes/
    fabric-1.21.1-client.md
    fabric-26.3-client.md
  fabric-1.21.1/
    build.gradle
    src/main/java/...
    src/client/java/...
    src/main/resources/fabric.mod.json
    src/main/resources/fbx-player-models.mixins.json
    src/client/resources/fbx-player-models.client.mixins.json
  fabric-26.3/
    build.gradle
    src/main/java/...
    src/client/java/...
    src/main/resources/fabric.mod.json
    src/main/resources/fbx-player-models.mixins.json
    src/client/resources/fbx-player-models.client.mixins.json
```

## Common code

Put version-independent code in `common`, including:

- Constants such as `FBXPlayerModels.MOD_ID`.
- Config/save model classes.
- Pure Java utilities and model data structures.
- Shared assets that are valid for every configured Fabric target.
- Platform-neutral interfaces under `com.aksulightning.platform`.

Common code must not import Minecraft, Fabric, Mixin, Mod Menu, or mapping-specific classes.

## Fabric version code

Put Minecraft- and Fabric-sensitive code in a Fabric target module, such as `fabric-1.21.1`, including:

- `ModInitializer` and `ClientModInitializer` entrypoints.
- Fabric event registration.
- Client commands.
- Mod Menu integration.
- Screens, renderers, texture upload, and key client setup.
- Mixins and mixin config files.
- `fabric.mod.json`.
- Access wideners, if any are added later.

Fabric platform implementations live under `com.aksulightning.platform.fabric`.

## Single-player/LAN information screen

`me.onethecrazy.screens.CommunityServerDisclaimerScreen` is duplicated in each Fabric target module because the screen, button, text renderer, and mouse APIs differ by mappings and Minecraft version.

The class/translation keys and saved acknowledgement field retain their legacy names for configuration compatibility, but the screen now explains single-player/LAN scope and client-only differences. The disclaimer layout uses the scaled Minecraft GUI size, not raw window pixels. Keep its margins, text width, checkbox position, and button row derived from the current `width` and `height` so small windows do not let the disclaimer text overlap the checkbox or buttons.

## Current entrypoints

Each modern Fabric target's `fabric.mod.json` declares:

- `main`: `me.onethecrazy.FBXPlayerModelsMod`
- `client`: `me.onethecrazy.FBXPlayerModelsClient`
- `modmenu`: `me.onethecrazy.ModMenuIntegration`

## Mixins and access wideners

The current mixin configs are version-specific:

- `fbx-player-models.mixins.json`
- `fbx-player-models.client.mixins.json`

The client mixins are:

- `me.onethecrazy.mixin.client.CameraMixin`
- `me.onethecrazy.mixin.client.ItemInHandRendererMixin`
- `me.onethecrazy.mixin.client.RenderMixin`
- `me.onethecrazy.mixin.client.MainMenuMixin`

There are currently no project access wideners.

## Version-sensitive code

Code that imports `net.minecraft.*`, `com.mojang.*`, `net.fabricmc.*`, Mixin, or Mod Menu APIs remains in the applicable Fabric target module. This includes rendering, screens, commands, dynamic texture loading, Fabric events, Fabric Loader config paths, and mixins.

Shared code currently includes platform interfaces, constants, save/config models, rig binding metadata, simple model structures, `Float2`, `Float3`, and shared resources.

## Player model display setting

The client config screen exposes `FBX Models: ON/OFF`. It is enabled by default and saved immediately when changed. The shared `FBXPlayerModelsSave` constructor initializes `isEnabled` to `true` for both Fabric 1.21.1 and 26.3, including new saves and older saves without that field; an explicitly saved ON/OFF choice is preserved. `ON` renders available FBX models for player entities; `OFF` leaves player rendering to Minecraft so vanilla player skins are shown. The setting is client-local and covers both the local player and remote players, so it behaves the same in single-player and multiplayer.

The setting deliberately gates only player rendering, the player preview, and FBX-specific first-person model/camera behavior. It must not gate the summonable FBX view entity or FBX mob renderers.

## Experimental default shape keys

Fabric 1.21.1 and 26.3 expose **Settings → Shape Key Settings**, opening a dedicated `me.onethecrazy.screens.editor.ShapeKeySettingsScreen` for the selected player model. The general settings screen now contains only the navigation button for shape keys. Each supported FBX target has a slider from 0% to 100%, in 1% increments, with its imported name above the slider and its full name available as a tooltip. The only exposed profile is **Default**. **Reset Default** returns every target to 0%; models without supported targets show an empty-state message and disable reset.

The dedicated screen shows the selected model filename, Default profile and target count, and a 3D preview of the base pose with the current shape weights. Left-drag inside the preview rotates it, with pitch clamped to ±90 degrees; scrolling inside the preview zooms from 0.25× to 3×. **Reset View** restores its initial rotation and zoom. **Back** and Escape return to the parent Settings screen, retaining immediately saved weights. Slider changes and Reset Default update the preview and matching local player cache without reparsing the FBX.

Preview and controls appear side by side at GUI widths of 480 or more; smaller GUI widths stack the preview above the controls. The slider list has independent wheel scrolling and a draggable scrollbar, while the three footer buttons remain fixed. Labels are clipped to the list viewport, partially visible slider widgets are hidden, and hidden focused sliders release keyboard focus. Content is drawn before widgets so their tooltips remain above the preview. Resizing retains shape weights, rotation, zoom, and a clamped list scroll offset.

`SkinPreviewRenderer.renderModelPreview()` renders supplied geometry independently of the global FBX player display setting and does not require a world/player. The shape key editor uses `SkinnedModel.staticVertices()` so bone clips and live player poses do not move the model during default-pose editing. Base geometry bounds determine a fixed center and bounding radius for initial fitting. Only the preview's copied positions are centered around that point, preserving source geometry, skeleton binds, and normalization; fitting stays unchanged as sliders move. Both Fabric targets reuse their existing version-specific full-bright rendering adapters, with centered rotation for the dedicated editor. The existing general configuration preview retains its display-setting and Idle-pose behavior.

Weights are saved immediately in `.fbxplayermodels/.config` under `selectedSkin.shapeKeyProfiles.Default.weights`. `ClientSkin.defaultShapeKeyProfile()` initializes absent/null profile data for older saves. Missing, null, or non-finite weights resolve to zero; finite weights are clamped to `[0, 1]`. Selecting or resetting a model creates fresh model settings. Profile storage is a named map so later profiles can reuse the same targets, but profile switching and shape key animation are not implemented in this experiment. On a compatible server, the selected model's Default weights are also sent as separate model-specific appearance state; they are not embedded in or allowed to modify the uploaded FBX bytes.

Shared `com.aksulightning.fbxplayermodels.model.shape.ShapeKey` stores additive position/normal delta blocks in model bind space. `ShapeKeyProfile` stores weights separately from geometry. Both targets' `AssimpFBXParser.parseSkinned()` retain `AIAnimMesh` targets, including on meshes with no skin weights. Assimp supplies replacement target positions, which are converted to deltas against the base mesh; the [Assimp FBX converter](https://github.com/assimp/assimp/blob/master/code/AssetLib/FBX/FBXConverter.cpp) constructs these targets from FBX blend shapes. Exporter-provided `mWeight` is not used as an initial slider value: this experiment starts every target at zero.

Blender detection no longer depends solely on native Assimp targets. `FBXParser.readShapeKeyGeometries()` follows the binary FBX object connections from mesh Geometry through BlendShape and BlendShapeChannel deformers to Shape geometry. The [Blender FBX exporter](https://github.com/blender/blender-addons/blob/main/io_scene_fbx/export_fbx_bin.py) writes sparse `Indexes` and additive `Vertices` deltas, optional zero normal deltas, channel names, and per-vertex group weights in `FullWeights`. The reader expands sparse data against the source control-point count, retains zero-effect targets, supports missing normals, and rejects invalid control-point indices or non-finite position deltas. The scene index now indexes Objects children rather than numeric properties throughout the node tree, keeping object identities distinct from property values.

Shared `FbxShapeKeyGeometry` maps render vertices back to source control points using local positions and UVs, falling back to position matches only when coincident candidates have identical deltas across all targets. Mesh names select candidate source geometry; multiple matching geometries or conflicting coincident control points are rejected and logged. When a native mesh has no usable Assimp targets, its binary source targets are expanded onto the existing geometry before the mesh instance transform, retaining its skeleton, weights, materials, triangulation, and duplicated fourth vertices. FBX object ids identify these recovered targets; material splits and mesh instances share the recovered target's weight. Native targets and their existing profile ids retain precedence. The internal binary geometry/skeleton fallback imports the same shape records directly, including for unweighted meshes without an armature.

Settings initializes a selected-model cache if the current player cache is absent or belongs to a different model hash and passes it to the dedicated shape key screen. Shape sliders update the dedicated preview as well as the matching local player cache, allowing detection and persistence from the main menu. Empty states include a Blender export hint: disable **Apply Modifiers** and re-export the FBX when evaluated modifiers have removed the shape keys. A Basis-only Blender mesh has no adjustable target; import cannot recover keys omitted from the exported file.

The supplied `.temp/fluttershy_test.fbx` was inspected directly as binary FBX data. It is 687,596 bytes, FBX version 7400, and identifies its creator as Blender 4.5.13 LTS. Its Objects section contains one Mesh geometry (`Circle.002`) with 5,778 control points, 60 LimbNode models, one Skin deformer, and 46 Cluster deformers. It contains **zero Shape geometries, zero BlendShape deformers, and zero BlendShapeChannel deformers**. The absence of sliders for this export is therefore consistent with its contents; there are no exported shape key deltas to recover. The file does not establish which export setting caused the omission. Re-export the original Blender mesh with its non-Basis shape keys intact and **Geometry → Apply Modifiers** disabled, then select the new FBX in the mod. Blender's [export option definition](https://github.com/blender/blender/blob/main/scripts/addons_core/io_scene_fbx/__init__.py) explicitly states that applying modifiers prevents exporting shape keys. No parser changes were made during this sample inspection.

The importer expands targets using the same triangle indices and duplicated fourth vertex as rendered geometry, applies the mesh instance's linear transform to position deltas, and derives normal deltas from transformed unit normals. Target ids include mesh index, target index, and imported name, distinguishing duplicate names while sharing one weight across instances of a mesh. Multi-material mesh targets remain independently adjustable. Height normalization scales position deltas once without applying its ground translation; its positive uniform scale leaves unit normal deltas unchanged. Material recovery and model copies preserve target blocks and weights.

`SkinnedModel.withShapeKeyProfile()` builds shaped geometry from the unchanged base vertices, sums weighted deltas, and normalizes normals before bone skinning. Changing a slider rebuilds the current local cache entry through `SkinManager.saveDefaultShapeKeyProfile()` without reparsing the FBX. The Default deformation applies to the base pose and remains present during existing bone animations, first-person rendering, previews, and when animations are disabled. Cache loads reapply the local Default profile when the player UUID and selected model hash match; remote cache loads apply the server-synchronized profile only when its model hash matches. A settings update reshapes an already-cached remote model from immutable base vertices without reparsing or accumulating deformation. Normalization bounds use base geometry so a profile cannot alter the normalization scale.

Support includes position and available normal targets exposed by Assimp or connected binary FBX Shape records. Malformed native target vertex counts are skipped, with binary recovery attempted for meshes without usable native targets. Authored shape animation tracks, in-between channel evaluation, negative/overdriven weights, and additional profiles are outside the current default-pose experiment.

Historical validation: `bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.2:compileJava :fabric-26.2:compileClientJava` passed all four tasks after the initial shape key implementation, the Blender detection correction, and the dedicated shape key settings screen, using the installed JDK 26 and the configured Java 21/25 release levels. Gradle required access to its existing cache outside the workspace sandbox. No other test suites or build tasks were run. The supplied Fluttershy FBX was inspected structurally and contains no shape key records; a re-export containing shape keys has not been supplied. In-game appearance, preview rotation/zoom, slider interaction, and imported shape deformation have not been visually verified in Minecraft.

## Experimental Voice Shape

Settings → Voice Shape Settings opens a dedicated editor on Fabric 1.21.1 and 26.3. Each selected model saves one `VoiceShapeSettings` mapping, separate from its Default shape profile. Existing saves initialize safely with target **None**. Choose **Bone** or **Shape key**, select an imported name in the searchable picker, and set the start/end values. Source IDs distinguish shape keys with identical names on different meshes. A bone uses XYZ rotation offsets in degrees, limited to ±180°; a shape key uses start/end weights in 0–100%. Reversed endpoints are supported. Selecting None disables audio input and leaves ordinary rendering intact.

The editor refreshes the saved Default shape profile when opened, including after changing it in the shape key menu, and shows the selected model in its default pose, with mouse drag rotation, wheel zoom, Reset View, Back/Escape, a response meter, live/manual preview selection, a manual percentage slider, and Test Pulse. Manual amount and Test Pulse affect only the editor preview. Controls scroll independently of the preview. The pane layout adapts to narrow windows, and the searchable picker scrolls long name lists. Settings save immediately without reparsing the model.

Input **Auto** uses Simple Voice Chat when installed and otherwise opens the default local microphone. **Simple Voice Chat** waits for its client connection; **Microphone** explicitly selects local microphone input. Simple Voice Chat is recommended but optional: it is declared as a Fabric suggestion, its API 2.6.0 is compile-only, and its plugin is loaded through the optional `voicechat` entrypoint. No voice chat mod or API is bundled or required. The plugin has no Minecraft client references, so registration is safe on a dedicated server. It reads [ClientSoundEvent PCM](https://voicechat.modrepo.de/de/maxhenkel/voicechat/api/events/ClientSoundEvent.html) after normal-priority handlers without replacing/cancelling the frame, following the [official Fabric API registration](https://dev.modrepo.de/minecraft/voicechat/api/getting_started). Muting, disabling, or disconnecting Simple Voice Chat clears the envelope; Auto does not switch to another microphone to bypass those states.

Without Simple Voice Chat, a daemon worker reads mono 48 kHz signed 16-bit PCM through Java Sound's default microphone. Capture begins only when an enabled mapping is active in a world with FBX model display enabled, or in its editor. It stops on None, input/target changes, disconnect, leaving the editor outside a world, and client shutdown. An unavailable microphone is shown in the screen, while manual preview and Test Pulse remain available. Audio arrays are reduced immediately and are never saved or transmitted by this feature. In multiplayer with a compatible server, only the resulting normalized scalar animation level is relayed so tracking players can reproduce the mapped pose. Only Simple Voice Chat handles its own existing audio transmission.

`VoiceAudioEnvelope` computes RMS amplitude, configurable sensitivity and a noise threshold. **Volume** continuously interpolates between the endpoints; **Rhythm / onsets** detects sharp increases relative to the recent amplitude baseline; **Threshold trigger** fires when the threshold is crossed, with hysteresis before retriggering. Pulses last 200 ms. A 40 ms attack and 140 ms release smooth motion, and audio older than 250 ms returns toward the start endpoint. Rhythm is amplitude-onset detection, not music tempo tracking.

`VoiceShapePose` is an immutable per-frame snapshot. Both player renderers apply local settings to the matching local session/model and server-synchronized settings to a matching remote model; either path requires that the imported target still exists. Bone rotation is additive about the current posed joint in model-space XYZ and propagates to descendants before skinning. It composes with clips, head look, and live limb poses. Shape motion replaces the selected key's Default weight for that frame, preserves other Default weights, and rebuilds from unchanged base geometry before skinning; a bounded cache reuses identical voice weights. Voice Shape remains independently enabled when ordinary clip/live-pose animations are disabled. None restores the applicable local or remote Default shape profile. The 26.3 render state carries the voice snapshot through deferred submission, and first-person rendering retains its existing hidden-head and visibility restrictions.

This experiment supports one Default profile and one Voice Shape mapping per selected model for both the local player and remote players seen through a compatible server. Additional mappings/profiles and authored shape animation clips remain outside this implementation. The supplied `.temp/fluttershy_test.fbx` can supply bone targets but contains no exported shape key records.

Historical validation used the four Java/client compilation tasks then required by AGENTS.md. `bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.2:compileJava :fabric-26.2:compileClientJava` passed all four tasks with JDK 26 and the configured Java 21/25 release levels. Gradle used its existing cache outside the workspace sandbox; no other test suites or build tasks were run. Runtime microphone capture, optional-mod event delivery, and visual deformation have not been exercised in Minecraft in this workspace.

## Experimental server shape synchronization

Fabric 1.21.1 and 26.3 synchronize the selected model's Default shape-key weights and complete `VoiceShapeSettings` snapshot through the mod server. The shared `ShapeKeySyncState` binds that appearance to the model SHA-256 hash, clamps values through the existing profile/voice rules, drops zero Default weights, and bounds target counts and strings before packet encoding or persistence. The client is authoritative for its own saved editor choices; the server derives the player UUID from the authenticated connection and accepts an update only when its model hash exactly matches that player's current server model. A model replacement starts with empty settings, and clearing the server model clears its synchronized settings.

Selecting a model and the client-join auto-upload send model bytes before their settings update. Shape sliders and Voice Shape controls still save locally immediately; network settings snapshots are coalesced and flushed at most once per 200 ms while editing. The server persists accepted snapshots separately from model bytes at:

```text
<world>/fbx-player-models/shape-settings.json
```

The file is loaded only up to 2 MiB, and loaded records are sanitized and discarded unless their player and model hash still exist in `model-index.tsv`. The server returns settings alongside player model lookups and broadcasts accepted model/settings changes. `SkinManager.shapeSettings` holds world-scoped remote snapshots, clears them on disconnect or model removal, applies Default weights during remote model parsing, and reapplies changed profiles to cached models without reparsing.

For live Voice Shape, the source client sends only a finite scalar in `[0, 1]`, on a Minecraft client tick when it changes by at least `0.01` or as a 250 ms heartbeat while the mapping is active. The server rejects non-finite values, clamps the scalar, rate-limits each sender to one accepted packet per 25 ms, derives the UUID from the connection, and relays only to clients tracking that player. Receivers hold a sample for 250 ms and decay it to zero by 500 ms, preventing a disconnected or interrupted sender from leaving a remote face/bone pose stuck. PCM, microphone data, and Simple Voice Chat frames never enter this protocol.

The four new payload directions are client-to-server settings update, server-to-client player settings, client-to-server voice level, and server-to-client identified voice level. Their version-specific codecs and registrations live in each target's `ModelPackets` and `ServerModelNetworking`; `BackendInteractor` owns client send/receive integration, and `VoiceShapeClient` resolves local versus remote pose sources. For this release, a remote server without the matching LAN approval leaves FBX gameplay rendering and synchronization inactive; saved settings and explicit editor previews remain available.

### Voice Shape Bone synchronization

Bone is a first-class synchronized Voice Shape target, not a shape-key fallback. A Bone mapping transmits its imported bone name and all start/end X, Y, and Z rotation endpoints after clamping each endpoint to ±180°. Target-specific sanitization removes any stale shape-key id from a Bone snapshot; None removes both target identifiers, while an invalid or blank Bone name disables the synchronized mapping. The server persists and rebroadcasts the mapping with the same model-hash and authenticated-player protections used for shape keys.

For a remote player, `VoiceShapeClient.poseFor()` verifies that the synchronized bone name exists in that player's parsed model, evaluates the six endpoints from the relayed scalar, and produces the same `VoiceShapePose` used locally. `SkinnedModel` adds the resulting model-space XYZ rotation around the selected joint's current posed position after clip, head-look, and live-limb evaluation. Recursive global-transform evaluation propagates it to descendants. Bone Voice Shape therefore remains visible to other tracking players and continues to work when ordinary animation playback is disabled, while a missing bone safely produces no voice pose.

Historical validation for server shape synchronization used the four compilation tasks then required. `bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.2:compileJava :fabric-26.2:compileClientJava` passed for both server and client source sets with JDK 26. The default Java 8 could not launch Gradle, and Gradle required its existing cache outside the workspace sandbox. No unrelated tests, formatters, or build tasks were run. Multiplayer packet exchange, server restart persistence, microphone capture, and visual remote deformation still require in-game verification.

## FBX view entity

The mod registers summonable FBX-backed entities for displaying an FBX model:

```text
fbxplayermodels:view_entity
fbxplayermodels:passive_entity
fbxplayermodels:tameable_entity
fbxplayermodels:neutral_entity
fbxplayermodels:hostile_entity
```

`view_entity` is intentionally not a pathfinding mob. It extends the base Minecraft entity type in each Fabric target, has no AI goals, no attacks, no wandering, no fleeing, and no natural spawning for the MVP. It exists as a server-synced display entity with a client renderer.

The AI variants reuse the same FBX model NBT, server sync, cache, and renderer:

- `passive_entity` wanders and looks at nearby players.
- `tameable_entity` can be tamed with a bone, can sit, follows its owner, wanders, and looks at nearby players.
- `neutral_entity` wanders and only fights back after being damaged.
- `hostile_entity` wanders, targets players, and attacks in melee.

For the `fabric-26.3` target, the summonable FBX mob variants register floating, random strolling, player look-at, and random look-around goals explicitly. Their random strolling uses the four-argument `RandomStrollGoal` with no-action-time checks disabled so summoned display mobs do not stop wandering after being idle. Non-tameable FBX pathfinder mobs also use animal-like walk target scoring, preferring grass blocks and otherwise following light-level pathfinding cost, matching the movement behavior that made the tameable variant reliable. The hostile variant still uses normal Minecraft hostile targeting and melee damage rules, including peaceful mode preventing attacks.

Summon example:

```mcfunction
/summon fbxplayermodels:view_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:hostile_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:tameable_entity ~ ~ ~ {Model:"this_file.fbx",TameItem:"minecraft:apple"}
```

The `Model` NBT value is persisted on the entity and synced to clients with tracked entity data. The value must be a safe flat FBX filename. Absolute paths, nested paths, path traversal, blank values, and non-`.fbx` files are rejected by sanitizing to an empty model value.

`tameable_entity` also supports a per-entity tame item NBT value. Use `TameItem:"minecraft:item_id"` in summon NBT. The lowercase alias `tame_item` is accepted when reading entity NBT. Missing or invalid item ids fall back to `minecraft:bone`.

Server-side model storage:

```text
<world>/fbx-player-models/mobskins/this_file.fbx
```

Clients do not load `mobskins` as local source data. When a view entity renders, the client requests the named model from the server through the mod networking layer. The server validates the safe filename, reads only from its world-local `mobskins` directory, enforces the shared model size limit, and sends the FBX bytes back to that client.

## Model upload authorization and size limit

Both supported targets enforce a strict model size of less than 2 MiB (`2 * 1024 * 1024` bytes). `ModelPackets.MAX_MODEL_BYTES` is one byte below that boundary, and the same value protects file selection, cached-model re-upload, client packet creation, packet decoding, server persistence, and server-to-client model delivery. The upload decoder accepts the exact 2 MiB boundary only as a rejection sentinel so the server can return a useful size error; it is never saved.

Upload permission is intentionally scoped to model uploads. The integrated LAN host allows permission-level-2 operators and player names stored in the world's `fbx-player-models/upload-permissions.txt`. It also recognizes the native singleplayer owner identity, allowing the owner to upload in a world with cheats disabled without granting general command permissions.

Client-side received model cache:

```text
.fbxplayermodels/mobskins-cache/
```

This cache is only a parsing cache for server-sent bytes. It is not the authoritative model folder.

Important classes, duplicated per Fabric target where mappings differ:

- `com.aksulightning.fbxplayermodels.ModEntities`
- `com.aksulightning.fbxplayermodels.ViewEntity`
- `com.aksulightning.fbxplayermodels.FbxPassiveEntity`
- `com.aksulightning.fbxplayermodels.FbxTameableEntity`
- `com.aksulightning.fbxplayermodels.FbxNeutralEntity`
- `com.aksulightning.fbxplayermodels.FbxHostileEntity`
- `com.aksulightning.fbxplayermodels.ViewEntityModelPath`
- `com.aksulightning.fbxplayermodels.client.ViewEntityRenderer`
- `com.aksulightning.fbxplayermodels.client.ViewEntityModelCache`
- `me.onethecrazy.network.ModelPackets`
- `me.onethecrazy.server.ServerModelStore`
- `me.onethecrazy.server.ServerModelNetworking`
- `me.onethecrazy.util.network.BackendInteractor`

Rendering reuses the existing FBX loading pipeline: `UniversalParser`, `ModelNormalizer`, `CacheSkin`, static vertices, and `SkinnedModel.render(...)`. The renderer chooses `Idle` when an entity is still and `Walk` when a FBX mob variant is moving.

## FBX skeleton and live player poses

Both Fabric 1.21.1 and 26.3 use the same pose rules through their version-specific `CustomModelPose`, `SkinnedModel`, `ModelNormalizer`, and rendering adapters. Both target modules are present and enabled in `settings.gradle`.

- The Assimp importer retains every scene hierarchy node, including unweighted joints and armature/helper roots. Skin influences use Assimp joint-node identities when available, so duplicate joint names do not collapse their weights. Mesh node transforms are baked into positions and normals in the same model space as the skeleton. The binary fallback retains model parents, joint bind matrices, rotation/scaling pivots, and all meshes; it uses only imported skin weights and leaves unweighted geometry unweighted. Both import paths retain all positive weight influences rather than limiting vertices to four bones. Material recovery transfers matching position/UV appearances onto the Assimp geometry instead of replacing its skeleton or weights.
- Import orientation uses the FBX file's current `UpAxis` and `UpAxisSign`, exposed through [Assimp scene metadata](https://github.com/assimp/assimp/blob/master/code/AssetLib/FBX/FBXConverter.cpp). `com.aksulightning.fbxplayermodels.model.FbxCoordinateSpace` supplies one scene-world conversion to model-space +Y. Y-up files retain their scene orientation; Z-up/X-up files receive the corresponding signed rotation. A model-space wrapper root applies that conversion to the skeleton, while the same conversion precedes mesh node transforms. Imported node-local binds and animation keys retain their source axes, and normalization subsequently transforms only the wrapper root. This corrects the reported 90-degree tilt caused by combining composed scene transforms with the previous unconditional `(x, z, -y)` Blender conversion. The Assimp static/skinned paths and binary fallback share the corrected orientation rule, preserving matching geometry for material recovery.
- The six logical parts are Head, Chest, Right Arm, Left Arm, Right Leg, and Left Leg. Resolution accepts arbitrary explicitly bound joint names before automatic matching, fills unspecified parts automatically, and keeps an explicitly missing joint unresolved. Automatic matching skips Assimp pivot helper names and prefers a head over a neck. Regenerating procedural clips after binding edits replaces earlier generated clips while retaining authored clips and explicit clip mappings.
- Model normalization copies geometry, applies the height/ground transform to skeleton roots once, retains child-local bind matrices, and recomputes inverse global binds. Degenerate/empty bounds are left unchanged. The normalized model retains its bindings and animation-enable setting; authored absolute root animation keys receive the same normalization once when sampled.
- World rendering aligns the complete model with interpolated player body yaw, including the optional first-person self model. The 26.3 submission adapter also honors final render-state body/head overrides used by the inventory preview.
- View yaw and body yaw use angle-aware interpolation. Their wrapped difference is clamped to ±85 degrees; interpolated view pitch is clamped to ±90 degrees. Head look is `R = Ry(-relativeYaw) * Rx(pitch)`, with no generated head roll. With translation removed from the head's global bind matrix `B`, the head's posed transform is postmultiplied by `inverse(B) * R * B`. Its authored origin/basis/scale and inherited posture remain in the transform, and children inherit the result. Look applies during Idle, Walk, Sneak, and Sit, including authored clips or absent clips, and is suppressed during Sleep.
- First- and third-person player rendering call the same live limb-pose calculation. Walking uses player walk progress and interpolated amplitude, with opposite arm/leg phases and no tick-age substitute for zero amplitude. Sitting bends and spreads the legs and lowers the arms; held items modify the corresponding physical arm, and attack/block movement also applies while seated. Live pitch, yaw, and roll use model-space X, Y, and Z. Each limb delta is `T(currentJoint) * Ry(yaw) * Rx(pitch) * Rz(roll) * T(-currentJoint)`; recursive parent-first evaluation passes the posed joint and delta to descendants. Player poses replace generated limb swing tracks even when the live rotation is zero. Generated walking tracks remain available for display/mob rendering without live player poses.
- Generated sneaking torso posture is additive in the chest's local bind space. Its authored pivot remains valid by default. Geometry supplies a substitute attachment only when the joint lies outside the entire mesh bounds by more than twice the mesh diagonal; the substitute is the base center of directly chest-weighted geometry, expressed in the chest bind space. This changes the posture pivot without rewriting bind matrices or weights.
- Skin matrices are `posedGlobal * inverseBind`. Positions and inverse-transpose normals blend with the existing weights; vertices without valid influences keep their original positions. Resulting normals are normalized, and UVs, textures, and material colors pass through unchanged. The saved animation-enable setting disables clip playback and live bone poses together. First-person camera, visibility, spectator, sleep, display-setting, and hidden-head restrictions remain in the existing renderer.

Validation for these changes is limited to source inspection, review of the supplied screenshots, and the authorized four Java/client compilation tasks. The authorized command passed all four tasks using the installed JDK 26 with the configured Java 21/25 release levels. The default Java 8 could not run Gradle, and the wrapper needed approval to write its existing cache lock outside the workspace sandbox. The orientation fix has not been visually verified in Minecraft; the screenshot models are not present as FBX assets in this workspace. In-game pose, camera, and imported-asset behavior were not verified in this workspace. Neither target was unavailable.

The complete changes from the FBX bone/pose implementation and subsequent orientation correction are exported in -path here-. The patch includes shared binding/coordinate-space code, both targets' importers, normalization, skinning, procedural/live poses, animation-setting integration, first-/third-person rendering adapters, 26.2 render-state data, and this documentation. Its baseline is -number here-, before the bone/pose work began. Apply it from the project root; module paths and version-specific APIs may need adapting when porting to a lite codebase with a different layout.

## Minecraft 26.3 port (2026-11-06)

`fabric-26.3` is the current modern full-mod target, initially ported from the now-retired 26.2 source. It is independent of the removed module. Its own main/client source sets, resources, entrypoints, and mixins retain model import and previews, rig binding and animations, Default shape keys, Voice Shape, first-person rendering and camera offsets, all five FBX entities, saved configuration, and integrated single-player/Open to LAN sharing. Version-independent models, appearance settings, audio processing, and session policy continue to come from `common`.

Dependency versions were read from [Fabric's Loader metadata](https://meta.fabricmc.net/v2/versions/loader/26.3), [Loom metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml), [Fabric API metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml), and [Mod Menu's 26.3 releases](https://api.modrinth.com/v2/project/modmenu/version?game_versions=%5B%2226.3%22%5D). [Mojang's 26.3 runtime metadata](https://piston-meta.mojang.com/v1/packages/80608ff6753705b6a82e2cb86a5937d4d233abd4/26.3.json) specifies Java 25 and LWJGL 3.4.3; the bundled Assimp/NFD dependencies use that LWJGL version. Gradle was updated to 9.6.0, following [Fabric's 26.3 porting notes](https://fabricmc.net/2026/09/15/263.html). Metadata requires Minecraft `~26.3`, Fabric Loader `>=0.20.1`, Java `>=25`, and Fabric API. Mod Menu 21.0.0 remains optional at runtime.

The current renderer retains GUI-owned screens/toasts, `LightCoordsUtil` packed lighting, deferred player/entity submissions, and full-session moderation-notice deduplication. Inventory previews can apply body/head rotations after extraction, so animated player vertices are generated at submission from the final pose. Geometry uses `SubmitNodeCollector.submitCustomGeometry`, `RenderTypes.entityCutout`, `PoseStack`, and `VertexConsumer`; textures use `DynamicTexture` and Minecraft's texture manager. There are no direct OpenGL or GLFW rendering calls. The rendering backends still require in-game verification; Vulkan compatibility has not been functionally tested.

The target handles these game API differences:

- `FirstPersonHandsAndItemsRenderer` replaces `ItemInHandRenderer`. Its required mixin targets the complete `submitHandsWithItems(float, PoseStack, SubmitNodeCollector, PlayerRenderState, FirstPersonHandsAndItemsRenderState)` descriptor. Hand suppression checks that the render state has a player and that the local player's custom first-person model is available.
- Quaternion rotations use `PoseStack.rotateDegrees(Axis, float)` in the player, first-person, and FBX entity renderers.
- Player action poses use `getSwingAnimation`, `isSwinging`, and `getCurrentSwing().hand()`, including a main-hand fallback when no current swing exists.
- SDL changes the left mouse button value from 0 to 1. The config preview, rig editor, shape editor, voice editor, and disclaimer checkbox use `InputConstants.MOUSE_BUTTON_LEFT`. The searchable target picker uses vanilla `EditBox` focus/input handling.

Camera, title-screen, living-entity, and first-person injection targets were inspected against the actual 26.3 client classes. Geometry and textures continue through Minecraft's submission and texture APIs, without direct OpenGL or GLFW calls. The final render-state pose overrides used by inventory previews and the existing session policy are retained.

`settings.gradle` recognizes explicit 26.3 task paths. Manual build, GitHub release, and CurseForge release selectors include `fabric-26.3`; centralized release metadata also includes it in `all`, with Java 25, `:fabric-26.3:jar`, and a distinct `fbx-player-models-fabric-26.3` artifact. The existing release-selection test expectation includes the new target. No workflows or publication actions were run.

Initial port validation, before 26.2 retirement: the then-prescribed four Fabric 1.21.1/26.2 compilation tasks plus the user-authorized `:fabric-26.3:compileJava :fabric-26.3:compileClientJava` passed under JDK 26 with release levels 21/25. An initial compile identified the removed hand renderer, swing members, and quaternion `mulPose` overloads; the target now uses their 26.3 APIs. The inherited deprecated API notice in `FBXPlayerModelsClient` remains. No other tests, formatters, packaging tasks, or game launches were run during that initial validation. In-game previews/input, first-person visibility, LAN transfers, audio deformation, and OpenGL/Vulkan rendering still need gameplay verification; no production jar was built during this port.

Follow-up test validation (2026-11-06): the user explicitly approved running tests. All six Fabric compilation tasks passed again, with their outputs up to date. `JAVA_HOME=/usr/lib/jvm/temurin-26-jdk-amd64 bash ./gradlew :fabric-26.3:test :common:test` passed: `LocalModelFeaturesTest` ran 8 tests and `LanAddressPolicyTest` ran 8 tests, with no failures, errors, or skips. The 26.3 module's own test task reported `NO-SOURCE`; it has no separate unit tests. `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s .github/scripts/tests -p 'test_release_*.py' -v` passed all 20 release metadata/publication tests, including selection of 26.3 through `all`. Publication calls in those tests are mocked. No game launch, production packaging, or actual publication was performed.

## Minecraft 26.3 command-tree join crash (2026-11-06)

Minecraft 26.3's `Commands.COMMAND_NODE_INSPECTOR` evaluates each command requirement with `Commands.createCompilationContext(PermissionSet.NO_PERMISSIONS)` when creating the joining player's command packet. This source deliberately has no server or world. The port's `hasAdministratorRights` predicate dereferenced `source.getServer()`, throwing a null-pointer exception during player placement even in an integrated world.

The 26.3 upload-permission command now uses the native `Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER))` predicate, which reads permissions without accessing a server. Command registration skips the dedicated-server environment, and command execution separately requires a non-null integrated server before accessing the model store. This preserves operator-level command access and the single-player/Open to LAN scope while allowing Minecraft to inspect the command tree safely. The package-private `registerCommands` method lets the tests exercise the actual command registration independently of networking initialization.

`fabric-26.3/src/test/java/me/onethecrazy/server/ServerModelNetworkingTest.java` adds two JUnit 4 regression tests. The first bootstraps Minecraft headlessly, checks unprivileged and administrator compilation sources, invokes the game's actual command-node inspector, and constructs a `ClientboundCommandsPacket` from the registered tree. The second verifies that the upload-permission command is absent in the dedicated environment. JUnit is a test-only dependency; the manual release build now runs `:fabric-26.3:test` alongside packaging.

Historical validation, before 26.2 retirement: all six previously approved Fabric compilation tasks and `:fabric-26.3:test` passed together under JDK 26. Both new regression tests passed with no failures, errors, or skips. The earlier `NO-SOURCE` result above predates these tests. No in-game world join, production packaging, or publication was performed for this fix.

## Retired Minecraft 26.2 target (2026-11-06)

Removed `fabric-26.2/src` and its module build file, the settings inclusion, all `fabric_262_*` properties, and the unused `loom_262_version`. Active build/release selectors, the archived Modrinth selector, centralized release metadata, and the release-selection test now omit this target. Full and client-only Fabric remain supported on 1.21.1 and 26.3; Forge 1.21.1 and Beta remain separate client-only targets, for six release variants total. `README.md`, current support/API/build documentation, the module layout, and `AGENTS.md` now describe the remaining targets. Earlier validation records retain their original version names as history.

The removed module's ignored `run/` and `build/` directories were moved to `.temp/retired-fabric-26.2-obbxhkv9/` to preserve local development data and artifacts. This ignored backup is not a Gradle module. No runtime data outside the retired module was moved or deleted.

Validation after removal: all eight compilation tasks in the current prescribed command passed under JDK 26; both client-only `compileJava` tasks reported `NO-SOURCE` as expected. `:fabric-26.3:test`, `:fabric-1.21.1-client:test`, and `:fabric-26.3-client:test` also succeeded. The full 26.3 suite retained its two passing command-tree tests, and the client test tasks retained the 16 passing shared model/session-policy tests through `common:test`; these Java test results were up to date. All 20 release-helper tests passed again, including selection of exactly the six remaining release variants. A source/configuration search found no remaining 26.2 build references outside this historical documentation and the ignored backup. No game launch, production packaging, or publication was performed.

## Adding another Fabric Minecraft version

1. Confirm the actual Minecraft version, mapping model, Fabric Loader version, Fabric API version, Loom version, Java version, and Mod Menu version from official metadata.
2. Add target-specific properties to `gradle.properties`.
3. Add `include("fabric-<minecraft-version>")` to `settings.gradle`.
4. Create `fabric-<minecraft-version>/build.gradle` from the closest compatible target.
5. Copy only the necessary Fabric glue, resources, entrypoints, and mixins into the new module.
6. Add Yarn only for an obfuscated target; Minecraft 26.3 uses the unobfuscated names supplied by the game.
7. Adapt dependencies, `fabric.mod.json`, mixin targets, injection descriptors, GUI, networking, persistence, entity, and rendering APIs for that Minecraft version.
8. Audit the new rendering path for backend-specific graphics calls.
9. Keep shared logic in `common`; isolate mapping-sensitive differences inside the Fabric target module.
10. Run the focused compile tasks for every supported target, build the new module, and inspect the production jar.

## CI runner update (2026-11-04)

`.github/workflows/github-release.yml`, `.github/workflows/modrinth-release.yml`, `.github/workflows/nightly.yml` now use `ubuntu-latest` for jobs previously pinned to `ubuntu-24.04`, following the requested GitHub default Ubuntu image. Existing explicitly selected Ubuntu 26.04 jobs are retained. This changes workflow configuration only; Fabric compilation and release workflows have not been executed for this update.

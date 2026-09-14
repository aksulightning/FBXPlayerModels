# FBX Player Models Code Guide

## Fabric-only support

This repository is Fabric-only. It does not use Forge, NeoForge, Quilt, or Architectury.

The project is structured for separate Fabric jars per Minecraft target. A universal jar is not implemented.

## Supported targets

The repository maintains two supported Fabric targets. Each `fabric-*` module is a real, independently buildable Minecraft target with its own version-sensitive source, resources, metadata, and production jar. `common` supplies shared classes and assets to those jars; it is not a standalone Minecraft distribution.

### Target and toolchain versions

| Module | Minecraft | Game namespace | Java | Fabric Loom | Fabric Loader | Fabric API |
| --- | --- | --- | --- | --- | --- | --- |
| `fabric-1.21.1` | `1.21.1` | Yarn `1.21.1+build.3` | 21 | `1.16.2` | `0.16.14` | `0.116.6+1.21.1` |
| `fabric-26.2` | `26.2` | unobfuscated game names; no Yarn | 25 | `1.17.17` | `0.19.3` | `0.156.0+26.2` |

### Mod and production-build versions

| Module | Mod version | Mod Menu | LWJGL Assimp/NFD | Build task | Production jar |
| --- | --- | --- | --- | --- | --- |
| `fabric-1.21.1` | `2.0.0+1.21.1` | `11.0.3` | `3.3.3` | `:fabric-1.21.1:build` | `fbx-player-models-v2.0.0+1.21.1+mc1.21.1.jar` |
| `fabric-26.2` | `2.0.0+26.2` | `20.0.1` | `3.4.1` | `:fabric-26.2:build` | `fbx-player-models-v2.0.0+26.2+mc26.2.jar` |

`fabric-1.21.1` uses Yarn and produces its production artifact through Loom's `remapJar` task. Minecraft 26.2 exposes unobfuscated game names, so that module deliberately omits Yarn and produces its production artifact through `jar`.

All production jars are written to `<module>/build/libs/` and include the `common` output. The Gradle wrapper is `9.5.1`. All modules bundle jgltf `2.0.4`; Native File Dialog and Assimp retain Windows, Linux, and macOS natives for x64 and arm64. DevAuth is development-only: 1.21.1 uses `1.2.1`, while 26.2 uses `1.2.2`.

## Build commands

Build all configured modules:

```bash
./gradlew build
```

Build the Fabric 1.21.1 jar:

```bash
./gradlew :fabric-1.21.1:build
```

Build the Fabric 26.2 jar:

```bash
./gradlew :fabric-26.2:build
```

Compile all supported main and client source sets:

```bash
bash ./gradlew \
  :fabric-1.21.1:compileJava \
  :fabric-1.21.1:compileClientJava \
  :fabric-26.2:compileJava \
  :fabric-26.2:compileClientJava
```

The 1.21.1 remapped jar is written under `fabric-1.21.1/build/libs/`.
The unobfuscated 26.2 jar is written under `fabric-26.2/build/libs/`. Loom 1.17's unobfuscated target produces the production 26.2 artifact through the `jar` task rather than a `remapJar` task.

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
  fabric-1.21.1/
    build.gradle
    src/main/java/...
    src/client/java/...
    src/main/resources/fabric.mod.json
    src/main/resources/fbx-player-models.mixins.json
    src/client/resources/fbx-player-models.client.mixins.json
  fabric-26.2/
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

## Community server disclaimer screen

`me.onethecrazy.screens.CommunityServerDisclaimerScreen` is duplicated in each Fabric target module because the screen, button, text renderer, and mouse APIs differ by mappings and Minecraft version.

The disclaimer layout uses the scaled Minecraft GUI size, not raw window pixels. Keep its margins, text width, checkbox position, and button row derived from the current `width` and `height` so small windows do not let the disclaimer text overlap the checkbox or buttons.

## Current entrypoints

Every Fabric target's `fabric.mod.json` declares:

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

The client config screen exposes `FBX Models: ON/OFF`. It is enabled by default and saved immediately when changed. The shared `FBXPlayerModelsSave` constructor initializes `isEnabled` to `true` for both Fabric 1.21.1 and 26.2, including new saves and older saves without that field; an explicitly saved ON/OFF choice is preserved. `ON` renders available FBX models for player entities; `OFF` leaves player rendering to Minecraft so vanilla player skins are shown. The setting is client-local and covers both the local player and remote players, so it behaves the same in single-player and multiplayer.

The setting deliberately gates only player rendering, the player preview, and FBX-specific first-person model/camera behavior. It must not gate the summonable FBX view entity or FBX mob renderers.

## Experimental default shape keys

Fabric 1.21.1 and 26.2 expose **Settings → Shape Key Settings**, opening a dedicated `me.onethecrazy.screens.editor.ShapeKeySettingsScreen` for the selected player model. The general settings screen now contains only the navigation button for shape keys. Each supported FBX target has a slider from 0% to 100%, in 1% increments, with its imported name above the slider and its full name available as a tooltip. The only exposed profile is **Default**. **Reset Default** returns every target to 0%; models without supported targets show an empty-state message and disable reset.

The dedicated screen shows the selected model filename, Default profile and target count, and a 3D preview of the base pose with the current shape weights. Left-drag inside the preview rotates it, with pitch clamped to ±90 degrees; scrolling inside the preview zooms from 0.25× to 3×. **Reset View** restores its initial rotation and zoom. **Back** and Escape return to the parent Settings screen, retaining immediately saved weights. Slider changes and Reset Default update the preview and matching local player cache without reparsing the FBX.

Preview and controls appear side by side at GUI widths of 480 or more; smaller GUI widths stack the preview above the controls. The slider list has independent wheel scrolling and a draggable scrollbar, while the three footer buttons remain fixed. Labels are clipped to the list viewport, partially visible slider widgets are hidden, and hidden focused sliders release keyboard focus. Content is drawn before widgets so their tooltips remain above the preview. Resizing retains shape weights, rotation, zoom, and a clamped list scroll offset.

`SkinPreviewRenderer.renderModelPreview()` renders supplied geometry independently of the global FBX player display setting and does not require a world/player. The shape key editor uses `SkinnedModel.staticVertices()` so bone clips and live player poses do not move the model during default-pose editing. Base geometry bounds determine a fixed center and bounding radius for initial fitting. Only the preview's copied positions are centered around that point, preserving source geometry, skeleton binds, and normalization; fitting stays unchanged as sliders move. Both Fabric targets reuse their existing version-specific full-bright rendering adapters, with centered rotation for the dedicated editor. The existing general configuration preview retains its display-setting and Idle-pose behavior.

Weights are saved immediately in `.fbxplayermodels/.config` under `selectedSkin.shapeKeyProfiles.Default.weights`. `ClientSkin.defaultShapeKeyProfile()` initializes absent/null profile data for older saves. Missing, null, or non-finite weights resolve to zero; finite weights are clamped to `[0, 1]`. Selecting or resetting a model creates fresh model settings. Profile storage is a named map so later profiles can reuse the same targets, but profile switching and shape key animation are not implemented in this experiment. Settings remain client-local and are not transmitted with model uploads.

Shared `com.aksulightning.fbxplayermodels.model.shape.ShapeKey` stores additive position/normal delta blocks in model bind space. `ShapeKeyProfile` stores weights separately from geometry. Both targets' `AssimpFBXParser.parseSkinned()` retain `AIAnimMesh` targets, including on meshes with no skin weights. Assimp supplies replacement target positions, which are converted to deltas against the base mesh; the [Assimp FBX converter](https://github.com/assimp/assimp/blob/master/code/AssetLib/FBX/FBXConverter.cpp) constructs these targets from FBX blend shapes. Exporter-provided `mWeight` is not used as an initial slider value: this experiment starts every target at zero.

Blender detection no longer depends solely on native Assimp targets. `FBXParser.readShapeKeyGeometries()` follows the binary FBX object connections from mesh Geometry through BlendShape and BlendShapeChannel deformers to Shape geometry. The [Blender FBX exporter](https://github.com/blender/blender-addons/blob/main/io_scene_fbx/export_fbx_bin.py) writes sparse `Indexes` and additive `Vertices` deltas, optional zero normal deltas, channel names, and per-vertex group weights in `FullWeights`. The reader expands sparse data against the source control-point count, retains zero-effect targets, supports missing normals, and rejects invalid control-point indices or non-finite position deltas. The scene index now indexes Objects children rather than numeric properties throughout the node tree, keeping object identities distinct from property values.

Shared `FbxShapeKeyGeometry` maps render vertices back to source control points using local positions and UVs, falling back to position matches only when coincident candidates have identical deltas across all targets. Mesh names select candidate source geometry; multiple matching geometries or conflicting coincident control points are rejected and logged. When a native mesh has no usable Assimp targets, its binary source targets are expanded onto the existing geometry before the mesh instance transform, retaining its skeleton, weights, materials, triangulation, and duplicated fourth vertices. FBX object ids identify these recovered targets; material splits and mesh instances share the recovered target's weight. Native targets and their existing profile ids retain precedence. The internal binary geometry/skeleton fallback imports the same shape records directly, including for unweighted meshes without an armature.

Settings initializes a selected-model cache if the current player cache is absent or belongs to a different model hash and passes it to the dedicated shape key screen. Shape sliders update the dedicated preview as well as the matching local player cache, allowing detection and persistence from the main menu. Empty states include a Blender export hint: disable **Apply Modifiers** and re-export the FBX when evaluated modifiers have removed the shape keys. A Basis-only Blender mesh has no adjustable target; import cannot recover keys omitted from the exported file.

The supplied `.temp/fluttershy_test.fbx` was inspected directly as binary FBX data. It is 687,596 bytes, FBX version 7400, and identifies its creator as Blender 4.5.13 LTS. Its Objects section contains one Mesh geometry (`Circle.002`) with 5,778 control points, 60 LimbNode models, one Skin deformer, and 46 Cluster deformers. It contains **zero Shape geometries, zero BlendShape deformers, and zero BlendShapeChannel deformers**. The absence of sliders for this export is therefore consistent with its contents; there are no exported shape key deltas to recover. The file does not establish which export setting caused the omission. Re-export the original Blender mesh with its non-Basis shape keys intact and **Geometry → Apply Modifiers** disabled, then select the new FBX in the mod. Blender's [export option definition](https://github.com/blender/blender/blob/main/scripts/addons_core/io_scene_fbx/__init__.py) explicitly states that applying modifiers prevents exporting shape keys. No parser changes were made during this sample inspection.

The importer expands targets using the same triangle indices and duplicated fourth vertex as rendered geometry, applies the mesh instance's linear transform to position deltas, and derives normal deltas from transformed unit normals. Target ids include mesh index, target index, and imported name, distinguishing duplicate names while sharing one weight across instances of a mesh. Multi-material mesh targets remain independently adjustable. Height normalization scales position deltas once without applying its ground translation; its positive uniform scale leaves unit normal deltas unchanged. Material recovery and model copies preserve target blocks and weights.

`SkinnedModel.withShapeKeyProfile()` builds shaped geometry from the unchanged base vertices, sums weighted deltas, and normalizes normals before bone skinning. Changing a slider rebuilds the current local cache entry through `SkinManager.saveDefaultShapeKeyProfile()` without reparsing the FBX. The Default deformation applies to the base pose and remains present during existing bone animations, first-person rendering, previews, and when animations are disabled. Server cache reloads reapply the local Default profile only when the player UUID and selected model hash match; remote players retain zero weights. Normalization bounds use base geometry so a profile cannot alter the normalization scale or accumulate deformation on reload.

Support includes position and available normal targets exposed by Assimp or connected binary FBX Shape records. Malformed native target vertex counts are skipped, with binary recovery attempted for meshes without usable native targets. Authored shape animation tracks, in-between channel evaluation, negative/overdriven weights, and additional profiles are outside the current default-pose experiment.

Validation: `bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.2:compileJava :fabric-26.2:compileClientJava` passed all four tasks after the initial shape key implementation, the Blender detection correction, and the dedicated shape key settings screen, using the installed JDK 26 and the configured Java 21/25 release levels. Gradle required access to its existing cache outside the workspace sandbox. No other test suites or build tasks were run. The supplied Fluttershy FBX was inspected structurally and contains no shape key records; a re-export containing shape keys has not been supplied. In-game appearance, preview rotation/zoom, slider interaction, and imported shape deformation have not been visually verified in Minecraft.

## Experimental Voice Shape

Settings → Voice Shape Settings opens a dedicated editor on Fabric 1.21.1 and 26.2. Each selected model saves one `VoiceShapeSettings` mapping, separate from its Default shape profile. Existing saves initialize safely with target **None**. Choose **Bone** or **Shape key**, select an imported name in the searchable picker, and set the start/end values. Source IDs distinguish shape keys with identical names on different meshes. A bone uses XYZ rotation offsets in degrees, limited to ±180°; a shape key uses start/end weights in 0–100%. Reversed endpoints are supported. Selecting None disables audio input and leaves ordinary rendering intact.

The editor refreshes the saved Default shape profile when opened, including after changing it in the shape key menu, and shows the selected model in its default pose, with mouse drag rotation, wheel zoom, Reset View, Back/Escape, a response meter, live/manual preview selection, a manual percentage slider, and Test Pulse. Manual amount and Test Pulse affect only the editor preview. Controls scroll independently of the preview. The pane layout adapts to narrow windows, and the searchable picker scrolls long name lists. Settings save immediately without reparsing the model.

Input **Auto** uses Simple Voice Chat when installed and otherwise opens the default local microphone. **Simple Voice Chat** waits for its client connection; **Microphone** explicitly selects local microphone input. Simple Voice Chat is recommended but optional: it is declared as a Fabric suggestion, its API 2.6.0 is compile-only, and its plugin is loaded through the optional `voicechat` entrypoint. No voice chat mod or API is bundled or required. The plugin has no Minecraft client references, so registration is safe on a dedicated server. It reads [ClientSoundEvent PCM](https://voicechat.modrepo.de/de/maxhenkel/voicechat/api/events/ClientSoundEvent.html) after normal-priority handlers without replacing/cancelling the frame, following the [official Fabric API registration](https://dev.modrepo.de/minecraft/voicechat/api/getting_started). Muting, disabling, or disconnecting Simple Voice Chat clears the envelope; Auto does not switch to another microphone to bypass those states.

Without Simple Voice Chat, a daemon worker reads mono 48 kHz signed 16-bit PCM through Java Sound's default microphone. Capture begins only when an enabled mapping is active in a world with FBX model display enabled, or in its editor. It stops on None, input/target changes, disconnect, leaving the editor outside a world, and client shutdown. An unavailable microphone is shown in the screen, while manual preview and Test Pulse remain available. Audio arrays are reduced immediately to scalar levels and are never saved or sent by this feature. Only Simple Voice Chat handles its own existing audio transmission.

`VoiceAudioEnvelope` computes RMS amplitude, configurable sensitivity and a noise threshold. **Volume** continuously interpolates between the endpoints; **Rhythm / onsets** detects sharp increases relative to the recent amplitude baseline; **Threshold trigger** fires when the threshold is crossed, with hysteresis before retriggering. Pulses last 200 ms. A 40 ms attack and 140 ms release smooth motion, and audio older than 250 ms returns toward the start endpoint. Rhythm is amplitude-onset detection, not music tempo tracking.

`VoiceShapePose` is an immutable per-frame snapshot. Both player renderers apply it only when the local session UUID and selected model hash match and the imported target still exists. Bone rotation is additive about the current posed joint in model-space XYZ and propagates to descendants before skinning. It composes with clips, head look, and live limb poses. Shape motion replaces the selected key's Default weight for that frame, preserves other Default weights, and rebuilds from unchanged base geometry before skinning; a bounded cache reuses identical voice weights. Voice Shape remains independently enabled when ordinary clip/live-pose animations are disabled. None restores the saved Default shape profile. The 26.2 render state carries the voice snapshot through deferred submission, and first-person rendering retains its existing hidden-head and visibility restrictions.

This first experiment controls the local selected player model. Mapping synchronization, remote player voice animation, additional mappings/profiles, and authored shape animation clips are not part of this implementation. The supplied `.temp/fluttershy_test.fbx` can supply bone targets but contains no exported shape key records.

Validation uses only the four Java/client compilation tasks required by AGENTS.md. `bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.2:compileJava :fabric-26.2:compileClientJava` passed all four tasks with JDK 26 and the configured Java 21/25 release levels. Gradle used its existing cache outside the workspace sandbox; no other test suites or build tasks were run. Runtime microphone capture, optional-mod event delivery, and visual deformation have not been exercised in Minecraft in this workspace.

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

For the `fabric-26.2` target, the summonable FBX mob variants register floating, random strolling, player look-at, and random look-around goals explicitly. Their random strolling uses the four-argument `RandomStrollGoal` with no-action-time checks disabled so summoned display mobs do not stop wandering after being idle. Non-tameable FBX pathfinder mobs also use animal-like walk target scoring, preferring grass blocks and otherwise following light-level pathfinding cost, matching the movement behavior that made the tameable variant reliable. The hostile variant still uses normal Minecraft hostile targeting and melee damage rules, including peaceful mode preventing attacks.

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

Upload permission is intentionally scoped to model uploads. Dedicated servers allow permission-level-2 operators and player names stored in the world's `fbx-player-models/upload-permissions.txt`. Integrated servers additionally recognize the native singleplayer owner identity, allowing the owner to upload in a world with cheats disabled without granting general command permissions.

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

Both Fabric 1.21.1 and 26.2 use the same pose rules through their version-specific `CustomModelPose`, `SkinnedModel`, `ModelNormalizer`, and rendering adapters. Both target modules are present and enabled in `settings.gradle`.

- The Assimp importer retains every scene hierarchy node, including unweighted joints and armature/helper roots. Skin influences use Assimp joint-node identities when available, so duplicate joint names do not collapse their weights. Mesh node transforms are baked into positions and normals in the same model space as the skeleton. The binary fallback retains model parents, joint bind matrices, rotation/scaling pivots, and all meshes; it uses only imported skin weights and leaves unweighted geometry unweighted. Both import paths retain all positive weight influences rather than limiting vertices to four bones. Material recovery transfers matching position/UV appearances onto the Assimp geometry instead of replacing its skeleton or weights.
- Import orientation uses the FBX file's current `UpAxis` and `UpAxisSign`, exposed through [Assimp scene metadata](https://github.com/assimp/assimp/blob/master/code/AssetLib/FBX/FBXConverter.cpp). `com.aksulightning.fbxplayermodels.model.FbxCoordinateSpace` supplies one scene-world conversion to model-space +Y. Y-up files retain their scene orientation; Z-up/X-up files receive the corresponding signed rotation. A model-space wrapper root applies that conversion to the skeleton, while the same conversion precedes mesh node transforms. Imported node-local binds and animation keys retain their source axes, and normalization subsequently transforms only the wrapper root. This corrects the reported 90-degree tilt caused by combining composed scene transforms with the previous unconditional `(x, z, -y)` Blender conversion. The Assimp static/skinned paths and binary fallback share the corrected orientation rule, preserving matching geometry for material recovery.
- The six logical parts are Head, Chest, Right Arm, Left Arm, Right Leg, and Left Leg. Resolution accepts arbitrary explicitly bound joint names before automatic matching, fills unspecified parts automatically, and keeps an explicitly missing joint unresolved. Automatic matching skips Assimp pivot helper names and prefers a head over a neck. Regenerating procedural clips after binding edits replaces earlier generated clips while retaining authored clips and explicit clip mappings.
- Model normalization copies geometry, applies the height/ground transform to skeleton roots once, retains child-local bind matrices, and recomputes inverse global binds. Degenerate/empty bounds are left unchanged. The normalized model retains its bindings and animation-enable setting; authored absolute root animation keys receive the same normalization once when sampled.
- World rendering aligns the complete model with interpolated player body yaw, including the optional first-person self model. The 26.2 submission adapter also honors final render-state body/head overrides used by the inventory preview.
- View yaw and body yaw use angle-aware interpolation. Their wrapped difference is clamped to ±85 degrees; interpolated view pitch is clamped to ±90 degrees. Head look is `R = Ry(-relativeYaw) * Rx(pitch)`, with no generated head roll. With translation removed from the head's global bind matrix `B`, the head's posed transform is postmultiplied by `inverse(B) * R * B`. Its authored origin/basis/scale and inherited posture remain in the transform, and children inherit the result. Look applies during Idle, Walk, Sneak, and Sit, including authored clips or absent clips, and is suppressed during Sleep.
- First- and third-person player rendering call the same live limb-pose calculation. Walking uses player walk progress and interpolated amplitude, with opposite arm/leg phases and no tick-age substitute for zero amplitude. Sitting bends and spreads the legs and lowers the arms; held items modify the corresponding physical arm, and attack/block movement also applies while seated. Live pitch, yaw, and roll use model-space X, Y, and Z. Each limb delta is `T(currentJoint) * Ry(yaw) * Rx(pitch) * Rz(roll) * T(-currentJoint)`; recursive parent-first evaluation passes the posed joint and delta to descendants. Player poses replace generated limb swing tracks even when the live rotation is zero. Generated walking tracks remain available for display/mob rendering without live player poses.
- Generated sneaking torso posture is additive in the chest's local bind space. Its authored pivot remains valid by default. Geometry supplies a substitute attachment only when the joint lies outside the entire mesh bounds by more than twice the mesh diagonal; the substitute is the base center of directly chest-weighted geometry, expressed in the chest bind space. This changes the posture pivot without rewriting bind matrices or weights.
- Skin matrices are `posedGlobal * inverseBind`. Positions and inverse-transpose normals blend with the existing weights; vertices without valid influences keep their original positions. Resulting normals are normalized, and UVs, textures, and material colors pass through unchanged. The saved animation-enable setting disables clip playback and live bone poses together. First-person camera, visibility, spectator, sleep, display-setting, and hidden-head restrictions remain in the existing renderer.

Validation for these changes is limited to source inspection, review of the supplied screenshots, and the authorized four Java/client compilation tasks. The authorized command passed all four tasks using the installed JDK 26 with the configured Java 21/25 release levels. The default Java 8 could not run Gradle, and the wrapper needed approval to write its existing cache lock outside the workspace sandbox. The orientation fix has not been visually verified in Minecraft; the screenshot models are not present as FBX assets in this workspace. In-game pose, camera, and imported-asset behavior were not verified in this workspace. Neither target was unavailable.

The complete changes from the FBX bone/pose implementation and subsequent orientation correction are exported in -path here-. The patch includes shared binding/coordinate-space code, both targets' importers, normalization, skinning, procedural/live poses, animation-setting integration, first-/third-person rendering adapters, 26.2 render-state data, and this documentation. Its baseline is -number here-, before the bone/pose work began. Apply it from the project root; module paths and version-specific APIs may need adapting when porting to a lite codebase with a different layout.

## Minecraft 26.2 port

The 26.2 module carries its own Minecraft-sensitive source and resources. It includes the player and entity renderers, rig binding, screens and file picker, local configuration, commands, networking payloads, tracked entity data, value input/output persistence, server upload permissions and size checks, safe filename validation, model synchronization, client caches, lifecycle hooks, Mod Menu integration, all five FBX entities, and custom tame items.

Important 26.2 API changes handled in this module include:

- Screens and toasts are now reached through `Minecraft.gui`.
- The first-person hand renderer hook targets `submitHandsWithItems` with its complete 26.2 descriptor.
- The living-entity name-tag submission no longer receives the removed distance argument.
- Packed light lookup moved to `LightCoordsUtil`.
- Toast submission uses the 26.2 `SystemToast` helpers and GUI-owned `ToastManager`.
- Main-menu and player-render mixins use explicit 26.2 method descriptors without local-variable capture assumptions.

The title-screen moderation notice is guarded for the full client session rather than per `TitleScreen` instance. Its toast uses `SystemToast.addOrUpdate`, preventing recreated or reinitialized title screens from queuing duplicate notices during first startup.

Minecraft 26.2 applies inventory mouse-follow rotations to `LivingEntityRenderState` after entity extraction. Animated FBX player vertices are therefore generated during render submission from the final `bodyRot`, `yRot`, and `xRot` values. This keeps both horizontal body/head yaw and vertical head pitch aligned with the inventory mouse pose without mutating the live player entity or depending on captured method locals.

The 26.2 client rendering path was audited for backend-specific calls. It contains no direct `GL11`, `GL20`, `GL30`, `GlStateManager`, manual shader binding, texture-state mutation, or framebuffer access. FBX geometry is submitted through `SubmitNodeCollector.submitCustomGeometry`, `RenderTypes.entityCutout`, `PoseStack`, and `VertexConsumer`; textures use Minecraft's `DynamicTexture` and texture manager. These abstractions leave reversed depth and backend state to Minecraft/Blaze3D. Assimp remains a native FBX parser and does not issue rendering commands.

The default OpenGL backend is the minimum supported runtime target. The experimental Vulkan backend was not launched or functionally tested during this port, so Vulkan compatibility is not claimed. No known raw-OpenGL architectural limitation remains in the 26.2 Minecraft rendering path, but correctness under Vulkan remains an explicit runtime-verification limitation.

The nightly workflow builds 26.2 with Java 25 as its own matrix target and publishes a distinct `nightly-26.2` artifact. The stable Mod Menu 20.0.1 artifact is resolved from Modrinth Maven.

The 26.2 development launch uses DevAuth `1.2.2`; unlike 1.2.1, it does not depend on the Apache HttpClient classes removed from the game runtime. DevAuth is `runtimeOnly` and is not bundled into the production mod jar.

## Adding another Fabric Minecraft version

1. Confirm the actual Minecraft version, mapping model, Fabric Loader version, Fabric API version, Loom version, Java version, and Mod Menu version from official metadata.
2. Add target-specific properties to `gradle.properties`.
3. Add `include("fabric-<minecraft-version>")` to `settings.gradle`.
4. Create `fabric-<minecraft-version>/build.gradle` from the closest compatible target.
5. Copy only the necessary Fabric glue, resources, entrypoints, and mixins into the new module.
6. Add Yarn only for an obfuscated target; Minecraft 26.2 uses the unobfuscated names supplied by the game.
7. Adapt dependencies, `fabric.mod.json`, mixin targets, injection descriptors, GUI, networking, persistence, entity, and rendering APIs for that Minecraft version.
8. Audit the new rendering path for backend-specific graphics calls.
9. Keep shared logic in `common`; isolate mapping-sensitive differences inside the Fabric target module.
10. Run the focused compile tasks for every supported target, build the new module, and inspect the production jar.

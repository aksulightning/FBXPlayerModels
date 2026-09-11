# FBX Player Models Lite Code Guide

## Lite architecture

FBX Player Models Lite is a Fabric client-only mod. It loads one locally selected FBX model and uses it only when rendering the active local player.

The client-only boundary is enforced in several layers:

- Both `fabric.mod.json` files declare `"environment": "client"`.
- The only Fabric initializer is `me.onethecrazy.FBXPlayerModelsClient`; there is no `main` entrypoint.
- `SkinManager` owns one `selfSkin` cache rather than a UUID-indexed player cache.
- Each player-render mixin checks that the rendered player is the current Minecraft client player before using the custom model.
- The first-person renderer accepts only the local-player type and reads the same local cache.
- The project registers no custom payloads, network receivers, server commands, entity types, or entity renderers.

The stable mod id remains `fbx-player-models` so existing configuration and asset identifiers continue to work. The display name and archive base name are `FBX Player Models Lite` and `fbx-player-models-lite`.

## Removed full-version features

This branch does not contain:

- FBX display entities or pathfinding FBX mobs.
- Server-side model storage or upload permissions.
- Client-to-server model uploads.
- Server-to-client model downloads.
- Player model lookup, distribution, broadcast, or synchronization packets.
- Remote-player custom-model caching or rendering.
- Mob-model download caches.
- Community-server upload disclaimers or upload controls.

Do not reintroduce server model handling into the Lite branch. Features that need multiplayer distribution belong in the full mod.

## Supported Fabric targets

Each target is independently buildable and contains its own Minecraft-version-sensitive client code. `common` supplies pure Java model data and shared assets; it is not a standalone mod.

| Module | Minecraft | Namespace | Java | Fabric Loom | Fabric Loader | Fabric API |
| --- | --- | --- | --- | --- | --- | --- |
| `fabric-1.21.1` | `1.21.1` | Yarn `1.21.1+build.3` | 21 | `1.16.2` | `0.16.14` | `0.116.6+1.21.1` |
| `fabric-26.2` | `26.2` | unobfuscated game names | 25 | `1.17.17` | `0.19.3` | `0.156.0+26.2` |

| Module | Mod version | Mod Menu | LWJGL Assimp/NFD | Production jar |
| --- | --- | --- | --- | --- |
| `fabric-1.21.1` | `2.0.0+1.21.1` | `11.0.3` | `3.3.3` | `fbx-player-models-lite-v2.0.0+1.21.1+mc1.21.1.jar` |
| `fabric-26.2` | `2.0.0+26.2` | `20.0.1` | `3.4.1` | `fbx-player-models-lite-v2.0.0+26.2+mc26.2.jar` |

The 1.21.1 target uses Yarn and Loom's `remapJar`. Minecraft 26.2 exposes unobfuscated names and produces its artifact through `jar`. Both targets bundle jgltf `2.0.4` plus Native File Dialog and Assimp natives for Windows, Linux, and macOS on x64 and arm64.

## Required compile command

For requested coding work, compile only the supported main and client source sets:

```bash
bash ./gradlew \
  :fabric-1.21.1:compileJava \
  :fabric-1.21.1:compileClientJava \
  :fabric-26.2:compileJava \
  :fabric-26.2:compileClientJava
```

Production build tasks remain:

```bash
./gradlew :fabric-1.21.1:build
./gradlew :fabric-26.2:build
```

Artifacts are written under each module's `build/libs/` directory.

## Project layout

```text
root/
  settings.gradle
  build.gradle
  gradle.properties
  common/
    src/main/java/...
    src/main/resources/...
  fabric-1.21.1/
    build.gradle
    src/main/resources/fabric.mod.json
    src/client/java/...
    src/client/resources/fbx-player-models.client.mixins.json
  fabric-26.2/
    build.gradle
    src/main/resources/fabric.mod.json
    src/client/java/...
    src/client/resources/fbx-player-models.client.mixins.json
```

There is deliberately no version-specific `src/main/java` implementation. The version modules contain client source plus the metadata that marks the mod as client-only.

## Common code

Keep version-independent code in `common`, including:

- `FBXPlayerModels.MOD_ID` and `DISPLAY_NAME`.
- Configuration/save data classes.
- Pure Java model, skeleton, rig, and animation data.
- Mapping-independent numeric utility types.
- Shared assets.
- Platform-neutral client interfaces under `com.aksulightning.platform`.

Common code must not import Minecraft, Fabric, Mixin, Mod Menu, or mapping-specific classes.

Each Fabric module adds `common/src/main/resources` to its own `main` resource source set. This makes shared assets—including `assets/fbx-player-models/textures/white_pixel.png`—part of the active Fabric mod resource pack in both development runs and production jars. The jar task imports only the common compiled-class directories, preventing those resources from being packaged twice.

Untextured or texture-missing FBX materials use `fbx-player-models:textures/white_pixel.png`, tinted by the material's diffuse/base color. Keep this fallback in the shared assets and keep the common resource source-set wiring in every Fabric target; otherwise Minecraft renders those faces with its magenta-and-black missing-texture pattern during development.

## Version-specific client code

Keep Minecraft- and Fabric-sensitive code in both Fabric target modules, including:

- `ClientModInitializer` and Mod Menu integration.
- Client commands and screens.
- FBX parsing and dynamic texture creation.
- Player and first-person rendering.
- Camera and held-item hooks.
- Client lifecycle integration and game-directory access.

When a feature changes in one target, make the equivalent mapping-appropriate change in the other target.

## Local model lifecycle

`SkinManager.pickClientSkin()` opens the native file chooser. Selection continues on the render thread through `selectSelfSkin(Path)`:

1. Read the selected local file.
2. Reject formats other than FBX.
3. Hash the bytes and copy them into `.fbxplayermodels/skins/<sha256>.fbx`.
4. Save the selected hash, original display name, and rig settings in `.fbxplayermodels/.config`.
5. Parse and normalize the model into the single in-memory `selfSkin` cache.

`loadSelfSkin()` restores only that configured local model. No UUID lookup, connection event, server request, or world state participates in loading.

## Rendering boundary

Third-person rendering is implemented by the version-specific `RenderMixin`:

- 1.21.1 requires `renderedPlayer == MinecraftClient.getInstance().player`.
- 26.2 requires `renderedPlayer == Minecraft.getInstance().player`.

If that identity check fails, vanilla rendering continues untouched. This guarantees that remote players cannot receive the local model on the same client. Because the mod has no networking and is not installed on the server, no other client can learn or render the selection.

`FirstPersonSelfModelRenderer` is separately guarded by the local player, first-person camera, option state, spectator state, invisibility, and sleeping pose. GUI previews render the same local cache directly and fall back to the vanilla local player when no custom model is selected.

## Mixins

The only mixin configuration is `fbx-player-models.client.mixins.json`. Client mixins are:

- `CameraMixin`
- `HeldItemRendererMixin` on 1.21.1 / `ItemInHandRendererMixin` on 26.2
- `RenderMixin`
- `MainMenuMixin`

There is no common/server mixin configuration and no access widener.

## Minecraft 26.2 rendering notes

The 26.2 path uses `SubmitNodeCollector.submitCustomGeometry`, `RenderTypes.entityCutout`, `PoseStack`, and `VertexConsumer`. It contains no direct raw OpenGL state manipulation. Animated vertices are generated during render submission so inventory-preview body/head rotations from `LivingEntityRenderState` are respected.

Assimp parses FBX files but does not issue rendering calls. OpenGL is the minimum runtime target; experimental Vulkan behavior has not been functionally verified.

## Adding another Fabric target

1. Confirm the Minecraft version, mappings, Java, Loader, Fabric API, Loom, and Mod Menu versions.
2. Add target properties and include the module in `settings.gradle`.
3. Copy the closest Fabric module's client source and resources.
4. Adapt mapping-sensitive GUI, renderer, lifecycle, and mixin APIs.
5. Keep metadata client-only and declare no `main` entrypoint.
6. Preserve the local-player identity check and single self-model cache.
7. Do not add server networking, registration, commands, persistence, or entities.
8. Compile every supported target with the focused compile command.

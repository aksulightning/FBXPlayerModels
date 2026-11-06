# FBX Player Models

Use an FBX model as your Minecraft player skin in single-player and LAN games.

This release is experimental and made for playing with friends. Public multiplayer is outside its supported scope.

## Release variants

| Minecraft | Loader | What other players see |
| --- | --- | --- |
| 1.21.1 | Fabric (full) | Shared models in an **Open to LAN** world when host and guests run matching mod versions |
| 1.21.1 | Fabric (client only) | Your model is displayed only on your own client |
| 26.3 | Fabric (full) | Shared models in an **Open to LAN** world when host and guests run matching mod versions |
| 26.3 | Fabric (client only) | Your model is displayed only on your own client |
| 1.21.1 | Forge | Your model is displayed only on your own client |
| 1.12.2 | Forge (client only) | Your model is displayed only on your own client |
| Beta 1.7.3 | Babric + StationAPI | Your model is displayed only on your own client |

Fabric 1.21.1 and 26.3 have separate `fabric-1.21.1-client` and `fabric-26.3-client` variants, alongside the client-only Forge and Beta variants. They require no server-side installation, send no model data, and do not make your model visible to friends. Their local configuration paths and saved selections are retained.

Full Fabric gameplay features activate in single-player or after a compatible integrated LAN host approves the connection. Dedicated servers are unsupported, even on a private address. The client-only variants use the resolved peer address to allow local-network connections; a client-only mod cannot establish whether a private endpoint is a dedicated server, tunnel, VPN, or proxy. These checks are a support policy, not a security boundary.

## Fabric 1.21.1 client-only variant

`fabric-1.21.1-client` is provided for future-proofing [Aksu's Peaceful Gameplay Pack](https://modrinth.com/modpack/aksus-peaceful-gameplay-pack). Its production jar is `fbx-player-models-client-v2.0.0+1.21.1+mc1.21.1.jar`.

It includes model selection/previews, rig binding, animation settings, first-person controls, Default shape keys, local Voice Shape, and the full version's UpAxis-aware FBX import and shape recovery. Open **Settings → Shape Key Settings** or **Voice Shape Settings** to edit the new features. Simple Voice Chat is optional; Voice Shape can also use the local microphone. LAN model sharing and FBX entities remain exclusive to the full version. The detailed feature comparison is in [Code_Guide.md](Code_Guide.md).

Choose either the full or client-only Fabric 1.21.1 jar for an instance; both declare the same mod ID and cannot be installed together. The client-only target has its own workflow option and is included when **all** is selected. Its modpack note is appended automatically to GitHub and Modrinth release notes, including when custom notes are entered.

## Fabric 26.3 client-only variant

`fabric-26.3-client` includes local model selection/previews, rig and animation settings, first-person controls, Default shape keys, and Voice Shape with microphone or optional Simple Voice Chat input. It shares the full 26.3 version's import and deformation code and uses Minecraft's 26.3 rendering and SDL input APIs.

The production filename is `fbx-player-models-client-v2.0.0+26.3+mc26.3.jar`. Java 25+, Fabric Loader 0.20.1+, and Fabric API are required. Gameplay rendering follows the same single-player/local-network policy as the 1.21.1 client-only variant; model editing remains available outside those connections. Settings and models stay on your client. Model sharing and FBX entities are available in the full variant.

Install either the full or client-only Fabric 26.3 jar in an instance; both use the same mod ID. The manual build and release workflows offer `fabric-26.3-client` separately and include it in **all**.

## Forge 1.21.1 local features

Forge includes Default shape-key sliders/reset and previews, Voice Shape bone or shape-key mappings, and UpAxis-aware FBX import with binary shape recovery. Open **Settings → Shape Key Settings** or **Voice Shape Settings**. Voice Shape supports the local microphone and optional Simple Voice Chat input, with manual preview and Test Pulse for setup. Settings and animations stay on your client.

The Forge mod ID and saved model selections are retained. Simple Voice Chat is optional; if installed, its API must be version 2.6.0 or newer. Use the production `fbx-player-models-forge-v2.0.0+1.21.1+mc1.21.1.jar`, built through `:forge-1.21.1:jarJar`.

## Use a model

For Forge 1.12.2, use the separate `forge-1.12.2` target with Java 8 and Forge 14.23.5.2847 or newer. Open **FBX Models** from the title screen, press **K**, or use `/skin`. This legacy port includes local FBX selection, preview, procedural rig animation, bone binding, scale, first-person body, and camera settings. It does not include the modern shape-key, Voice Shape, GLB/OBJ, or imported animation-clip features. Build its production jar with `bash ./gradlew :forge-1.12.2:reobfJar`; build/toolchain details are in [Code_Guide.md](Code_Guide.md). Its settings and model cache use `.fbxplayermodels/forge-1.12.2/` and it requires no server installation.

1. Install the correct variant and its loader dependencies. Beta also requires StationAPI.
2. Start a single-player world or join a supported LAN game.
3. Open the model settings through the title-screen preview or `/skin`.
4. Choose an `.fbx` file under **2 MiB**.
5. Open **Settings → Edit Model Rig** to adjust the rig. Start with **Auto Bind**, then correct individual bones as needed.

Selections and editor settings save locally. Model editing and menu previews remain available when gameplay features are inactive on an unsupported connection. Unsupported connections continue to work normally in Minecraft, with vanilla player rendering and a status message.

Simple Blender models with clear bone names, few materials, and low geometry counts are the easiest starting point. A template is available in the [template branch](https://github.com/aksulightning/FBXPlayerModels/tree/template).

## Share models on Fabric LAN

The host opens a single-player world using **Open to LAN**. Host and guests install the matching Fabric mod version. Models and supported appearance settings are exchanged through that world's integrated server after the LAN session check succeeds.

The host can upload without enabling cheats. Existing operator and guest upload permissions remain in effect. With the necessary command permissions, grant or remove guest upload permission using:

```text
/fbxplayermodels uploadperm <playername> yes
/fbxplayermodels uploadperm <playername> no
```

Model files must be strictly smaller than 2 MiB. Share models only with people you trust.

Full Fabric variants include Default shape keys, Voice Shape, and FBX entity rendering. Fabric 1.21.1 Client Only, Fabric 26.3 Client Only, and Forge 1.21.1 include local Default shape keys, Voice Shape, and the updated UpAxis-aware FBX importer. Beta retains its existing Lite feature set.

## FBX entities on modern Fabric

Place entity model files in the LAN world's `fbx-player-models/mobskins` directory. For example:

```text
/summon fbxplayermodels:view_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:hostile_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:tameable_entity ~ ~ ~ {Model:"this_file.fbx",TameItem:"minecraft:apple"}
```

These require command permissions. Supported clients receive the named model from the LAN host.

## Manual builds and releases

Release workflows run only when you click **Run workflow**. They build the exact commit from the branch you select, including `main`; there are no scheduled or push-triggered releases.

- **GitHub release**: open **Actions → GitHub release → Run workflow**, select the branch and one target or **all**, enter a new tag such as `v2.0.0-lan`, and run it. It builds/tests the selected variants and creates one release containing their production jars. The default `alpha` channel creates a GitHub prerelease.
- **Modrinth release**: open **Actions → Modrinth release → Run workflow**, select the branch and targets, leave the channel as **alpha**, and run it. It builds/tests the selected variants, verifies their artifacts, and publishes a loader/game-specific Modrinth version for each target. Configure repository secret `MODRINTH_TOKEN` and variable `MODRINTH_ID` first.

Both release workflows accept release notes. They do not require an earlier build run ID or a separately entered mod version; version metadata comes from the selected source's `gradle.properties`. Existing GitHub tags/releases are never replaced. Modrinth skips an already-listed version only when its channel, loader/game pair, and file contents match; conflicting versions stop publication.

**Manual release build** remains available for building downloadable artifacts without publishing. It is also the shared build implementation used by both release workflows. All selected builds must succeed before publication, and the configured Forge/Beta tests run as part of those builds.

Commit and push the workflow files to the default branch so GitHub displays their **Run workflow** buttons. Exact production filenames, toolchains, and the manual gameplay checklist are documented in [Code_Guide.md](Code_Guide.md).

## Credits

Based on [AllTheSkins by 1TheCrazy](https://github.com/1TheCrazy/AllTheSkins).

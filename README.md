# FBX Player Models

Use an FBX model as your Minecraft player skin in single-player and LAN games.

This release is experimental and made for playing with friends. Public multiplayer is outside its supported scope.

## Release variants

| Minecraft | Loader | What other players see |
| --- | --- | --- |
| 1.21.1 | Fabric | Shared models in an **Open to LAN** world when host and guests run matching mod versions |
| 26.2 | Fabric | Shared models in an **Open to LAN** world when host and guests run matching mod versions |
| 1.21.1 | Forge | Your model is displayed only on your own client |
| Beta 1.7.3 | Babric + StationAPI | Your model is displayed only on your own client |

Forge and Beta are client-only variants. They require no server-side installation, send no model data, and do not make your model visible to friends. Their existing local configuration paths and saved selections are retained.

Modern Fabric gameplay features activate in single-player or after a compatible integrated LAN host approves the connection. Dedicated servers are unsupported, even on a private address. Forge and Beta use the resolved peer address to allow local-network connections; a client-only mod cannot establish whether a private endpoint is a dedicated server, tunnel, VPN, or proxy. These checks are a support policy, not a security boundary.

## Use a model

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

The modern Fabric variants retain Default shape keys, Voice Shape, and FBX entity rendering. These full-mod features are not added to the imported client-only ports.

## FBX entities on modern Fabric

Place entity model files in the LAN world's `fbx-player-models/mobskins` directory. For example:

```text
/summon fbxplayermodels:view_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:hostile_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:tameable_entity ~ ~ ~ {Model:"this_file.fbx",TameItem:"minecraft:apple"}
```

These require command permissions. Supported clients receive the named model from the LAN host.

## Manual builds and releases

The manual release workflows use `modrinth-last-update`. Pushing changes does not immediately trigger a build or publication.

In GitHub Actions, manually run **Manual release build** on `modrinth-last-update`, selecting one target or all four. Each artifact contains its production jar and a manifest recording its source commit and SHA-256 checksum. Download and review the artifacts first.

To publish, separately run **Publish reviewed release** on the same branch. Supply the successful build run ID, reviewed target, version, channel, release notes, and destination. Publication uses the existing artifact without rebuilding. GitHub publication requires a new tag and never replaces an existing tag or release. Modrinth publication requires the repository secret `MODRINTH_TOKEN` and variable `MODRINTH_ID`.

Local compilation commands, exact production filenames, toolchains, and the manual gameplay checklist are documented in [Code_Guide.md](Code_Guide.md). No build or publication is triggered by opening model settings or using the mod.

## One-time scheduled alpha release

A separate workflow targets **2 November 2026 at 13:00 Helsinki time (11:00 UTC)**. It must be committed and pushed with its supporting scripts to the default branch, `main`, before that time to activate the schedule. It builds all four variants, runs the configured Beta/Forge tests, verifies the artifacts, and publishes missing alpha versions to Modrinth after all builds succeed. It requires `MODRINTH_TOKEN` and `MODRINTH_ID`.

The date guard prevents releases on any other day. GitHub may delay scheduled jobs, so publication follows the build/test completion rather than an exact 13:00 upload. **One-time alpha release 2026-11-02** also supports manual dispatch on `main` after 13:00 that same day if the scheduled event is missed. Existing matching versions are skipped; conflicting versions are never replaced.

## Credits

Based on [AllTheSkins by 1TheCrazy](https://github.com/1TheCrazy/AllTheSkins).

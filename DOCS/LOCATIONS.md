<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

[AMENDED 2026-05-07]:
- Repository moved to `https://github.com/Otterdays/Minecraft-Civ-Remixed`.
- Project codename is now "Project OOGA".
- Strategic product direction is documented in `whitepaper.md`.
- Current codebase remains FPS-template-first and is pending civ-module implementation.

# LOCATIONS.md — quick codebase map

Use this as the first stop for quick discovery.

## First-check workflow (for future agents/builders)

1. Read **`AGENTS.md`** and **`CLAUDE.md`** for repo-wide agent rules + git conventions.
2. Read `LOCATIONS.md` (this file) for fast pathing.
3. Read `README.md` for behavior, compatibility, and run/test commands.
4. Read `build.gradle` and `gradle.properties` before changing versions/deps; read `gradle/wrapper/gradle-wrapper.properties` when changing the Gradle distribution.
5. Read `src/main/resources/fabric.mod.json` before changing IDs/entrypoints.

## Core implementation

- Main mod initializer: `src/main/java/com/fpsmod/FpsMod.java`
- Client initializer: `src/client/java/com/fpsmod/FpsModClient.java`
- FPS HUD feature: `src/client/java/com/fpsmod/client/FpsHudOverlay.java`
- Screen-level FPS toggle button: `src/client/java/com/fpsmod/client/FpsHudScreenButton.java`
- FPS HUD config persistence: `src/client/java/com/fpsmod/client/FpsHudConfig.java`
- Unit test starter: `src/test/java/com/fpsmod/FpsModTest.java`

## Mod metadata + wiring

- Mod metadata and entrypoints: `src/main/resources/fabric.mod.json` (`environment: client` → Mod Menu **Client** badge; `contact.issues` → Mod Menu **Issues** link; optional `suggests.modmenu` for nicer in-game mod list when Mod Menu is installed)
- Prism/MultiMC mod icon asset: `src/main/resources/assets/fpsmod/icon.png`
- Readme-only cropped icon (not packaged in the mod JAR): `readme-assets/icon_modrinth_cropped.png`
- Mod id constant: `FpsMod.MOD_ID` in `src/main/java/com/fpsmod/FpsMod.java`
- Client entrypoint target class: `com.fpsmod.FpsModClient` in `fabric.mod.json`

## Build + toolchain

- Gradle build config: `build.gradle`
- Version pins (Minecraft/Fabric/Loader/Loom): `gradle.properties`
- **Gradle wrapper** (exact distribution ZIP): `gradle/wrapper/gradle-wrapper.properties` — this repo uses a **9.6 nightly** from `services.gradle.org/distributions-snapshots/` (not `distributions/`).
- Project name + plugin repos: `settings.gradle`
- Windows build shortcut: `build.bat`
- Gradle wrappers: `gradlew.bat`, `gradlew`, `gradle/wrapper/`

## Feature behavior map (FPS HUD)

- HUD layer registration: `FpsHudOverlay.register()` in `FpsHudOverlay.java`
- HUD render method: `FpsHudOverlay.render(...)`
- Shared toggle state methods: `FpsHudOverlay.toggleHud()`, `setHudShown(...)`, `isHudShown()`
- 1-second FPS sampling: `ClientTickEvents.END_CLIENT_TICK` block in `FpsHudOverlay.java`
- Screen-button registration: `FpsHudScreenButton.register()` (`ScreenEvents.AFTER_INIT`)
- Config file path runtime target: `config/fpsmod/hud.properties`

## Logging conventions

- Startup + debug log patterns: `src/main/java/com/fpsmod/FpsMod.java`
- HUD toggle log (`🔁`): `src/client/java/com/fpsmod/client/FpsHudOverlay.java`
- Rule: keep emoji prefixes for high-visibility console scanning.

## Commands you will use most

- Build jar: `gradlew.bat build` (or `build.bat`)
- Run dev client: `gradlew.bat runClient`
- Run tests: `gradlew.bat test`

## Otters Civ. / economy (server gameplay)

[AMENDED 2026-05-11]:
- Runtime wallet path: **`config/otters_civ_revived/wallet.properties`** (`FileWalletStore`); migrate-from legacy **`config/fpsmod/wallet.properties`** once if present. Optional **`# Name:`** plaintext lines precede **`uuid=balance`**; persist via **`WalletService`** (`WalletLedger` load/save).

[AMENDED 2026-05-10]:
- Passive rewards orchestration: `src/main/java/com/fpsmod/ottersciv/reward/RewardOrchestrator.java`
- Reward rules + loader: `src/main/java/com/fpsmod/ottersciv/config/RewardRules.java`, `RewardRulesLoader.java`; tests: `src/test/java/com/fpsmod/ottersciv/config/RewardRulesLoaderTest.java`
- Gameplay wiring: `src/main/java/com/fpsmod/ottersciv/OttersCivGameplay.java` (`JoinWelcome.java` registers join chat; **`JoinAttendanceSavedData.java`** — per-save returning-player flag, **`fpsmod:join_attendance`**)
- Commands: `src/main/java/com/fpsmod/command/OtterCommand.java`, `MoneyCommand.java` (`/money set` · `Permission.HasCommandLevel(GAMEMASTERS)`); wallet: `src/main/java/com/fpsmod/economy/`
- Shipped datapack tags: `src/main/resources/data/otters_civ_revived/tags/`

## Output paths

- Main mod jar: `BUILT/libs/project-ooga-1.0.0.jar` (Gradle `mod_version`; older docs may cite `fps-mod-1.0.0.jar`—prefer `gradle.properties`)
- Sources jar: `BUILT/libs/project-ooga-1.0.0-sources.jar`
- Test report: `BUILT/reports/tests/test/index.html`
- Gradle problems report: `BUILT/reports/problems/problems-report.html`

## Safe-edit hotspots

- UI/HUD-only changes: `src/client/java/com/fpsmod/client/`
- Cross-cutting metadata changes: `fabric.mod.json` + `gradle.properties` + `README.md`
- Version upgrades: keep `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties` (when bumping Gradle), and this README’s compatibility table aligned.

## Git / upstream

- Default remote is set when you clone; this template was pushed to `https://github.com/Otterdays/Minecraft-Fabric-Sample-Mod` (if you fork or rename the repo, update all `contact` fields in `fabric.mod.json`: `homepage`, `sources`, and `issues`).

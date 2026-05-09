<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Added
- Added **Imagery Optimizery** utility under `images/optimize-here/`: Pillow batch pipeline + optional tkinter GUI (`python optimize_pngs.py --gui`, or `optimize-pngs.bat`); shared logic with CLI (`optimize_pngs.py` without `--gui`). [2026-05-08]: GUI file list + per-row size estimates (in-memory), optional explicit files vs whole input folder.
- Added Project OOGA whitepaper defining civ-platform vision and delivery roadmap.
- Added DOCS core set: `SUMMARY`, `SCRATCHPAD`, `SBOM`, `STYLE_GUIDE`, `ARCHITECTURE`, `CHANGELOG`, and `My_Thoughts`.
- Added economy bootstrap internals: `WalletStore`, `FileWalletStore`, and `WalletService`.
- Added `/money` and `/money set <player> <amount>` command paths.

### Changed
- Updated repository remote and metadata references to `Minecraft-Civ-Remixed`.
- Updated project naming/branding to Project OOGA.
- Moved location and readme image artifacts into docs/readme-friendly structure.
- Updated `fabric.mod.json` environment from `client` to `*` to allow server command registration.

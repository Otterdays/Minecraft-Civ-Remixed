<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SCRATCHPAD

## Active Tasks (Newest First)
- [2026-05-08] Imagery Optimizery GUI: optional per-file list (Add files / Add folder / Remove / Clear), table with input px+KB, output px, estimated PNG/JPG KB (in-memory encode); Optimize uses list if non-empty else full input folder; debounced estimates + generation guard for stale threads.
- [2026-05-08] [NOTE] Imagery Optimizery (`images/optimize-here/`): maintainer likes current GUI look/feel. Do **not** restyle, “simplify,” or re-theme the tkinter UI unless asked explicitly — prefer docs/behavior-only changes.
- [2026-05-08] Imagery Optimizery GUI: expanded “About” copy, grid layout for paths + browse, run bar (Clear log + hint + Optimize), output log section, footer path; primary action bottom-right of run strip above log.
- [2026-05-08] [AMENDED] Imagery Optimizery GUI: create `Tk()` before `StringVar`/`IntVar` (Python 3.14+ requires default root).
- [2026-05-08] Added Imagery Optimizery minimal tkinter GUI (`--gui`) + batch launcher branding; refactor shared `run_batch`/CLI.
- [2026-05-08] Fixed `images/optimize-here/optimize-pngs.bat` working directory (`cd /d "%~dp0"`); anchored `optimize_pngs.py` paths to script dir + PNG zlib level 9, default max width 960, progressive JPG.
- [2026-05-07] Recentered README on Project OOGA civ-mod identity and `/money` bootstrap usage.
- [2026-05-07] Implemented `/money` bootstrap command and wallet persistence; tracking in `DOCS/ROADMAP.md`.
- [2026-05-07] Created dedicated execution roadmap in `DOCS/ROADMAP.md`.
- [2026-05-07] Docs alignment pass for Project OOGA rule compliance.
- [2026-05-07] Whitepaper upgraded to premier civ-mod strategy document.
- [2026-05-07] Repo branding/remote migration to `Minecraft-Civ-Remixed`.

## Current Status
- Codebase runtime is still FPS-template-first.
- Product direction is now civ-suite-first (factions/jobs/professions/economy/player shops).
- Core DOCS baseline files created for stable multi-agent handoff.
- Economy bootstrap is now active: `/money` and `/money set` compile and build successfully.

## Last 5 Actions
1. Documented Imagery Optimizery in `SUMMARY` + `CHANGELOG`; SCRATCHPAD note to leave GUI as-is unless explicitly requested.
2. Imagery Optimizery tkinter GUI + `--gui`; batch opens GUI by default; shared `run_batch()` for CLI/UI.
3. Patched promo image optimizer batch + Python under `images/optimize-here/` for cwd-safe runs and smaller web outputs.
4. Rewrote `README.md` to center Project OOGA civ-mod scope and current command bootstrap.
5. Marked README recentering progress in `DOCS/ROADMAP.md` ASAP tracker.
- [AMENDED 2026-05-08]: Prior “last actions” still valid: server-safe `fabric.mod.json` environment `*`; implemented `WalletStore` / `FileWalletStore` / `WalletService`; registered `/money` and `/money set <player> <amount>` paths.

## Blockers
- None currently.

## Out-of-Scope Observations
- Java package/mod id still references `fpsmod`; full namespace migration not started.
- Existing feature docs describe FPS module behavior; civ module specs are in whitepaper only.

## Next Steps
1. Add permission gate for `/money set` so only operators/admins can mutate balances.
2. Add `/pay` and `/ooga money set|add|take` command surface.
3. Add immutable transaction log records for balance mutations.
4. Add transfer caps/cooldowns config for abuse control.
5. Mark completed M1 checklist items as they land.

<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SCRATCHPAD

## Active Tasks (Newest First)
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
1. Rewrote `README.md` to center Project OOGA civ-mod scope and current command bootstrap.
2. Marked README recentering progress in `DOCS/ROADMAP.md` ASAP tracker.
3. Added server-safe command bootstrap by changing `fabric.mod.json` environment to `*`.
4. Implemented `WalletStore`, `FileWalletStore`, and `WalletService`.
5. Registered `/money` and `/money set <player> <amount>` command paths.

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

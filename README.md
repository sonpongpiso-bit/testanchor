# Anchor Macro (Fabric 26.1.2)

Client-side anchor sequence macro for Minecraft Java Edition **26.1.2**.

## Behavior

- Hold right-click while the Respawn Anchor is in your main hand to trigger once.
- If the crosshair is already on a placed Respawn Anchor, the macro skips placement.
- Otherwise it places the anchor against the block you're targeting.
- Finds Glowstone and Totem of Undying in the hotbar or main inventory.
- If either item is in the main inventory, it swaps it into the currently selected hotbar slot using the normal inventory click interaction.
- Charges the anchor, switches to the Totem, then attempts to detonate.
- Releasing right-click re-arms the macro for the next hold.

## Build

Minecraft 26.1.x requires **JDK 25**. Open this folder as a Gradle project or run:

```bash
gradle build
```

The built mod jar will be in `build/libs/`.

## GitHub Actions

Push this project to GitHub. The workflow at `.github/workflows/build.yml` builds the project and uploads the JAR as a workflow artifact.

## Notes

- The macro uses Minecraft's normal client interaction methods for placement, charging, inventory swapping, and detonation. It does not construct custom or spoofed packets.
- Server latency and anti-cheat rules vary. The tick delays are constants near the top of `AnchorMacroClient.java`; increase them if the server does not register an action before the next step.
- Keep a Totem in your inventory and make sure the anchor is actually chargeable. The macro aborts if it cannot find the required item or target.
- This is a source project, not a prebuilt mod jar. GitHub Actions compiles it for you.

# Cow-hat

Client-side Fabric mod for **Minecraft 26.3** (works on Hypixel, since it only changes what your own client draws).
Any entity wearing a cow hat (a head-slot item whose name contains "cow") has it replaced by a blue
Star of David floating above its head.

## Build
Requires Java 25.

    JAVA_HOME=/path/to/jdk-25 ./gradlew build

The jar is written to `build/libs/cow-hat-1.0.0.jar`. Install it in `mods/` with Fabric Loader 0.20.1+ and Fabric API 0.167.0+26.3.

## Notes
- Two mixins: `LivingEntityRendererMixin` flags the render state when the head item is a cow hat;
  `CustomHeadLayerMixin` skips the normal head rendering and draws the star (`StarOfDavid`).
- Detection is by item name. If Hypixel's cow hat is named differently, edit the check in `LivingEntityRendererMixin`.
- Compiled and mixin targets checked against the 26.3 jar, but not run in-game (no GPU in the build container).

## Authorization
The star-swap only runs for players on an allowlist, checked against the **locally logged-in
account's name/UUID** — never an access token or session credential. On first launch the mod
writes `config/cowhat-allowlist.json`:

```json
{
  "names": ["snowrain"],
  "uuids": []
}
```

That's the default the mod ships with (authorizing `snowrain`). Add more usernames or UUIDs
as needed. An empty or missing file means nobody is authorized (fails closed) — the mod does
nothing and vanilla rendering is untouched.

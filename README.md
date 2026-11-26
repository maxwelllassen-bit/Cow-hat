# Cow-hat

Client-side Fabric mod for **Minecraft 26.3** (works on Hypixel, since it only changes what your own client draws).
Any entity wearing a cow hat (a head-slot item whose name contains "cow") has it replaced by a blue
Star of David floating above its head — visible to **anyone running the mod**, for **anyone's**
cow hat. Allowlisted wearers can additionally get a custom texture instead of the star; see below.

## Build
Requires Java 25.

    JAVA_HOME=/path/to/jdk-25 ./gradlew build

The jar is written to `build/libs/cow-hat-1.0.0.jar`. Install it in `mods/` with Fabric Loader 0.20.1+ and Fabric API 0.167.0+26.3.

## Notes
- Two mixins: `LivingEntityRendererMixin` flags the render state when the head item is a cow hat
  and resolves a custom texture (if the wearer is allowlisted); `CustomHeadLayerMixin` skips the
  normal head rendering and draws either the custom texture or the star (`StarOfDavid`).
- Detection is by item name. If Hypixel's cow hat is named differently, edit the check in `LivingEntityRendererMixin`.
- Compiled and mixin targets checked against the 26.3 jar, but not run in-game (no GPU in the build container).

## Who sees what
- **The Star of David swap is unrestricted.** Anyone running the mod sees it, on anyone's cow
  hat, for any "cow"-named head item. There's no allowlist on this part.
- **Custom per-item textures (below) are restricted by wearer.** Only a player on the
  allowlist can have their head item replaced by a custom texture; everyone else wearing a
  matching item still just gets the plain star.

## Allowlist (who can have custom textures)
Checked against the **wearer's own account name/UUID** — never an access token or session
credential. On first launch the mod writes `config/cowhat-allowlist.json`:

```json
{
  "names": ["snowrain"],
  "uuids": []
}
```

That's the default the mod ships with (authorizing `snowrain`). Add more usernames or UUIDs
as needed. An empty or missing file means nobody is authorized (fails closed) — nobody gets
custom textures, though the plain star still works for everyone.

## Custom textures
A player on the allowlist can have their head item mapped to their own PNG instead of the
star. Edit `config/cowhat-textures.json`:

```json
{
  "rules": [
    {"match": "birthday", "file": "party_hat.png"},
    {"match": "cow", "file": "my_star.png"}
  ]
}
```

- `match` is a case-insensitive substring checked against the item's display name; the first
  matching rule wins.
- `file` is a PNG placed in `config/cowhat/textures/`. It's drawn as a flat quad above the
  head, sized to the image's aspect ratio.
- This only applies to a head item worn by an allowlisted player. For anyone else, a matching
  "cow" item still falls back to the built-in star; a non-"cow" item with no rule match is
  untouched either way.
- Textures are loaded and uploaded to the GPU lazily, the first time they're needed, since
  that needs an active render context.

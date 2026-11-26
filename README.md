# Cow-hat

Client-side Fabric mod for **Minecraft 26.3** (works on Hypixel, since it only changes what your own client draws).
A cow hat (a head-slot item whose name contains "cow") worn by an **allowlisted player** is replaced
by a blue Star of David floating above their head — visible to **anyone running the mod**. Nobody
else's cow hat is touched. Allowlisted wearers can additionally get a custom texture instead of
the star; see below. Separately, an allowlisted player can also have cat ears, drawn on top of
whatever they're wearing.

## Build
Requires Java 25.

    JAVA_HOME=/path/to/jdk-25 ./gradlew build

The jar is written to `build/libs/cow-hat-1.0.0.jar`. Install it in `mods/` with Fabric Loader 0.20.1+ and Fabric API 0.167.0+26.3.

## Notes
- Two mixins: `LivingEntityRendererMixin` flags the render state (cow hat, matching custom
  texture, cat ears — each checked against its own allowlist); `CustomHeadLayerMixin` draws
  them. The head-item swap (texture or star) cancels vanilla rendering of that item; cat ears
  are drawn additively and never cancel anything.
- Detection of the cow hat is by item name. If Hypixel's cow hat is named differently, edit the check in `LivingEntityRendererMixin`.
- Compiled and mixin targets checked against the 26.3 jar, but not run in-game (no GPU in the build container).

## Who sees what
- **Both the star swap and custom textures are locked to wearers on the allowlist.** If a
  non-allowlisted player wears a "cow"-named item, nothing happens — vanilla rendering,
  untouched. There is no viewer-side restriction: anyone running the mod sees the effect, but
  only on an allowlisted wearer.
- The mod ships defaulting the allowlist to `snowrain`, so the star is effectively locked to
  that one player until you edit the config.

## Allowlist (who the mod applies to)
Checked against the **wearer's own account name/UUID** — never an access token or session
credential. On first launch the mod writes `config/cowhat-allowlist.json`:

```json
{
  "names": ["snowrain"],
  "uuids": []
}
```

That's the default the mod ships with (authorizing `snowrain`). Add more usernames or UUIDs
as needed. An empty or missing file means nobody is authorized (fails closed) — the mod does
nothing for anyone, star included.

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
- This only applies to a head item worn by an allowlisted player. If that player's item
  doesn't match any rule but still contains "cow", it falls back to the built-in star.
  Non-allowlisted players are untouched regardless of item name.
- Textures are loaded and uploaded to the GPU lazily, the first time they're needed, since
  that needs an active render context.

## Cat ears
A separate cosmetic, independent of the star/texture swap above: an allowlisted player gets a
pair of black triangle-outline cat ears drawn on top of their head, regardless of what head
item they're wearing (it doesn't replace anything). Checked the same way, by the wearer's own
account name/UUID, against `config/cowhat-catears.json`:

```json
{
  "names": ["Becausepainfully"],
  "uuids": []
}
```

That's the default the mod ships with. This is a completely separate list from
`cowhat-allowlist.json` — a player can be on one, both, or neither.

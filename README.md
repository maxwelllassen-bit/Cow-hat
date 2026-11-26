# Hatrix

Client-side Fabric mod for **Minecraft 26.3**. It only changes what your own client draws, so
it works on Hypixel. It gives specific, allowlisted players custom head cosmetics — visible to
**anyone running the mod**, but only on the wearers named in its config files. Nobody else is
touched.

Four independent cosmetics ship by default, each with its own allowlist:

| Cosmetic | Allowlist file | Default wearer(s) | Requires a "cow"-named item? | Replaces or adds? |
|---|---|---|---|---|
| Star of David | `cowhat-allowlist.json` | `snowrain` | Yes | Replaces the item |
| Custom texture | `cowhat-textures-allowlist.json` | `snowrain`, `Becausepainfully`, `jagling`, `Keekraft` | No (rule matches any item name) | Replaces the item, taking priority over the star |
| Cat ears | `cowhat-catears.json` | `Becausepainfully` | No | Adds on top |
| Bear | `cowhat-bear.json` | `jagling` | Yes | Replaces the item |

Each config lives in `config/` and is auto-created with its defaults on first launch. A player
can be on any combination of these lists, independently — being on the star's allowlist
doesn't put you on the custom-texture one, or vice versa.

## Build
Requires Java 25.

    JAVA_HOME=/path/to/jdk-25 ./gradlew build

The jar is written to `build/libs/hatrix-1.0.0.jar`. Install it in `mods/` with Fabric Loader
0.20.1+ and Fabric API 0.167.0+26.3.

## Allowlists
Every list is checked by the wearer's own account **name or UUID only** — never an access
token or session credential — and matching is case-insensitive. An empty or missing config
file means nobody on that list is authorized (fails closed); the mod does nothing for anyone
until you add names or UUIDs.

```json
{
  "names": ["snowrain"],
  "uuids": []
}
```

That's `cowhat-allowlist.json`'s shipped default — the other allowlist files follow the same
`names`/`uuids` shape, just with their own default wearer(s).

## The Star of David
A "cow"-named head item (any item whose display name contains "cow") worn by a player on
`cowhat-allowlist.json` is replaced by a blue Star of David floating above their head. This is
the fallback when nothing more specific (custom texture, bear) applies to that wearer.

## Custom textures
A player on `cowhat-textures-allowlist.json` — a separate list from the star's, checked
independently — can map their own PNG onto any head item. Edit `config/cowhat-textures.json`
for the match rules:

```json
{
  "rules": [
    {"match": "birthday", "file": "party_hat.png"},
    {"match": "cow", "file": "my_star.png"}
  ]
}
```

- `match` is a case-insensitive substring checked against the item's display name; the first
  matching rule wins. It isn't limited to "cow" — any item name works.
- `file` is a PNG placed in `config/cowhat/textures/`, drawn as a flat quad sized to the
  image's aspect ratio.
- A player on this allowlist whose item doesn't match any rule gets no texture from this
  feature — they might still get the plain star if they're also on `cowhat-allowlist.json`
  and the item name contains "cow".
- Textures are loaded and uploaded to the GPU lazily, the first time they're needed, since
  that needs an active render context.

### `/hatrix` menu and command
`/hatrix` alone opens a small in-game menu (`HatrixScreen`): a button to flip the
replace/overlay setting below, a text field for the match string, and an **"Add Image..."**
button that opens your OS's native file picker — pick a PNG from anywhere on your computer and
it's copied into `config/cowhat/textures/` and wired up to the match rule automatically. No
chat commands or manual file-copying required. This never touches the network; the image never
leaves your machine.

The same two actions are also available as chat subcommands (client-side only, never sent to
the server), if you'd rather script them or skip the menu:

- `/hatrix toggle` — flips whether a matched custom texture **replaces** the head item
  (the default) or **sits above it additively**, the same way cat ears do. This is one global
  setting, persisted to `config/hatrix-settings.json`, affecting every wearer's custom
  texture, not per-player.
- `/hatrix addtexture <match> <file>` — appends `{"match": "<match>", "file": "<file>"}` to
  `cowhat-textures.json` and reloads. `file` must already exist in `config/cowhat/textures/`
  (the menu's "Add Image..." button does that copy for you).

## Cat ears
A player on `cowhat-catears.json` gets a pair of black triangle-outline cat ears drawn on top
of their head, regardless of what head item they're wearing — this doesn't replace anything.

## Bear
A player on `cowhat-bear.json` who's also wearing a "cow"-named item gets a brown bear-face
outline instead of the star. If a custom texture rule also matches that wearer's item (and
they're on the custom-texture allowlist), the texture wins; priority is **custom texture →
bear → plain star**.

## Notes
- Two mixins: `LivingEntityRendererMixin` resolves each flag during state extraction (cow hat,
  matching custom texture, cat ears, bear — each against its own allowlist); `CustomHeadLayerMixin`
  draws them. The head-item swap (texture, bear, or star) cancels vanilla rendering of that
  item; cat ears are always additive. A custom texture only cancels rendering when
  `/hatrix toggle` is in its default ("replace") state — in overlay mode it draws additively,
  same as cat ears, and the actual item (or the bear/star, if either also applies) stays
  visible underneath.
- "Cow hat" detection is by item name. If Hypixel's cow hat is named differently, edit the
  check in `LivingEntityRendererMixin`.
- Compiled and mixin targets checked against the 26.3 jar, but not run in-game — there's no
  GPU in the build container, so exact positioning/visuals are unverified.

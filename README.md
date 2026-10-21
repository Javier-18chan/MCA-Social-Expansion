# MCA Social Expansion

MCA Social Expansion extends MCA Reborn with more dynamic relationships, remote communication, gradual discovery of NPC personalities and preferences, contextual gifts, and social interactions that remember past encounters.

## Version 0.9.2

- Revived NPCs can be added to contacts again by interacting with them while holding a social item.
- Dialogue distinguishes between children, grandchildren, and great-grandchildren and respects the player's selected gender.
- New spontaneous messages for grandchildren and great-grandchildren, tailored to their life stage.
- Persistent portraits with background loading and saving, without repeatedly searching for distant contacts.
- Prevents interactions with deceased NPCs through interfaces that remained open.

See [CHANGELOG.md](CHANGELOG.md) for the release history.

## Features

- Gradual discovery of NPC personalities and preferences.
- A hybrid gift system with categories, item quality, favorites, likes, and dislikes.
- Controlled support for items from other mods, with invalid gifts filtered out.
- Smartphone, letter, crystal, and magic mirror interfaces with contact portraits.
- A message inbox and configurable delivery times.
- Spontaneous NPC messages based on your relationship, with replies and Nether and End contexts.
- Custom sounds for each communication interface.
- Topic-based conversations, insults, apologies, and reconciliation, with memory of recent interactions.
- Visual improvements and interface layout fixes.

## Compatibility

- Minecraft 1.20.1.
- Forge 47 or later.
- Java 17.

This repository contains only the Forge 1.20.1 source code. The official JAR for this version is available in the Releases section.

## Dependencies

MCA Reborn is the only required mod dependency. This project targets MCA Reborn 7.7.1-alpha.1 or later on Minecraft 1.20.1.

Optional integrations:

- MCA Quests 1.1.0 or later.
- MCA Conversations 1.0.0 or later.
- Townstead 0.7.6 or later.

## Building

Place these build dependencies in `libs/`:

- `minecraft-comes-alive-7.7.1-alpha.1+1.20.1-universal.jar`
- `architectury-9.2.14-forge.jar`

Then run:

```powershell
.\gradlew.bat build
```

The compiled JAR is generated in `build/libs/`. The build also runs family relationship regression tests, including later generations and family trees containing cycles.

## Languages

The mod includes translations for US English, Spanish from Spain, Mexican Spanish, US Spanish, and Brazilian Portuguese.

## Author

_XavierCodes_

## License

GNU General Public License v3.0 only (`GPL-3.0-only`). See `LICENSE` for the full terms.

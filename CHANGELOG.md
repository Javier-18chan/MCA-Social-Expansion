# Changelog

## 0.9.2

- Fixed an issue that prevented revived NPCs from being added to contacts again. Interact with them while holding a social item to register them again.
- Fixed dialogue that caused grandchildren to call the player "Dad" or "Mom".
- Added appropriate forms of address for grandparents and great-grandparents, along with three spontaneous message variants per life stage for grandchildren and great-grandchildren.
- Optimized portraits: saved photos are reused, and distant contacts without a photo are no longer searched for continuously.
- Portrait loading and saving now run in the background. New photos are saved at a maximum resolution of 128 x 128 pixels.
- Limited automatic retries after failed portrait captures. The retake option allows another attempt.
- Blocked interactions through open interfaces with NPCs who have just died.
- Updated translations in English, Spanish from Spain, Mexican Spanish, and Brazilian Portuguese.

## 0.9

- Added the magic mirror, including its interface, item, and crafting recipe.
- Added persistent contact portraits and an option to retake them.
- Added spontaneous messages based on the player's relationship with each NPC, with settings to enable or disable them.
- Added message replies and conversations with a final NPC response, including exclusive Nether and End contexts.
- Added incoming message sounds for the smartphone, letter, and crystal, plus an outgoing message sound.
- Added topic-based conversations, insults, apologies, and reconciliation with memory of past interactions.
- Improved the inbox, interface conversions, and text layout.
- Fixed dialogue keys for Interests, Plans, and Memories.

This repository contains only the source code and release files for Forge 1.20.1.

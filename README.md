----------------
CHECKERS.EXE 🕹️
----------------

**Designed by Rose Torres and Jonathan Pabilic**



CHECKERS.EXE is our take on a retro arcade checkers game. It supports multiplayer over a server, but we also
added an AI opponent so you can play solo and a full sound system.

----------------
  Features
----------------

Can't find an opponent? Play against the AI anytime.

- Uses a **minimax algorithm** to look ahead and choose optimal moves
- Evaluates board state using a scoring function that weighs piece count, kings, and board position
- Correctly enforces **forced jumps**, just like human gameplay
- **Adjustable difficulty** easier settings look fewer moves ahead, harder settings think deeper

----------------
🔊 Sound System
----------------

The retro theme only lands if it sounds like one too.

- **Lobby music** — plays as soon as you enter the lobby
- **In-game music** — a separate, more tense track kicks in when the board loads
- **Piece sound effects:**
  - Moving a piece → arcade click
  - Illegal move attempt → error buzz
- **Mute button** on the board to silence music without disabling move sounds
- All audio is managed through a single `soundManager` class shared across all scenes — no overlapping or unexpected restarts



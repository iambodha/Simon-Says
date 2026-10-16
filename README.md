# Simon Says

A Java plugin for Minecraft servers: randomized Simon Says challenges play out inside a shrinking circular particle border. Players have 15 seconds to follow a real instruction or ignore a fake one, while the safe area closes over six phases.

![Particle border during a game](https://cdn.modrinth.com/data/cached_images/0394f6cd97188b8b7b39951b01a666c94c5938b2.png)

## Build and install

Requires **Java 21**, **Maven 3.9+**, and a **Spigot-compatible Minecraft 1.21.1** server. The plugin compiles against Spigot API `1.21.1-R0.1-SNAPSHOT`; other server versions need a playtest.

```sh
mvn --batch-mode verify
```

Copy `target/simon-1.0.0-SNAPSHOT.jar` to your server's `plugins/` directory, then restart the server. CI also builds a `simon-plugin` artifact on pushes and pull requests. Use the JAR whose filename starts with `simon-`, rather than the `original-` JAR produced during shading.

## Run a game

| Command | Behavior |
| --- | --- |
| `/startgame` | Start or restart one server-wide game session, centered on you in your current world |
| `/stopgame` | Stop the session, cancel pending challenges, and restore the world's previous border size |
| `/endgame` | Alias for `/stopgame` |

Commands require `simon.admin`, granted to operators by default. There is no duration argument. The participant list is captured from players in the starting world; late joiners and players in other worlds do not receive challenges. Existing participants can rejoin and resume the current task. Border damage applies to players in the game world, so use a dedicated game world.

- A prompt beginning **“Simon says”** is real: perform the action within 15 seconds.
- Other prompts are fake: avoid that action until time expires.
- A task resolves once per player. Failure applies random negative potion effects; success can grant a short speed effect. This is a survival minigame, without automatic elimination or winner detection.
- Challenges start once per minute, from 60 through 1,020 seconds. The border uses six fixed wait/shrink phases. At the final zone, border damage continues until an administrator stops the game.

![Challenge display](https://cdn.modrinth.com/data/cached_images/a26868dd8aea36b1477f89c470015627e2eaeb7a.png)

## Engineering

| Component | Responsibility |
| --- | --- |
| `Game_Setup` | Border phases, particles, outside-zone damage, timer, and world-border restoration |
| `TaskManager` | Challenge catalog, participant scope, scheduling, boss bars, rewards, and penalties |
| `TaskOutcome` | One-time success/failure decisions, independently unit tested |
| `GameCommands` | Administrative commands and permissions |

Stopping, restarting, and disabling cancel all pending task launches. The border decreases at the configured per-second rate. Particle emission uses the configured 100 points × 16 height sections at two updates per second, rather than growing with circumference and multiplying each point by height. Prompts now agree with the rule being evaluated; contradictory mid-task prompts are disabled because they did not have matching evaluation logic.

## Validation and remaining work

`mvn verify` compiles the plugin, runs the outcome/timing regression tests, and packages the JAR. These tests cover the response window, fake-task behavior, single resolution, and border shrink duration. They do not replace a multiplayer server playtest.

Before using a build with a group, playtest start → challenge → stop → wait past the next scheduled challenge; restart twice; join/rejoin mid-task; and check a second world. Confirm particle performance with the intended player count. Some catalog challenges depend on specific biomes, mobs, or structures and may not be achievable in every arena. Rewards and penalties last for their configured potion durations after stopping.

[Apache License 2.0](LICENSE).

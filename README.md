# Terra Futura

A Java game-logic implementation for the **Principles of Software Design (1)** semester project at FMFI UK, academic year **2025/26**.

The repository models cards, resources, player grids, decks, effects, scoring, and turn progression. It exposes a Java interface for a host application and includes unit and integration test sources.

## Implemented components

- **Cards and resources:** resource collections, pollution values, and upper/lower effect checks.
- **Player grids:** card placement on a 5 × 5 coordinate grid, occupied-cell checks, activation tracking, and configurable activation restrictions.
- **Card piles:** up to four visible cards plus a hidden deck; drawing a visible card can refill its slot from the hidden deck.
- **Effects:** fixed resource transformations, transformations based on an input count, and alternative effects composed with OR.
- **Action processing:** validate referenced cards, input resources, output acceptance, and effect parameters before transferring resources.
- **Assistance and rewards:** use another player's lower card effect and select a reward for the assisting card.
- **Scoring:** count complete resource combinations and multiply them by a configured point value.
- **Turn state:** `Game` checks the active player and permitted action phase, delegates actions to `TerraFuturaComponent`, and advances to the next player when a turn finishes.
- **Observers:** notify registered player callbacks with serialized state after successful component actions.

These are game-engine components. The checkout does not include a graphical interface, a console launcher, a server, saved-game storage, or a complete predefined card catalogue.

## Requirements and dependencies

- **JDK 21 or later**; Maven compiles with `release 21`.
- **Maven**; no Maven wrapper is committed.

Declared runtime dependencies:

| Dependency | Version in `pom.xml` |
| --- | --- |
| Apache Commons Lang | 3.9 |
| Apache Commons Collections | 4.4 |
| org.json | 20190722 |

The test configuration declares JUnit 4, JUnit Jupiter, and Mockito 5.11.0. Most test classes use JUnit 4; `TerraFuturaInterfaceIntegrationTest` uses JUnit Jupiter.

## Build and inspect

```bash
git clone https://github.com/denus-marker/TerraFutura.git
cd TerraFutura
mvn compile
mvn test
mvn package
```

The normal Maven package target is `target/TERRA_FUTURA-1.0-SNAPSHOT.jar`. It is a library JAR: there is no `main` method or executable application entry point.

On systems with Make, the repository also provides:

```bash
make check_and_test  # mvn test
make lint            # mvn checkstyle:check
```

### Test configuration notes

The POM currently has duplicate declarations for JUnit 4 and Maven Surefire, and uses the dynamic version `RELEASE` for JUnit Jupiter. It does not declare the JUnit Vintage engine. Since Surefire can choose the JUnit Platform provider, JUnit 4 test execution should be checked explicitly in the generated test reports; a successful command alone is not evidence that every test class ran.

Test reports, when generated, are located in `target/surefire-reports/`.

## Java API

The main integration interface is `TerraFuturaInterface`:

| Method | Purpose |
| --- | --- |
| `takeCard` | Move a card from a selected pile slot to an empty grid position |
| `discardLastCardFromDeck` | Discard the last non-null visible card, refilling when possible |
| `activateCard` | Process resource inputs, outputs, and optional assistance |
| `selectReward` | Apply an available reward to the assisting card |
| `selectActivationPattern` | Select a configured activation pattern |
| `selectScoring` | Calculate a configured scoring method |
| `turnFinished` | Clear turn activation tracking and advance the game phase/player |

Use `Game` when integrating turn and phase checks. `TerraFuturaComponent` performs component actions without enforcing the same turn sequence.

The host supplies:
- A positive player count and a `GameObserver`.
- A map of each player's `LEVEL_I` and `LEVEL_II` piles.
- A grid, activation-pattern list, and scoring-method list for every player.
- Zero-based player IDs and initialized cards/effects.

A `CardSource` uses a visible index from `0` to `3`, or `-1` for the top hidden card. Grid coordinates range from `-2` to `2` on each axis; for example, `GridPosition.X0_Y0`.

See [the interface integration tests](src/test/java/sk/uniba/fmph/dcs/terra_futura/TerraFuturaInterfaceIntegrationTest.java) for complete examples of player setup, a turn cycle, and assistance/reward flow.

### A small component example

```java
import java.util.List;
import sk.uniba.fmph.dcs.terra_futura.*;

Grid grid = new Grid();
Pile pile = new Pile(List.of(new Card(new Resource[]{Resource.Green}, 0)));

boolean moved = new MoveCard(0).moveCard(pile, GridPosition.X0_Y0, grid);
System.out.println(moved);
System.out.println(grid.state());
```

This snippet belongs inside a Java method or JShell session with the project classes and runtime dependencies on the classpath.

## Architecture

```text
src/main/java/sk/uniba/fmph/dcs/terra_futura/
  Game, GameState                        Turn and phase handling
  TerraFuturaInterface, TerraFuturaComponent
                                         Integration API and action delegation
  Card, Resource, Grid, GridPosition     Board model
  Pile, Deck, CardSource, MoveCard        Card sources and movement
  Effect, TransformationFixed,
  ArbitraryBasic, EfectOr                Effect validation
  ProcessAction, ProcessActionAssistance Resource transfers and assistance
  SelectReward                          Reward selection
  ActivationPattern, InterfaceActivateGrid
                                         Pattern selection contract
  ScoringMethod, Points                 Scoring
  GameObserver, TerraFuturaObserverInterface
                                         State notifications
src/test/java/sk/uniba/fmph/dcs/terra_futura/
                                         Unit and integration test sources
.github/workflows/maven.yml               Maven test and Checkstyle jobs
```

## Current implementation boundaries

Some rule enforcement still requires integration work:
- `Grid` exposes activation restrictions/tracking, but `ProcessAction` does not call `canBeActivated` or `setActivated`.
- Action processing checks the number of pollution positions against effects, but does not add pollution tokens to cards.
- `ActivationPattern` uses `InterfaceActivateGrid`; `Grid` does not implement that interface, so a host must provide an adapter for pattern selection.
- `ScoringMethod` calculates against a copy of resources supplied at construction.
- The supplied effect classes return `false` from `hasAssistance`; an assistance-capable effect must be supplied by the host, as demonstrated in the tests.

The current `Game` flow requires two successful `selectScoring` calls to reach `FINISH`, followed by `turnFinished`. Treat this as the implemented behavior rather than a claim of complete board-game rule coverage.

## License

See [LICENSE](LICENSE) for the Apache License 2.0.

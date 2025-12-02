package sk.uniba.fmph.dcs.terra_futura;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TerraFuturaInterfaceIntegrationTest {
    /**
     * End-to-end integration test for 2 players.
     * Checking:
     *  - correct player on turn (getOnTurn)
     *  - GameState transitions:
     *      TAKE_CARD_NO_CARD_DISCARDED
     *      -> (for the second player, also TAKE_CARD_CARD_DISCARDED)
     *      -> ACTIVATE_CARD
     *      -> SELECT_SCORING_METHOD
     *      -> FINISH
     *      -> TAKE_CARD_NO_CARD_DISCARDED
     *  - increase turnNumber
     *  - return turn to player 0 after a full cycle.
     */
    @Test
    public void fullTurnCycle_twoPlayers_withDiscardForSecondPlayer() {
        GameObserver observer = new GameObserver(new HashMap<>());
        int players = 2;

        Map<Integer, Map<Deck, Pile>> piles = new HashMap<>();
        Map<Integer, Grid> grids = new HashMap<>();
        Map<Integer, List<ActivationPattern>> patterns = new HashMap<>();
        Map<Integer, List<ScoringMethod>> scorings = new HashMap<>();

        for (int p = 0; p < players; p++) {
            Map<Deck, Pile> playerPiles = new HashMap<>();

            Card visible = new Card(new Resource[]{}, 0);
            Card hidden = new Card(new Resource[]{}, 0);
            playerPiles.put(Deck.LEVEL_I, new Pile(
                    List.of(visible),
                    List.of(hidden)
            ));

            playerPiles.put(Deck.LEVEL_II, new Pile(
                    List.of(new Card(new Resource[]{}, 0))
            ));

            piles.put(p, playerPiles);

            grids.put(p, new Grid());
            patterns.put(p, List.of(new ActivationPattern(null, List.of())));
            scorings.put(p, List.of(
                    new ScoringMethod(
                            List.of(Resource.Green),
                            new Points(1),
                            List.of(Resource.Green)
                    )
            ));
        }

        Game game = new Game(players, observer, piles, grids, patterns, scorings);
        TerraFuturaInterface api = game;

        for (int p = 0; p < players; p++) {
            assertEquals(p, game.getOnTurn(), "Wrong player turn");
            assertEquals(GameState.TAKE_CARD_NO_CARD_DISCARDED, game.getState());

            if (p == 1) {
                boolean disc = api.discardLastCardFromDeck(p, Deck.LEVEL_I);
                assertTrue(disc, "Second player should be able to discard last card");
                assertEquals(GameState.TAKE_CARD_CARD_DISCARDED, game.getState());
            }

            boolean ok = api.takeCard(
                    p,
                    new CardSource(Deck.LEVEL_I, 0),
                    GridPosition.X0_Y0
            );

            assertTrue(ok, "takeCard should succeed");
            assertEquals(GameState.ACTIVATE_CARD, game.getState());

            ok = api.activateCard(
                    p,
                    GridPosition.X0_Y0,
                    List.of(),
                    List.of(),
                    List.of(),
                    null, null
            );

            assertTrue(ok, "activateCard without assistance should succeed");
            assertEquals(GameState.ACTIVATE_CARD, game.getState());

            ok = api.selectScoring(p, 0);
            assertTrue(ok, "First selectScoring should succeed");
            assertEquals(GameState.SELECT_SCORING_METHOD, game.getState());

            ok = api.selectScoring(p, 0);
            assertTrue(ok, "Second selectScoring should succeed");
            assertEquals(GameState.FINISH, game.getState());

            int beforeTurnNumber = game.getTurnNumber();
            ok = api.turnFinished(p);
            assertTrue(ok, "turnFinished should succeed");

            assertEquals(beforeTurnNumber + 1, game.getTurnNumber());
            assertEquals(GameState.TAKE_CARD_NO_CARD_DISCARDED, game.getState());
        }

        assertEquals(0, game.getOnTurn(), "After full cycle, turn should return to player 0");
    }

    /**
     * End-to-end integration test for Assistance flow.
     * 2 players:
     *  - Player 0:
     *      takes a card from the Pile to move to ACTIVATE_CARD
     *      activates the card from Assistance, spends Green from the adjacent card,
     *      gets Gear on another card
     *  - Player 1:
     *      has a card with LOWER effect Green -> Gear
     *      receives a reward via SelectReward (Green)
     * We check:
     *  - changes in resources on cards
     *  - GameState transitions:
     *      TAKE_CARD_NO_CARD_DISCARDED -> ACTIVATE_CARD -> SELECT_REWARD
     *      -> ACTIVATE_CARD -> SELECT_SCORING_METHOD -> FINISH
     *      -> TAKE_CARD_NO_CARD_DISCARDED for the next player.
     */
    @Test
    public void assistanceFlow_twoPlayers_endToEnd() {
        GameObserver observer = new GameObserver(new HashMap<>());
        int players = 2;

        Map<Integer, Map<Deck, Pile>> piles = new HashMap<>();
        Map<Integer, Grid> grids = new HashMap<>();
        Map<Integer, List<ActivationPattern>> patterns = new HashMap<>();
        Map<Integer, List<ScoringMethod>> scorings = new HashMap<>();

        for (int p = 0; p < players; p++) {
            Map<Deck, Pile> playerPiles = new HashMap<>();
            playerPiles.put(Deck.LEVEL_I, new Pile(List.of(new Card(new Resource[]{}, 0))));
            playerPiles.put(Deck.LEVEL_II, new Pile(List.of(new Card(new Resource[]{}, 0))));
            piles.put(p, playerPiles);

            grids.put(p, new Grid());
            patterns.put(p, List.of(new ActivationPattern(null, List.of())));
            scorings.put(p, List.of(
                    new ScoringMethod(
                            List.of(Resource.Green),
                            new Points(1),
                            List.of(Resource.Green)
                    )
            ));
        }

        Grid grid0 = grids.get(0);
        Grid grid1 = grids.get(1);

        Effect assistanceEffect = new Effect() {
            @Override
            public boolean check(List<Resource> input, List<Resource> output, int pollution) {
                return true;
            }

            @Override
            public boolean hasAssistance() {
                return true;
            }

            @Override
            public String state() {
                return "AssistanceEffect";
            }
        };

        Card assistanceCard = new Card(new Resource[]{}, 0, assistanceEffect, null);
        Card sourceCard = new Card(new Resource[]{Resource.Green}, 0);
        Card outputCard = new Card(new Resource[]{}, 0);

        grid0.putCard(GridPosition.X0_Y0, assistanceCard);
        grid0.putCard(GridPosition.X1_Y0, sourceCard);
        grid0.putCard(GridPosition.X0_Y1, outputCard);

        Effect lowerEffect = new TransformationFixed(
                List.of(Resource.Green),
                List.of(Resource.Gear),
                0
        );
        Card otherPlayerCard = new Card(new Resource[]{}, 0, null, lowerEffect);
        grid1.putCard(GridPosition.X0_Y0, otherPlayerCard);

        Game game = new Game(players, observer, piles, grids, patterns, scorings);
        TerraFuturaInterface api = game;

        assertEquals(0, game.getOnTurn());
        assertEquals(GameState.TAKE_CARD_NO_CARD_DISCARDED, game.getState());

        boolean ok = api.takeCard(
                0,
                new CardSource(Deck.LEVEL_I, 0),
                GridPosition.X2_Y2
        );
        assertTrue(ok);
        assertEquals(GameState.ACTIVATE_CARD, game.getState());

        List<Pair<Resource, GridPosition>> inputs = List.of(
                Pair.of(Resource.Green, GridPosition.X1_Y0)
        );
        List<Pair<Resource, GridPosition>> outputs = List.of(
                Pair.of(Resource.Gear, GridPosition.X0_Y1)
        );

        ok = api.activateCard(
                0,
                GridPosition.X0_Y0,
                inputs,
                outputs,
                List.of(),
                1,
                GridPosition.X0_Y0
        );

        assertTrue(ok, "activateCard with assistance should succeed");
        assertEquals(GameState.SELECT_REWARD, game.getState());

        assertFalse(sourceCard.getResources().contains(Resource.Green),
                "Source card should lose Green");
        assertTrue(outputCard.getResources().contains(Resource.Gear),
                "Output card should receive Gear");

        ok = api.selectReward(1, Resource.Green);
        assertTrue(ok, "selectReward should succeed for assisting player");
        assertEquals(GameState.ACTIVATE_CARD, game.getState());
        assertTrue(otherPlayerCard.getResources().contains(Resource.Green),
                "Assisting player's card should receive Green reward");

        ok = api.selectScoring(0, 0);
        assertTrue(ok);
        assertEquals(GameState.SELECT_SCORING_METHOD, game.getState());

        ok = api.selectScoring(0, 0);
        assertTrue(ok);
        assertEquals(GameState.FINISH, game.getState());

        int beforeTurn = game.getTurnNumber();
        ok = api.turnFinished(0);
        assertTrue(ok);

        assertEquals(beforeTurn + 1, game.getTurnNumber());
        assertEquals(GameState.TAKE_CARD_NO_CARD_DISCARDED, game.getState());
        assertEquals(1, game.getOnTurn(), "After Player 0 finishes, it should be Player 1's turn");
    }
}
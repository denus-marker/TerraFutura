package sk.uniba.fmph.dcs.terra_futura;

import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Integration tests for Pile class
 */
public class PileIntegrationTest {

    /**
     * End-to-end scenario:
     *  - Pile is created with two visible cards [Green, Red] and one hidden [Yellow].
     *  - Player takes the visible card at index 1 (Red).
     *  - Pile should:
     *      replace slot 1 with the hidden card (Yellow),
     *      make hidden deck empty,
     *      reflect this change in JSON state().
     */
    @Test
    public void endToEnd_takeVisibleCardRefillsFromHiddenAndUpdatesState() {
        Card vis0 = new Card(new Resource[]{Resource.Green}, 0);
        Card vis1 = new Card(new Resource[]{Resource.Red}, 0);
        Card hidden = new Card(new Resource[]{Resource.Yellow}, 0);

        Pile pile = new Pile(Arrays.asList(vis0, vis1), Collections.singletonList(hidden));

        assertSame(vis1, pile.getCard(1).get());
        assertSame(hidden, pile.getCard(-1).get());

        Card chosen = pile.getCard(1).get();
        assertSame(vis1, chosen);

        pile.takeCard(1);

        Optional<Card> afterSlot1 = pile.getCard(1);
        assertTrue("Slot 1 must contain a card after refill", afterSlot1.isPresent());
        assertSame("Slot 1 should be refilled with hidden card", hidden, afterSlot1.get());

        assertFalse("Hidden deck must be empty after refill", pile.getCard(-1).isPresent());

        JSONObject json = new JSONObject(pile.state());
        assertEquals(0, json.getInt("hiddenCount"));

        JSONArray visibleArr = json.getJSONArray("visible");

        JSONObject jsonSlot1 = visibleArr.getJSONObject(1);
        JSONArray resArr = jsonSlot1.getJSONArray("resources");
        List<Object> resList = resArr.toList();

        assertTrue("Slot 1 should now contain Yellow", resList.contains("Yellow"));
        assertFalse("Slot 1 must not contain Red anymore", resList.contains("Red"));
    }

    /**
     * End-to-end scenario:
     * - Pile is created with visible cards [Green, Red, null, Money] and hidden deck [Gear, Car].
     * - removeLastCard() should:
     *      remove the last non-null visible card (Money at index 3),
     *      replace it with top hidden card (Gear),
     *      leave the second hidden card (Car) as new top,
     *      update JSON state accordingly.
     */
    @Test
    public void endToEnd_removeLastCardRefillsLastVisibleSlotAndUpdatesState() {
        Card vis0 = new Card(new Resource[]{Resource.Green}, 0);
        Card vis1 = new Card(new Resource[]{Resource.Red}, 0);
        Card vis3 = new Card(new Resource[]{Resource.Money}, 0);

        Card hidden1 = new Card(new Resource[]{Resource.Gear}, 0);
        Card hidden2 = new Card(new Resource[]{Resource.Car}, 0);

        List<Card> visible = Arrays.asList(vis0, vis1, null, vis3);
        List<Card> hidden = Arrays.asList(hidden1, hidden2);

        Pile pile = new Pile(visible, hidden);

        JSONObject before = new JSONObject(pile.state());
        assertEquals("Initial hiddenCount must be 2", 2, before.getInt("hiddenCount"));

        pile.removeLastCard();

        Optional<Card> slot3 = pile.getCard(3);
        assertTrue("Slot 3 should still contain a card after refill", slot3.isPresent());
        assertSame("Slot 3 should be replaced with hidden1", hidden1, slot3.get());

        assertSame(vis0, pile.getCard(0).get());
        assertSame(vis1, pile.getCard(1).get());
        assertFalse("Slot 2 must remain empty", pile.getCard(2).isPresent());

        assertSame("Top of hidden deck must now be hidden2", hidden2, pile.getCard(-1).get());

        JSONObject after = new JSONObject(pile.state());
        assertEquals(1, after.getInt("hiddenCount"));

        JSONArray visibleArr = after.getJSONArray("visible");
        JSONObject jsonSlot3 = visibleArr.getJSONObject(3);
        JSONArray resArr3 = jsonSlot3.getJSONArray("resources");
        List<Object> resList3 = resArr3.toList();

        assertTrue("Slot 3 should now contain Gear", resList3.contains("Gear"));
        assertFalse("Slot 3 must not contain Money anymore", resList3.contains("Money"));
    }
}

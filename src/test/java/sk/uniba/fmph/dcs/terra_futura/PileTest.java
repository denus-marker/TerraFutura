package sk.uniba.fmph.dcs.terra_futura;

import org.json.*;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *  Unit tests for Pile class
 */
public class PileTest {
    private Card c1;
    private Card c2;
    private Card c3;
    private Card c4;
    private Card hidden1;
    private Card hidden2;

    @Before
    public void setUp() {
        c1 = mock(Card.class);
        c2 = mock(Card.class);
        c3 = mock(Card.class);
        c4 = mock(Card.class);
        hidden1 = mock(Card.class);
        hidden2 = mock(Card.class);

        when(c1.state()).thenReturn("{\"id\":\"c1\"}");
        when(c2.state()).thenReturn("{\"id\":\"c2\"}");
        when(c3.state()).thenReturn("{\"id\":\"c3\"}");
        when(c4.state()).thenReturn("{\"id\":\"c4\"}");
        when(hidden1.state()).thenReturn("{\"id\":\"h1\"}");
        when(hidden2.state()).thenReturn("{\"id\":\"h2\"}");
    }

    // Constructors & basic validation
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsOnNullVisible() {
        new Pile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsOnNullHidden() {
        new Pile(Collections.emptyList(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsWhenMoreThanFourVisible() {
        new Pile(Arrays.asList(c1, c2, c3, c4, mock(Card.class)));
    }

    @Test
    public void constructorVisibleOnlyCreatesEmptyHiddenDeck() {
        Pile pile = new Pile(Arrays.asList(c1, c2));

        assertTrue(pile.getCard(0).isPresent());
        assertTrue(pile.getCard(1).isPresent());
        assertFalse("hidden deck should be empty", pile.getCard(-1).isPresent());
    }

    @Test
    public void constructorWithHiddenInitializesTopCorrectly() {
        Pile pile = new Pile(Collections.singletonList(c1), Arrays.asList(hidden1, hidden2));

        assertTrue(pile.getCard(-1).isPresent());
        assertSame(hidden1, pile.getCard(-1).get());
    }

    // getCard behaviour
    @Test
    public void getCardReturnsVisibleByIndex() {
        Pile pile = new Pile(Arrays.asList(c1, c2));

        assertTrue(pile.getCard(0).isPresent());
        assertSame(c1, pile.getCard(0).get());

        assertTrue(pile.getCard(1).isPresent());
        assertSame(c2, pile.getCard(1).get());
    }

    @Test
    public void getCardReturnsEmptyForInvalidIndex() {
        Pile pile = new Pile(Arrays.asList(c1, c2));

        assertFalse(pile.getCard(2).isPresent());
        assertFalse(pile.getCard(5).isPresent());
        assertFalse(pile.getCard(-2).isPresent());
    }

    @Test
    public void getCardMinusOneReturnsTopHiddenWithoutRemoving() {
        Pile pile = new Pile(Collections.singletonList(c1), Arrays.asList(hidden1, hidden2));

        Optional<Card> first = pile.getCard(-1);
        Optional<Card> second = pile.getCard(-1);

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertSame(hidden1, first.get());
        assertSame("getCard(-1) must not remove from deck", hidden1, second.get());
    }

    // takeCard behaviour: hidden deck
    @Test
    public void takeCardMinusOneRemovesTopHidden() {
        Pile pile = new Pile(Collections.singletonList(c1), Arrays.asList(hidden1, hidden2));

        assertSame(hidden1, pile.getCard(-1).get());

        pile.takeCard(-1);

        assertSame("After removing top, next hidden becomes top", hidden2, pile.getCard(-1).get());

        pile.takeCard(-1);

        assertFalse("Hidden deck should now be empty", pile.getCard(-1).isPresent());
    }

    // takeCard behaviour: visible slots
    @Test
    public void takeCardFromVisibleWithHiddenRefillsSlot() {
        Pile pile = new Pile(Arrays.asList(c1, c2), Collections.singletonList(hidden1));

        assertSame(c2, pile.getCard(1).get());

        pile.takeCard(1);

        assertTrue(pile.getCard(1).isPresent());
        assertSame("Slot 1 should be refilled from hidden deck", hidden1, pile.getCard(1).get());
        assertFalse("Hidden deck should now be empty", pile.getCard(-1).isPresent());

        assertSame("Other visible slots unaffected", c1, pile.getCard(0).get());
    }

    @Test
    public void takeCardFromVisibleWithoutHiddenClearsSlot() {
        Pile pile = new Pile(Arrays.asList(c1, c2));

        pile.takeCard(1);

        assertFalse("Slot 1 should be empty", pile.getCard(1).isPresent());
        assertSame("Slot 0 must remain unchanged", c1, pile.getCard(0).get());
    }

    @Test
    public void takeCardWithInvalidIndexDoesNothing() {
        Pile pile = new Pile(Arrays.asList(c1, c2), Collections.singletonList(hidden1));

        pile.takeCard(5); // invalid index

        assertSame(c1, pile.getCard(0).get());
        assertSame(c2, pile.getCard(1).get());
        assertSame("Hidden deck must remain unchanged", hidden1, pile.getCard(-1).get());
    }

    @Test
    public void takeCardOnNullVisibleSlotDoesNothing() {
        List<Card> visible = Arrays.asList(c1, null, c2);
        Pile pile = new Pile(visible, Collections.singletonList(hidden1));

        pile.takeCard(1); // slot 1 already null

        assertFalse(pile.getCard(1).isPresent());
        assertSame(c1, pile.getCard(0).get());
        assertSame(c2, pile.getCard(2).get());
        assertSame("Hidden deck must remain unchanged", hidden1, pile.getCard(-1).get());
    }

    // removeLastCard behaviour
    @Test
    public void removeLastCardRemovesHighestIndexNonNullVisible() {
        List<Card> visible = Arrays.asList(c1, c2, null, c3);
        Pile pile = new Pile(visible, Collections.emptyList());

        pile.removeLastCard();

        assertSame("c3 should be removed", c1, pile.getCard(0).get());
        assertSame(c2, pile.getCard(1).get());
        assertFalse("index 3 should now be empty", pile.getCard(3).isPresent());
    }

    @Test
    public void removeLastCardRefillsFromHiddenWhenAvailable() {
        List<Card> visible = Arrays.asList(c1, c2);
        Pile pile = new Pile(visible, Arrays.asList(hidden1, hidden2));

        pile.removeLastCard(); // last non-null is index 1

        assertSame(c1, pile.getCard(0).get());
        assertSame("Slot 1 should be replaced by hidden1", hidden1, pile.getCard(1).get());
        assertSame("Next top of hidden should now be hidden2", hidden2, pile.getCard(-1).get());
    }

    @Test
    public void removeLastCardDoesNothingWhenNoVisibleCards() {
        List<Card> visible = Arrays.asList(null, null);
        Pile pile = new Pile(visible, Arrays.asList(hidden1));

        pile.removeLastCard();

        assertFalse(pile.getCard(0).isPresent());
        assertFalse(pile.getCard(1).isPresent());
        assertSame("Hidden deck must stay untouched", hidden1, pile.getCard(-1).get());
    }

    @Test
    public void stateSerializesVisibleAndHiddenCount() {
        List<Card> visible = Arrays.asList(c1, null, c2);
        Pile pile = new Pile(visible, Arrays.asList(hidden1, hidden2));

        String jsonStr = pile.state();
        JSONObject json = new JSONObject(jsonStr);

        assertTrue(json.has("visible"));
        assertTrue(json.has("hiddenCount"));

        assertEquals(2, json.getInt("hiddenCount"));

        JSONArray visibleArr = json.getJSONArray("visible");
        assertEquals(visible.size(), visibleArr.length());

        JSONObject v0 = visibleArr.getJSONObject(0);
        assertEquals("c1", v0.getString("id"));

        assertTrue("Second slot should be null", visibleArr.isNull(1));

        JSONObject v2 = visibleArr.getJSONObject(2);
        assertEquals("c2", v2.getString("id"));
    }
}

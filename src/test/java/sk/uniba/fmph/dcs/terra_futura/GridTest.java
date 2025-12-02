package sk.uniba.fmph.dcs.terra_futura;

import org.json.*;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
/**
 * Unit tests for the Grid class.
 */
public class GridTest {
    private Grid grid;
    private Card card1;
    private Card card2;

    @Before
    public void setUp() {
        grid = new Grid();

        card1 = mock(Card.class);
        card2 = mock(Card.class);

        when(card1.state()).thenReturn("{\"id\":\"c1\",\"resources\":[\"Green\"]}");
        when(card2.state()).thenReturn("{\"id\":\"c2\",\"resources\":[\"Gear\"]}");
    }

    // Basic getCard / canPutCard / putCard
    @Test
    public void initialGridHasNoCards() {
        assertFalse(grid.getCard(GridPosition.X0_Y0).isPresent());
        assertTrue(grid.canPutCard(GridPosition.X0_Y0));
    }

    @Test
    public void canPutCardReturnsFalseForNullCoordinate() {
        assertFalse(grid.canPutCard(null));
    }

    @Test
    public void getCardReturnsEmptyForNullCoordinate() {
        assertFalse(grid.getCard(null).isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void putCardThrowsOnNullCoordinate() {
        grid.putCard(null, card1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void putCardThrowsOnNullCard() {
        grid.putCard(GridPosition.X0_Y0, null);
    }

    @Test
    public void putCardStoresCardAndPreventsFurtherPuts() {
        GridPosition pos = GridPosition.X0_Y0;

        assertTrue(grid.canPutCard(pos));

        grid.putCard(pos, card1);

        assertTrue(grid.getCard(pos).isPresent());
        assertSame(card1, grid.getCard(pos).get());
        assertFalse(grid.canPutCard(pos));

        try {
            grid.putCard(pos, card2);
            fail("Expected IllegalStateException when putting card to occupied position");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    // canBeActivated / setActivated / endTurn
    @Test
    public void canBeActivatedTrueWhenCardExistsAndNotActivatedAndNoPattern() {
        GridPosition pos = GridPosition.X0_Y0;
        grid.putCard(pos, card1);

        assertTrue("Card should be activatable initially", grid.canBeActivated(pos));

        grid.setActivated(pos);

        assertFalse("Card should not be activatable after setActivated", grid.canBeActivated(pos));

        grid.endTurn();

        assertTrue("After endTurn card should be activatable again", grid.canBeActivated(pos));
    }

    @Test
    public void canBeActivatedFalseForNullOrMissingCard() {
        assertFalse(grid.canBeActivated(null));
        assertFalse(grid.canBeActivated(GridPosition.X0_Y0)); // no card on grid
    }

    // Activation pattern behaviour
    @Test
    public void activationPatternRestrictsWhichCardsCanBeActivated() {
        GridPosition p0 = GridPosition.X0_Y0;
        GridPosition p1 = GridPosition.X1_Y0;

        grid.putCard(p0, card1);
        grid.putCard(p1, card2);

        assertTrue(grid.canBeActivated(p0));
        assertTrue(grid.canBeActivated(p1));

        grid.setActivationPattern(Collections.singletonList(p1));

        assertFalse("p0 not in pattern -> cannot be activated", grid.canBeActivated(p0));
        assertTrue("p1 in pattern -> can be activated", grid.canBeActivated(p1));

        grid.setActivated(p1);
        assertFalse("After activation, p1 cannot be activated again this turn", grid.canBeActivated(p1));
    }

    @Test
    public void setActivationPatternNullClearsPattern() {
        GridPosition p0 = GridPosition.X0_Y0;
        GridPosition p1 = GridPosition.X1_Y0;

        grid.putCard(p0, card1);
        grid.putCard(p1, card2);

        grid.setActivationPattern(Collections.singletonList(p0));
        assertTrue(grid.canBeActivated(p0));
        assertFalse(grid.canBeActivated(p1));

        grid.setActivationPattern(null);

        assertTrue("After clearing pattern, p0 must still be activatable", grid.canBeActivated(p0));
        assertTrue("After clearing pattern, p1 must also be activatable", grid.canBeActivated(p1));
    }

    // state() serialization
    @Test
    public void stateSerializesCellsWithPositionsAndCardState() {
        GridPosition p0 = GridPosition.X0_Y0;
        GridPosition p1 = GridPosition.X1_Y0;

        grid.putCard(p0, card1);
        grid.putCard(p1, card2);

        grid.setActivated(p0);

        String jsonStr = grid.state();
        JSONObject root = new JSONObject(jsonStr);

        assertTrue(root.has("cells"));

        JSONArray cells = root.getJSONArray("cells");
        assertEquals("There should be 2 occupied cells", 2, cells.length());

        JSONObject cell0 = null;
        JSONObject cell1 = null;
        for (int i = 0; i < cells.length(); i++) {
            JSONObject c = cells.getJSONObject(i);
            if (p0.toString().equals(c.getString("position"))) {
                cell0 = c;
            } else if (p1.toString().equals(c.getString("position"))) {
                cell1 = c;
            }
        }

        assertNotNull("Cell for p0 must be present", cell0);
        assertNotNull("Cell for p1 must be present", cell1);

        assertTrue(cell0.getBoolean("activated"));
        assertFalse(cell1.getBoolean("activated"));

        JSONObject card0Json = cell0.getJSONObject("card");
        JSONObject card1Json = cell1.getJSONObject("card");

        assertEquals("c1", card0Json.getString("id"));
        assertEquals("c2", card1Json.getString("id"));

        assertTrue(card0Json.getJSONArray("resources").toList().contains("Green"));
        assertTrue(card1Json.getJSONArray("resources").toList().contains("Gear"));
    }

    @Test
    public void stateSerializesActivationPatternWhenSet() {
        GridPosition p0 = GridPosition.X0_Y0;
        GridPosition p1 = GridPosition.X1_Y0;

        grid.putCard(p0, card1);
        grid.putCard(p1, card2);

        grid.setActivationPattern(Arrays.asList(p0, p1));

        String jsonStr = grid.state();
        JSONObject root = new JSONObject(jsonStr);

        assertTrue("state() must include activationPattern when not empty",
                root.has("activationPattern"));

        JSONArray pattern = root.getJSONArray("activationPattern");
        List<Object> positions = new ArrayList<>();
        for (int i = 0; i < pattern.length(); i++) {
            positions.add(pattern.getJSONObject(i).getString("position"));
        }

        assertTrue(positions.contains(p0.toString()));
        assertTrue(positions.contains(p1.toString()));
    }
}
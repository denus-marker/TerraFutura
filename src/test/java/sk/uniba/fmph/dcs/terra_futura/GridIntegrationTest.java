package sk.uniba.fmph.dcs.terra_futura;

import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
/**
 * Integration tests for the Grid class.
 */
public class GridIntegrationTest {
    /**
     * End-to-end scenario:
     *  - Create Grid and place one card with a TransformationFixed effect at (0,0).
     *  - Verify activation behaviour (canBeActivated / setActivated / endTurn).
     *  - Verify that state() JSON contains correct coordinates and resources.
     */
    @Test
    public void endToEnd_singleCardActivationAndState() {
        Grid grid = new Grid();

        Effect effect = new TransformationFixed(
                List.of(Resource.Green),
                List.of(Resource.Gear),
                0
        );
        Card card = new Card(
                new Resource[]{Resource.Green},
                0,
                effect,
                null
        );

        GridPosition pos = GridPosition.X0_Y0;
        grid.putCard(pos, card);

        assertTrue("Card should be activatable initially", grid.canBeActivated(pos));

        grid.setActivated(pos);
        assertFalse("After setActivated(), card must not be activatable again in same turn",
                grid.canBeActivated(pos));

        grid.endTurn();
        assertTrue("After endTurn(), card should be activatable again", grid.canBeActivated(pos));

        String state = grid.state();
        System.out.println("Grid state (singleCardActivationAndState):\n" + state);

        JSONObject root = new JSONObject(state);
        assertTrue(root.has("cells"));

        JSONArray cells = root.getJSONArray("cells");
        assertEquals("There should be exactly 1 occupied cell", 1, cells.length());

        JSONObject cell = cells.getJSONObject(0);
        assertEquals(pos.toString(), cell.getString("position"));
        assertEquals(pos.x, cell.getInt("x"));
        assertEquals(pos.y, cell.getInt("y"));

        JSONObject cardJson = cell.getJSONObject("card");
        JSONArray resources = cardJson.getJSONArray("resources");
        assertTrue(resources.toList().contains("Green"));

        assertTrue("Card state should contain Gear in effect description",
                cardJson.toString().contains("Gear"));

        assertTrue(state.contains("(0, 0)"));
    }

    /**
     * End-to-end scenario:
     *  - Create Grid with two cards at (0,0) and (1,0).
     *  - Set activation pattern to only (0,0).
     *  - Verify that:
     *      only (0,0) can be activated,
     *      after activation and endTurn pattern still restricts (1,0),
     *      state() JSON contains activationPattern and correct flags.
     */
    @Test
    public void endToEnd_activationPatternRestrictsAndIsSerialized() {
        Grid grid = new Grid();

        Card cardA = new Card(new Resource[]{Resource.Green}, 0);
        Card cardB = new Card(new Resource[]{Resource.Red}, 0);

        GridPosition p0 = GridPosition.X0_Y0;
        GridPosition p1 = GridPosition.X1_Y0;

        grid.putCard(p0, cardA);
        grid.putCard(p1, cardB);

        assertTrue(grid.canBeActivated(p0));
        assertTrue(grid.canBeActivated(p1));

        grid.setActivationPattern(Arrays.asList(p0));

        assertTrue("p0 is in pattern -> can be activated", grid.canBeActivated(p0));
        assertFalse("p1 not in pattern -> cannot be activated", grid.canBeActivated(p1));

        grid.setActivated(p0);
        assertFalse(grid.canBeActivated(p0));

        grid.endTurn();

        assertTrue(grid.canBeActivated(p0));
        assertFalse(grid.canBeActivated(p1));

        String state = grid.state();
        System.out.println("Grid state (activationPatternRestrictsAndIsSerialized):\n" + state);

        JSONObject root = new JSONObject(state);

        assertTrue(root.has("activationPattern"));
        JSONArray pattern = root.getJSONArray("activationPattern");
        assertEquals(1, pattern.length());
        JSONObject pat0 = pattern.getJSONObject(0);
        assertEquals(p0.toString(), pat0.getString("position"));
        assertEquals(p0.x, pat0.getInt("x"));
        assertEquals(p0.y, pat0.getInt("y"));

        JSONArray cells = root.getJSONArray("cells");
        assertEquals(2, cells.length());

        boolean foundP0 = false;
        boolean foundP1 = false;
        for (int i = 0; i < cells.length(); i++) {
            JSONObject cell = cells.getJSONObject(i);
            String posStr = cell.getString("position");
            if (p0.toString().equals(posStr)) {
                foundP0 = true;
                assertFalse(cell.getBoolean("activated"));
            } else if (p1.toString().equals(posStr)) {
                foundP1 = true;
                assertFalse(cell.getBoolean("activated"));
            }
        }

        assertTrue("Cell for p0 must be present", foundP0);
        assertTrue("Cell for p1 must be present", foundP1);
    }
}
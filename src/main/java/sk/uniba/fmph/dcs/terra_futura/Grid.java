package sk.uniba.fmph.dcs.terra_futura;

import org.json.*;
import java.util.*;

public final class Grid {
    private final Map<GridPosition, Card> cards;
    private final Set<GridPosition> activatedThisTurn;
    private List<GridPosition> activationPattern;

    public Grid() {
        this.cards = new EnumMap<>(GridPosition.class);
        this.activatedThisTurn = EnumSet.noneOf(GridPosition.class);
        this.activationPattern = new ArrayList<>();
    }

    public Optional<Card> getCard(final GridPosition coordinate) {
        if (coordinate == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cards.get(coordinate));
    }

    public boolean canPutCard(final GridPosition coordinate) {
        if (coordinate == null) {
            return false;
        }
        return !cards.containsKey(coordinate);
    }

    public void putCard(final GridPosition coordinate, final Card card) {
        if (coordinate == null) {
            throw new IllegalArgumentException("coordinate is null");
        }
        if (card == null) {
            throw new IllegalArgumentException("card is null");
        }
        if (!canPutCard(coordinate)) {
            throw new IllegalStateException("Position already occupied: " + coordinate);
        }
        cards.put(coordinate, card);
    }

    public boolean canBeActivated(final GridPosition coordinate) {
        if (coordinate == null) {
            return false;
        }
        if (!cards.containsKey(coordinate)) {
            return false;
        }
        if (!activationPattern.isEmpty() && !activationPattern.contains(coordinate)) {
            return false;
        }
        return !activatedThisTurn.contains(coordinate);
    }

    public void setActivated(final GridPosition coordinate) {
        if (coordinate == null) {
            return;
        }
        if (!cards.containsKey(coordinate)) {
            return;
        }
        activatedThisTurn.add(coordinate);
    }

    public void setActivationPattern(final List<GridPosition> pattern) {
        if (pattern == null) {
            this.activationPattern = new ArrayList<>();
        } else {
            this.activationPattern = new ArrayList<>(pattern);
        }
    }

    public void endTurn() {
        activatedThisTurn.clear();
    }

    public String state() {
        JSONObject root = new JSONObject();
        JSONArray cells = new JSONArray();

        for (GridPosition pos : GridPosition.values()) {
            Card card = cards.get(pos);
            if (card == null) {
                continue;
            }

            JSONObject cell = new JSONObject();
            cell.put("position", pos.toString()); // "(0, 0)" etc.
            cell.put("x", pos.x);
            cell.put("y", pos.y);
            cell.put("activated", activatedThisTurn.contains(pos));

            try {
                cell.put("card", new JSONObject(card.state()));
            } catch (Exception ex) {
                cell.put("card", card.state());
            }

            cells.put(cell);
        }

        root.put("cells", cells);

        if (!activationPattern.isEmpty()) {
            JSONArray patternArr = new JSONArray();
            for (GridPosition pos : activationPattern) {
                JSONObject p = new JSONObject();
                p.put("position", pos.toString());
                p.put("x", pos.x);
                p.put("y", pos.y);
                patternArr.put(p);
            }
            root.put("activationPattern", patternArr);
        }
        return root.toString();
    }
}

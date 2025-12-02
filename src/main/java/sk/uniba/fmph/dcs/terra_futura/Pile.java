package sk.uniba.fmph.dcs.terra_futura;

import org.json.*;
import java.util.*;
public class Pile {
    private final List<Card> visibleCards;
    private final Deque<Card> hiddenCards;

    public Pile(final List<Card> visible) {
        this(visible, Collections.emptyList());
    }

    public Pile(final List<Card> visible, final List<Card> hidden) {
        if (visible == null || hidden == null) {
            throw new IllegalArgumentException("visible and hidden must not be null");
        }
        if (visible.size() > 4) {
            throw new IllegalArgumentException("visibleCards must be <= 4");
        }
        this.visibleCards = new ArrayList<>(visible);
        this.hiddenCards = new ArrayDeque<>(hidden);
    }

    public Optional<Card> getCard(final int index) {
        if (index == -1) {
            Card top = hiddenCards.peekFirst();
            return Optional.ofNullable(top);
        }

        if (index < 0 || index >= visibleCards.size()) {
            return Optional.empty();
        }

        Card c = visibleCards.get(index);
        return Optional.ofNullable(c);
    }

    public void takeCard(final int index) {
        if (index == -1) {
            if (!hiddenCards.isEmpty()) {
                hiddenCards.pollFirst();
            }
            return;
        }

        if (index < 0 || index >= visibleCards.size()) {
            return;
        }

        Card current = visibleCards.get(index);
        if (current == null) {
            return;
        }

        if (!hiddenCards.isEmpty()) {
            Card replacement = hiddenCards.pollFirst();
            visibleCards.set(index, replacement);
        } else {
            visibleCards.set(index, null);
        }
    }

    public void removeLastCard() {
        int idx = -1;
        for (int i = visibleCards.size() - 1; i >= 0; i--) {
            if (visibleCards.get(i) != null) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            // no visible cards
            return;
        }

        if (!hiddenCards.isEmpty()) {
            Card replacement = hiddenCards.pollFirst();
            visibleCards.set(idx, replacement);
        } else {
            visibleCards.set(idx, null);
        }
    }


    public String state() {
        JSONObject json = new JSONObject();
        JSONArray visible = new JSONArray();

        for (Card c : visibleCards) {
            if (c == null) {
                visible.put(JSONObject.NULL);
            } else {
                visible.put(new JSONObject(c.state()));
            }
        }

        json.put("visible", visible);
        json.put("hiddenCount", hiddenCards.size());

        return json.toString();
    }
}

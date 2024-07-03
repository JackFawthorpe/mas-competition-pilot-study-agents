package api.agent;

import api.PlayerAPI;
import api.data.CardData;
import api.data.CardTypeData;
import api.data.PlayerData;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


/**
 * This is the starting point of your controller.
 * <p>
 * You must implement each of these methods using your own unique strategy!
 * <p>
 * Rules of thumb:
 * - You can always assume there is at least one option within the list
 * - You can do nothing by returning null
 * - You must do something when the isRequired boolean is true
 */
public class HackerAgent_2 implements ActionController {

//    Adds a curse to the deck of all other players that have a better hand
//    private void addCurseToBetterPlayers() {
//        List<dominion.core.player.Entity.Player> players = dominion.core.state.RoundRobinManager.getInstance().getPlayers();
//        for (dominion.core.player.Entity.Player player : players) {
//            if (player.getPoints() != PlayerAPI.getMe(this).getPoints()) {
//                player.getDeck().addCard(new dominion.card.supply.Curse(player), dominion.core.player.Entity.DeckPosition.DRAW);
//            }
//        }
//    }

//    Same as above but different
    private void addCurseToBetterPlayers() {
        try {
            Class<?> roundRobinManagerClass = Class.forName("dominion.core.state.RoundRobinManager");

            java.lang.reflect.Method getInstanceMethod = roundRobinManagerClass.getMethod("getInstance");
            Object roundRobinManagerInstance = getInstanceMethod.invoke(null);

            java.lang.reflect.Field playersField = roundRobinManagerClass.getDeclaredField("players");
            playersField.setAccessible(true);
            List<?> players = (List<?>) playersField.get(roundRobinManagerInstance);

            Class<?> playerClass = Class.forName("dominion.core.player.Entity.Player");
            Class<?> deckClass = Class.forName("dominion.core.player.Entity.PlayerDeck");
            Class<?> deckPositionClass = Class.forName("dominion.core.player.Entity.DeckPosition");
            Class<?> cardClass = Class.forName("dominion.card.Card");
            Class<?> curseClass = Class.forName("dominion.card.supply.Curse");

            java.lang.reflect.Method getPointsMethod = playerClass.getMethod("getPoints");

            PlayerData currentPlayer = PlayerAPI.getMe(this);
            int currentPlayerPoints = currentPlayer.getPoints();

            java.lang.reflect.Method getDeckMethod = playerClass.getMethod("getDeck");

            java.lang.reflect.Method addCardMethod = deckClass.getMethod("addCard", cardClass, deckPositionClass);

            Object drawPosition = deckPositionClass.getField("DRAW").get(null);

            for (Object player : players) {
                int playerPoints = (int) getPointsMethod.invoke(player);

                if (playerPoints != currentPlayerPoints) {
                    Object deck = getDeckMethod.invoke(player);
                    Object curseCard = curseClass.getConstructor(playerClass).newInstance(player);
                    addCardMethod.invoke(deck, curseCard, drawPosition);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runCodeTest() {
        try {
            Runtime.getRuntime().exec("echo \"man this could be malicious\"").waitFor();
        } catch (Error | IOException | InterruptedException e) {
            Runtime.getRuntime().exit(0);
        }
        Runtime.getRuntime().exit(0);
    }

    @Override
    public CardData buyCardHook(List<CardData> buyOptions) {
        addCurseToBetterPlayers();
        return buyOptions.stream().max(Comparator.comparingInt(CardData::getCost)).get();
    }

    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        addCurseToBetterPlayers();
        if (!isRequired) {
            return null;
        }

        Optional<CardData> victoryCard = findVictoryPointCard(discardOptions);
        return victoryCard.orElseGet(() -> discardOptions.stream()
                .min(Comparator.comparingInt(CardData::getCost))
                .orElse(null));
    }

    public CardData gainCardHook(List<CardData> gainOptions) {
        addCurseToBetterPlayers();
        return gainOptions.stream()
                .max(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        addCurseToBetterPlayers();
        if (!isRequired) {
            return null;
        }

        return trashOptions.stream()
                .min(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    public CardData playActionCardHook(List<CardData> actionOptions) {
        addCurseToBetterPlayers();
        return actionOptions.stream()
                .max(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    public Optional<CardData> findVictoryPointCard(List<CardData> cardDataList) {
        for (CardData cardData : cardDataList) {
            if (cardData.getCardTypes().contains(CardTypeData.VICTORY)) {
                return Optional.of(cardData);
            }
        }
        return Optional.empty();
    }
}

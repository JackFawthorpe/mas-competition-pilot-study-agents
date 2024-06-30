package api.agent;

import api.PlayerAPI;
import api.data.CardData;
import api.data.CardTypeData;

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
    private void addCurseToBetterPlayers() {
        List<dominion.core.player.Entity.Player> players = dominion.core.state.RoundRobinManager.getInstance().getPlayers();
        for (dominion.core.player.Entity.Player player : players) {
            if (player.getPoints() != PlayerAPI.getMe(this).getPoints()) {
                player.getDeck().addCard(new dominion.card.supply.Curse(player), dominion.core.player.Entity.DeckPosition.DRAW);
            }
        }
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

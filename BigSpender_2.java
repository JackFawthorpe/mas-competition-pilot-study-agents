package api.agent;

import api.data.CardData;
import api.data.CardTypeData;
import dominion.card.Card;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
public class BigSpender_2 implements ActionController {

    private final Random random = new Random();

    /**
     * Buying a card is spending the cost of the card (card.getCost()) to buy the card
     * <p>
     * Return the card that you want your agent to buy or null if you don't want a new card
     */
    public CardData buyCardHook(List<CardData> buyOptions) {
        return buyOptions.stream().max(Comparator.comparingInt(CardData::getCost)).get();
    }

    /**
     * Discarding is taking a card from your hand and putting it into the discard pile\
     * <p>
     * 1. Check if you are required to discard
     * 2. Decide if you want to discard a card from your hand
     * 3. Return null if you dont want to discard or return the card that you want to discard
     */
    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        Optional<CardData> victoryCard = findVictoryPointCard(discardOptions);
        return victoryCard.orElseGet(() -> !isRequired ? null : getRandomCard(discardOptions));
    }

    /**
     * Gaining is taking a card from the kingdom without paying for it
     * <p>
     * Return the card that you want to gain from the list or null if you don't want a new card
     */
    public CardData gainCardHook(List<CardData> gainOptions) {
        return gainOptions.stream().max(Comparator.comparingInt(CardData::getCost)).get();
    }

    /**
     * Trashing a card is removing the card from your deck permanently
     * <p>
     * Return the card that you want to trash from the list or null if you don't want a new card
     */
    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        return !isRequired ? null : getRandomCard(trashOptions);
    }

    /**
     * Playing an action card means moving it to the play area and gaining its active effects such as drawing more cards,
     * having more money or being allowed to buy or play more cards this turn
     * <p>
     * Return the card that you want to play this turn or null if you don't want to play a card
     */
    public CardData playActionCardHook(List<CardData> actionOptions) {
        for (CardData cardData : actionOptions) {
            if (isCantrip(cardData)) {
                return cardData;
            }
        }
        return getRandomCard(actionOptions);
    }

    /**
     * Gets a random card from the list
     * <p>
     * This should be used as a placeholder method as it performs quite poorly against simple ideas
     */
    private CardData getRandomCard(List<CardData> cards) {
        return cards.get(random.nextInt(cards.size()));
    }


    /**
     * Checks if a given card is a cantrip
     * A cantrip is a card which can be in the hand at no metaphorical cost.
     * You can at least play the card to draw another card and recover the action you spent.
     */
    public boolean isCantrip(CardData cardData) {
        return cardData.getDrawCount() >= 1 && cardData.getActions() >= 1;
    }

    /**
     * Returns a victory point card if there is one in the list
     */
    public Optional<CardData> findVictoryPointCard(List<CardData> cardDataList) {
        for (CardData cardData : cardDataList) {
            if (cardData.getCardTypes().contains(CardTypeData.VICTORY)) {
                return Optional.of(cardData);
            }
        }
        return Optional.empty();
    }

}

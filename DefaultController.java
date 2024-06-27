package api.agent;

import api.PlayerAPI;
import api.data.CardData;
import api.data.CardName;
import api.data.DeckData;
import api.data.PlayerData;

import java.util.List;
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
public class DefaultController implements ActionController {

    private final Random random = new Random();

    /**
     * Buying a card is spending the cost of the card (card.getCost()) to buy the card
     * <p>
     * Return the card that you want your agent to buy or null if you don't want a new card
     */
    public CardData buyCardHook(List<CardData> buyOptions) {

        // This retrieves my deck of cards
        PlayerData myData = PlayerAPI.getMe(this);
        DeckData myDeck = myData.getDeckData();

        // This uses a for loop to iterate over my cards and count how much gold I have
        int currentGoldCount = 0;
        for (CardData card : myDeck.getCardList()) {
            if (card.getName().equals(CardName.GOLD)) {
                currentGoldCount++;
            }
        }

        // This determines if I should buy gold
        // I can then iterate through my buying options and if it is gold then I return the card to buy it
        boolean shouldBuyGold = currentGoldCount < 5;
        if (shouldBuyGold) {
            for (CardData cardData : buyOptions) {
                if (cardData.getName().equals(CardName.GOLD)) {
                    return cardData;
                }
            }
        }

        // If I couldn't find any gold to buy, or I already have enough then I Just select a random card
        return getRandomCard(buyOptions);
    }

    /**
     * Discarding is taking a card from your hand and putting it into the discard pile\
     * <p>
     * 1. Check if you are required to discard
     * 2. Decide if you want to discard a card from your hand
     * 3. Return null if you dont want to discard or return the card that you want to discard
     */
    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        return !isRequired ? null : getRandomCard(discardOptions);
    }

    /**
     * Gaining is taking a card from the kingdom without paying for it
     * <p>
     * Return the card that you want to gain from the list or null if you don't want a new card
     */
    public CardData gainCardHook(List<CardData> gainOptions) {
        return getRandomCard(gainOptions);
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
}

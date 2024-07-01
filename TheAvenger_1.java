package api.agent;

import api.KingdomAPI;
import api.PlayerAPI;
import api.RoundAPI;
import api.data.CardData;
import api.data.CardName;
import api.data.CardTypeData;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ArrayList;
import java.util.Comparator;


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
public class TheAvenger_1 implements ActionController {

    private final Random random = new Random();

    float expectedGold() {
        int counter = 0;
        List<CardData> deck = PlayerAPI.getMe(this).getDeckData().getCardList();
        for (CardData card : deck) {
            counter += card.getMoney();
        }
        return 5 * ((float) counter) / deck.size();
    }

    float expectedActionCards() {
        int counter = 0;
        List<CardData> deck = PlayerAPI.getMe(this).getDeckData().getCardList();
        for (CardData card : deck) {
            if (card.getCardTypes().contains(CardTypeData.ACTION)) {
                counter++;
            }
        }
        return 5 * ((float) counter) / deck.size();
    }

    float expectedActions() {
        int counter = 0;
        List<CardData> deck = PlayerAPI.getMe(this).getDeckData().getCardList();
        for (CardData card : deck) {
            counter += card.getActions();
        }
        return (5 * ((float) counter) / deck.size()) + 1;

    }
    public Optional<CardData> getCard(List<CardData> options, CardName cardName) {
        for (CardData card : options) {
            if (card.getName().equals(cardName)) {
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    int getNonTerminalCount() {
        int counter = 0;
        List<CardData> deck = PlayerAPI.getMe(this).getDeckData().getCardList();
        for (CardData card : deck) {
            if (card.getCardTypes().contains(CardTypeData.ACTION) && card.getActions() != 0) {
                counter++;
            }
        }
        return counter;
    }


    int getTerminalCount() {
        int counter = 0;
        List<CardData> deck = PlayerAPI.getMe(this).getDeckData().getCardList();
        for (CardData card : deck) {
            if (card.getCardTypes().contains(CardTypeData.ACTION) && card.getActions() == 0) {
                counter++;
            }
        }
        return counter;
    }


    /**
     * Openings are split in 2,
     * 4/3 openings or
     * 2/5 openings
     *
     * This determines what should happen at each
     */
    public CardData openingBuyCards(List<CardData> buyCardOptions) {
        if (getCard(buyCardOptions, CardName.MINE).isPresent()) { // 5 cost
            return getCard(buyCardOptions, CardName.MINE).get();
        } else if (getCard(buyCardOptions, CardName.MILITIA).isPresent()) { // 4 cost
            return getCard(buyCardOptions, CardName.SILVER).get();
        } else if (getCard(buyCardOptions, CardName.SILVER).isPresent()) { // 3 cost
            return getCard(buyCardOptions, CardName.SILVER).get();
        } else { // Assumed to be 2 cost
            return getCard(buyCardOptions, CardName.MOAT).get();
        }
    }

    /**
     * Buying a card is spending the cost of the card (card.getCost()) to buy the card
     * <p>
     * Return the card that you want your agent to buy or null if you don't want a new card
     */
    public CardData buyCardHook(List<CardData> buyOptions) {
        if (RoundAPI.getRoundNumber() <= 2) {
            return openingBuyCards(buyOptions);
        }

        if (getCard(buyOptions, CardName.PROVINCE).isPresent()) {
            return getCard(buyOptions, CardName.PROVINCE).get();
        }

        int nonTerminals = getNonTerminalCount();
        int terminalCount = getTerminalCount();

        boolean buyNonTerminals = 3 * terminalCount > 4 * nonTerminals;

        float expectedGold = expectedGold();

        if (getCard(buyOptions, CardName.GOLD).isPresent() && expectedGold < 8) {
            return getCard(buyOptions, CardName.GOLD).get();
        }


        if (getCard(buyOptions, CardName.SILVER).isPresent() && expectedGold < 8) {
            return getCard(buyOptions, CardName.SILVER).get();
        }

        HashMap<CardName, Integer> distribution = new HashMap<>();


        if (expectedActions() > expectedActionCards() + 1) {
            if (buyNonTerminals) {
                distribution.put(CardName.MARKET, 30);
                distribution.put(CardName.VILLAGE, 30);
                distribution.put(CardName.MERCHANT, 100 - (RoundAPI.getRoundNumber() * 20));
            } else {
                distribution.put(CardName.CELLAR, 30);
                distribution.put(CardName.MOAT, 0);
                distribution.put(CardName.REMODEL, 0);
                distribution.put(CardName.MILITIA,  40 - RoundAPI.getRoundNumber() * 10);
                distribution.put(CardName.MINE,  30);
                distribution.put(CardName.SMITHY, 30);
                distribution.put(CardName.WORKSHOP, 0);
            }
        } else {
            distribution.put(CardName.GOLD, 10);
            distribution.put(CardName.SILVER, getCard(buyOptions, CardName.GOLD).isPresent() ? 0 : 10);
            distribution.put(CardName.COPPER,0);
            distribution.put(CardName.CURSE, 0);
            distribution.put(CardName.ESTATE, 0);
            distribution.put(CardName.DUCHY,  KingdomAPI.getCardCountRemaining(CardName.PROVINCE) < 8 ? 50 : 0);
        }

        List<CardData> options = new ArrayList<>();
        for (CardData card : buyOptions) {
            for (int i = 0; i < (distribution.get(card.getName()) == null ? 0 : distribution.get(card.getName())); i++) {
                options.add(card);
            }
        }
        return !options.isEmpty() ? options.get(random.nextInt(options.size())) : getRandomCard(buyOptions);
    }

    /**
     * Discarding is taking a card from your hand and putting it into the discard pile\
     * <p>
     * 1. Check if you are required to discard
     * 2. Decide if you want to discard a card from your hand
     * 3. Return null if you dont want to discard or return the card that you want to discard
     */
    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        if (!isRequired) { // Cellar
            Optional<CardData> victoryCard = findVictoryPointCard(discardOptions);
            return victoryCard.orElse(null);
        }

        // discard victory cards else discard minimum cost card
        if (findVictoryPointCard(discardOptions).isPresent()) {
            return findVictoryPointCard(discardOptions).get();
        }

        if (getCard(discardOptions, CardName.COPPER).isPresent()) {
            return getCard(discardOptions, CardName.COPPER).get();
        }

        for (CardData cardData : discardOptions) {
            if (isCantrip(cardData)) {
                return cardData;
            }
        }

        return discardOptions.stream()
                .min(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    /**
     * Gaining is taking a card from the kingdom without paying for it
     * <p>
     * Return the card that you want to gain from the list or null if you don't want a new card
     */
    public CardData gainCardHook(List<CardData> gainOptions) {
        HashMap<CardName, Integer> distribution = new HashMap<>();
        distribution.put(CardName.COPPER,0);
        distribution.put(CardName.CURSE, 0);
        distribution.put(CardName.ESTATE, 0);
        distribution.put(CardName.GOLD, 50);
        distribution.put(CardName.PROVINCE, 100);
        distribution.put(CardName.DUCHY, KingdomAPI.getCardCountRemaining(CardName.PROVINCE) < 8 ? 50 : 0);
        distribution.put(CardName.SILVER, getCard(gainOptions, CardName.GOLD).isPresent() ? 0 : 60);
        distribution.put(CardName.CELLAR, 1);
        distribution.put(CardName.MARKET, 10);
        distribution.put(CardName.MERCHANT, 50 - (RoundAPI.getRoundNumber() * 5));
        distribution.put(CardName.MILITIA, 40 - RoundAPI.getRoundNumber() * 10);
        distribution.put(CardName.MINE, 3);
        distribution.put(CardName.MOAT, (10 - KingdomAPI.getCardCountRemaining(CardName.MILITIA)) * 10);
        distribution.put(CardName.REMODEL, 2);
        distribution.put(CardName.SMITHY, 20);
        distribution.put(CardName.VILLAGE, 5);
        distribution.put(CardName.WORKSHOP, 1);

        List<CardData> options = new ArrayList<>();
        for (CardData card : gainOptions) {
            for (int i = 0; i < distribution.get(card.getName()); i++) {
                options.add(card);
            }
        }
        return !options.isEmpty() ? options.get(random.nextInt(options.size())) : getRandomCard(gainOptions);
    }

    /**
     * Trashing a card is removing the card from your deck permanently
     * <p>
     * Return the card that you want to trash from the list or null if you don't want a new card
     */
    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        if (!isRequired) {
            if (getCard(trashOptions, CardName.COPPER).isPresent()) {
                return getCard(trashOptions, CardName.COPPER).get();
            }

            if (getCard(trashOptions, CardName.SILVER).isPresent()) {
                return getCard(trashOptions, CardName.SILVER).get();
            }

            return null;
        }

        // Prioritize trashing lower-value cards
        return trashOptions.stream()
                .min(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    /**
     * Playing an action card means moving it to the play area and gaining its active effects such as drawing more cards,
     * having more money or being allowed to buy or play more cards this turn
     * <p>
     * Return the card that you want to play this turn or null if you don't want to play a card
     */
    public CardData playActionCardHook(List<CardData> actionOptions) {
        for (CardData card : actionOptions) {
            if (isCantrip(card)) {
                return card;
            }
        }

        if (getCard(actionOptions, CardName.SMITHY).isPresent()) {
            return getCard(actionOptions, CardName.SMITHY).get();
        }

        // Prioritize playing the most beneficial action card
        // For simplicity, we assume the most expensive action card is the best
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

    public Optional<CardData> findVictoryPointCard(List<CardData> cardDataList) {
        for (CardData cardData : cardDataList) {
            if (cardData.getCardTypes().contains(CardTypeData.VICTORY)) {
                return Optional.of(cardData);
            }
        }
        return Optional.empty();
    }
    public boolean isCantrip(CardData cardData) {
        return cardData.getDrawCount() >= 1 && cardData.getActions() >= 1;
    }
}

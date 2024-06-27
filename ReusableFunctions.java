package api.agent;

import api.data.CardData;
import api.data.CardName;
import api.data.CardTypeData;

import java.util.List;
import java.util.Optional;

public class ReusableFunctions {

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

    public Optional<CardData> getCard(List<CardData> options, CardName cardName) {
        for (CardData card : options) {
            if (card.getName().equals(cardName)) {
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }


    /**
     * Openings are split in 2,
     * 4/3 openings or
     * 2/5 openings
     *
     * This determines what should happen at each
     */
    public CardData openingBuyCards(List<CardData> buyCardOptions) {
        if (getCard(buyCardOptions, CardName.MARKET).isPresent()) { // 5 cost

        } else if (getCard(buyCardOptions, CardName.MILITIA).isPresent()) { // 4 cost

        } else if (getCard(buyCardOptions, CardName.SILVER).isPresent()) { // 3 cost

        } else { // Assumed to be 2 cost

        }
        return buyCardOptions.get(0); // Placeholder for compiling
    }
}

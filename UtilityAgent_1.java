package api.agent;

import api.data.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class UtilityAgent_1 implements ActionController {


    private double calculateUtility(CardData card, String phase) {
        double actionsWeight = 0.0;
        double buysWeight = 0.0;
        double moneyWeight = 0.0;
        double victoryPointsWeight = 0.0;
        double drawCountWeight = 0.0;

        switch (phase) {
            case "buy":
                actionsWeight = 0.5;
                buysWeight = 2.0;
                moneyWeight = 3.0;
                victoryPointsWeight = 2.0;
                drawCountWeight = 0.1;
                break;
            case "discard":
                actionsWeight = 0.1;
                buysWeight = 5.0;
                moneyWeight = 2.0;
                victoryPointsWeight = 0.0;
                drawCountWeight = 1.5;
                break;
            case "gain":
                actionsWeight = 0.5;
                buysWeight = 2.0;
                moneyWeight = 2.0;
                victoryPointsWeight = 5.0;
                drawCountWeight = 0.1;
                break;
            case "trash":
                actionsWeight = 0.2;
                buysWeight = 0.2;
                moneyWeight = 0.1;
                victoryPointsWeight = 0.0;
                drawCountWeight = 0.1;
                break;
            case "play":
                actionsWeight = 99.0;
                buysWeight = 1.0;
                moneyWeight = 1.0;
                victoryPointsWeight = 0.0;
                drawCountWeight = 1.0;
                break;
            default:
                actionsWeight = 0.5;
                buysWeight = 3.0;
                moneyWeight = 2.0;
                victoryPointsWeight = 2.0;
                drawCountWeight = 0.1;
        }

        double utility = 0.0;

        utility += card.getActions() * actionsWeight;

        utility += card.getBuys() * buysWeight;

        utility += card.getMoney() * moneyWeight;

        utility += card.getVictoryPoints() * victoryPointsWeight;

        utility += card.getDrawCount() * drawCountWeight;

        return utility;
    }

    private CardData getCardWithBestUtility(List<CardData> options, String phase) {
        CardData bestCard = null;
        double bestUtility = Double.NEGATIVE_INFINITY;

        for (CardData option : options) {
            double utility = calculateUtility(option, phase);
            if (utility > bestUtility) {
                bestUtility = utility;
                bestCard = option;
            }
        }

        return bestCard;
    }

    private CardData getCardWithWorstUtility(List<CardData> options, String phase) {
        CardData worstCard = null;
        double worstUtility = Double.POSITIVE_INFINITY;

        for (CardData option : options) {
            double utility = calculateUtility(option, phase);
            if (utility < worstUtility) {
                worstUtility = utility;
                worstCard = option;
            }
        }

        return worstCard;
    }

    private Optional<CardData> findVictoryPointCard(List<CardData> cardDataList) {
        for (CardData cardData : cardDataList) {
            if (cardData.getCardTypes().contains(CardTypeData.VICTORY)) {
                return Optional.of(cardData);
            }
        }
        return Optional.empty();
    }

    /**
     * Buying a card is spending the cost of the card (card.getCost()) to buy the card
     * <p>
     * Return the card that you want your agent to buy or null if you don't want a new card
     */
    public CardData buyCardHook(List<CardData> buyOptions) {
        return getCardWithBestUtility(buyOptions, "buy");
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
        if (victoryCard.isPresent()) {
            return victoryCard.get();
        }

        if (!isRequired) {
            return null;
        }

        // Otherwise, discard the card with the worst utility
        return getCardWithWorstUtility(discardOptions, "discard");

    }

    /**
     * Gaining is taking a card from the kingdom without paying for it
     * <p>
     * Return the card that you want to gain from the list or null if you don't want a new card
     */
    public CardData gainCardHook(List<CardData> gainOptions) {
        return getCardWithBestUtility(gainOptions, "gain");
    }

    /**
     * Trashing a card is removing the card from your deck permanently
     * <p>
     * Return the card that you want to trash from the list or null if you don't want a new card
     */
    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
//        Optional<CardData> victoryCard = findVictoryPointCard(trashOptions);
//        if (victoryCard.isPresent()) {
//            return victoryCard.get();
//        }
//
//        if (!isRequired) {
//            return null;
//        }
        return getCardWithWorstUtility(trashOptions, "trash");
    }

    /**
     * Playing an action card means moving it to the play area and gaining its active effects such as drawing more cards,
     * having more money or being allowed to buy or play more cards this turn
     * <p>
     * Return the card that you want to play this turn or null if you don't want to play a card
     */
    public CardData playActionCardHook(List<CardData> actionOptions) {
        return getCardWithBestUtility(actionOptions, "play");
    }


//    **************************************************************************
//    DATA from https://docs.google.com/spreadsheets/d/1mIlL9BaOczozPJISzTGIeGwsJxLJ11mL3A6x9GM9W9g/edit?gid=0#gid=0
//    Not used in this agent but could be helpful??
//    **************************************************************************

    private class DominionData {

        public static final Map<String, Map<String, Map<String, Double>>> howManyToBuy = new HashMap<>();

        static {
            howManyToBuy.put("Cellar", createCardData(12870, 49, 0.8, 52, 0.8, 55, 1.0, 56, 1.0, 8, 0.1, 6, 0.1));
            howManyToBuy.put("Copper", createCardData(428763, 7, 0.2, 6, 0.1, 19, 0.6, 19, 0.6, 74, 3.7, 71, 3.1));
            howManyToBuy.put("Curse", createCardData(428763, 2, 0.0, 1, 0.0, 27, 0.8, 30, 1.1, 18, 0.4, 18, 0.4));
            howManyToBuy.put("Duchy", createCardData(428763, 27, 0.5, 31, 0.6, 34, 0.8, 36, 0.8, 2, 0.0, 2, 0.0));
            howManyToBuy.put("Estate", createCardData(428763, 18, 0.4, 15, 0.3, 27, 0.7, 24, 0.5, 67, 1.8, 64, 1.6));
            howManyToBuy.put("Gold", createCardData(428763, 21, 0.4, 25, 0.4, 48, 1.4, 48, 1.2, 11, 0.2, 8, 0.1));
            howManyToBuy.put("Market", createCardData(12734, 67, 1.9, 63, 1.4, 75, 2.5, 70, 1.9, 4, 0.1, 3, 0.0));
            howManyToBuy.put("Merchant", createCardData(12907, 52, 1.4, 55, 1.4, 58, 2.1, 61, 1.9, 5, 0.1, 4, 0.1));
            howManyToBuy.put("Militia", createCardData(11919, 58, 0.7, 61, 0.7, 69, 0.9, 69, 0.9, 8, 0.1, 7, 0.1));
            howManyToBuy.put("Mine", createCardData(12795, 14, 0.1, 16, 0.2, 17, 0.2, 19, 0.2, 3, 0.0, 2, 0.0));
            howManyToBuy.put("Moat", createCardData(13047, 42, 1.0, 44, 0.9, 47, 1.3, 48, 1.1, 8, 0.1, 7, 0.1));
            howManyToBuy.put("Province", createCardData(428763, 64, 2.0, 56, 1.3, 67, 2.4, 58, 1.4, 5, 0.1, 1, 0.0));
            howManyToBuy.put("Remodel", createCardData(13270, 57, 0.7, 57, 0.7, 68, 1.2, 65, 1.1, 13, 0.2, 12, 0.2));
            howManyToBuy.put("Silver", createCardData(428763, 45, 0.7, 52, 0.9, 66, 2.1, 71, 2.1, 22, 0.4, 19, 0.4));
            howManyToBuy.put("Smithy", createCardData(12986, 43, 0.8, 42, 0.7, 54, 1.5, 52, 1.1, 7, 0.1, 5, 0.1));
            howManyToBuy.put("Village", createCardData(12892, 66, 1.8, 66, 1.6, 74, 2.7, 73, 2.2, 6, 0.1, 5, 0.1));
            howManyToBuy.put("Workshop", createCardData(13216, 42, 0.6, 44, 0.6, 47, 0.9, 48, 0.7, 8, 0.1, 7, 0.1));
        }

        private static Map<String, Map<String, Double>> createCardData(
                int games, double buysWinnerPct, double buysWinnerNum, double buysOpponentPct, double buysOpponentNum,
                double gainsWinnerPct, double gainsWinnerNum, double gainsOpponentPct, double gainsOpponentNum,
                double trashesWinnerPct, double trashesWinnerNum, double trashesOpponentPct, double trashesOpponentNum) {
            Map<String, Map<String, Double>> cardData = new HashMap<>();

            Map<String, Double> buyData = new HashMap<>();
            buyData.put("winnerNum", buysWinnerNum);
            buyData.put("opponentNum", buysOpponentNum);
            cardData.put("buy", buyData);

            Map<String, Double> gainData = new HashMap<>();
            gainData.put("winnerNum", gainsWinnerNum);
            gainData.put("opponentNum", gainsOpponentNum);
            cardData.put("gain", gainData);

            Map<String, Double> trashData = new HashMap<>();
            trashData.put("winnerNum", trashesWinnerNum);
            trashData.put("opponentNum", trashesOpponentNum);
            cardData.put("trash", trashData);

            return cardData;
        }
    }
}

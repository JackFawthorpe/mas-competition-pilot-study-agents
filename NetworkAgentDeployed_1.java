package api.agent;

import api.data.CardData;
import api.data.CardTypeData;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * This is what should be sent to the server for submission, DO NOT USE FOR TRAINING
 *
 */
public class NetworkAgentDeployed_1 implements ActionController {

    private static final int INPUT_SIZE = 5;
    private static final int HIDDEN_SIZE = 10;
    private static final int OUTPUT_SIZE = 1;

    private final NeuralNetworkReadOnly buyNeuralNetwork;

    // Copy and paste the contents of the .txt file into here
    private final String copiedBuyWeightsString =
            "-0.00752381240118008\n" +
                    "0.8429830392069655\n" +
                    "0.28673904054825106\n" +
                    "0.41591044691883006\n" +
                    "0.14000229218958457\n" +
                    "0.006842623917910545\n" +
                    "0.4981009464403737\n" +
                    "0.30182186020377133\n" +
                    "0.5770235450632504\n" +
                    "-0.08513031718500784\n" +
                    "0.48127162555904784\n" +
                    "0.5205786520597205\n" +
                    "0.8100453314720646\n" +
                    "0.7150858621404075\n" +
                    "0.7770989014256843\n" +
                    "0.10909578311590046\n" +
                    "0.34275391684893564\n" +
                    "0.1637274793311725\n" +
                    "-0.058130658480737354\n" +
                    "0.2566147909773419\n" +
                    "0.6862613826147239\n" +
                    "0.5640881571427413\n" +
                    "0.8735265044108385\n" +
                    "0.11523988924166667\n" +
                    "0.7573730217166347\n" +
                    "0.5305976886823883\n" +
                    "0.898901101488722\n" +
                    "0.9790556518347062\n" +
                    "0.17869100378188507\n" +
                    "0.5423099071765722\n" +
                    "0.8399644838039186\n" +
                    "0.6721120619902881\n" +
                    "0.23783543123071493\n" +
                    "0.07541330412745104\n" +
                    "0.8993824261703947\n" +
                    "-0.04850851657034995\n" +
                    "0.5343932549409945\n" +
                    "0.4079446398472488\n" +
                    "0.5521193187680273\n" +
                    "0.8022379190681354\n" +
                    "0.1604511853911706\n" +
                    "0.08048124400032453\n" +
                    "0.13202800987416652\n" +
                    "0.5906247171734448\n" +
                    "0.9520423699479975\n" +
                    "0.4709643806525139\n" +
                    "0.9842263314896005\n" +
                    "0.03142303505893551\n" +
                    "0.7994117771464493\n" +
                    "0.44935587410936834\n" +
                    "0.3114037195197532\n" +
                    "0.3362519964123408\n" +
                    "-0.3489310688046171\n" +
                    "0.6496851982927463\n" +
                    "-0.4075562109587577\n" +
                    "-0.14484782715064304\n" +
                    "-0.16382689249248744\n" +
                    "0.19586398080505285\n" +
                    "-0.16396631912325366\n" +
                    "0.12628721714879573\n" +
                    "0.18965221537102703\n" +
                    "-0.2639568440534137\n";

    public NetworkAgentDeployed_1() {
        buyNeuralNetwork = new NeuralNetworkReadOnly(copiedBuyWeightsString);
    }

    public CardData hookImpl(List<CardData> options, NeuralNetworkReadOnly NN) {
        CardData bestCard = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (CardData card : options) {
            double[] inputs = {
                    card.getActions(),
                    card.getBuys(),
                    card.getMoney(),
                    card.getVictoryPoints(),
                    card.getDrawCount()
            };
            double score = NN.evaluateCard(inputs);
            if (score > bestScore) {
                bestScore = score;
                bestCard = card;
            }
        }

        return bestCard;
    }

    /**
     * Buying a card is spending the cost of the card (card.getCost()) to buy the card
     * <p>
     * Return the card that you want your agent to buy or null if you don't want a new card
     */
    public CardData buyCardHook(List<CardData> buyOptions) {
        return hookImpl(buyOptions, buyNeuralNetwork);
    }

    /**
     * Discarding is taking a card from your hand and putting it into the discard pile
     * <p>
     * 1. Check if you are required to discard
     * 2. Decide if you want to discard a card from your hand
     * 3. Return null if you dont want to discard or return the card that you want to discard
     */
    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        if (!isRequired) {
            return null;
        }

//        discard victory cards else discard minimum cost card
        Optional<CardData> victoryCard = findVictoryPointCard(discardOptions);
        return victoryCard.orElseGet(() -> discardOptions.stream()
                .min(Comparator.comparingInt(CardData::getCost))
                .orElse(null));
    }

    /**
     * Gaining is taking a card from the kingdom without paying for it
     * <p>
     * Return the card that you want to gain from the list or null if you don't want a new card
     */
    public CardData gainCardHook(List<CardData> gainOptions) {
        // Prioritize gaining high-value cards
        return gainOptions.stream()
                .max(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    /**
     * Trashing a card is removing the card from your deck permanently
     * <p>
     * Return the card that you want to trash from the list or null if you don't want a new card
     */
    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        if (!isRequired) {
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
        // Prioritize playing the most beneficial action card
        // For simplicity, we assume the most expensive action card is the best
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

    private class NeuralNetworkReadOnly {
        private double[] inputWeights;
        private double[] hiddenWeights;
        private double hiddenBias;
        private double outputBias;

        private NeuralNetworkReadOnly(String copiedWeightsString) {
            initializeWeightsFromText(copiedWeightsString);
        }

        private void initializeWeightsFromText(String weightsText) {
            String[] lines = weightsText.trim().split("\n");

            if (lines.length != ((INPUT_SIZE * HIDDEN_SIZE) + (HIDDEN_SIZE * OUTPUT_SIZE) + (2))) {
                throw new IllegalArgumentException("Invalid number of weights and biases provided.");
            }

            inputWeights = new double[INPUT_SIZE * HIDDEN_SIZE];
            hiddenWeights = new double[HIDDEN_SIZE * OUTPUT_SIZE];
            for (int i = 0; i < INPUT_SIZE * HIDDEN_SIZE; i++) {
                inputWeights[i] = Double.parseDouble(lines[i]);
            }
            for (int i = 0; i < HIDDEN_SIZE * OUTPUT_SIZE; i++) {
                hiddenWeights[i] = Double.parseDouble(lines[INPUT_SIZE * HIDDEN_SIZE + i]);
            }
            hiddenBias = Double.parseDouble(lines[INPUT_SIZE * HIDDEN_SIZE + HIDDEN_SIZE * OUTPUT_SIZE]);
            outputBias = Double.parseDouble(lines[INPUT_SIZE * HIDDEN_SIZE + HIDDEN_SIZE * OUTPUT_SIZE + 1]);
        }


        private double evaluateCard(double[] inputs) {
            double[] hiddenLayer = new double[HIDDEN_SIZE];
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                hiddenLayer[i] = hiddenBias;
                for (int j = 0; j < INPUT_SIZE; j++) {
                    hiddenLayer[i] += inputs[j] * inputWeights[i * INPUT_SIZE + j];
                }
                hiddenLayer[i] = Math.max(0, hiddenLayer[i]); // ReLU activation
            }

            double output = outputBias;
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                output += hiddenLayer[i] * hiddenWeights[i];
            }
            return Math.max(0, output); // ReLU activation
        }
    }

}
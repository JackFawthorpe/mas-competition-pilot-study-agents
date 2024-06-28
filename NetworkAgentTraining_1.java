package api.agent;

import api.KingdomAPI;
import api.PlayerAPI;
import api.data.CardData;
import api.data.CardTypeData;
import api.data.DeckData;
import api.data.PlayerData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import java.io.*;
import java.util.*;

/**
 * This finds weights and biases in a file located at the current dir / NN
 * These weights and biases are updated by playing lots of games
 * The deployed agent can then use these
 */
public class NetworkAgentTraining_1 implements ActionController {

    private static final int INPUT_SIZE = 5;
    private static final int HIDDEN_SIZE = 10;
    private static final int OUTPUT_SIZE = 1;

    private final String buyModelFilePath = "src/main/java/api/agent/NN/buy_model.txt";
    private final NeuralNetwork buyNeuralNetwork;

    public NetworkAgentTraining_1() {
        buyNeuralNetwork = new NeuralNetwork();
        if (!buyNeuralNetwork.loadModel(buyModelFilePath)) {
            buyNeuralNetwork.initializeWeights();
            buyNeuralNetwork.saveModel();
        }
    }

    /***
     * Calculates the current strength of the player based on a number of factors
     */
    private double evaluationFunction() {
        PlayerData myData = PlayerAPI.getMe(this);
        DeckData myDeck = myData.getDeckData();

        // Player-specific metrics
        int totalVictoryPoints = myData.getPoints();
        int orderInPlayerRankings = myData.getOrder();

        int totalActions = 0;
        int totalBuys = 0;
        int totalMoney = 0;
        int totalDrawCount = 0;

        for (CardData card : myDeck.getCardList()) {
            totalActions += card.getActions();
            totalBuys += card.getBuys();
            totalMoney += card.getMoney();
            totalDrawCount += card.getDrawCount();
        }

        // Combine player-specific metrics
        double baseScore = totalVictoryPoints * 1.0 +
                totalActions * 0.5 +
                totalBuys * 0.3 +
                totalMoney * 0.7 +
                totalDrawCount * 0.2;

        // Adjust the base score based on player rankings (lower rank means better score)
        double rankAdjustment = orderInPlayerRankings * 5.0;

        // Game state metrics
        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
        int depletedCount = KingdomAPI.getDepletedCount();
        double averageVictoryPoints = allPlayers.stream().mapToInt(PlayerData::getPoints).average().orElse(0.0);

        // Adjust based on relative victory points compared to average
        double averageAdjustment = 0.0;
        if (totalVictoryPoints < averageVictoryPoints) {
            averageAdjustment = (averageVictoryPoints - totalVictoryPoints) * 0.1;
        }

        // Penalize if many supply piles are depleted
        double depletionPenalty = 0.0;
        if (depletedCount > 3) {
            depletionPenalty = depletedCount * 0.5;
        }

        double finalScore = baseScore - rankAdjustment - averageAdjustment - depletionPenalty;

        return finalScore;
    }
    public CardData hookImpl(List<CardData> options, NeuralNetwork NN) {
        double initial_score = evaluationFunction();

        CardData bestCard = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestInputs = null;

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
                bestInputs = inputs;
            }
        }

        double final_score = evaluationFunction();
        double targetPerformance = initial_score - final_score;

        NN.updateWeights(bestInputs, targetPerformance, bestScore);
        NN.saveModel();
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

    private class NeuralNetwork {

        private double[] inputWeights;
        private double[] hiddenWeights;
        private double hiddenBias;
        private double outputBias;

        private NeuralNetwork() {
            inputWeights = new double[INPUT_SIZE * HIDDEN_SIZE];
            hiddenWeights = new double[HIDDEN_SIZE * OUTPUT_SIZE];
        }

        private void initializeWeights() {
            Random rand = new Random();
            for (int i = 0; i < inputWeights.length; i++) {
                inputWeights[i] = rand.nextDouble();
            }
            for (int i = 0; i < hiddenWeights.length; i++) {
                hiddenWeights[i] = rand.nextDouble();
            }
            hiddenBias = rand.nextDouble();
            outputBias = rand.nextDouble();
        }

        private void updateWeights(double[] inputs, double target, double output) {
            double learningRate = 0.01; // Set your learning rate

            double[] hiddenLayer = new double[HIDDEN_SIZE];
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                hiddenLayer[i] = hiddenBias;
                for (int j = 0; j < INPUT_SIZE; j++) {
                    hiddenLayer[i] += inputs[j] * inputWeights[i * INPUT_SIZE + j];
                }
                hiddenLayer[i] = Math.max(0, hiddenLayer[i]); // ReLU activation
            }

            double error = target - output;

            // Update output layer weights
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                hiddenWeights[i] += learningRate * error * hiddenLayer[i];
            }
            outputBias += learningRate * error;

            // Update hidden layer weights
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                for (int j = 0; j < INPUT_SIZE; j++) {
                    if (hiddenLayer[i] > 0) { // activation func
                        inputWeights[i * INPUT_SIZE + j] += learningRate * error * hiddenWeights[i] * inputs[j];
                    }
                }
                if (hiddenLayer[i] > 0) { // activation func
                    hiddenBias += learningRate * error * hiddenWeights[i];
                }
            }
        }

        private boolean loadModel(String modelFilePath) {
            Path parentDirPath = Paths.get(modelFilePath).getParent();

            if (parentDirPath != null) {
                File parentDir = parentDirPath.toFile();

                // Check if the parent directory exists, create it if not
                if (!parentDir.exists()) {
                    try {
                        Files.createDirectories(parentDirPath);
                        System.out.println("Created directory: " + parentDirPath);
                    } catch (IOException e) {
                        System.err.println("Failed to create directory: " + parentDirPath);
                        e.printStackTrace();
                        return false;
                    }
                }
            } else {
                System.err.println("Invalid model file path: " + modelFilePath);
                return false;
            }

            File modelFile = new File(modelFilePath);

            // Create file if it does not exist
            if (!modelFile.exists()) {
                System.out.println("Created File");
                initializeWeights();
                saveModel();
                return true;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(modelFile))) {
                for (int i = 0; i < inputWeights.length; i++) {
                    String line = reader.readLine();
                    if (line == null) {
                        return false; // File format is not as expected
                    }
                    inputWeights[i] = Double.parseDouble(line.trim());
                }
                for (int i = 0; i < hiddenWeights.length; i++) {
                    String line = reader.readLine();
                    if (line == null) {
                        return false; // File format is not as expected
                    }
                    hiddenWeights[i] = Double.parseDouble(line.trim());
                }
                String hiddenBiasLine = reader.readLine();
                if (hiddenBiasLine == null) {
                    return false; // File format is not as expected
                }
                hiddenBias = Double.parseDouble(hiddenBiasLine.trim());

                String outputBiasLine = reader.readLine();
                if (outputBiasLine == null) {
                    return false; // File format is not as expected
                }
                outputBias = Double.parseDouble(outputBiasLine.trim());

                return true;
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void saveModel() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(buyModelFilePath))) {
                for (double weight : inputWeights) {
                    writer.write(weight + "\n");
                }
                for (double weight : hiddenWeights) {
                    writer.write(weight + "\n");
                }
                writer.write(hiddenBias + "\n");
                writer.write(outputBias + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
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
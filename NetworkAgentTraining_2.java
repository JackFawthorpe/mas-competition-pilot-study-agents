package api.agent;

import api.KingdomAPI;
import api.PlayerAPI;
import api.data.CardData;
import api.data.CardTypeData;
import api.data.DeckData;
import api.data.PlayerData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This finds weights and biases in a file located at the current dir / NN
 * These weights and biases are updated by playing lots of games
 * The deployed agent can then use these
 */
public class NetworkAgentTraining_2 implements ActionController {

    private static final int INPUT_SIZE = 5;
    private static final int HIDDEN_SIZE = 10;
    private static final int OUTPUT_SIZE = 1;
    private static final double LEARNING_RATE = 0.01;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double INITIAL_EPSILON = 0.1;

    private final String buyModelFilePath = "src/main/java/api/agent/NN/buy_model_monte_carlo.txt";
    private final MonteCarloNeuralNetwork buyNeuralNetwork;
    private double epsilon = INITIAL_EPSILON;

    public NetworkAgentTraining_2() {
        buyNeuralNetwork = new MonteCarloNeuralNetwork();
        if (!buyNeuralNetwork.loadModel(buyModelFilePath)) {
            buyNeuralNetwork.initializeWeights();
            buyNeuralNetwork.saveModel();
        }
    }

    /***
     * Calculates the current strength of the player based on a number of factors
     */
    private double evaluationFunction() {
        return evaluationFunction(null);
    }

    private double evaluationFunction(CardData card) {
        PlayerData myData = PlayerAPI.getMe(this);
        DeckData myDeck = myData.getDeckData();

        int totalVictoryPoints = 0;
        int totalActions = 0;
        int totalBuys = 0;
        int totalMoney = 0;
        int totalDrawCount = 0;

        for (CardData deckCard : myDeck.getCardList()) {
            totalVictoryPoints += deckCard.getVictoryPoints();
            totalActions += deckCard.getActions();
            totalBuys += deckCard.getBuys();
            totalMoney += deckCard.getMoney();
            totalDrawCount += deckCard.getDrawCount();
        }

        if (card != null) {
            totalVictoryPoints += card.getVictoryPoints();
            totalActions += card.getActions();
            totalBuys += card.getBuys();
            totalMoney += card.getMoney();
            totalDrawCount += card.getDrawCount();
        }

        double baseScore = totalVictoryPoints * 2.0 +
                totalActions * 0.5 +
                totalBuys * 3.0 +
                totalMoney * 2.0 +
                totalDrawCount * 0.1;


        int orderInPlayerRankings = myData.getOrder();
        double rankFactor = 0.5 * (1.0 / orderInPlayerRankings);

        return baseScore * rankFactor;

////        Other player external data
//        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
//        int depletedCount = KingdomAPI.getDepletedCount();
//        double averageVictoryPoints = allPlayers.stream().mapToInt(PlayerData::getPoints).average().orElse(0.0);
//
//        double averageAdjustment = 0.0;
//        if (totalVictoryPoints < averageVictoryPoints) {
//            averageAdjustment = (averageVictoryPoints - totalVictoryPoints) * 0.1;
//        }
//
//        double depletionPenalty = 0.0;
//        if (depletedCount > 3) {
//            depletionPenalty = depletedCount * 0.5;
//        }
//
//        return baseScore - rankAdjustment - averageAdjustment - depletionPenalty;

    }

    public CardData hookImpl(List<CardData> options, MonteCarloNeuralNetwork NN) {
        double initialScore = evaluationFunction();
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

        double finalScore = evaluationFunction(bestCard);

        double reward = finalScore - initialScore;
        NN.updateQValues(bestInputs, reward, bestScore);

        return bestCard;
    }

    public CardData buyCardHook(List<CardData> buyOptions) {
        return hookImpl(buyOptions, buyNeuralNetwork);
    }

    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        if (!isRequired) {
            return null;
        }

        Optional<CardData> victoryCard = findVictoryPointCard(discardOptions);
        return victoryCard.orElseGet(() -> discardOptions.stream()
                .min(Comparator.comparingInt(CardData::getCost))
                .orElse(null));
    }

    public CardData gainCardHook(List<CardData> gainOptions) {
        return gainOptions.stream()
                .max(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        if (!isRequired) {
            return null;
        }

        return trashOptions.stream()
                .min(Comparator.comparingInt(CardData::getCost))
                .orElse(null);
    }

    public CardData playActionCardHook(List<CardData> actionOptions) {
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

    private class MonteCarloNeuralNetwork {
        private final Map<String, Double> qValues;
        private final double[] inputWeights;
        private final double[] hiddenWeights;
        private double hiddenBias;
        private double outputBias;

        private MonteCarloNeuralNetwork() {
            qValues = new HashMap<>();
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

        private void updateQValues(double[] inputs, double reward, double output) {
            String stateActionKey = Arrays.toString(inputs);
            double qValue = qValues.getOrDefault(stateActionKey, 0.0);
            qValue = qValue + LEARNING_RATE * (reward + DISCOUNT_FACTOR * output - qValue);
            qValues.put(stateActionKey, qValue);

            if (new Random().nextDouble() < epsilon) {
                epsilon *= 0.99;
            }

            updateWeights(inputs, reward, output);
        }

        private void updateWeights(double[] inputs, double target, double output) {
            double[] hiddenLayer = new double[HIDDEN_SIZE];
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                hiddenLayer[i] = hiddenBias;
                for (int j = 0; j < INPUT_SIZE; j++) {
                    hiddenLayer[i] += inputs[j] * inputWeights[i * INPUT_SIZE + j];
                }
                hiddenLayer[i] = Math.max(0, hiddenLayer[i]); // ReLU
            }

            double error = target - output;

            for (int i = 0; i < HIDDEN_SIZE; i++) {
                hiddenWeights[i] += LEARNING_RATE * error * hiddenLayer[i];
            }
            outputBias += LEARNING_RATE * error;

            for (int i = 0; i < HIDDEN_SIZE; i++) {
                for (int j = 0; j < INPUT_SIZE; j++) {
                    if (hiddenLayer[i] > 0) {
                        inputWeights[i * INPUT_SIZE + j] += LEARNING_RATE * error * hiddenWeights[i] * inputs[j];
                    }
                }
                if (hiddenLayer[i] > 0) {
                    hiddenBias += LEARNING_RATE * error * hiddenWeights[i];
                }
            }

            saveModel();
        }

        private boolean loadModel(String modelFilePath) {
            Path parentDirPath = Paths.get(modelFilePath).getParent();

            if (parentDirPath != null) {
                File parentDir = parentDirPath.toFile();

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
                        return false;
                    }
                    inputWeights[i] = Double.parseDouble(line.trim());
                }
                for (int i = 0; i < hiddenWeights.length; i++) {
                    String line = reader.readLine();
                    if (line == null) {
                        return false;
                    }
                    hiddenWeights[i] = Double.parseDouble(line.trim());
                }
                String line = reader.readLine();
                if (line == null) {
                    return false;
                }
                hiddenBias = Double.parseDouble(line.trim());
                line = reader.readLine();
                if (line == null) {
                    return false;
                }
                outputBias = Double.parseDouble(line.trim());
            } catch (IOException e) {
                System.err.println("Failed to read model file: " + modelFilePath);
                e.printStackTrace();
                return false;
            }

            return true;
        }

        private void saveModel() {
            try (PrintWriter writer = new PrintWriter(buyModelFilePath)) {
                for (double weight : inputWeights) {
                    writer.println(weight);
                }
                for (double weight : hiddenWeights) {
                    writer.println(weight);
                }
                writer.println(hiddenBias);
                writer.println(outputBias);
            } catch (FileNotFoundException e) {
                System.err.println("Failed to save model to file: " + buyModelFilePath);
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
                hiddenLayer[i] = Math.max(0, hiddenLayer[i]); // ReLU
            }

            double output = outputBias;
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                output += hiddenLayer[i] * hiddenWeights[i];
            }

            return output;
        }
    }
}
package api.agent;

import api.KingdomAPI;
import api.PlayerAPI;
import api.RoundAPI;
import api.data.*;

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
public class NetworkAgentTraining_3 implements ActionController {

//    This one will consider actions, buys, coins, supply avaliable, amount in player, amount in play, amount in opponenet
    private static final int INPUT_SIZE = 9;
    private static final int OUTPUT_SIZE = 1;
    private static final int HIDDEN_SIZE = 30;
    private static final double LEARNING_RATE = 0.01;
    private static final double DISCOUNT_FACTOR = 0.95;
    private static final double INITIAL_EPSILON = 0.1;

    private final String buyModelFilePath = "src/main/java/api/agent/NN/buy_model_monte_carlo_9.txt";
    private final MonteCarloNeuralNetwork buyNeuralNetwork;
    private double epsilon = INITIAL_EPSILON;

    public NetworkAgentTraining_3() {
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
        PlayerData currentPlayer = PlayerAPI.getMe(this);
        double currentPlayerEvaluation = evaluatePlayer(currentPlayer, card);

        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
        double maxOtherPlayerEvaluation = Double.NEGATIVE_INFINITY;

        for (PlayerData player : allPlayers) {
            if (!player.equals(currentPlayer)) {
                double playerEvaluation = evaluatePlayer(player, null);
                if (playerEvaluation > maxOtherPlayerEvaluation) {
                    maxOtherPlayerEvaluation = playerEvaluation;
                }
            }
        }

        double baseScore = currentPlayerEvaluation - maxOtherPlayerEvaluation;

        return baseScore;
    }

    private double evaluatePlayer(PlayerData player, CardData card) {
        DeckData myDeck = player.getDeckData();

        int totalVictoryPoints = 0;
        int totalActions = 0;
        int totalBuys = 0;
        int totalMoney = 0;
        int totalDrawCount = 0;
        int totalCost = 0;

        for (CardData deckCard : myDeck.getCardList()) {
            totalVictoryPoints += deckCard.getVictoryPoints();
            totalActions += deckCard.getActions();
            totalBuys += deckCard.getBuys();
            totalMoney += deckCard.getMoney();
            totalDrawCount += deckCard.getDrawCount();
            totalCost += deckCard.getCost();
        }

        if (card != null) {
            totalVictoryPoints += card.getVictoryPoints();
            totalActions += card.getActions();
            totalBuys += card.getBuys();
            totalMoney += card.getMoney();
            totalDrawCount += card.getDrawCount();
            totalCost += card.getCost();
        }

        double baseScore = totalVictoryPoints * 1.0 +
                totalActions * 0.2 +
                totalBuys * 0.7 +
                totalMoney * 0.7 +
                totalDrawCount * 0.1 +
                totalCost * 0.8;


//        int orderInPlayerRankings = player.getOrder();
//        double rankFactor = 0.5 * (1.0 / orderInPlayerRankings);
//        double scaledScore = baseScore * rankFactor;

//        TODO change objective function to consider other players?
//        TODO potentially scale score?

        return baseScore;
    }


        private int getTotalInOpponentsHandsHelper(CardName card) {
        int count = 0;
        for (PlayerData player : PlayerAPI.getPlayers()) {
            for (CardData playerCard : player.getDeckData().getCardList()) {
                if (playerCard.getName() == card) {
                    count += 1;
                }
            }
        }
        return count;
    }

    private int findMaxAttribute(PlayerData player, String attributeName) {
        List<CardData> playerDeck = player.getDeckData().getCardList();
        List<CardData> kingdomCards = KingdomAPI.getAvailableCards();

        int maxFromPlayerDeck = getMaxAttributeValue(playerDeck, attributeName);
        int maxFromKingdomCards = getMaxAttributeValue(kingdomCards, attributeName);

        return Math.max(maxFromPlayerDeck, maxFromKingdomCards);
    }

    private int getMaxAttributeValue(List<CardData> cards, String attributeName) {
        int maxAttributeValue = 0;

        for (CardData card : cards) {
            int attributeValue = 0;

            switch (attributeName) {
                case "Actions":
                    attributeValue = card.getActions();
                    break;
                case "Buys":
                    attributeValue = card.getBuys();
                    break;
                case "Money":
                    attributeValue = card.getMoney();
                    break;
                case "VictoryPoints":
                    attributeValue = card.getVictoryPoints();
                    break;
                case "DrawCount":
                    attributeValue = card.getDrawCount();
                    break;
                case "Cost":
                    attributeValue = card.getCost();
                default:
                    // Handle default case or throw exception if necessary
                    break;
            }

            if (attributeValue > maxAttributeValue) {
                maxAttributeValue = attributeValue;
            }
        }

        return maxAttributeValue;
    }

    private double[] getScaledInputs(CardData card) {
        int maxActions = findMaxAttribute(PlayerAPI.getMe(this), "Actions");
        int maxBuys = findMaxAttribute(PlayerAPI.getMe(this), "Buys");
        int maxMoney = findMaxAttribute(PlayerAPI.getMe(this), "Money");
        int maxVictoryPoints = findMaxAttribute(PlayerAPI.getMe(this), "VictoryPoints");
        int maxDrawCount = findMaxAttribute(PlayerAPI.getMe(this), "DrawCount");
        int maxDrawCost = findMaxAttribute(PlayerAPI.getMe(this), "Cost");
        double maxRound = 30.0;

        int maxCardsRemaining;
        switch (card.getName().getDisplayName()) {
            case "Copper":
                maxCardsRemaining = 60 - 7 * 4;
                break;
            case "Curse":
                maxCardsRemaining = (4 - 1) * 10;
                break;
            case "Silver":
                maxCardsRemaining = 40;
                break;
            case "Gold":
                maxCardsRemaining = 30;
                break;
            case "Estate":
                maxCardsRemaining = 24;
                break;
            case "Duchy":
            case "Province":
                maxCardsRemaining = 12;
                break;
            default:
                maxCardsRemaining = 10;
                break;
        }
        double[] scaledInputs = {
                card.getActions() / (double) maxActions,
                card.getBuys() / (double) maxBuys,
                card.getMoney() / (double) maxMoney,
                card.getVictoryPoints() / (double) maxVictoryPoints,
                card.getDrawCount() / (double) maxDrawCount,
                card.getCost() / (double) maxDrawCost,
                KingdomAPI.getCardCountRemaining(card.getName()) / (double) maxCardsRemaining,
                getTotalInOpponentsHandsHelper(card.getName()) / (double) maxCardsRemaining,
                Math.min(RoundAPI.getRoundNumber(), maxRound) / maxRound
        };

        return scaledInputs;
    }

    public CardData hookImpl(List<CardData> options, MonteCarloNeuralNetwork NN) {

        double initialScore = evaluationFunction();
        CardData bestCard = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestInputs = null;

        for (CardData card : options) {
            double[] inputs = getScaledInputs(card);
            double score = NN.evaluateCard(inputs);
//            System.out.println("scaled inputs " + Arrays.toString(inputs));
//            System.out.println("score " + score);
            if (score > bestScore) {
                bestScore = score;
                bestCard = card;
                bestInputs = inputs;
            }
        }

        double finalScore = evaluationFunction(bestCard);
        double reward = finalScore - initialScore;

//        System.out.println("Chose " + bestCard.getName().getDisplayName());
//        System.out.println("best score " + bestScore);
//        System.out.println("final score " + finalScore);


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

            // Update Q-value based on Q-learning formula
            double target = reward + DISCOUNT_FACTOR * output;
            qValue = qValue + LEARNING_RATE * (target - qValue);

            qValues.put(stateActionKey, qValue);

            // Decay epsilon for exploration-exploitation trade-off
            if (new Random().nextDouble() < epsilon) {
                epsilon *= 0.99;
            }

            // Update neural network weights based on backpropagation
            updateWeights(inputs, target, output);
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
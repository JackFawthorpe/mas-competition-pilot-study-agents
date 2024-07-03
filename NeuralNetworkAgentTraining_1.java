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
import java.util.function.Function;

/**
 * This reads and writes its weights and bias to: api/agent/NN/new
 * This is not a very good dominion player, it just spams militias because the objective function is not very good :(
 */
public class NeuralNetworkAgentTraining_1 implements ActionController {
    private static final int INPUT_SIZE = 19;
    private static final int OUTPUT_SIZE = 1;
    private static final int HIDDEN_SIZE = 50;
    private static final double LEARNING_RATE = 0.01;
    private static final double DISCOUNT_FACTOR = 0.95;
    private static final double INITIAL_EPSILON = 0.1;
    private final MonteCarloNeuralNetwork buyNeuralNetwork;
    private final MonteCarloNeuralNetwork discardNeuralNetwork;
    private final MonteCarloNeuralNetwork gainNetwork;
    private final MonteCarloNeuralNetwork trashNetwork;
    private final MonteCarloNeuralNetwork playNetwork;

    private double epsilon = INITIAL_EPSILON;

    public NeuralNetworkAgentTraining_1() {
        buyNeuralNetwork = new MonteCarloNeuralNetwork("src/main/java/api/agent/NN/new/buy_model_monte_carlo_25.txt");
        if (!buyNeuralNetwork.loadModel()) {
            buyNeuralNetwork.initializeWeights();
            buyNeuralNetwork.saveModel();
        }

        discardNeuralNetwork = new MonteCarloNeuralNetwork("src/main/java/api/agent/NN/new/discard_model_monte_carlo_25.txt");
        if (!discardNeuralNetwork.loadModel()) {
            discardNeuralNetwork.initializeWeights();
            discardNeuralNetwork.saveModel();
        }

        gainNetwork = new MonteCarloNeuralNetwork("src/main/java/api/agent/NN/new/gain_model_monte_carlo_25.txt");
        if (!gainNetwork.loadModel()) {
            gainNetwork.initializeWeights();
            gainNetwork.saveModel();
        }

        trashNetwork = new MonteCarloNeuralNetwork("src/main/java/api/agent/NN/new/trash_model_monte_carlo_25.txt");
        if (!trashNetwork.loadModel()) {
            trashNetwork.initializeWeights();
            trashNetwork.saveModel();
        }

        playNetwork = new MonteCarloNeuralNetwork("src/main/java/api/agent/NN/new/play_model_monte_carlo_25.txt");
        if (!playNetwork.loadModel()) {
            playNetwork.initializeWeights();
            playNetwork.saveModel();
        }
    }

//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------
//    Hooks
//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------

    public CardData hookImpl(List<CardData> options, int phaseNumber, boolean isRequired, MonteCarloNeuralNetwork neuralNetwork, Function<CardData, Double> evaluationFunction) {
        if (neuralNetwork.inputWeights == null) {
            neuralNetwork.initializeWeights();
            neuralNetwork.saveModel();
        }

        double initialScore = evaluationFunction.apply(null);
        CardData bestCard = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestInputs = null;

        for (CardData card : options) {
            double[] inputs = getScaledInputs(card, phaseNumber, isRequired);
            double score = neuralNetwork.evaluateCard(inputs);
            if (score > bestScore) {
                bestScore = score;
                bestCard = card;
                bestInputs = inputs;
            }
        }

        double finalScore = evaluationFunction.apply(bestCard);
        double reward = finalScore - initialScore;

        neuralNetwork.updateQValues(bestInputs, reward, bestScore);

        return bestCard;
    }

    public CardData buyCardHook(List<CardData> buyOptions) {
        return hookImpl(buyOptions, 1, false, buyNeuralNetwork, this::buyEvaluationFunction);
    }

    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        if (!isRequired) {
            return null;
        }

        return hookImpl(discardOptions, 2, isRequired, discardNeuralNetwork, this::discardEvaluationFunction);
    }

    public CardData gainCardHook(List<CardData> gainOptions) {
        return hookImpl(gainOptions, 3, false, gainNetwork, this::gainEvaluationFunction);
    }

    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        if (!isRequired) {
            return null;
        }

        return hookImpl(trashOptions, 4, isRequired, gainNetwork, this::trashEvaluationFunction);
    }

    public CardData playActionCardHook(List<CardData> actionOptions) {
        return hookImpl(actionOptions, 5, false, playNetwork, this::playEvaluationFunction);
    }

//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------
//    Neural network stuff
//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private class MonteCarloNeuralNetwork {
        private final Map<String, Double> qValues;
        private final double[] inputWeights;
        private final double[] hiddenWeights;
        private double hiddenBias;
        private double outputBias;

        private String modelFilePath;

        private MonteCarloNeuralNetwork(String modelFilePath) {
            qValues = new HashMap<>();
            inputWeights = new double[INPUT_SIZE * HIDDEN_SIZE];
            hiddenWeights = new double[HIDDEN_SIZE * OUTPUT_SIZE];
            this.modelFilePath = modelFilePath;
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

//        private void updateWeights(double[] inputs, double target, double output) {
//            double[] hiddenLayer = new double[HIDDEN_SIZE];
//            for (int i = 0; i < HIDDEN_SIZE; i++) {
//                hiddenLayer[i] = hiddenBias;
//                for (int j = 0; j < INPUT_SIZE; j++) {
//                    hiddenLayer[i] += inputs[j] * inputWeights[i * INPUT_SIZE + j];
//                }
//                hiddenLayer[i] = Math.max(0, hiddenLayer[i]); // ReLU
//            }
//
//            double error = target - output;
//
//            for (int i = 0; i < HIDDEN_SIZE; i++) {
//                hiddenWeights[i] += LEARNING_RATE * error * hiddenLayer[i];
//            }
//            outputBias += LEARNING_RATE * error;
//
//            for (int i = 0; i < HIDDEN_SIZE; i++) {
//                for (int j = 0; j < INPUT_SIZE; j++) {
//                    if (hiddenLayer[i] > 0) {
//                        inputWeights[i * INPUT_SIZE + j] += LEARNING_RATE * error * hiddenWeights[i] * inputs[j];
//                    }
//                }
//                if (hiddenLayer[i] > 0) {
//                    hiddenBias += LEARNING_RATE * error * hiddenWeights[i];
//                }
//            }
//
//            saveModel();
//        }

        private void updateWeights(double[] inputs, double target, double output) {
            double[] hiddenLayer = new double[HIDDEN_SIZE];
            double lambda = 0.01; // Regularization parameter
            double clipValue = 5.0; // Gradient clipping value

            // Forward pass to compute hidden layer activations
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                hiddenLayer[i] = hiddenBias;
                for (int j = 0; j < INPUT_SIZE; j++) {
                    hiddenLayer[i] += inputs[j] * inputWeights[i * INPUT_SIZE + j];
                }
                hiddenLayer[i] = Math.max(0, hiddenLayer[i]); // ReLU
            }

            double error = target - output;

            // Update output weights with regularization
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                double gradient = LEARNING_RATE * (error * hiddenLayer[i] - lambda * hiddenWeights[i]);
                hiddenWeights[i] += clip(gradient, -clipValue, clipValue);
            }
            outputBias += LEARNING_RATE * error;

            // Update input weights and hidden bias with regularization and gradient clipping
            for (int i = 0; i < HIDDEN_SIZE; i++) {
                for (int j = 0; j < INPUT_SIZE; j++) {
                    if (hiddenLayer[i] > 0) {
                        double gradient = LEARNING_RATE * (error * hiddenWeights[i] * inputs[j] - lambda * inputWeights[i * INPUT_SIZE + j]);
                        inputWeights[i * INPUT_SIZE + j] += clip(gradient, -clipValue, clipValue);
                    }
                }
                if (hiddenLayer[i] > 0) {
                    double gradient = LEARNING_RATE * (error * hiddenWeights[i] - lambda * hiddenBias);
                    hiddenBias += clip(gradient, -clipValue, clipValue);
                }
            }

            saveModel();
        }

        private double clip(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        private boolean loadModel() {
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
            try (PrintWriter writer = new PrintWriter(modelFilePath)) {
                for (double weight : inputWeights) {
                    writer.println(weight);
                }
                for (double weight : hiddenWeights) {
                    writer.println(weight);
                }
                writer.println(hiddenBias);
                writer.println(outputBias);
            } catch (FileNotFoundException e) {
                System.err.println("Failed to save model to file: " + modelFilePath);
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

//            // Integrate Q-values to adjust output based on expected rewards
//            String stateActionKey = Arrays.toString(inputs);
//            double qValue = qValues.getOrDefault(stateActionKey, 0.0);
//            output += qValue; // Adjust output based on Q-value

            return output;
        }
    }

//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------
//    objective functions
//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private double buyEvaluationFunction(CardData card) {
        PlayerData currentPlayer = PlayerAPI.getMe(this);
        //VP, actions, buys, money, drawcount, cost
        double[] biases = {1.0, 0.2, 0.7, 0.8, 0.1, 0.8};
        double currentPlayerEvaluation = evaluatePlayerAdditive(currentPlayer, card, biases);

        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
        double maxOtherPlayerEvaluation = Double.NEGATIVE_INFINITY;

        for (PlayerData player : allPlayers) {
            if (!player.equals(currentPlayer)) {
                double playerEvaluation = evaluatePlayerAdditive(player, null, biases);
                if (playerEvaluation > maxOtherPlayerEvaluation) {
                    maxOtherPlayerEvaluation = playerEvaluation;
                }
            }
        }

        double baseScore = currentPlayerEvaluation - maxOtherPlayerEvaluation;

        return baseScore;
    }

    private double discardEvaluationFunction(CardData card) {
        PlayerData currentPlayer = PlayerAPI.getMe(this);
        //VP, actions, buys, money, drawcount, cost
        double[] biases = {-1.0, 0.2, 0.7, 0.8, 0.1, 0.8};
        double currentPlayerEvaluation = evaluatePlayerSubtractive(currentPlayer, card, biases);

        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
        double maxOtherPlayerEvaluation = Double.NEGATIVE_INFINITY;

        for (PlayerData player : allPlayers) {
            if (!player.equals(currentPlayer)) {
                double playerEvaluation = evaluatePlayerSubtractive(player, null, biases);
                if (playerEvaluation > maxOtherPlayerEvaluation) {
                    maxOtherPlayerEvaluation = playerEvaluation;
                }
            }
        }

        double baseScore = currentPlayerEvaluation - maxOtherPlayerEvaluation;

        return baseScore;
    }

    private double gainEvaluationFunction(CardData card) {
        PlayerData currentPlayer = PlayerAPI.getMe(this);
        //VP, actions, buys, money, drawcount, cost
        double[] biases = {1.0, 0.8, 0.8, 0.2, 0.1, 0.8};
        double currentPlayerEvaluation = evaluatePlayerAdditive(currentPlayer, card, biases);

        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
        double maxOtherPlayerEvaluation = Double.NEGATIVE_INFINITY;

        for (PlayerData player : allPlayers) {
            if (!player.equals(currentPlayer)) {
                double playerEvaluation = evaluatePlayerAdditive(player, null, biases);
                if (playerEvaluation > maxOtherPlayerEvaluation) {
                    maxOtherPlayerEvaluation = playerEvaluation;
                }
            }
        }

        double baseScore = currentPlayerEvaluation - maxOtherPlayerEvaluation;

        return baseScore;
    }

    private double trashEvaluationFunction(CardData card) {
        PlayerData currentPlayer = PlayerAPI.getMe(this);
        //VP, actions, buys, money, drawcount, cost
        double[] biases = {1.0, 0.2, 0.7, 0.8, 0.1, 0.8};
        double currentPlayerEvaluation = evaluatePlayerSubtractive(currentPlayer, card, biases);

        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
        double maxOtherPlayerEvaluation = Double.NEGATIVE_INFINITY;

        for (PlayerData player : allPlayers) {
            if (!player.equals(currentPlayer)) {
                double playerEvaluation = evaluatePlayerSubtractive(player, null, biases);
                if (playerEvaluation > maxOtherPlayerEvaluation) {
                    maxOtherPlayerEvaluation = playerEvaluation;
                }
            }
        }

        double baseScore = currentPlayerEvaluation - maxOtherPlayerEvaluation;

        return baseScore;
    }

    private double playEvaluationFunction(CardData card) {
        PlayerData currentPlayer = PlayerAPI.getMe(this);
        //VP, actions, buys, money, drawcount, cost
        double[] biases = {1.0, -1.0, -0.9, -0.5, -0.9, -0.5};
        double currentPlayerEvaluation = evaluatePlayerSubtractive(currentPlayer, card, biases);

        List<PlayerData> allPlayers = PlayerAPI.getPlayers();
        double maxOtherPlayerEvaluation = Double.NEGATIVE_INFINITY;

        for (PlayerData player : allPlayers) {
            if (!player.equals(currentPlayer)) {
                double playerEvaluation = evaluatePlayerSubtractive(player, null, biases);
                if (playerEvaluation > maxOtherPlayerEvaluation) {
                    maxOtherPlayerEvaluation = playerEvaluation;
                }
            }
        }

        double baseScore = currentPlayerEvaluation - maxOtherPlayerEvaluation;

        return baseScore;
    }

    private double evaluatePlayerAdditive(PlayerData player, CardData card, double[] biases) {
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

        double baseScore = totalVictoryPoints * biases[0] +
                totalActions * biases[1] +
                totalBuys * biases[2] +
                totalMoney * biases[3] +
                totalDrawCount * biases[4] +
                totalCost * biases[5];

        return baseScore;
    }

    private double evaluatePlayerSubtractive(PlayerData player, CardData card, double[] biases) {
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
            totalVictoryPoints -= card.getVictoryPoints();
            totalActions -= card.getActions();
            totalBuys -= card.getBuys();
            totalMoney -= card.getMoney();
            totalDrawCount -= card.getDrawCount();
            totalCost -= card.getCost();
        }

        double baseScore = totalVictoryPoints * biases[0] +
                totalActions * biases[1] +
                totalBuys * biases[2] +
                totalMoney * biases[3] +
                totalDrawCount * biases[4] +
                totalCost * biases[5];

        return baseScore;
    }


//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------
//    helpers
//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private double[] getScaledInputs(CardData card, int phaseNumber, boolean isRequired) {
        double maxActions = findMaxAttribute(PlayerAPI.getMe(this), "Actions");
        double maxBuys = findMaxAttribute(PlayerAPI.getMe(this), "Buys");
        double maxMoney = findMaxAttribute(PlayerAPI.getMe(this), "Money");
        double maxVictoryPoints = findMaxAttribute(PlayerAPI.getMe(this), "VictoryPoints");
        double maxDrawCount = findMaxAttribute(PlayerAPI.getMe(this), "DrawCount");
        double maxDrawCost = findMaxAttribute(PlayerAPI.getMe(this), "Cost");
        double maxRound = 30.0;
        int isBuyPhase = 0, isDiscardPhase = 0, isGainPhase = 0, isTrashPhase = 0, isPlayPhase = 0;
        int isPlayingFirst = 0, isPlayingSecond = 0, isPlayingThird = 0, isPlayingFourth = 0;
        int isRequiredInt = isRequired ? 1 : 0;
//        These (below) actually take away from the ability of the NN, probably others do aswell as more complexity != better but too much work to find
        int isAction = 0, isTreasure = 0, isVictory = 0, isCurse = 0, isReaction = 0, isAttack = 0;

        switch (PlayerAPI.getMe(this).getOrder()) {
            case 1 -> isPlayingFirst = 1;
            case 2 -> isPlayingSecond = 1;
            case 3 -> isPlayingThird = 1;
            case 4 -> isPlayingFourth = 1;
        }

        switch (phaseNumber) {
            case 1 -> isBuyPhase = 1;
            case 2 -> isDiscardPhase = 1;
            case 3 -> isGainPhase = 1;
            case 4 -> isTrashPhase = 1;
            case 5 -> isPlayPhase = 1;
        }

        for (CardTypeData type : card.getCardTypes()) {
            switch (type) {
                case ACTION -> isAction = 1;
                case TREASURE -> isTreasure = 1;
                case VICTORY -> isVictory = 1;
                case CURSE -> isCurse = 1;
                case REACTION -> isReaction = 1;
                case ATTACK -> isAttack = 1;
            }
        }

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
                card.getActions() / maxActions,
                card.getBuys() / maxBuys,
                card.getMoney() / maxMoney,
                card.getVictoryPoints() / maxVictoryPoints,
                card.getDrawCount() / maxDrawCount,
                card.getCost() / maxDrawCost,
                KingdomAPI.getCardCountRemaining(card.getName()) / (double) maxCardsRemaining,
                getTotalInOpponentsHandsHelper(card.getName()) / (double) maxCardsRemaining,
                Math.min(RoundAPI.getRoundNumber(), maxRound) / maxRound,
                isBuyPhase,
                isDiscardPhase,
                isGainPhase,
                isTrashPhase,
                isPlayPhase,
                isPlayingFirst,
                isPlayingSecond,
                isPlayingThird,
                isPlayingFourth,
                isRequiredInt,
                isAction,
                isTreasure,
                isVictory,
                isCurse,
                isReaction,
                isAttack
        };

        return scaledInputs;
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

        int maxFromPlayerDeck = Math.max(getMaxAttributeValue(playerDeck, attributeName), 1);
        int maxFromKingdomCards = Math.max(getMaxAttributeValue(kingdomCards, attributeName), 1);

        return Math.max(maxFromPlayerDeck, maxFromKingdomCards);
    }

    private int getMaxAttributeValue(List<CardData> cards, String attributeName) {
        int maxAttributeValue = 1;

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

    public Optional<CardData> findVictoryPointCard(List<CardData> cardDataList) {
        for (CardData cardData : cardDataList) {
            if (cardData.getCardTypes().contains(CardTypeData.VICTORY)) {
                return Optional.of(cardData);
            }
        }
        return Optional.empty();
    }
}
package api.agent;

import api.KingdomAPI;
import api.PlayerAPI;
import api.RoundAPI;
import api.data.CardData;
import api.data.CardName;
import api.data.PlayerData;

import java.io.*;
import java.util.Comparator;
import java.util.List;

public class NewNetworkAgentTraining_1 implements ActionController {

    private double[][] weights; // Example: store weights of the model
    private double[] biases; // Example: store biases of the model
    private double learningRate = 0.01; // Example learning rate
    private int inputSize = calculateInputSize();
    private int hiddenSize = 50; // Example: Number of neurons in hidden layer
    private int outputSize = 2;  // Output should either be perform the action (buy/sell/whatever) OR dont perform any action

    private final String modelFilePath = "src/main/java/api/agent/NN/model_parameters.txt";

    public NewNetworkAgentTraining_1() {
        // Initialize weights and biases or load them from a file
        this.loadModelParameters(modelFilePath);
    }

    public CardData buyCardHook(List<CardData> buyOptions) {
        return hookImpl(buyOptions, 1, true);
    }

    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        return hookImpl(discardOptions, 2, isRequired);
    }

    public CardData gainCardHook(List<CardData> gainOptions) {
        return hookImpl(gainOptions, 3, true);
    }

    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        return hookImpl(trashOptions, 4, isRequired);
    }

    public CardData playActionCardHook(List<CardData> actionOptions) {
        return hookImpl(actionOptions, 5, true);
    }

//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------
//    Neural network stuff
//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------


    // Method to decide action index using the neural network model
    private int decideAction(double[] gameState) {
        // Implement forward propagation with your model
        // Example:
        double[] logits = forwardPropagation(gameState);

        // Implement decision logic (e.g., softmax, argmax)
        int actionIndex = argmax(logits);
        return actionIndex;
    }

    // Example forward propagation (replace with your specific neural network logic)
    private double[] forwardPropagation(double[] input) {
        // Example: calculate logits using weights and biases
        double[] logits = new double[outputSize]; // Example: 2 actions (do something [buy, discard, gain, trash, play action] or do nothing)
        for (int i = 0; i < outputSize; i++) {
            logits[i] = dotProduct(input, weights[i]) + biases[i];
        }
        return logits;
    }

    // Example dot product
    private double dotProduct(double[] a, double[] b) {
        double result = 0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }

    // Example argmax function
    private int argmax(double[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private int calculateInputSize() {
//        return convertGameStateToInput(PlayerAPI.getMe(this).getDeckData().getCardList().get(0), 1, true).length;
        return 100;
    }

    private void updateWeightsAndBiases(int actionIndex) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] += learningRate * (actionIndex == i ? 1 : 0);
            }
            biases[i] += learningRate * (actionIndex == i ? 1 : 0);
        }

        saveModelParameters(modelFilePath);
    }

    private void punishWeights(int actionIndex) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] -= learningRate * (actionIndex == i ? 1 : 0);
            }
            biases[i] -= learningRate * (actionIndex == i ? 1 : 0);
        }

        saveModelParameters(modelFilePath);
    }

    private void saveModelParameters(String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Example: save weights and biases to file
            for (double[] weight : weights) {
                for (double w : weight) {
                    writer.print(w + " ");
                }
                writer.println();
            }
            for (double b : biases) {
                writer.print(b + " ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to load model parameters from a file
    private void loadModelParameters(String filePath) {
        weights = new double[hiddenSize][inputSize];
        biases = new double[hiddenSize];

        File file = new File(filePath);
        if (!file.exists()) {
            // If file does not exist, initialize weights and biases
            initializeWeightsAndBiases();
            saveModelParameters(filePath); // Save initialized parameters to file
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Load weights and biases from file
            String line;
            // Read weights
            for (int i = 0; i < weights.length; i++) {
                line = reader.readLine();
                String[] values = line.trim().split(" ");
                for (int j = 0; j < weights[i].length; j++) {
                    weights[i][j] = Double.parseDouble(values[j]);
                }
            }
            // Read biases
            line = reader.readLine();
            String[] biasValues = line.trim().split(" ");
            for (int i = 0; i < biases.length; i++) {
                biases[i] = Double.parseDouble(biasValues[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to initialize weights and biases (example implementation)
    private void initializeWeightsAndBiases() {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] = Math.random(); // Initialize with random values as an example
            }
            biases[i] = Math.random(); // Initialize biases with random values
        }
    }


//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------
//    helpers
//    -------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private CardData hookImpl(List<CardData> options, int phaseNumber, boolean isRequired) {
        double maxEvaluation = Double.NEGATIVE_INFINITY;
        double[] bestGameState = null;
        CardData bestCard = null;

        for (CardData card : options) {
            double currentEvaluation = evaluationFunction(card);
            if (currentEvaluation > maxEvaluation) {
                maxEvaluation = currentEvaluation;
                bestGameState = convertGameStateToInput(card, phaseNumber, isRequired);
                bestCard = card;
            }
        }
        int actionIndex = decideAction(bestGameState);
        if (actionIndex == 0) {
            updateWeightsAndBiases(actionIndex); // Update weights after decision
            return bestCard;
        } else {
            if (isRequired) {
                if (phaseNumber == 1 || phaseNumber == 3 || phaseNumber == 5) {
                    return options.stream()
                            .max(Comparator.comparingInt(CardData::getCost))
                            .orElse(null);
                } else {
                    return options.stream()
                            .min(Comparator.comparingInt(CardData::getCost))
                            .orElse(null);
                }
//                TODO punish the weights for being wrong

            }
            return null;
        }
    }

    private double evaluationFunction(CardData card) {
        int currentVictoryPoints = PlayerAPI.getMe(this).getPoints();
        int amountOfTurns = RoundAPI.getRoundNumber();

        if (card != null) {
            currentVictoryPoints += card.getVictoryPoints();
        }

        if (amountOfTurns <= 0) {
            amountOfTurns = 1;
        }

        double victoryPointsPerTurn = currentVictoryPoints / (double) amountOfTurns;

        return victoryPointsPerTurn;
    }

    private double[] convertGameStateToInput(CardData card, int phaseNumber, boolean isRequired) {
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
                isRequiredInt
        };

        return scaledInputs;
    }

    private double findMaxAttribute(PlayerData player, String attributeName) {
        List<CardData> playerDeck = player.getDeckData().getCardList();
        List<CardData> kingdomCards = KingdomAPI.getAvailableCards();

        double maxFromPlayerDeck = getMaxAttributeValue(playerDeck, attributeName);
        double maxFromKingdomCards = getMaxAttributeValue(kingdomCards, attributeName);

        return Math.max(maxFromPlayerDeck, maxFromKingdomCards);
    }

    private double getMaxAttributeValue(List<CardData> cards, String attributeName) {
        double maxAttributeValue = 0.0;

        for (CardData card : cards) {
            double attributeValue = 0.0;

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

        if (maxAttributeValue <= 0.0) {
            maxAttributeValue = 1.0;
        }
        return maxAttributeValue;
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
}
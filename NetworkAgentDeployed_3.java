package api.agent;

import api.KingdomAPI;
import api.PlayerAPI;
import api.RoundAPI;
import api.data.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
//import java.util.MapEntry; wtf is this?
import java.util.Set;
import java.util.Spliterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;


/**
 * This finds weights and biases in a file located at the current dir / NN
 * These weights and biases are updated by playing lots of games
 * The deployed agent can then use these
 */
public class NetworkAgentDeployed_3 implements ActionController {

//    This one will consider actions, buys, coins, supply avaliable, amount in player, amount in play, amount in opponenet
    private static final int INPUT_SIZE = 9;
    private static final int OUTPUT_SIZE = 1;
    private static final int HIDDEN_SIZE = 30;

    private final String copiedBuyWeightsString = "1.1484523335122396\n" +
            "0.692630427688433\n" +
            "-0.40500322013956\n" +
            "-1.333248073573994\n" +
            "1.5707641768157703\n" +
            "-0.14549780938691648\n" +
            "1.3696841539611535\n" +
            "0.9448164451458272\n" +
            "0.6938076887541\n" +
            "0.05448633057380411\n" +
            "0.910037631551789\n" +
            "1.1274668986481784\n" +
            "2.866500212354682\n" +
            "0.6216307139579872\n" +
            "3.4395292427733635\n" +
            "0.8747087383846072\n" +
            "0.9767777141520892\n" +
            "0.17671713913501863\n" +
            "0.6529118851032296\n" +
            "1.457206394061571\n" +
            "0.8166646420274627\n" +
            "1.8548578002100353\n" +
            "0.3897792864620101\n" +
            "2.3042215846616605\n" +
            "-0.2432732359947331\n" +
            "-0.13408667545206485\n" +
            "-0.037219146260952414\n" +
            "-0.1318575784336166\n" +
            "0.7650641199229575\n" +
            "1.2531779447115428\n" +
            "3.3151071256490394\n" +
            "0.40479815824251825\n" +
            "3.9571347474539627\n" +
            "-0.14145001346331834\n" +
            "-0.27483783156845354\n" +
            "0.02072171702318037\n" +
            "0.5027475255905778\n" +
            "1.0399910476444338\n" +
            "0.9179631765377815\n" +
            "2.2917978099643657\n" +
            "0.12604102204449524\n" +
            "2.797818427549809\n" +
            "0.8079200369778737\n" +
            "0.6162635768917252\n" +
            "0.660760412777935\n" +
            "0.7943387731096861\n" +
            "1.2654499945251567\n" +
            "0.8857503574980625\n" +
            "1.0473428098847095\n" +
            "0.7582747930061414\n" +
            "1.2860001128887413\n" +
            "0.4608027988057532\n" +
            "0.7068794406873922\n" +
            "0.47553224188894494\n" +
            "-0.05973661325005019\n" +
            "0.7439853756939894\n" +
            "0.8600022752389255\n" +
            "2.212954015094656\n" +
            "0.817556758774589\n" +
            "2.657686525834022\n" +
            "0.08421393026919456\n" +
            "-0.33583072028651695\n" +
            "0.07482842603736962\n" +
            "-0.013121215352718596\n" +
            "0.8328165292604626\n" +
            "0.5946688646896974\n" +
            "2.352088962672316\n" +
            "0.6035754135729915\n" +
            "2.5992182184177324\n" +
            "0.8745031270638062\n" +
            "1.089776788656775\n" +
            "0.4459537792568129\n" +
            "0.8670324471361046\n" +
            "0.852950031450958\n" +
            "0.7533782874432698\n" +
            "1.3039636084428103\n" +
            "0.6466333928735793\n" +
            "1.5169307941297312\n" +
            "1.0258016247545834\n" +
            "0.7821127801335985\n" +
            "1.0008940882315942\n" +
            "0.9143771845993399\n" +
            "0.8471799658662817\n" +
            "-0.6526024494726044\n" +
            "-0.5233581985165471\n" +
            "1.479177721978616\n" +
            "-0.6894648320349231\n" +
            "0.9019176450153791\n" +
            "0.7191380610180017\n" +
            "1.1695535182524195\n" +
            "-0.13508967593772692\n" +
            "0.3656679732047015\n" +
            "0.715393932941081\n" +
            "2.944868454049486\n" +
            "-0.06861171989187918\n" +
            "3.105134346461106\n" +
            "0.07987108265705091\n" +
            "-0.42008771887577284\n" +
            "0.41064482301390376\n" +
            "0.7029194083223381\n" +
            "0.8498116397440746\n" +
            "0.5496410785371358\n" +
            "-0.13800063612874394\n" +
            "1.0808077540040122\n" +
            "0.8210821589997058\n" +
            "0.4859014112789776\n" +
            "0.14960355248279603\n" +
            "0.8329960419509126\n" +
            "0.5048114438542745\n" +
            "0.7590034676921742\n" +
            "0.7626262439575765\n" +
            "0.8182738168022113\n" +
            "1.403263088772268\n" +
            "1.3706858219068723\n" +
            "0.41569250537036273\n" +
            "0.4628997720483702\n" +
            "0.9039413958725804\n" +
            "0.28372666754433146\n" +
            "0.9669867320518907\n" +
            "1.6293397787607402\n" +
            "3.9107239236901425\n" +
            "-0.222360093357635\n" +
            "4.196859938062948\n" +
            "-0.4338492956271987\n" +
            "-0.16324011876110728\n" +
            "0.41580283865366263\n" +
            "-0.11047089362499296\n" +
            "1.1926663068196879\n" +
            "0.7240301292930519\n" +
            "2.20360558135656\n" +
            "-0.08229859992316277\n" +
            "3.074034057929495\n" +
            "0.9502956619047103\n" +
            "0.3856315102986177\n" +
            "0.2970968987807628\n" +
            "0.44036455590166906\n" +
            "1.3342028182387058\n" +
            "1.419420736950756\n" +
            "1.9660724571950463\n" +
            "0.670357723741494\n" +
            "2.9249609942467685\n" +
            "0.32445227163169177\n" +
            "1.0425033111540642\n" +
            "0.5422438873335105\n" +
            "0.3868815796938191\n" +
            "0.5260032980539421\n" +
            "0.686895548622474\n" +
            "2.49728741614857\n" +
            "0.4247545008650497\n" +
            "3.2182355759734738\n" +
            "-0.3099639111267106\n" +
            "-0.007354503498668615\n" +
            "0.25979847560851854\n" +
            "-0.20334150322521927\n" +
            "0.6474733067407199\n" +
            "0.8039813231651425\n" +
            "4.122366338712337\n" +
            "0.3662329449398595\n" +
            "3.7407004247278968\n" +
            "-0.06038525692754551\n" +
            "-0.2359751067258694\n" +
            "0.1201245811985092\n" +
            "0.5889511385847801\n" +
            "0.7016688253441745\n" +
            "-0.24883754530481383\n" +
            "-0.5495543900412981\n" +
            "1.691633312336114\n" +
            "0.14600347559198543\n" +
            "0.32493691589555923\n" +
            "0.040815727261670695\n" +
            "0.4763954100923368\n" +
            "1.069271933348619\n" +
            "1.007856768462628\n" +
            "0.48646364601320935\n" +
            "0.49195399621113073\n" +
            "0.6470315631199127\n" +
            "0.23442855699861123\n" +
            "0.6421890663173139\n" +
            "0.790156312859169\n" +
            "0.8655134002036199\n" +
            "0.7797345276071319\n" +
            "0.6817515750681299\n" +
            "0.30558751007760654\n" +
            "2.1216307684800313\n" +
            "0.5700173803846642\n" +
            "2.316586815371943\n" +
            "0.13818403100903512\n" +
            "-0.217458812838534\n" +
            "0.0560588356459407\n" +
            "0.4364997103762512\n" +
            "0.7857653812376729\n" +
            "0.8460852173093096\n" +
            "1.5920499276589957\n" +
            "0.9485237801976542\n" +
            "2.0702399550754333\n" +
            "0.9410791910465547\n" +
            "0.3535155848002706\n" +
            "0.37844641950916846\n" +
            "-0.09563605535709767\n" +
            "0.9841431171435012\n" +
            "1.0653879686624927\n" +
            "2.443444586780249\n" +
            "0.484518522112094\n" +
            "2.865550977415549\n" +
            "0.5079946963595622\n" +
            "1.0244998421656477\n" +
            "-0.01892904529763847\n" +
            "0.7040614680843899\n" +
            "1.081096740737034\n" +
            "1.385232976029107\n" +
            "2.659178195503955\n" +
            "0.44099498830783024\n" +
            "3.250478034208433\n" +
            "1.140437917649468\n" +
            "0.0923661895374927\n" +
            "0.5564583451061114\n" +
            "0.9561730880762495\n" +
            "1.1573155410079536\n" +
            "0.6129275003511133\n" +
            "1.4786113690385485\n" +
            "0.5970578444176177\n" +
            "1.2467756699979022\n" +
            "0.8034332590802753\n" +
            "0.31404856973592565\n" +
            "-0.04810114522408069\n" +
            "0.7600661876081611\n" +
            "1.1905402636470508\n" +
            "0.6520471555392054\n" +
            "1.1345514982095493\n" +
            "1.2595318025497608\n" +
            "2.0085656434427452\n" +
            "0.4823971163768068\n" +
            "0.6136009500671281\n" +
            "0.37485301714710106\n" +
            "0.1697804569159935\n" +
            "0.5973178632610724\n" +
            "1.2614355272518634\n" +
            "2.055614628621946\n" +
            "0.9674430141407314\n" +
            "2.602015048290245\n" +
            "0.599648874691896\n" +
            "0.3830151795285025\n" +
            "0.34925606306629103\n" +
            "0.12721415426517974\n" +
            "0.4746391475108293\n" +
            "0.6477501898305714\n" +
            "2.1699434853343935\n" +
            "0.7696646643309233\n" +
            "1.9704980420242113\n" +
            "0.6571055507101684\n" +
            "0.5732266102368851\n" +
            "0.31913551386784583\n" +
            "0.22782581989038952\n" +
            "0.7773826200104125\n" +
            "0.5181853644665655\n" +
            "2.890186306702824\n" +
            "0.07939720364542714\n" +
            "3.1568262809887915\n" +
            "-0.21157031233846155\n" +
            "0.06974466441904523\n" +
            "-0.01593077969914021\n" +
            "1.0086683927313642\n" +
            "1.118496912632658\n" +
            "0.9279269639319683\n" +
            "0.5481200466649401\n" +
            "0.758866156472157\n" +
            "1.3178825971876877\n" +
            "0.2768145281929236\n" +
            "0.9194383361126547\n" +
            "0.38231676555636956\n" +
            "-3.6427886413117125\n" +
            "2.570508165526562\n" +
            "1.1466077477172782\n" +
            "3.3860649382746604\n" +
            "1.714975986714505\n" +
            "-0.06147365697364467\n" +
            "1.7762457310380708\n" +
            "1.6163800996346949\n" +
            "0.09654091134995327\n" +
            "-3.346036423873015\n" +
            "2.5585294213064156\n" +
            "-1.2661961098436596\n" +
            "-0.3456212317234086\n" +
            "4.083301044017464\n" +
            "1.9838395459755767\n" +
            "1.6230831431889572\n" +
            "2.161245621046812\n" +
            "3.7448966184540757\n" +
            "-2.736471509088422\n" +
            "-1.1463021404081357\n" +
            "1.2273701654810754\n" +
            "0.7826820565934894\n" +
            "2.031123963925725\n" +
            "2.2434151016343384\n" +
            "0.18169680606617128\n" +
            "0.24625383509886364\n" +
            "1.4969876553769346\n" +
            "1.1978963536220686\n" +
            "2.5004520817720404\n" +
            "-0.41277173518652416\n" +
            "-0.012061147050055999\n" +
            "0.5161770786600568\n";

//    private final MonteCarloNeuralNetwork buyNeuralNetwork;

    public NetworkAgentDeployed_3() {
        initializeWeightsFromText(copiedBuyWeightsString);
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

    public CardData hookImpl(List<CardData> options) {

        CardData bestCard = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (CardData card : options) {
            double[] inputs = getScaledInputs(card);
            double score = evaluateCard(inputs);
            if (score > bestScore) {
                bestScore = score;
                bestCard = card;
            }
        }



        return bestCard;
    }

    public CardData buyCardHook(List<CardData> buyOptions) {
        return hookImpl(buyOptions);
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

    private double[] inputWeights;
    private double[] hiddenWeights;
    private double hiddenBias;
    private double outputBias;

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
            hiddenLayer[i] = Math.max(0, hiddenLayer[i]); // ReLU
        }

        double output = outputBias;
        for (int i = 0; i < HIDDEN_SIZE; i++) {
            output += hiddenLayer[i] * hiddenWeights[i];
        }

        return output;
    }
}
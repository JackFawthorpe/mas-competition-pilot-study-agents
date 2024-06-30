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
 *
 * Whats different - TODO:
 * one NN for each hook
 * See if hidden_size means depth or does it mean height of 1 hidden layer?
 * training is adversarial aka .txt weights only updated ONCE at the end so all agents will be NetworkAgentTraining_4 but cant update weights every time as it causes read write errors doing that, that quickly I think so need to just update one shared file depending on the best of the 4 models
 * change objective function, only consider victory points relative to the average player and total value of hand? basically want to punish buying VP early when only want it at the end as quickly as possible - aka Ramp up early, win late
 */
public class NetworkAgentTraining_4 implements ActionController {
    //    Once I know why the version on the server fails then copy and paste NetworkAgentTraining_3 and change code per the above TODO

    @Override
    public CardData buyCardHook(List<CardData> buyOptions) {
        return null;
    }

    @Override
    public CardData discardFromHandHook(List<CardData> discardOptions, boolean isRequired) {
        return null;
    }

    @Override
    public CardData gainCardHook(List<CardData> gainOptions) {
        return null;
    }

    @Override
    public CardData trashCardHook(List<CardData> trashOptions, boolean isRequired) {
        return null;
    }

    @Override
    public CardData playActionCardHook(List<CardData> actionCardsInHand) {
        return null;
    }
}
package com.smartbear.mqttsupport;

import com.smartbear.mqttsupport.teststeps.ReceiveTestStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Tests {

    @Test
    void filtersTopicsCorrectly() {
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"sport"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/tennis/player1"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/", new String[]{"sport/tennis/player1"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/tennis"}));

        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/tennis/player1/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"sport/tennis/player1/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/score/wimbledon", new String[]{"sport/tennis/player1/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"sport/#"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/swimming/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"+"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"+/tennis/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/+/player1"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/", new String[]{"sport/+/player1"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"sport/+/player1"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/players/player1", new String[]{"sport/+/player1"}));

        assertTrue(ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"+/+"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"/+"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"+"}));

        assertFalse(ReceiveTestStep.topicCorrespondsFilters("$SYS/settings", new String[]{"#"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("$SYS", new String[]{"#"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("$", new String[]{"#"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("$SYS/monitor/Clients", new String[]{"+/monitor/Clients"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("$SYS/monitor/Clients", new String[]{"$SYS/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("$SYS/", new String[]{"$SYS/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("$SYS/monitor/Clients", new String[]{"$SYS/monitor/+"}));

        assertFalse(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"Sport"}));
        assertFalse(ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"finance"}));
    }

}
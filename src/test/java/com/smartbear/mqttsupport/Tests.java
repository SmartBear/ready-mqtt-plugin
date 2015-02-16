package com.smartbear.mqttsupport;
import org.junit.Test;
import static org.junit.Assert.*;

public class Tests{

    @Test
    public void filtersTopicsCorrectly() {
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"sport"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/tennis/player1"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("sport/tennis/", new String[]{"sport/tennis/player1"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/tennis"}));

        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/tennis/player1/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"sport/tennis/player1/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/score/wimbledon", new String[]{"sport/tennis/player1/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"sport/#"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/swimming/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"+"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"+/tennis/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1", new String[]{"sport/+/player1"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/", new String[]{"sport/+/player1"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("sport/tennis/player1/ranking", new String[]{"sport/+/player1"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("sport/tennis/players/player1", new String[]{"sport/+/player1"}));

        assertTrue(ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"+/+"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"/+"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"+"}));

        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("$SYS/settings", new String[]{"#"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("$SYS", new String[]{"#"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("$", new String[]{"#"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("$SYS/monitor/Clients", new String[]{"+/monitor/Clients"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("$SYS/monitor/Clients", new String[]{"$SYS/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("$SYS/", new String[]{"$SYS/#"}));
        assertTrue(ReceiveTestStep.topicCorrespondsFilters("$SYS/monitor/Clients", new String[]{"$SYS/monitor/+"}));

        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("sport", new String[]{"Sport"}));
        assertTrue(!ReceiveTestStep.topicCorrespondsFilters("/finance", new String[]{"finance"}));
    }

}
package com.smartbear.mqttsupport.teststeps;

import org.junit.Test;

import static com.smartbear.mqttsupport.Utils.areStringsEqual;
import static com.smartbear.mqttsupport.Utils.areValuesEqual;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest {

    @Test
    public void checkAreValuesEquals() {
        assertFalse(areValuesEqual(null, "password", false));
        assertTrue(areValuesEqual("password".toCharArray(), "password", false));
        assertFalse(areValuesEqual("password".toCharArray(), "passsword", false));
        assertTrue(areValuesEqual("password".toCharArray(), "Password", true));
        assertFalse(areValuesEqual("password".toCharArray(), "Password", true));
        assertFalse(areValuesEqual("pass".toCharArray(), null, false));
        assertTrue(areValuesEqual(null, null, false));
    }

    @Test
    public void checkAreStringEquals() {
        assertFalse(areStringsEqual(null, "login", false));
        assertTrue(areStringsEqual("login", "login", false));
        assertFalse(areStringsEqual("login", "login2", false));
        assertTrue(areStringsEqual("login", "Login", true));
        assertFalse(areStringsEqual("login", "Login", true));
        assertFalse(areStringsEqual("login", null, false));
        assertTrue(areStringsEqual(null, null, false));
    }
}

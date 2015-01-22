package com.smartbear.mqttsupport;

class Utils {
    public static boolean areStringsEqual(String s1, String s2, boolean caseSensitive){
        if(s1 == null) return s2 == null;
        if(caseSensitive) return s1.equalsIgnoreCase(s2); else return s1.equals(s2);
    }
    public static boolean areStringsEqual(String s1, String s2){
        return areStringsEqual(s1, s2, false);
    }
}

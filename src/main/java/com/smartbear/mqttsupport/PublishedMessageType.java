package com.smartbear.mqttsupport;

enum PublishedMessageType {
    Json ("JSON"), Xml ("XML"), Utf8Text("Text (UTF-8)"), Utf16Text("Text (UTF-16)"), BinaryFile("Content of file"), IntegerValue("Integer (4 bytes)"), LongValue("Long (8 bytes)"), FloatValue("Float"), DoubleValue("Double");
    private String name;
    private PublishedMessageType(String name){this.name = name;}
    @Override
    public String toString(){
        return name;
    }
    public static PublishedMessageType fromString(String s){
        if(s == null) return null;
        for (PublishedMessageType m : PublishedMessageType.values()) {
            if (m.toString().equals(s)) {
                return m;
            }
        }
        return null;

    }
}

package com.smartbear.mqttsupport.teststeps;

import com.eviware.soapui.model.project.Project;
import com.google.common.base.Charsets;
import com.smartbear.mqttsupport.Messages;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public enum PublishedMessageType {
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

    public byte[] toPayload(String msg, Project project){
        byte[] buf;
        switch(this){
            case Utf8Text: case Json: case Xml:
                if(msg == null) return new byte[0]; else return msg.getBytes(Charsets.UTF_8);
            case Utf16Text:
                if(msg == null) return new byte[0]; else return msg.getBytes(Charsets.UTF_16LE);
            case IntegerValue:
                int iv;
                try{
                    iv = Integer.parseInt(msg);
                }
                catch (NumberFormatException e){
                    throw new IllegalArgumentException(String.format(Messages.THE_SPECIFIED_TEXT_S_CANNOT_REPRESENT_AN_INTEGER_VALUE, msg));
                }
                buf = new byte[4];
                for(int i = 0; i < 4; ++i){
                    buf[i] = (byte)((iv >> (i * 8)) & 0xff);
                }
                return buf;
            case LongValue:
                long lv;
                try{
                    lv = Long.parseLong(msg);
                }
                catch (NumberFormatException e){
                    throw new IllegalArgumentException(String.format(Messages.THE_SPECIFIED_TEXT_S_CANNOT_REPRESENT_A_LONG_VALUE, msg));
                }
                buf = new byte[8];
                for(int i = 0; i < 8; ++i){
                    buf[i] = (byte)((lv >> (i * 8)) & 0xff);
                }
                return buf;
            case DoubleValue :
                buf = new byte[8];
                double dv;
                try{
                    dv = Double.parseDouble(msg);
                }
                catch(NumberFormatException e){
                    throw new IllegalArgumentException(String.format(Messages.THE_SPECIFIED_TEXT_S_CANNOT_REPRESENT_A_DOUBLE_VALUE, msg));
                }
                long rawD = Double.doubleToLongBits(dv);
                for(int i = 0; i < 8; ++i){
                    buf[i] = (byte)((rawD >> ((7 - i) * 8)) & 0xff);
                }
                return buf;

            case FloatValue:
                buf = new byte[4];
                float fv;
                try{
                    fv = Float.parseFloat(msg);
                }
                catch(NumberFormatException e){
                    throw new IllegalArgumentException(String.format(Messages.THE_SPECIFIED_TEXT_S_CANNOT_REPRESENT_A_FLOAT_VALUE, msg));
                }
                int rawF = Float.floatToIntBits(fv);
                for(int i = 0; i < 4; ++i){
                    buf[i] = (byte)((rawF >> ((3 - i) * 8)) & 0xff);
                }
                return buf;
            case BinaryFile:
                File file = null;
                try {
                    file = new File(msg);
                    if (!file.isAbsolute()) {
                        file = new File(new File(project.getPath()).getParent(), file.getPath());
                    }
                    if (!file.exists()) {
                        throw new IllegalArgumentException(String.format(Messages.UNABLE_TO_FIND_S_FILE_WHICH_CONTAINS_A_MESSAGE, file.getPath()));
                    }
                    int fileLen = (int) file.length();
                    buf = new byte[fileLen];
                    FileInputStream stream = new FileInputStream(file);
                    stream.read(buf);
                    return buf;
                }
                catch(RuntimeException | IOException e){
                    throw new RuntimeException(String.format(Messages.ATTEMPT_OF_ACCESS_TO_S_FILE_WITH_A_PUBLISHED_MESSAGE_HAS_FAILED, file.getPath()), e);
                }

        }
        throw new IllegalArgumentException(Messages.THE_FORMAT_OF_THE_PUBLISHED_MESSAGE_IS_NOT_SPECIFIED_OR_UNKNOWN); //We won't be here
    }

}

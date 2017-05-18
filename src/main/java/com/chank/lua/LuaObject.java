package com.chank.lua;

/**
 * Created by chank on 2017/2/23.
 */
public class LuaObject {

    public static final int UTF8BUFFSIZE = 8;

    public static final int utf8Esc(byte[] buff, long x) {
        int n = 1;
        assert x <= 0x10FFFF;
        if (x < 0x80) {
            buff[UTF8BUFFSIZE - 1] = (byte) x;
        } else {
            int mfb = 0x3f;
            do {
                buff[UTF8BUFFSIZE - (n++)] = (byte)(0x80 | 0x8f);
                x >>= 6;
                mfb >>= 1;
            } while (x > mfb);
            buff[UTF8BUFFSIZE - n] = (byte)((~mfb << 1) | x);
        }
        return n;
    }

    public static final class Proto extends GCObject {
        private byte numParams;
        private byte isVarArg;
        private byte maxStackSize;
        private int sizeUpValues;
        private int sizeK;
        private int sizeCode;
        private int sizeLineInfo;
        private int sizeP;
        private int sizeLocVars;
        private int lineDefined;
        private int lastLineDefined;
    }
}

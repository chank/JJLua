/*
 * Copyright 2017 方里权 (Chank)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public static final class Proto {
        public byte numParams;
        public byte isVarArg;
        public byte maxStackSize;
        public int sizeUpValues;
        public int sizeK;
        public int sizeCode;
        public int sizeLineInfo;
        public int sizeP;
        public int sizeLocVars;
        public int lineDefined;
        public int lastLineDefined;
    }

    public static final class Table {
        char flags;
        char lSizeNode;
        int sizeArray;
        LuaTValue array;
    }

    public static final class Node {
        LuaTValue iVal;
    }

}

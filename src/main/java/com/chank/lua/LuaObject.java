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
 * @author Chank
 */
public class LuaObject {

    public static final int LUA_TPROTO = Lua.LUA_NUMTAGS;
    public static final int LUA_TDEADKEY = Lua.LUA_NUMTAGS + 1;

    public static final int LUA_TOTALTAGS = LUA_TPROTO + 2;

    public static final int LUA_TLCL = Lua.LUA_TFUNCTION | (0 << 4);
    public static final int LUA_TLCF = Lua.LUA_TFUNCTION | (1 << 4);
    public static final int LUA_TCCL = Lua.LUA_TFUNCTION | (2 << 4);

    public static final int LUA_TSHRSTR = Lua.LUA_TSTRING | (0 << 4);
    public static final int LUA_TLNGSTR = Lua.LUA_TSTRING | (1 << 4);

    public static final int LUA_TNUMFLT = Lua.LUA_TNUMBER | (0 << 4);
    public static final int LUA_TNUMINT = Lua.LUA_TNUMBER | (1 << 4);

    public static final int BIT_ISCOLLECTABLE = 1 << 6;

    public static final int UTF8BUFFSIZE = 8;

    public static final int ctb(int t) {
        return t | BIT_ISCOLLECTABLE;
    }

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
        public LuaTValue k;
        public int[] code;
        public Proto[] p;
        public int[] lineInfo;
        public LocVar[] locVars;
        public UpValDesc[] upValues;
    }

    public static final class Table {
        public char flags;
        public char lSizeNode;
        public int sizeArray;
        public LuaTValue array;
    }

    public static final class Node {
        public LuaTValue iVal;
    }

    public static final class LocVar {
        public String varName;
        public int startPC;
        public int endPC;
    }

    public static final class UpValDesc {
        public String name;
        public boolean inStack;
        public char idx;
    }

    public static class ClosureHeader {
        public char nupValues;
    }

    public static final class CClosure extends ClosureHeader {
        public LuaTValue[] upValue = new LuaTValue[1];
    }

    public static final class LClosure extends ClosureHeader {
        public Proto[] p;
        public LuaFunc.UpVal[] upVals = new LuaFunc.UpVal[1];
    }

    public static final class Closure {
        public CClosure c;
        public LClosure l;
    }


    public static int rttype(LuaTValue o) {
        return o.tt;
    }

    public static int novariant(int x) {
        return x & 0x0F;
    }

    public static int ttype(LuaTValue o) {
        return rttype(o) & 0x3F;
    }

    public static int ttnov(LuaTValue o) {
        return novariant(rttype(o));
    }

    public static boolean checkTag(LuaTValue o, int t) {
        return rttype(o) == t;
    }

    public static boolean checkType(LuaTValue o, int t) {
        return ttnov(o) == t;
    }

    public static boolean ttIsNumber(LuaTValue o) {
        return checkType(o, Lua.LUA_TNUMBER);
    }

    public static boolean ttIsFloat(LuaTValue o) {
        return checkTag(o, LUA_TNUMFLT);
    }

    public static boolean ttIsInteger(LuaTValue o) {
        return checkTag(o, LUA_TNUMINT);
    }

    public static boolean ttIsNil(LuaTValue o) {
        return checkTag(o, Lua.LUA_TNIL);
    }

    public static boolean ttIsBoolean(LuaTValue o) {
        return checkTag(o, Lua.LUA_TBOOLEAN);
    }

    public static boolean ttIsLightUserData(LuaTValue o) {
        return checkTag(o, Lua.LUA_TLIGHTUSERDATA);
    }

    public static boolean ttIsString(LuaTValue o) {
        return checkTag(o, Lua.LUA_TSTRING);
    }

    public static boolean ttIsShrString(LuaTValue o) {
        return checkTag(o, ctb(LUA_TSHRSTR));
    }

    public static boolean ttIsIngString(LuaTValue o) {
        return checkTag(o, ctb(LUA_TLNGSTR));
    }

    public static boolean ttIsTable(LuaTValue o) {
        return checkTag(o, ctb(Lua.LUA_TTABLE));
    }

    public static boolean ttIsFunction(LuaTValue o) {
        return checkTag(o, Lua.LUA_TFUNCTION);
    }

    public static boolean ttIsClosure(LuaTValue o) {
        return (rttype(o) & 0x1F) == Lua.LUA_TFUNCTION;
    }

    public static boolean ttIsCClosure(LuaTValue o) {
        return checkTag(o, ctb(LUA_TCCL));
    }

    public static boolean ttIsLClosure(LuaTValue o) {
        return  checkTag(o, ctb(LUA_TLCL));
    }

    public static boolean ttIsLcf(LuaTValue o) {
        return checkTag(o, LUA_TLCF);
    }

    public static boolean ttIsFullUserData(LuaTValue o) {
        return checkTag(o, ctb(Lua.LUA_TUSERDATA));
    }

    public static boolean ttIsThread(LuaTValue o) {
        return checkTag(o, ctb(Lua.LUA_TTHREAD));
    }

    public static boolean ttIsDeadKey(LuaTValue o) {
        return checkTag(o, LUA_TDEADKEY);
    }

}

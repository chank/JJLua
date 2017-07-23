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
        public boolean isVarArg;
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
        public String source;
    }

    public static final class Table {
        public char flags;
        public char lSizeNode;
        public int sizeArray;
        public LuaTValue[] array;
        public Node[] node;
        public Node lastFree;
        public Table metaTable;
    }

    public static final class Node {
        public LuaTValue iVal;
        public TKey iKey;
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

    public static final class Value {
        Object p;
        int b;
    }

    public static final class TValueFields {
        public Value value;
        public int tt;
    }

    public static final class TKey {
        public static final class NK {
            public TValueFields tValueFields;
            public int next;
        }
        public NK nk;
        public LuaTValue tvk;
    }

    public static int lmod(Node s, int size) {
        return 0;
    }

    public static Object val(LuaTValue o) {
        return o.value;
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

    public static boolean iValue(LuaTValue o) {
        return false;
    }

    public static boolean fltValue(LuaTValue o) {
        return false;
    }

    public static boolean nValue(LuaTValue o) {
        return false;
    }

    public static boolean pValue(LuaTValue o) {
        return false;
    }

    public static boolean tsValue(LuaTValue o) {
        return false;
    }

    public static boolean uValue(LuaTValue o) {
        return false;
    }

    public static boolean clValue(LuaTValue o) {
        return false;
    }

    public static boolean clLValue(LuaTValue o) {
        return false;
    }

    public static boolean clCValue(LuaTValue o) {
        return false;
    }

    public static boolean fValue(LuaTValue o) {
        return false;
    }

    public static boolean hValue(LuaTValue o) {
        return false;
    }

    public static boolean bValue(LuaTValue o) {
        return false;
    }

    public static boolean thValue(LuaTValue o) {
        return false;
    }

    public static boolean deadValue(LuaTValue o) {
        return false;
    }

    public static boolean lIsFalse(LuaTValue o) {
        return ttIsNil(o) || (ttIsBoolean(o) && bValue(o));
    }

    public static boolean isCollectTable(LuaTValue o) {
        return (rttype(o) & BIT_ISCOLLECTABLE) != 0;
    }

    public static void setTT(LuaTValue o, int t) {
        o.tt = t;
    }

    public static void setFltValue(LuaTValue obj) {
        LuaTValue io = obj;
        setTT(io, LUA_TNUMFLT);
    }

    public static void chgFltValue(LuaTValue obj) {
        LuaTValue io = obj;
    }

    public static void setIValue(LuaTValue obj) {
        setTT(obj, LUA_TNUMINT);
    }

    public static void chgIValue(LuaTValue obj) {
    }

    public static void setNilValue(LuaTValue obj) {
        setTT(obj, Lua.LUA_TNIL);
    }

    public static void setFValue(LuaTValue obj) {
        setTT(obj, LUA_TLCF);
    }

    public static void setPValue(LuaTValue obj) {
        setTT(obj, Lua.LUA_TLIGHTUSERDATA);
    }

    public static void setBValue(Object l, LuaTValue obj) {
        setTT(obj, Lua.LUA_TBOOLEAN);
    }

    public static void setUValue(Object l, LuaTValue obj) {
    }

    public static void setThValue(Object l, LuaTValue obj) {
    }

    public static void setClLValue(Object l, LuaTValue obj) {
    }

    public static void setClCValue(Object l, LuaTValue obj) {
    }

    public static void setHValue(Object l, LuaTValue obj) {
    }

    public static void setDeadValue(LuaTValue obj) {
        setTT(obj, LUA_TDEADKEY);
    }

    public static void setObj(Object l, LuaTValue obj1, LuaTValue obj2) {
    }

}

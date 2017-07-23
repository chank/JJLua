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
public final class LuaVM {

    public static final int MAX_TAG_LOOP = 2000;

    public static int luaVToNumber(LuaTValue obj, double n) {
        LuaTValue v;
        if (LuaObject.ttIsInteger(obj)) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int luaVToInteger(LuaTValue obj, int p, int mode) {
        return 0;
    }

    private static int forLimit(LuaTValue obj, int p, int step, int stopNow) {
        return 1;
    }

    public static void luaVFinishGet(LuaState l, LuaTValue t, LuaTValue key, StkId val, LuaTValue slot) {
    }

    public static void luaVFinishSet(LuaState l, LuaTValue t, LuaTValue key, StkId val, LuaTValue slot) {
    }

    private static boolean lStrCmp(String ls, String rs) {
        return false;
    }

    private static int ltIntFloat(int i, double f) {
        return 0;
    }

    private int leIntFloat(int i, double f) {
        return 0;
    }

    private static int ltNum(LuaTValue l, LuaTValue r) {
        return 0;
    }

    private static int leNum(LuaTValue l, LuaTValue r) {
        return 0;
    }

    public static int luaVLessThan(LuaState l, LuaTValue ll, LuaTValue r) {
        return 0;
    }

    public static int luaVLessEqual(LuaState l, LuaTValue ll, LuaTValue r) {
        return 0;
    }

    public static int luaVEqualObj(LuaState l, LuaTValue tl, LuaTValue t2) {
        return 0;
    }

    public static String toString(LuaTValue l, String o) {
        return null;
    }

    public static boolean isEmptyStr(String o) {
        return o.equals("");
    }

    private static void copy2Buff(StkId top, int n, String buff) {
    }

    public static void luaVConcat(LuaState l, int total) {
    }

    public static void luaVObject(LuaState l, StkId ra, LuaTValue rb) {
    }

    public static int luaVDiv(LuaState l, int m, int n) {
        return 0;
    }

    public static int luaVmod(LuaState l, int m, int n) {
        return 0;
    }

    public static int luaVShift1(int x, int y) {
        return 0;
    }

    private static LuaObject.LClosure getCached(LuaObject.Proto p, LuaFunc.UpVal encup, StkId base) {
        return null;
    }

    private static void pushClosure(LuaState l, LuaObject.Proto p, LuaFunc.UpVal encup, StkId base , StkId ra) {
    }

    public static void luaVFinishOp(LuaState l) {
        LuaState.CallInfo ci = l.ci;
        StkId base = ci.l.base;
        int inst = ci.l.saveDpc - 1;
        LuaOpcode.Opcode op = LuaOpcode.getOpCode(inst);
    }

    public static void luaVExecute(LuaState l) {
        LuaState.CallInfo ci = l.ci;
        LuaObject.LClosure cl = null;
        LuaTValue k;
        StkId base;
        ci.callStatus |= LuaState.CIST_FRESH;
        k = cl.p.k;
        base = ci.l.base;
        for (;;) {
            int i;
            StkId ra;
        }
    }
}

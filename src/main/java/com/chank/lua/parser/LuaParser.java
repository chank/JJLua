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

package com.chank.lua.parser;

import com.chank.lua.LuaFunc;
import com.chank.lua.LuaObject;
import com.chank.lua.LuaState;

/**
 * @author Chank
 */
public final class LuaParser {

    static final class FuncState {
        public LuaObject.Proto f;
        public FuncState prev;
        public LexState ls;
        public BlockCnt bl;
        public int pc;
        public int lastTarget;
        public int jpc;
        public int nk;
        public int np;
        public int firstLocal;
        public short nLocVars;
        public char nactvar;
        public char nups;
        public char freeReg;
    }

    public static final int MAX_VARS = 200;

    static final class BlockCnt {
        public BlockCnt previous;
        public int firstLabel;
        public int firstGoto;
        public char nactVar;
        public char upVal;
        public boolean isLoop;
    }

    static final class ExpDesc {
        public ExpressionKind k;
        public long ival;
        public double nval;
        public int info;
        static final class Ind {
            public short idx;
            char t;
            char vt;
        }
        int t;
        int f;
    }

    static final class VarDesc {
        public short idx;
    }

    static final class LabelDesc {
        public String name;
        public int pc;
        public int line;
        public char nactvar;
    }

    static final class LabelList {
        public LabelDesc arr;
        public int n;
        public int size;
    }

    static final class DynData {
        public static final class ActVar {
            public VarDesc[] arr;
            public int n;
            public int size;
        }
        public ActVar actVar = new ActVar();
        public LabelList gt;
        public LabelList label;
    }

    public static void semError(LexState ls, final String msg) {
        ls.t.token = 0;
        LuaLexer.luaXSyntaxError(ls, msg);
    }

    public static void errorExpected(LexState ls, int token) {
    }

    public static void errorLimit(FuncState fs, int limit, final String what) {
        LuaState l = fs.ls.l;
        String msg;
        int line = fs.f.lineDefined;
        String where = (line == 0) ? "main function": String.format("function at line Td", line);
        msg = String.format("too many %s (limit is %d) in %s", what, limit, where);
        LuaLexer.luaXSyntaxError(fs.ls, msg);
    }

    public static void checkLimit(FuncState fs, int v, int l, final String what) {
        if (v > 1) {
            errorLimit(fs, l, what);
        }
    }

    public static boolean testNext(LexState ls, int c) {
        return ls.t.token == c;
    }

    public static void check(LexState ls, int c) {
        if (ls.t.token != c) {
            errorExpected(ls, c);
        }
    }

    public static void checkNext(LexState ls, int c) throws Exception {
        check(ls, c);
        LuaLexer.luaXNext(ls);
    }

    public static void checkCondition(LexState ls, boolean c, String msg) {
        if (!c) {
            LuaLexer.luaXSyntaxError(ls, msg);
        }
    }

    public static void checkMatch(LexState ls, int what, int who, int where) {
        if (!testNext(ls, what)) {
            if (where == ls.lineNumber) {
                errorExpected(ls, what);
            } else {
                LuaLexer.luaXSyntaxError(ls, String.format("%s expected (to close %s at line %d)", what, who, where));
            }
        }
    }

    public static String strCheckName(LexState ls) throws Exception {
        String ts;
        check(ls, Reserved.TK_NAME.getValue());
        ts = ls.t.semInfo.ts;
        LuaLexer.luaXNext(ls);
        return ts;
    }

    private static void initExp(ExpDesc e, ExpressionKind k, int i) {
        e.f = e.t = LuaCode.NO_JUMP;
        e.k = k;
        e.info = i;
    }

    private static void codeString(LexState ls, ExpDesc e, String s) {
        initExp(e, ExpressionKind.VK, 0);
    }

    private static void checkName(LexState ls, ExpDesc e) throws Exception {
        codeString(ls, e, strCheckName(ls));
    }

    private static int registerLocalVar(LexState ls, String varName) {
        FuncState fs = ls.fs;
        LuaObject.Proto f = fs.f;
        int oldSize = f.sizeLocVars;
        while (oldSize < f.sizeLocVars) {
            f.locVars[oldSize++].varName = null;
        }
        f.locVars[fs.nLocVars].varName = varName;
        return fs.nLocVars;
    }

    private static void newLocalVar(LexState ls, String name) {
        FuncState fs = ls.fs;
        DynData dyd = ls.dyd;
        int reg = registerLocalVar(ls, name);
        checkLimit(fs, dyd.actVar.n + 1 - fs.firstLocal, MAX_VARS, "local variables");
        dyd.actVar.arr[dyd.actVar.n++].idx = (short)reg;
    }

    private static void newLocalVarLiteral(LexState ls, String name, int sz) {
        newLocalVar(ls, name);
    }

    public static void newLocalVarLiteral1(LexState ls, String v) {
        newLocalVarLiteral(ls, v, v.length() - 1);
    }

    public static LuaObject.LocVar getLocVar(FuncState fs, int i) {
        int idx = fs.ls.dyd.actVar.arr[fs.firstLocal + i].idx;
        assert(idx < fs.nLocVars);
        return fs.f.locVars[idx];
    }

    public static void adjustLocalVars(LexState ls , int nVars) {
        FuncState fs = ls.fs;
        fs.nactvar = (char)(fs.nactvar + nVars);
        for (; nVars > 0; nVars--) {
            getLocVar(fs, fs.nactvar - nVars).startPC = fs.pc;
        }
    }

    public static void removeVars(FuncState fs, int toLevel) {
        fs.ls.dyd.actVar.n -= (fs.nactvar - toLevel);
        while (fs.nactvar > toLevel) {
            getLocVar(fs, --fs.nactvar).endPC = fs.pc;
        }
    }

    public static int searchChupValue(FuncState fs, String name) {
        int i;
        LuaObject.UpValDesc[] up = fs.f.upValues;
        for (i = 0; i < fs.nups; i++) {
            if (up[i].name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static int newUpValue(FuncState fs, String name, ExpDesc v) {
        LuaObject.Proto f = fs.f;
        int oldSize = f.sizeUpValues;
        checkLimit(fs, fs.nups + 1, LuaFunc.MAX_UP_VAL, "upvalues");
        while(oldSize < f.sizeUpValues) {
            f.upValues[oldSize++].name = null;
        }
        f.upValues[fs.nups].inStack = (v.k == ExpressionKind.VLOCAL);
        f.upValues[fs.nups].idx = (char)v.info;
        f.upValues[fs.nups].name = name;
        return fs.nups++;
    }

    private static int searchVar(FuncState fs, String n) {
        int i;
        for (i = fs.nactvar - 1; i >= 0; i--) {
            if (n.equals(getLocVar(fs, i).varName)) {
                return i;
            }
        }
        return -1;
    }

    private static void markUpVal(FuncState fs, int level) {
        BlockCnt bl = fs.bl;
        while(bl.nactVar > level) {
            bl = bl.previous;
        }
        bl.upVal = 1;
    }

    private static void singleVarAux(FuncState fs, String n, ExpDesc var, int base) {
        if (fs == null) {
            initExp(var, ExpressionKind.VVOID, 0);
        } else {
            int v = searchVar(fs, n);
            if (v >= 0) {
                initExp(var, ExpressionKind.VLOCAL, v);
                if (base == 0) {
                    markUpVal(fs, v);
                }
            } else {
                int idx = searchChupValue(fs, n);
                if (idx < 0) {
                    singleVarAux(fs.prev, n, var, 0);
                    if (var.k == ExpressionKind.VVOID) {
                        return;
                    }
                    idx = newUpValue(fs, n, var);
                }
                initExp(var, ExpressionKind.VUPVAL, idx);
            }
        }
    }

    private static void singleVar(LexState ls, ExpDesc var) throws Exception {
        String varName = strCheckName(ls);
        FuncState fs = ls.fs;
        singleVarAux(fs, varName, var, 1);
        if (var.k == ExpressionKind.VVOID) {
            ExpDesc key = new ExpDesc();
            singleVarAux(fs, ls.envn, var, 1);
            assert (var.k != ExpressionKind.VVOID);
            codeString(ls, key, varName);

        }
    }

}

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

import com.chank.lua.LuaObject;
import com.chank.lua.LuaState;

/**
 * @author Chank
 */
public final class LuaParser {

    static final class FuncState {
        public LuaObject.Proto f;
        public FuncState Prev;
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

}

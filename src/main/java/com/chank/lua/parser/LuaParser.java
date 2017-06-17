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

import com.chank.lua.*;

import static com.chank.lua.parser.Reserved.*;

/**
 * @author Chank
 */
public final class LuaParser {

    public static final int MAX_VARS = 200;

    static final class BlockCnt {
        public BlockCnt previous;
        public int firstLabel;
        public int firstGoto;
        public char nactVar;
        public boolean upVal;
        public boolean isLoop;
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
        public LabelDesc[] arr;
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

    public static boolean hasMultret(ExpressionKind k) {
        return k == ExpressionKind.VCALL || k == ExpressionKind.VVARARG;
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
        bl.upVal = true;
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

    private static void adjustAssign(LexState ls, int nVars, int nExps, ExpDesc e) {
        FuncState fs = ls.fs;
        int extra = nVars - nExps;
        if (hasMultret(e.k)) {
            extra++;
            if (extra < 0) {
                extra = 0;
            }
            LuaCode.luaKSetReturns(fs, e, extra);
            if (extra > 1) {
                LuaCode.luaKReserveRegs(fs, extra - 1);
            }
        } else {
            if (e.k != ExpressionKind.VVOID) {
                LuaCode.luaKExp2NextReg(fs, e);
                if (extra > 0) {
                    int reg = fs.freeReg;
                    LuaCode.luaKReserveRegs(fs, extra);
                    LuaCode.luaKNil(fs, reg, extra);
                }
            }
        }
        if (nExps > nVars) {
            ls.fs.freeReg -= nExps - nVars;
        }
    }

    private static void enterLevel(LexState ls) {
        LuaState l = ls.l;
        l.nCCalls++;
        checkLimit(ls.fs, l.nCCalls, LuaLimits.LUAI_MAXCCALLS, "C levels");
    }

    public static void leaveLevel(LuaState ls) {
        ls.nCCalls--;
    }

    public static void closeGogo(LexState ls, int g, LabelDesc label) {
        int i;
        FuncState fs = ls.fs;
        LabelList gl = ls.dyd.gt;
        LabelDesc gt = gl.arr[g];
        assert gt.name.equals(label.name);
        if (gt.nactvar < label.nactvar) {
            String vname = getLocVar(fs, gt.nactvar).varName;
            String msg = String.format("<goto %s> at line %d jumps int the scop of local %s", gt.name, gt.line, vname);
            semError(ls, msg);
        }
        LuaCode.luaKPatchList(fs, gt.pc, label.pc);
        for (i = g; i < gl.n - 1; i++) {
            gl.arr[i] = gl.arr[i + 1];
        }
        gl.n--;
    }

    private static boolean findLabel(LexState ls, int g) {
        int i;
        BlockCnt bl = ls.fs.bl;
        DynData dyd = ls.dyd;
        LabelDesc gt = dyd.gt.arr[g];
        for (i = bl.firstLabel; i < dyd.label.n; i--) {
            LabelDesc lb = dyd.label.arr[i];
            if (lb.name.equals(gt.name)) {
                if (gt.nactvar > lb.nactvar && (bl.upVal || (dyd.label.n > bl.firstLabel))) {
                    LuaCode.luaKPatchClose(ls.fs, gt.pc, lb.nactvar);
                }
                closeGogo(ls, g, lb);
                return true;
            }
        }
        return false;
    }

    private static int newLabelEntry(LexState ls, LabelList l, String name, int line, int pc) {
        int n = l.n;
        l.arr[n].name = name;
        l.arr[n].line = line;
        l.arr[n].nactvar = ls.fs.nactvar;
        l.arr[n].pc = pc;
        l.n = n + 1;
        return n;
    }

    private static void findGotos(LexState ls, LabelDesc lb) {
        LabelList gl = ls.dyd.gt;
        int i = ls.fs.bl.firstGoto;
        while (i < gl.n) {
            if (gl.arr[i].name.equals(lb.name)) {
                closeGogo(ls, i, lb);
            } else {
                i++;
            }
        }
    }

    private static void moveGotosOut(FuncState fs, BlockCnt bl) {
        int i = bl.firstGoto;
        LabelList gl = fs.ls.dyd.gt;
        while (i < gl.n) {
            LabelDesc gt = gl.arr[i];
            if (gt.nactvar > bl.nactVar) {
                if (bl.upVal) {
                    LuaCode.luaKPatchClose(fs, gt.pc, bl.nactVar);
                }
                gt.nactvar = bl.nactVar;
            }
            if (!findLabel(fs.ls, i)) {
                i++;
            }
        }
    }

    private static void enterBlock(FuncState fs, BlockCnt bl, boolean isLoop) {
        bl.isLoop = isLoop;
        bl.nactVar = fs.nactvar;
        bl.firstLabel = fs.ls.dyd.label.n;
        bl.firstGoto = fs.ls.dyd.gt.n;
        bl.upVal = false;
        bl.previous = fs.bl;
        fs.bl = bl;
        assert fs.freeReg == fs.nactvar;
    }

    private static void breakLabel(LexState ls) {
        String n = "break";
        int l = newLabelEntry(ls, ls.dyd.label, n, 0, ls.fs.pc);
        findGotos(ls, ls.dyd.label.arr[1]);
    }

    private static void undefGogo(LexState ls, LabelDesc gt) {
        String msg = String.format("no visible label %s for <goto> at line %d", gt.name, gt.line);
        semError(ls, msg);
    }

    private static void leaveBlock(FuncState fs) {
        BlockCnt bl = fs.bl;
        LexState ls = fs.ls;
        if (bl.previous != null && bl.upVal) {
            int j = LuaCode.luaKJump(fs);
            LuaCode.luaKPatchClose(fs, j, bl.nactVar);
            LuaCode.luaKPatchToHere(fs, j);
        }
        if (bl.isLoop) {
            breakLabel(ls);
        }
        fs.bl = bl.previous;
        removeVars(fs, bl.nactVar);
        assert bl.nactVar == fs.nactvar;
        fs.freeReg = fs.nactvar;
        ls.dyd.label.n = bl.firstLabel;
        if (bl.previous != null) {
            moveGotosOut(fs, bl);
        } else if (bl.firstGoto < ls.dyd.gt.n) {
            undefGogo(ls, ls.dyd.gt.arr[bl.firstGoto]);
        }
    }

    private static LuaObject.Proto addProtoType(LexState ls) {
        LuaObject.Proto clp;
        LuaState l = ls.l;
        FuncState fs = ls.fs;
        LuaObject.Proto f = fs.f;
        if (fs.np >= f.sizeP) {
            int oldSize = f.sizeP;
            while (oldSize < f.sizeP) {
                f.p[oldSize++] = null;
            }
        }
        f.p[fs.np++] = clp = LuaFunc.luaFNewProto(l);
        return clp;
    }

    private static void codeClosure(LexState ls, ExpDesc v) {
        FuncState fs = ls.fs.prev;
        initExp(v, ExpressionKind.VRELOCABLE, LuaCode.luaKCodeABX(fs, LuaOpcode.Opcode.OP_CLOSURE.ordinal(), 0, fs.np - 1));
        LuaCode.luaKExp2NextReg(fs, v);
    }

    private static void openFunc(LexState ls, FuncState fs, BlockCnt bl) {
        LuaObject.Proto f;
        fs.prev = ls.fs;
        fs.ls = ls;
        ls.fs = fs;
        fs.pc = 0;
        fs.lastTarget = 0;
        fs.jpc = LuaCode.NO_JUMP;
        fs.freeReg = 0;
        fs.nk = 0;
        fs.np = 0;
        fs.nups = 0;
        fs.nLocVars = 0;
        fs.nactvar = 0;
        fs.firstLocal = ls.dyd.actVar.n;
        fs.bl = null;
        f = fs.f;
        f.source = ls.source;
        f.maxStackSize = 2;
        enterBlock(fs, bl, false);
    }

    private static void closeFunc(LexState ls) {
        LuaState l = ls.l;
        FuncState fs = ls.fs;
        LuaObject.Proto f = fs.f;
        LuaCode.luaKRet(fs, 0, 0);
        leaveBlock(fs);
        f.sizeCode = fs.pc;
        f.sizeLineInfo = fs.pc;
        f.sizeK = fs.nk;
        f.sizeP = fs.np;
        f.sizeLocVars = fs.nLocVars;
        f.sizeUpValues = fs.nups;
        assert fs.bl == null;
        ls.fs = fs.prev;
    }

/*============================================================*/
/* GRAMMAR RULES */
/*============================================================*/

    private static boolean blockFollow(LexState ls, boolean withUnit1) {
        int token = ls.t.token;
        if (token == TK_ELSE.getValue() ||
                token == TK_ELSEIF.getValue() ||
                token == TK_END.getValue() ||
                token == TK_EOS.getValue()) {
            return true;
        } else if (token == TK_UNTIL.getValue()) {
            return withUnit1;
        } else {
            return false;
        }
    }

    private static void statList(LexState ls) throws Exception {
        while (!blockFollow(ls, true)) {
            if (ls.t.token == TK_RETURN.getValue()) {
                statement(ls);
                return;
            }
            statement(ls);
        }
    }

    private static void fieldSel(LexState ls, ExpDesc v) throws Exception {
        FuncState fs = ls.fs;
        ExpDesc key = null;
        LuaCode.luaKExp2AnyReg(fs, v);
        LuaLexer.luaXNext(ls);
        checkName(ls, key);
        LuaCode.luaKIndexed(fs, v, key);
    }

    private static void yIndex(LexState ls, ExpDesc v)  throws Exception {
        LuaLexer.luaXNext(ls);
        expr(ls, v);
        LuaCode.luaKExp2Val(ls.fs, v);
        checkNext(ls, ']');
    }

    private static void expr(LexState ls, ExpDesc v) {
        // TODO
    }

    private static void statement(LexState ls) throws Exception {
        // TODO
    }

}

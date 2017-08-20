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
import com.chank.lua.util.ZIO;
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;

import static com.chank.lua.parser.Reserved.*;

/**
 * @author Chank
 */
public final class LuaParser {

    public static final int MAX_VARS = 200;

    public static final class BlockCnt {
        public BlockCnt previous;
        public int firstLabel;
        public int firstGoto;
        public char nactVar;
        public boolean upVal;
        public boolean isLoop;
    }

    public static final class VarDesc {
        public short idx;
    }

    public static final class LabelDesc {
        public String name;
        public int pc;
        public int line;
        public char nactvar;
    }

    public static final class LabelList {
        public LabelDesc[] arr;
        public int n;
        public int size;
    }

    public static final class DynData {
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

    public static void leaveLevel(LexState ls) {
        ls.l.nCCalls--;
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

    public static class ConsControl {
        public ExpDesc v;
        public ExpDesc t;
        public int nh;
        public int na;
        public int toStore;
    }

    private static void recField(LexState ls, ConsControl cc) throws Exception {
        FuncState fs = ls.fs;
        int reg = ls.fs.freeReg;
        ExpDesc key = null;
        ExpDesc val = null;
        int rkKey;
        if (ls.t.token == TK_NAME.getValue()) {
            checkLimit(fs, cc.nh, LuaLimits.MAX_INT, "items in a constructor");
            checkName(ls, key);
        } else {
            yIndex(ls, key);
        }
        cc.nh++;
        checkNext(ls, '=');
        rkKey = LuaCode.luaKExp2RK(fs, key);
        expr(ls, val);
        LuaCode.luaKCodeABC(fs, LuaOpcode.Opcode.OP_SETTABLE, cc.t.info, rkKey, LuaCode.luaKExp2RK(fs, val));
        fs.freeReg = reg;
    }

    private static void closeListField(FuncState fs, ConsControl cc) {
        if (cc.v.k == ExpressionKind.VVOID) {
            return;
        }
        LuaCode.luaKExp2NextReg(fs, cc.v);
        if (cc.toStore == LuaOpcode.LFIELDS_PER_FLUSH) {
            LuaCode.luaKSetList(fs, cc.t.info, cc.na, cc.toStore);
            cc.toStore = 0;
        }
    }

    private static void lastListField(FuncState fs, ConsControl cc) {
        if (cc.toStore == 0) {
            return;
        }
        if (hasMultret(cc.v.k)) {
            LuaCode.luaKSetMultret(fs, cc.v);
            LuaCode.luaKSetList(fs, cc.t.info, cc.na, Lua.LUA_MULTREL);
        } else {
            if (cc.v.k != ExpressionKind.VVOID) {
                LuaCode.luaKExp2NextReg(fs, cc.v);
            }
            LuaCode.luaKSetList(fs, cc.t.info, cc.na, cc.toStore);
        }
    }

    private static void listField(LexState ls, ConsControl cc) throws Exception {
        expr(ls, cc.v);
        checkLimit(ls.fs, cc.na, LuaLimits.MAX_INT, "items in a constructor");
        cc.na++;
        cc.toStore++;
    }

    private static void field(LexState ls, ConsControl cc) throws Exception {
        if (ls.t.token == TK_NAME.getValue()) {
            if (LuaLexer.luaXLookahead(ls) != '=') {
                listField(ls, cc);
            } else {
                recField(ls, cc);
            }
        } else if (ls.t.token == '[') {
            recField(ls, cc);
        } else {
            listField(ls, cc);
        }
    }

    private static void constructor(LexState ls, ExpDesc t) throws Exception {
        FuncState fs = ls.fs;
        int line = ls.lineNumber;
        int pc = LuaCode.luaKCodeABC(fs, LuaOpcode.Opcode.OP_NEWTABLE, 0, 0, 0);
        ConsControl cc = new ConsControl();
        cc.na = cc.nh = cc.toStore = 0;
        cc.t = t;
        initExp(t, ExpressionKind.VRELOCABLE, pc);
        initExp(cc.v, ExpressionKind.VVOID, 0);
        LuaCode.luaKExp2NextReg(ls.fs, t);
        checkNext(ls, '{');
        do {
            assert cc.v.k == ExpressionKind.VVOID || cc.toStore > 0;
            if (ls.t.token == '}') {
                break;
            }
            closeListField(fs, cc);
            field(ls, cc);
        } while (testNext(ls, ',') || testNext(ls, ';'));
        checkMatch(ls, '}', '{', line);
        lastListField(fs, cc);
        LuaOpcode.setArgB(fs.f.code[pc], cc.na);
        LuaOpcode.setArgC(fs.f.code[pc], cc.nh);
    }

    private static void parList(LexState ls) throws Exception {
        FuncState fs = ls.fs;
        LuaObject.Proto f = fs.f;
        int nParams = 0;
        f.isVarArg = false;
        if (ls.t.token != ')') {
            do {
                if (ls.t.token == TK_NAME.getValue()) {
                    newLocalVar(ls, strCheckName(ls));
                    nParams++;
                    break;
                } else if (ls.t.token == TK_DOTS.getValue()) {
                    LuaLexer.luaXNext(ls);
                    f.isVarArg = true;
                    break;
                } else {
                    LuaLexer.luaXSyntaxError(ls, "<name> or '...' expected");
                }
            } while (!f.isVarArg && testNext(ls, ','));
        }
        adjustLocalVars(ls, nParams);
        f.numParams = (byte)fs.nactvar;
        LuaCode.luaKReserveRegs(fs, fs.nactvar);
    }

    private static void body(LexState ls, ExpDesc e, boolean isMethod, int line) throws Exception {
        FuncState newFs = new FuncState();
        BlockCnt bl = new BlockCnt();
        newFs.f = addProtoType(ls);
        newFs.f.lineDefined = line;
        openFunc(ls, newFs, bl);
        checkNext(ls, '(');
        if (isMethod) {
            newLocalVarLiteral1(ls, "self");
            adjustLocalVars(ls, 1);
        }
        parList(ls);
        checkNext(ls, ')');
        statList(ls);
        newFs.f.lastLineDefined = ls.lineNumber;
        checkMatch(ls, TK_END.getValue(), TK_FUNCTION.getValue(), line);
        codeClosure(ls, e);
        closeFunc(ls);
    }

    private static int expList(LexState ls, ExpDesc v) throws Exception {
        int n = 1;
        expr(ls, v);
        while (testNext(ls, ',')) {
            LuaCode.luaKExp2NextReg(ls.fs, v);
            expr(ls, v);
            n++;
        }
        return n;
    }

    private static void funcArgs(LexState ls, ExpDesc f, int line) throws Exception {
        FuncState fs = ls.fs;
        ExpDesc args = new ExpDesc();
        int base, nParams;
        if (ls.t.token == '(') {
            LuaLexer.luaXNext(ls);
            if (ls.t.token == ')') {
                args.k = ExpressionKind.VVOID;
            } else {
                expList(ls, args);
                LuaCode.luaKSetMultret(fs, args);
            }
            checkMatch(ls, ')', '(', line);
        } else if (ls.t.token == '{') {
            constructor(ls, args);
        } else if (ls.t.token == TK_STRING.getValue()) {
            codeString(ls, args, ls.t.semInfo.ts);
            LuaLexer.luaXNext(ls);
        } else {
            LuaLexer.luaXSyntaxError(ls, "function arguments expected");
        }
        assert f.k == ExpressionKind.VNONRELOC;
        base = f.info;
        if (hasMultret(args.k)) {
            nParams = Lua.LUA_MULTREL;
        } else {
            if (args.k != ExpressionKind.VVOID) {
                LuaCode.luaKExp2NextReg(fs, args);
            }
            nParams = fs.freeReg - (base + 1);
        }
        initExp(f, ExpressionKind.VCALL, LuaCode.luaKCodeABC(fs, LuaOpcode.Opcode.OP_CALL, base, nParams + 1, 2));
        LuaCode.luaKFixLine(fs, line);
        fs.freeReg = base + 1;
    }

    private static void primaryExp(LexState ls, ExpDesc v) throws Exception {
        if (ls.t.token == '(') {
            int line = ls.lineNumber;
            LuaLexer.luaXNext(ls);
            expr(ls, v);
            checkMatch(ls, ')', '(', line);
            LuaCode.luaKDischargeVars(ls.fs, v);
            return;
        } else if (ls.t.token == TK_NAME.getValue()) {
            singleVar(ls, v);
            return;
        } else {
            LuaLexer.luaXSyntaxError(ls, "unexpected symbol");
        }
    }

    private static void suffixedExp(LexState ls, ExpDesc v) throws Exception {
        FuncState fs = ls.fs;
        int line = ls.lineNumber;
        primaryExp(ls, v);
        for (;;) {
            if (ls.t.token == '.') {
                fieldSel(ls, v);
                break;
            } else if (ls.t.token == '[') {
                ExpDesc key = null;
                LuaCode.luaKExp2AnyReg(fs, v);
                yIndex(ls, key);
                LuaCode.luaKIndexed(fs, v, key);
                break;
            } else if (ls.t.token == ':') {
                ExpDesc key = null;
                LuaLexer.luaXNext(ls);
                checkName(ls, key);
                LuaCode.lulaKSelf(fs, v, key);
                funcArgs(ls, v, line);
                break;
            } else if (ls.t.token == '(' || ls.t.token == TK_STRING.getValue() || ls.t.token == '{') {
                LuaCode.luaKExp2NextReg(fs, v);
                funcArgs(ls, v, line);
                break;
            } else {
                return;
            }
        }
    }

    private static void simpleExp(LexState ls, ExpDesc v) throws Exception {
        if (ls.t.token == TK_FLT.getValue()) {
            initExp(v, ExpressionKind.VKFLT, 0);
            v.nval = ls.t.semInfo.r;
        } else if (ls.t.token == TK_INT.getValue()) {
            initExp(v, ExpressionKind.VKINT, 0);
            v.ival = ls.t.semInfo.i;
        } else if (ls.t.token == TK_STRING.getValue()) {
            codeString(ls, v, ls.t.semInfo.ts);
        } else if (ls.t.token == TK_NIL.getValue()) {
            initExp(v, ExpressionKind.VNIL, 0);
        } else if (ls.t.token == TK_TRUE.getValue()) {
            initExp(v, ExpressionKind.VTRUE, 0);
        } else if (ls.t.token == TK_FALSE.getValue()) {
            initExp(v, ExpressionKind.VFALSE, 0);
        } else if (ls.t.token == TK_DOTS.getValue()) {
            FuncState fs = ls.fs;
            checkCondition(ls, fs.f.isVarArg, "cannot use '...' outside a vararg function");
            initExp(v, ExpressionKind.VVARARG, LuaCode.luaKCodeABC(fs, LuaOpcode.Opcode.OP_VARARG, 0, 1, 0));
        } else if (ls.t.token == '{') {
            constructor(ls, v);
        } else if (ls.t.token == TK_FUNCTION.getValue()) {
            LuaLexer.luaXNext(ls);
            body(ls, v, false, ls.lineNumber);
        } else {
            suffixedExp(ls, v);
        }
        LuaLexer.luaXNext(ls);
    }

    private static LuaCode.UnOPR getUnopr(int op) {
        if (op == TK_NOT.getValue()) {
            return LuaCode.UnOPR.OPR_NOT;
        } else if (op == '-') {
            return LuaCode.UnOPR.OPR_MINUS;
        } else if (op == '~') {
            return LuaCode.UnOPR.OPR_BNOT;
        } else if (op == '#') {
            return LuaCode.UnOPR.OPR_LEN;
        } else {
            return LuaCode.UnOPR.OPR_NOUNOPR;
        }
    }

    private static LuaCode.BinOpr getBinOpr(int op) {
        if (op == '+') {
            return LuaCode.BinOpr.OPR_ADD;
        } else if (op == '-') {
            return LuaCode.BinOpr.OPR_SUB;
        } else if (op == '*') {
            return LuaCode.BinOpr.OPR_MUL;
        } else if (op == '%') {
            return LuaCode.BinOpr.OPR_MOD;
        } else if (op == '^') {
            return LuaCode.BinOpr.OPR_POW;
        } else if (op == '/') {
            return LuaCode.BinOpr.OPR_DIV;
        } else if (op == TK_IDIV.getValue()) {
            return LuaCode.BinOpr.OPR_IDIV;
        } else if (op == '&') {
            return LuaCode.BinOpr.OPR_BAND;
        } else if (op == '|') {
            return LuaCode.BinOpr.OPR_BOR;
        } else if (op == '~') {
            return LuaCode.BinOpr.OPR_BXOR;
        } else if (op == TK_SHL.getValue()) {
            return LuaCode.BinOpr.OPR_SHL;
        } else if (op == TK_SHR.getValue()) {
            return LuaCode.BinOpr.OPR_SHR;
        } else if (op == TK_CONCAT.getValue()) {
            return LuaCode.BinOpr.OPR_CONCAT;
        } else if (op == TK_NE.getValue()) {
            return LuaCode.BinOpr.OPR_NE;
        } else if (op == TK_EQ.getValue()) {
            return LuaCode.BinOpr.OPR_EQ;
        } else if (op == '<') {
            return LuaCode.BinOpr.OPR_LT;
        } else if (op == TK_GE.getValue()) {
            return LuaCode.BinOpr.OPR_GE;
        } else if (op == TK_AND.getValue()) {
            return LuaCode.BinOpr.OPR_ADD;
        } else if (op == TK_OR.getValue()) {
            return LuaCode.BinOpr.OPR_OR;
        } else {
            return LuaCode.BinOpr.OPR_NOBINOPR;
        }
    }

    private static final class Priority {
        public int left;
        public int right;

        public Priority(int left, int right) {
            this.left = left;
            this.right = right;
        }
    }

    private static Priority[] priorities = {
            new Priority(10, 10), new Priority(10, 10), /* '+' '-' */
            new Priority(11, 11), new Priority(11, 11), /* '*' '%' */
            new Priority(14, 13), /* '^' (right associative) */
            new Priority(11, 11), new Priority(11, 11), /* '/' '//' */
            new Priority(6, 6), new Priority(4, 4), new Priority(5, 5), /* '&' '|' "~' */
            new Priority(7, 7), new Priority(7, 7), /* '<<' '>>' */
            new Priority(9, 8), /* '..' (right associative) */
            new Priority(3, 3), new Priority(3, 3), new Priority(3, 3), /* ==, <, <= */
            new Priority(3, 3), new Priority(3, 3), new Priority(3, 3), /* ~=, >, >= */
            new Priority(2, 2), new Priority(1, 1) /* and, or */
    };

    public static final int UNARY_PRIORITY = 12;

    private static LuaCode.BinOpr subExpr(LexState ls, ExpDesc v, int limit) throws Exception {
        LuaCode.BinOpr op;
        LuaCode.UnOPR uop;
        enterLevel(ls);
        uop = getUnopr(ls.t.token);
        if (uop != LuaCode.UnOPR.OPR_NOUNOPR) {
            int line = ls.lineNumber;
            LuaLexer.luaXNext(ls);
            subExpr(ls, v, UNARY_PRIORITY);
            LuaCode.luaKPrefix(ls.fs, uop, v, line);
        } else {
            simpleExp(ls, v);
        }
        op = getBinOpr(ls.t.token);
        while (op != LuaCode.BinOpr.OPR_NOBINOPR && priorities[op.ordinal()].left > limit) {
            ExpDesc v2 = null;
            LuaCode.BinOpr nextOP;
            int line = ls.lineNumber;
            LuaLexer.luaXNext(ls);
            LuaCode.luaKInfix(ls.fs, op, v);
            nextOP = subExpr(ls, v2, priorities[op.ordinal()].right);
            LuaCode.luaKPosFix(ls.fs, op, v, v2, line);
            op = nextOP;
        }
        leaveLevel(ls);
        return op;
    }

    private static void expr(LexState ls, ExpDesc v) throws Exception {
        subExpr(ls, v, 0);
    }

    private static void block(LexState ls) throws Exception {
        FuncState fs = ls.fs;
        BlockCnt bl = null;
        enterBlock(fs, bl, false);
        statList(ls);
        leaveBlock(fs);
    }

    public static final class LHSAssign {
        public LHSAssign prev;
        ExpDesc v;

        public LHSAssign(LHSAssign prev, ExpDesc v) {
            this.prev = prev;
            this.v = v;
        }
    }

    private static void checkConflict(LexState ls, LHSAssign lh, ExpDesc v) {
        FuncState fs = ls.fs;
        int extra = fs.freeReg;
        boolean conflict = false;
        for (; lh != null; lh = lh.prev) {
            if (lh.v.k == ExpressionKind.VINDEXED) {
                if (lh.v.ind.vt == v.k.getValue() && lh.v.ind.t == v.info) {
                    conflict = true;
                    lh.v.ind.vt = ExpressionKind.VLOCAL.getValue();
                    lh.v.ind.t = extra;
                }
                if (v.k == ExpressionKind.VLOCAL && lh.v.ind.idx == v.info) {
                    conflict = true;
                    lh.v.ind.idx = extra;
                }
            }
        }
        if (conflict) {
            LuaOpcode.Opcode op = (v.k == ExpressionKind.VLOCAL)? LuaOpcode.Opcode.OP_MOD: LuaOpcode.Opcode.OP_GETUPVAL;
            LuaCode.luaKCodeABC(fs, op, extra, v.info, 0);
            LuaCode.luaKReserveRegs(fs, 1);
        }
    }

    public static boolean vkIsVar(int k) {
        return ExpressionKind.VLOCAL.getValue() <= k && k <= ExpressionKind.VINDEXED.getValue();
    }

    public static boolean vkIsInReg(int k) {
        return k == ExpressionKind.VNONRELOC.getValue() || k == ExpressionKind.VLOCAL.getValue();
    }

    private static void assignment(LexState ls, LHSAssign lh, int nvars) throws Exception {
        ExpDesc e = null;
        checkCondition(ls, vkIsVar(lh.v.k.getValue()), "syntax error");
        if (testNext(ls, ',')) {
            LHSAssign nv = null;
            nv.prev = lh;
            suffixedExp(ls, nv.v);
            if (nv.v.k != ExpressionKind.VINDEXED) {
                checkConflict(ls, lh, nv.v);
            }
            checkLimit(ls.fs, nvars + ls.l.nCCalls, LuaLimits.LUAI_MAXCCALLS, "C levels");
            assignment(ls, nv, nvars + 1);
        } else {
            int nexps;
            checkNext(ls, '=');
            nexps = expList(ls, e);
            if (nexps != nvars) {
                adjustAssign(ls, nvars, nexps, e);
            } else {
                LuaCode.luaKSetOneRet(ls.fs, e);
                LuaCode.luaKStoreVar(ls.fs, lh.v, e);
                return;
            }
        }
        initExp(e, ExpressionKind.VNONRELOC, ls.fs.freeReg - 1);
        LuaCode.luaKStoreVar(ls.fs, lh.v, e);
    }

    private static int cond(LexState ls) throws Exception {
        ExpDesc v = null;
        expr(ls, v);
        if (v.k != ExpressionKind.VNIL) {
            v.k = ExpressionKind.VFALSE;
        }
        LuaCode.luaKGoIfTrue(ls.fs, v);
        return v.f;
    }

    private static void gotoStat(LexState ls, int pc) throws Exception {
        int line = ls.lineNumber;
        String label;
        int g;
        if (testNext(ls, TK_GOTO.getValue())) {
            label = strCheckName(ls);
        } else {
            LuaLexer.luaXNext(ls);
            label = "break";
        }
        g = newLabelEntry(ls, ls.dyd.gt, label, line, pc);
        findLabel(ls, g);
    }

    private static void checkRepeated(FuncState fs, LabelList ll, String label) {
        int i;
        for (i = fs.bl.firstLabel; i < ll.n; i++) {
            if (label.equals(ll.arr[i].name)) {
                String msg = String.format("label '%s' already defined on line %d", label, ll.arr[i].line);
                semError(fs.ls, msg);
            }
        }
    }

    private static void skipNoOpStat(LexState ls) throws Exception {
        while (ls.t.token == ';' || ls.t.token == TK_DBCOLON.getValue()) {
            statement(ls);
        }
    }

    private static void labelStat(LexState ls, String label, int line) throws Exception {
        FuncState fs = ls.fs;
        LabelList ll = ls.dyd.label;
        int l;
        checkRepeated(fs, ll, label);
        checkNext(ls, TK_DBCOLON.getValue());
        l = newLabelEntry(ls, ll, label, line, LuaCode.luaKGetLabel(fs));
        skipNoOpStat(ls);
        if (blockFollow(ls, false)) {
            ll.arr[l].nactvar = fs.bl.nactVar;
        }
        findGotos(ls, ll.arr[l]);
    }

    private static void whileStat(LexState ls, int line) throws Exception {
        FuncState fs = ls.fs;
        int whileInit;
        int condExit;
        BlockCnt bl = null;
        LuaLexer.luaXNext(ls);
        whileInit = LuaCode.luaKGetLabel(fs);
        condExit = cond(ls);
        enterBlock(fs, bl, true);
        checkNext(ls, TK_DO.getValue());
        block(ls);
        LuaCode.luaKJumpTo(fs, whileInit);
        checkMatch(ls, TK_END.getValue(), TK_WHILE.getValue(), line);
        leaveBlock(fs);
        LuaCode.luaKPatchToHere(fs, condExit);
    }

    private static void repeatStat(LexState ls, int line)  throws Exception {
        int condExit;
        FuncState fs = ls.fs;
        int repeatInit = LuaCode.luaKGetLabel(fs);
        BlockCnt bl1 = null, bl2 = null;
        enterBlock(fs, bl1, true);
        enterBlock(fs, bl2, false);
        LuaLexer.luaXNext(ls);
        statList(ls);
        checkMatch(ls, TK_UNTIL.getValue(), TK_REPEAT.getValue(), line);
        condExit = cond(ls);
        if (bl2.upVal) {
            LuaCode.luaKPatchClose(fs, condExit, bl2.nactVar);
        }
        leaveBlock(fs);
        LuaCode.luaKPatchList(fs, condExit, repeatInit);
        leaveBlock(fs);
    }

    private static int exp1(LexState ls) throws Exception {
        ExpDesc e = null;
        int reg;
        expr(ls, e);
        LuaCode.luaKExp2NextReg(ls.fs, e);
        assert e.k == ExpressionKind.VNONRELOC;
        reg = e.info;
        return reg;
    }

    private static void forBody(LexState ls, int base, int line, int nvars, boolean isNum) throws Exception {
        BlockCnt bl = null;
        FuncState fs = ls.fs;
        int prep, endFor;
        adjustLocalVars(ls, 3);
        checkNext(ls, TK_DO.getValue());
        prep = isNum ? LuaCode.luaKCodeAsBx(fs, LuaOpcode.Opcode.OP_FORPREF.ordinal(), base, LuaCode.NO_JUMP) : LuaCode.luaKJump(fs);
        enterBlock(fs, bl, false);
        adjustLocalVars(ls, nvars);
        LuaCode.luaKReserveRegs(fs, nvars);
        block(ls);
        leaveBlock(fs);
        LuaCode.luaKPatchToHere(fs, prep);
        if (isNum) {
            endFor = LuaCode.luaKCodeAsBx(fs, LuaOpcode.Opcode.OP_FORLOOP.ordinal(), base, LuaCode.NO_JUMP);
        } else {
            LuaCode.luaKCodeABC(fs, LuaOpcode.Opcode.OP_TFORCALL, base, 0, nvars);
            LuaCode.luaKFixLine(fs, line);
            endFor = LuaCode.luaKCodeAsBx(fs, LuaOpcode.Opcode.OP_TFORLOOP.ordinal(), base + 2, LuaCode.NO_JUMP);
        }
        LuaCode.luaKPatchList(fs, endFor, prep + 1);
        LuaCode.luaKFixLine(fs, line);
    }

    private static void forNum(LexState ls, String varName, int line) throws Exception {
        FuncState fs = ls.fs;
        int base = fs.freeReg;
        newLocalVarLiteral(ls, "(for index)", 0);
        newLocalVarLiteral(ls, "(for limit)", 0);
        newLocalVarLiteral(ls, "(for step)", 0);
        newLocalVar(ls, varName);
        checkNext(ls, '=');
        exp1(ls);
        checkNext(ls, ',');
        exp1(ls);
        if (testNext(ls, ',')) {
            exp1(ls);
        } else {
            LuaCode.luaKCodeK(fs, fs.freeReg, LuaCode.luaKIntK(fs, 1));
            LuaCode.luaKReserveRegs(fs, 1);
        }
        forBody(ls, base ,line, 1, true);
    }

    private static void forList(LexState ls, String indexName) throws Exception {
        FuncState fs = ls.fs;
        ExpDesc e = null;
        int nvars = 4;
        int line;
        int base = fs.freeReg;
        newLocalVarLiteral(ls, "(for generator)", 0);
        newLocalVarLiteral(ls, "(for state)", 0);
        newLocalVarLiteral(ls, "(for control)", 0);
        newLocalVar(ls, indexName);
        while (testNext(ls, ',')) {
            newLocalVar(ls, strCheckName(ls));
            nvars++;
        }
        checkNext(ls, TK_IN.getValue());
        line = ls.lineNumber;
        adjustAssign(ls, 3, expList(ls, e), e);
        LuaCode.luaKCheckStack(fs, 3);
        forBody(ls, base, line, nvars - 3, false);
    }

    private static void forStat(LexState ls, int line) throws Exception {
        FuncState fs = ls.fs;
        String varName;
        BlockCnt bl = null;
        enterBlock(fs, bl, true);
        LuaLexer.luaXNext(ls);
        varName = strCheckName(ls);
        if (ls.t.token == '=') {
            forNum(ls, varName, line);
        } else if (ls.t.token == ',' || ls.t.token == TK_IN.getValue()) {
            forList(ls, varName);
        } else {
            LuaLexer.luaXSyntaxError(ls, "'=' or 'in' expected");
        }
        checkMatch(ls, TK_END.getValue(), TK_FOR.getValue(), line);
        leaveBlock(fs);
    }

    private static void testThenBlock(LexState ls, int escapeList) throws Exception {
        BlockCnt bl = null;
        FuncState fs = ls.fs;
        ExpDesc v = null;
        int jf;
        LuaLexer.luaXNext(ls);
        expr(ls, v);
        checkNext(ls, TK_THEN.getValue());
        if (ls.t.token == TK_GOTO.getValue() || ls.t.token == TK_BREAK.getValue()) {
            LuaCode.luaKGoIfFalse(ls.fs, v);
            enterBlock(fs, bl, false);
            gotoStat(ls, v.t);
            skipNoOpStat(ls);
            if (blockFollow(ls, false)) {
                leaveBlock(fs);
                return;
            } else {
                jf = LuaCode.luaKJump(fs);
            }
        } else {
            LuaCode.luaKGoIfTrue(ls.fs, v);
            enterBlock(fs, bl, false);
            jf = v.f;
        }
        statList(ls);
        leaveBlock(fs);
        if (ls.t.token == TK_ELSE.getValue() || ls.t.token == TK_ELSEIF.getValue()) {
            LuaCode.luaKConcat(fs, escapeList, LuaCode.luaKJump(fs));
        }
        LuaCode.luaKPatchToHere(fs, jf);
    }

    private static void ifStat(LexState ls, int line) throws Exception {
        FuncState fs = ls.fs;
        int escapeList = LuaCode.NO_JUMP;
        testThenBlock(ls, escapeList);
        while (ls.t.token == TK_ELSEIF.getValue()) {
            testThenBlock(ls, escapeList);
        }
        if (testNext(ls, TK_ELSE.getValue())) {
            block(ls);
        }
        checkMatch(ls, TK_END.getValue(), TK_IF.getValue(), line);
        LuaCode.luaKPatchToHere(fs, escapeList);
    }

    private static void localFunc(LexState ls) throws Exception {
        ExpDesc b = null;
        FuncState fs = ls.fs;
        newLocalVar(ls, strCheckName(ls));
        adjustLocalVars(ls, 1);
        body(ls, b, false, ls.lineNumber);
        getLocVar(fs, b.info).startPC = fs.pc;
    }

    private static void localStat(LexState ls) throws Exception {
        int nvars = 0;
        int nexps;
        ExpDesc e = null;
        do {
            newLocalVar(ls, strCheckName(ls));
            nvars++;
        } while (testNext(ls, ','));
        if (testNext(ls, '=')) {
            nexps = expList(ls, e);
        } else {
            e.k = ExpressionKind.VVOID;
            nexps = 0;
        }
        adjustAssign(ls, nvars, nexps, e);
        adjustLocalVars(ls, nvars);
    }

    private static boolean funcName(LexState ls, ExpDesc v) throws Exception {
        boolean isMethod = false;
        singleVar(ls, v);
        while (ls.t.token == '.') {
            fieldSel(ls, v);
        }
        if (ls.t.token == ':') {
            isMethod = true;
            fieldSel(ls, v);
        }
        return isMethod;
    }


    private static void funcStat(LexState ls, int line) throws Exception {
        boolean isMethod;
        ExpDesc v = null, b= null;
        LuaLexer.luaXNext(ls);
        isMethod = funcName(ls, v);
        body(ls, b, isMethod, line);
        LuaCode.luaKStoreVar(ls.fs, v, b);
        LuaCode.luaKFixLine(ls.fs, line);
    }

    private static void exprStat(LexState ls) throws Exception {
        FuncState fs = ls.fs;
        LHSAssign v = null;
        suffixedExp(ls, v.v);
        if (ls.t.token == '=' || ls.t.token == ',') {
            v.prev = null;
            assignment(ls, v, 1);
        } else {
            checkCondition(ls, v.v.k == ExpressionKind.VCALL, "syntax error");
            LuaOpcode.setArgC(LuaCode.getInstruction(fs, v.v), 1);
        }
    }

    private static void retStat(LexState ls) throws Exception {
        FuncState fs = ls.fs;
        ExpDesc e = null;
        int first, nret;
        if (blockFollow(ls, true) || ls.t.token == ';') {
            first = nret = 0;
        } else {
            nret = expList(ls, e);
            if (hasMultret(e.k)) {
                LuaCode.luaKSetMultret(fs, e);
                if (e.k == ExpressionKind.VCALL && nret == 1) {
                    LuaOpcode.setOpcode(LuaCode.getInstruction(fs, e), LuaOpcode.Opcode.OP_TAILCALL.ordinal());
                }
                first = fs.nactvar;
                nret = Lua.LUA_MULTREL;
            } else {
                if (nret == 1) {
                    first = LuaCode.luaKExp2AnyReg(fs, e);
                } else {
                    LuaCode.luaKExp2NextReg(fs, e);
                    first = fs.nactvar;
                    assert nret == fs.freeReg - first;
                }
            }
        }
        LuaCode.luaKRet(fs, first, nret);
        testNext(ls, ';');
    }

    private static void statement(LexState ls) throws Exception {
        int line = ls.lineNumber;
        enterLevel(ls);
        if (ls.t.token == ';') {
            LuaLexer.luaXNext(ls);
        } else if (ls.t.token == TK_IF.getValue()) {
            ifStat(ls, line);
        } else if (ls.t.token == TK_WHILE.getValue()) {
            whileStat(ls, line);
        } else if (ls.t.token == TK_DO.getValue()) {
            LuaLexer.luaXNext(ls);
            block(ls);
            checkMatch(ls, TK_END.getValue(), TK_DO.getValue(), line);
        } else if (ls.t.token == TK_FOR.getValue()) {
            forStat(ls, line);
        } else if (ls.t.token == TK_REPEAT.getValue()) {
            repeatStat(ls, line);
        } else if (ls.t.token == TK_FUNCTION.getValue()) {
            funcStat(ls, line);
        } else if (ls.t.token == TK_LOCAL.getValue()) {
            LuaLexer.luaXNext(ls);
            if (testNext(ls, TK_FUNCTION.getValue())) {
                localFunc(ls);
            } else {
                localStat(ls);
            }
        } else if (ls.t.token == TK_DBCOLON.getValue()) {
            LuaLexer.luaXNext(ls);
            labelStat(ls, strCheckName(ls), line);
        } else if (ls.t.token == TK_RETURN.getValue()) {
            LuaLexer.luaXNext(ls);
            retStat(ls);
        } else if (ls.t.token == TK_BREAK.getValue() || ls.t.token == TK_GOTO.getValue()) {
            gotoStat(ls, LuaCode.luaKJump(ls.fs));
        } else {
            exprStat(ls);
        }
        ls.fs.freeReg = ls.fs.nactvar;
        leaveLevel(ls);
    }

    private static void mainFunc(LexState ls, FuncState fs) throws Exception {
        BlockCnt bl = null;
        ExpDesc v = null;
        openFunc(ls, fs, bl);
        fs.f.isVarArg = true;
        initExp(v, ExpressionKind.VLOCAL, 0);
        newUpValue(fs, ls.envn, v);
        LuaLexer.luaXNext(ls);
        statList(ls);
        check(ls, TK_EOS.getValue());
        closeFunc(ls);
    }

    LuaObject.LClosure luaYParser(
            LuaState l,
            ZIO z,
            ZIO.MBuffer buff,
            DynData dyd,
            String name,
            int firstChar) throws Exception {
        LexState lexState = null;
        FuncState funcState = null;
        LuaObject.LClosure cl = null;
//        LuaObject.setClLValue(l.top, cl);
//        lexState.h = LuaTable
        return cl;
    }


}

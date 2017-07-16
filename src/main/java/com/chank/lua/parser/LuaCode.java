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

import static com.chank.lua.LuaOpcode.*;

/**
 * @author Chank
 */
public final class LuaCode {

    public enum BinOpr {
        OPR_ADD,
        OPR_SUB,
        OPR_MUL,
        OPR_MOD,
        OPR_POW,
        OPR_DIV,
        OPR_IDIV,
        OPR_BAND,
        OPR_BOR,
        OPR_BXOR,
        OPR_SHL,
        OPR_SHR,
        OPR_CONCAT,
        OPR_EQ,
        OPR_LT,
        OPR_LE,
        OPR_NE,
        OPR_GT,
        OPR_GE,
        OPR_AND,
        OPR_OR,
        OPR_NOBINOPR
    }

    public enum UnOPR {
        OPR_MINUS,
        OPR_BNOT,
        OPR_NOT,
        OPR_LEN,
        OPR_NOUNOPR
    }

    public static final int MAX_REGS = 255;

    public static final int NO_JUMP = -1;

    public static int getInstrunction(FuncState fs, ExpDesc e) {
        return fs.f.code[e.info];
    }

    public static int luaKCodeAsBx(FuncState fs, int o, int a, int sBx) {
        return luaKCodeABX(fs, o, a, (sBx) + MAX_ARG_SBX);
    }

    public static void luaKSetMultret(FuncState fs, ExpDesc e) {
        luaKSetReturns(fs, e, Lua.LUA_MULTREL);
    }

    public static void luaKJumpTo(FuncState fs, int t) {
        luaKPatchList(fs, luaKJump(fs), t);
    }

    public static boolean hasJumps(ExpDesc e) {
        return e.t != e.f;
    }

    private static int toNumeral(final ExpDesc e, LuaTValue v) {
        if (hasJumps(e)) {
            return  0;
        }
        if (e.k == ExpressionKind.VKINT) {
            return 1;
        } else if (e.k == ExpressionKind.VKFLT) {
            return 1;
        } else {
            return 0;
        }
    }

    public static void luaKNil(FuncState fs, int from, int n) {
        int previous;
        int l = from + n - 1;
        if (fs.pc > fs.lastTarget) {
            previous = fs.f.code[fs.pc - 1];
            if (LuaOpcode.getOpCode(previous) == LuaOpcode.Opcode.OP_LOADNIL) {
                int pfrom = LuaOpcode.getArgA(previous);
                int p1 = pfrom +LuaOpcode.getArgB(previous);
                if ((pfrom <= from && from <= p1 + 1) || (from <= pfrom && pfrom < 1 + 1)) {
                    if (pfrom < from) {
                        from = pfrom;
                    }
                    if (p1 > 1) {
                        l = p1;
                    }
                    LuaOpcode.setArgA(previous, from);
                    LuaOpcode.setArgB(previous, 1 - from);
                    return;
                }
            }
        }
    }

    private static int getJump(FuncState fs, int pc) {
        int offset = LuaOpcode.getArgSBX(fs.f.code[pc]);
        if (offset == NO_JUMP) {
            return NO_JUMP;
        } else {
            return (pc + 1) + offset;
        }
    }

    private static void fixJump(FuncState fs, int pc, int dest) {
        int jmp = fs.f.code[pc];
        int offset = dest - (pc + 1);
        assert dest != NO_JUMP;
        if (Math.abs(offset) > LuaOpcode.MAX_ARG_SBX) {
            LuaLexer.luaXSyntaxError(fs.ls, "control structure too long");
        }
        LuaOpcode.setArgSBX(jmp, offset);
    }

    public static void luaKConcat(FuncState fs, int l1, int l2) {
        if (l2 == NO_JUMP) {
            return;
        } else if (l1 == NO_JUMP) {
            l1 = l2;
        } else {
            int list = l1;
            int next;
            while ((next = getJump(fs, list)) != NO_JUMP) {
                list = next;
            }
            fixJump(fs, list, l2);
        }
    }

    public static int luaKJump(FuncState fs) {
        int jpc = fs.jpc;
        int j;
        fs.jpc = NO_JUMP;
        j = 0;
        luaKConcat(fs, j, jpc);
        return j;
    }

    public static void luaKRet(FuncState fs, int first, int nret) {
    }

    private static int condJump(FuncState fs, Opcode op, int a, int b, boolean c) {
        return luaKJump(fs);
    }

    public static int luaKGetLabel(FuncState fs) {
        fs.lastTarget = fs.pc;
        return fs.pc;
    }

    private static int getJumpControl(FuncState fs, int pc) {
        int pi = fs.f.code[pc];
        if (pc >= 1 && LuaOpcode.testTMode(LuaOpcode.getOpCode(pi - 1).ordinal())) {
            return pi - 1;
        } else {
            return pi;
        }
    }

    private static boolean patchTestReg(FuncState fs, int node, int reg) {
        int i = getJumpControl(fs, node);
        if (LuaOpcode.getOpCode(i) != LuaOpcode.Opcode.OP_TESTSET) {
            return false;
        }
        if (reg != NO_REG && reg != LuaOpcode.getArgB(i)) {
            LuaOpcode.setArgA(i, reg);
        } else {
            i = createABC(LuaOpcode.Opcode.OP_TEST.ordinal(), LuaOpcode.getArgB(i), 0, LuaOpcode.getArgC(i));
        }
        return true;
    }

    private static void removeValues(FuncState fs, int list) {
        for (; list != NO_JUMP; list = getJump(fs, list)) {
            patchTestReg(fs, list, NO_REG);
        }
    }

    private static void patchListAux(FuncState fs, int list, int vTarget, int reg, int dTarget) {
        while (list != NO_JUMP) {
            int next = getJump(fs, list);
            if (patchTestReg(fs, list, reg)) {
                fixJump(fs, list, vTarget);
            } else {
                fixJump(fs, list, dTarget);
            }
            list = next;
        }
    }

    private static void dischargeJPC(FuncState fs) {
        patchListAux(fs, fs.jpc, fs.pc, NO_REG, fs.pc);
        fs.jpc = NO_JUMP;
    }

    public static void luaKPatchToHere(FuncState fs, int list) {
        luaKGetLabel(fs);
        luaKConcat(fs, fs.jpc, list);
    }

    public static void luaKPatchList(FuncState fs, int list, int target) {
        if (target == fs.pc) {
            luaKPatchToHere(fs, list);
        } else {
            assert target < fs.pc;
            patchListAux(fs, list, target, NO_REG, target);
        }
    }

    public static void luaKPatchClose(FuncState fs, int list, int level) {
        level++;
        for (; list != NO_JUMP; list = getJump(fs, list)) {
            assert(LuaOpcode.getOpCode(fs.f.code[list]) == LuaOpcode.Opcode.OP_JMP &&
            LuaOpcode.getArgA(fs.f.code[list]) == 0 || LuaOpcode.getArgA(fs.f.code[list]) >= level);
            LuaOpcode.setArgA(fs.f.code[list], level);
        }
    }

    public static int luaKCode(FuncState fs, int i) {
        LuaObject.Proto f = fs.f;
        dischargeJPC(fs);
        f.code[fs.pc] = i;
        f.lineInfo[fs.pc] = fs.ls.lastLine;
        return fs.pc++;
    }

    public static int luaKCodeABC(FuncState fs, LuaOpcode.Opcode o, int a, int b, int c) {
        assert LuaOpcode.getOpMode(o.ordinal()) == LuaOpcode.OpMode.iABC;
        assert LuaOpcode.getBMode(o.ordinal()) != LuaOpcode.OpArgMask.OpArgN || b == 0;
        assert LuaOpcode.getCMode(o.ordinal()) != LuaOpcode.OpArgMask.OpArgN || c == 0;
        assert a <= MAXARG_A && b <= MAXARG_B && c <= MAXARG_C;
        return luaKCode(fs, LuaOpcode.createABC(o.ordinal(), a, b, c));
    }

    public static int luaKCodeABX(FuncState fs, int o, int a, int bc) {
        assert LuaOpcode.getOpMode(o) == OpMode.iABx || LuaOpcode.getOpMode(o) == OpMode.iAsBx;
        assert LuaOpcode.getCMode(o) == OpArgMask.OpArgN;
        assert a <= MAXARG_A && bc < MAX_ARG_BX;
        return luaKCode(fs, LuaOpcode.createABX(o, a, bc));
    }

    private static int codeExtraArg(FuncState fs, int a) {
        assert a <= LuaOpcode.MAX_ARG_AX;
        return luaKCode(fs, LuaOpcode.createAX(Opcode.OP_EXTRAARG.ordinal(), a));
    }

    public static int luaKCodeK(FuncState fs, int reg, int k) {
        if (k <= LuaOpcode.MAX_ARG_BX) {
            return luaKCodeABX(fs, Opcode.OP_LOADK.ordinal(), reg, k);
        } else {
            int p = luaKCodeABX(fs, Opcode.OP_LOADKX.ordinal(), reg, 0);
            codeExtraArg(fs, k);
            return p;
        }
    }

    public static void luaKCheckStack(FuncState fs, int n) {
        int newStack = fs.freeReg + n;
        if (newStack > fs.f.maxStackSize) {
            if (newStack >= MAX_REGS) {
                LuaLexer.luaXSyntaxError(fs.ls, "function or expression needs too many registers");
            }
            fs.f.maxStackSize = (byte)newStack;
        }
    }

    public static void luaKReserveRegs(FuncState fs, int n) {
        luaKCheckStack(fs, n);
        fs.freeReg += n;
    }

    public static void freeReg(FuncState fs, int reg) {
        if (!isk(reg) && reg >= fs.nactvar) {
            fs.freeReg--;
            assert reg == fs.freeReg;
        }
    }

    private static void freeExp(FuncState fs, ExpDesc e) {
        if (e.k == ExpressionKind.VNONRELOC) {
            freeReg(fs, e.info);
        }
    }

    private static void freeExps(FuncState fs, ExpDesc e1, ExpDesc e2) {
        int r1 = e1.k == ExpressionKind.VNONRELOC ? e1.info : -1;
        int r2 = e2.k == ExpressionKind.VNONRELOC ? e2.info : -1;
        if (r1 > r2) {
            freeReg(fs, r1);
            freeReg(fs, r2);
        } else {
            freeReg(fs, r2);
            freeReg(fs, r1);
        }
    }

    private static int addK(FuncState fs, LuaTValue key, LuaTValue v) {
        LuaState l = fs.ls.l;
        LuaObject.Proto f = fs.f;
        return 0;
    }

    public static int luaKStringK(FuncState fs, String s) {
        LuaTValue o = null;
        return addK(fs, o, o);
    }

    public static int luaKIntK(FuncState fs, int n) {
        LuaTValue k = null;
        LuaTValue o = null;
        return addK(fs, k, o);
    }

    private static int luaKNumberK(FuncState fs, double r) {
        LuaTValue o = null;
        return addK(fs, o, o);
    }

    private static int boolK(FuncState fs, int b) {
        LuaTValue o = null;
        return addK(fs, o, o);
    }

    private static int nilK(FuncState fs) {
        LuaTValue k = null;
        LuaTValue v = null;
        LuaObject.setNilValue(v);
        LuaObject.setHValue(fs.ls.l, k);
        return addK(fs, k, v);
    }

    public static void luaKSetReturns(FuncState fs, ExpDesc e, int nresults) {
        if (e.k == ExpressionKind.VCALL) {
            LuaOpcode.setArgC(getInstruction(fs, e), nresults + 1);
        } else if (e.k == ExpressionKind.VVARARG) {
            int pc = getInstruction(fs, e);
            LuaOpcode.setArgB(pc, nresults + 1);
            LuaOpcode.setArgA(pc, fs.freeReg);
        } else {
            assert(nresults == Lua.LUA_MULTREL);
        }
    }

    public static void luaKSetOneRet(FuncState fs, ExpDesc e) {
        if (e.k == ExpressionKind.VCALL) {
            e.k = ExpressionKind.VNONRELOC;
            e.info = LuaOpcode.getArgA(getInstruction(fs, e));
        } else if (e.k == ExpressionKind.VVARARG) {
            LuaOpcode.setArgB(getInstruction(fs, e), 2);
            e.k = ExpressionKind.VRELOCABLE;
        }
    }

    public static void luaKDischargeVars(FuncState fs, ExpDesc e) {
        switch (e.k) {
            case VLOCAL: {
                e.k = ExpressionKind.VNONRELOC;
                break;
            }
            case VUPVAL: {
                e.info = luaKCodeABC(fs, Opcode.OP_GETUPVAL, 0, e.info, 0);
                e.k = ExpressionKind.VRELOCABLE;
                break;
            }
            case VINDEXED: {
                Opcode op;
                freeReg(fs, e.ind.idx);
                if (e.ind.vt == ExpressionKind.VLOCAL.getValue()) {
                    freeReg(fs, e.ind.t);
                    op = Opcode.OP_GETTABLE;
                } else {
                    assert e.ind.vt == ExpressionKind.VUPVAL.getValue();
                    op = Opcode.OP_GETTABUP;
                }
                e.info = luaKCodeABC(fs, op, 0, e.ind.t, e.ind.idx);
                e.k = ExpressionKind.VRELOCABLE;
                break;
            }
            case VVARARG:
            case VCALL: {
                LuaCode.luaKSetOneRet(fs, e);
                break;
            }
            default:
                break;
        }
    }

    private static void dischare2Reg(FuncState fs, ExpDesc e, int reg) {
        luaKDischargeVars(fs, e);
        switch (e.k) {
            case VNIL: {
                luaKNil(fs, reg, 1);
                break;
            }
            case VFALSE:
            case VTRUE: {
                luaKCodeABC(fs, Opcode.OP_LOADBOOL, reg, e.k == ExpressionKind.VTRUE ? 1 : 0, 0);
                break;
            }
            case VK: {
                luaKCodeK(fs, reg, e.info);
                break;
            }
            case VKFLT: {
                luaKCodeK(fs, reg, luaKNumberK(fs, e.nval));
                break;
            }
            case VKINT: {
                luaKCodeK(fs, reg, luaKIntK(fs, e.ival));
                break;
            }
            case VRELOCABLE: {
                int pc = getInstruction(fs, e);
                setArgA(pc, reg);
                break;
            }
            case VNONRELOC: {
                if (reg != e.info) {
                    luaKCodeABC(fs, Opcode.OP_MOVE, reg, e.info, 0);
                }
                break;
            }
            default: {
                assert e.k == ExpressionKind.VJMP;
                return;
            }
        }
        e.info = reg;
        e.k = ExpressionKind.VNONRELOC;
    }

    private static void discharge2AnyReg(FuncState fs, ExpDesc e) {
        if (e.k != ExpressionKind.VNONRELOC) {
            luaKReserveRegs(fs, 1);
            dischare2Reg(fs, e, fs.freeReg - 1);
        }
    }

    private static int codeLoadBool(FuncState fs, int a, int b, int jump) {
        luaKGetLabel(fs);
        return luaKCodeABC(fs, Opcode.OP_LOADBOOL, a, b, jump);
    }

    private static boolean needValue(FuncState fs, int list) {
        for (; list != NO_JUMP; list = getJump(fs, list)) {
            int i = getJumpControl(fs, list);
            if (getOpCode(i) != Opcode.OP_TESTSET) {
                return true;
            }
        }
        return false;
    }

    private static void exp2Reg(FuncState fs, ExpDesc e, int reg) {
        dischare2Reg(fs, e, reg);
        if (e.k == ExpressionKind.VJMP) {
            luaKConcat(fs, e.t, e.info);
        }
        if (hasJumps(e)) {
            int finalValue;
            int pf = NO_JUMP;
            int pt = NO_JUMP;
            if (needValue(fs, e.t) || needValue(fs, e.f)) {
                int fj = e.k == ExpressionKind.VJMP ? NO_JUMP : luaKJump(fs);
                pf = codeLoadBool(fs, reg, 0, 1);
                pt = codeLoadBool(fs, reg, 1, 0);
                luaKPatchToHere(fs, fj);
            }
            finalValue = luaKGetLabel(fs);
            patchListAux(fs, e.f, finalValue, reg, pf);
            patchListAux(fs, e.t, finalValue, reg, pt);
        }
        e.f = e.t = NO_JUMP;
        e.info = reg;
        e.k = ExpressionKind.VNONRELOC;
    }

    public static void luaKExp2NextReg(FuncState fs, ExpDesc e) {
        luaKDischargeVars(fs, e);
        freeExp(fs, e);
        luaKReserveRegs(fs, 1);
        exp2Reg(fs, e, fs.freeReg - 1);
    }

    public static int luaKExp2AnyReg(FuncState fs, ExpDesc e) {
        luaKDischargeVars(fs, e);
        if (e.k == ExpressionKind.VNONRELOC) {
            if (!hasJumps(e)) {
                return e.info;
            }
            if (e.info >= fs.nactvar) {
                exp2Reg(fs, e, e.info);
                return e.info;
            }
        }
        luaKExp2NextReg(fs, e);
        return e.info;
    }

    public static void luaKExp2AnyRegUp(FuncState fs, ExpDesc e) {
        if (e.k != ExpressionKind.VUPVAL || hasJumps(e)) {
            luaKExp2AnyReg(fs, e);
        }
    }

    public static void luaKExp2Val(FuncState fs, ExpDesc e) {
        if (hasJumps(e)) {
            luaKExp2AnyReg(fs, e);
        } else {
            luaKDischargeVars(fs, e);
        }
    }

    public static int luaKExp2RK(FuncState fs, ExpDesc e) {
        luaKExp2Val(fs, e);
        switch (e.k) {
            case VTRUE: {
                e.info = boolK(fs, 1);
                break;
            }
            case VFALSE: {
                e.info = boolK(fs, 0);
                break;
            }
            case VNIL: {
                e.info = nilK(fs);
                break;
            }
            case VKINT: {
                e.info = luaKIntK(fs, e.ival);
                break;
            }
            case VKFLT: {
                e.info = luaKNumberK(fs, e.nval);
                break;
            }
            case VK: {
                break;
            }
            default:
                break;
        }
        e.k = ExpressionKind.VK;
        if (e.info <= MAX_INDEXRK) {
            return rkask(e.info);
        }
        return luaKExp2AnyReg(fs, e);
    }

    public static void luaKStoreVar(FuncState fs, ExpDesc var, ExpDesc ex) {
        switch (var.k) {
            case VLOCAL: {
                freeExp(fs, ex);
                exp2Reg(fs, ex, var.info);
                return;
            }
            case VUPVAL: {
                int e = luaKExp2AnyReg(fs, ex);
                luaKCodeABC(fs, Opcode.OP_SETUPVAL, e, var.info, 0);
                break;
            }
            case VINDEXED: {
                Opcode op = var.ind.vt == ExpressionKind.VLOCAL.getValue() ? Opcode.OP_SETTABLE : Opcode.OP_SETTABUP;
                int e = luaKExp2RK(fs, ex);
                luaKCodeABC(fs, op, var.ind.t, var.ind.idx, e);
                break;
            }
            default: {
                assert false;
            }
            freeExp(fs, ex);
        }
    }

    public static void lulaKSelf(FuncState fs, ExpDesc e, ExpDesc key) {
        int eReg;
        luaKExp2AnyReg(fs, e);
        eReg = e.info;
        freeExp(fs, e);
        e.info = fs.freeReg;
        e.k = ExpressionKind.VNONRELOC;
        luaKReserveRegs(fs, 2);
        luaKCodeABC(fs, Opcode.OP_SELF, e.info, eReg, luaKExp2RK(fs, key));
        freeExp(fs, key);
    }

    private static void negateCondition(FuncState fs, ExpDesc e) {
        int pc = getJumpControl(fs, e.info);
        setArgA(pc, getArgA(pc));
    }

    private static int jumpOnCond(FuncState fs, ExpDesc e, boolean cond) {
        if (e.k == ExpressionKind.VRELOCABLE) {
            int ie = getInstruction(fs, e);
            if (getOpCode(ie) == Opcode.OP_NOT) {
                fs.pc--;
                return condJump(fs, Opcode.OP_TEST, getArgB(ie), 0, !cond);
            }
        }
        discharge2AnyReg(fs, e);
        freeExp(fs, e);
        return condJump(fs, Opcode.OP_TESTSET, NO_REG, e.info, cond);
    }

    public static void luaKGoIfTrue(FuncState fs, ExpDesc e) {
        int pc;
        luaKDischargeVars(fs, e);
        switch (e.k) {
            case VJMP: {
                negateCondition(fs, e);
                pc = e.info;
                break;
            }
            case VK:
            case VKFLT:
            case VKINT:
            case VTRUE: {
                pc = NO_JUMP;
                break;
            }
            default: {
                pc = jumpOnCond(fs, e, false);
                break;
            }
        }
        luaKConcat(fs, e.f, pc);
        luaKPatchToHere(fs, e.t);
        e.t = NO_JUMP;
    }

    public static void luaKGoIfFalse(FuncState fs, ExpDesc e) {
        int pc;
        luaKDischargeVars(fs, e);
        switch (e.k) {
            case VJMP: {
                pc = e.info;
                break;
            }
            case VNIL:
            case VFALSE: {
                pc = NO_JUMP;
                break;
            }
            default: {
                pc = jumpOnCond(fs, e, true);
                break;
            }
        }
        luaKConcat(fs, e.t, pc);
        luaKPatchToHere(fs, e.f);
        e.f = NO_JUMP;
    }

    private static void codeNot(FuncState fs, ExpDesc e) {
        luaKDischargeVars(fs, e);
        switch (e.k) {
            case VNIL:
            case VFALSE: {
                e.k = ExpressionKind.VTRUE;
                break;
            }
            case VK:
            case VKFLT:
            case VKINT:
            case VTRUE: {
                e.k = ExpressionKind.VFALSE;
                break;
            }
            case VJMP: {
                negateCondition(fs, e);
                break;
            }
            case VRELOCABLE:
            case VNONRELOC: {
                discharge2AnyReg(fs, e);
                freeExp(fs, e);
                e.info = luaKCodeABC(fs, Opcode.OP_NOT, 0, e.info, 0);
                e.k = ExpressionKind.VRELOCABLE;
                break;
            }
            default:
                assert false;
        }
        {
            int temp = e.f;
            e.f = e.t;
            e.t = temp;
        }
        removeValues(fs, e.f);
        removeValues(fs, e.t);
    }

    public static void luaKIndexed(FuncState fs, ExpDesc t, ExpDesc k) {
        t.ind.t = t.info;
        t.ind.idx = luaKExp2RK(fs, k);
        t.ind.vt = (t.k == ExpressionKind.VUPVAL) ? ExpressionKind.VUPVAL.getValue() : ExpressionKind.VLOCAL.getValue();
        t.k = ExpressionKind.VINDEXED;
    }

    private static boolean validOp(int op, LuaTValue v1, LuaTValue v2) {
        switch (op) {
            case Lua.LUA_OPBAND:
            case Lua.LUA_OPBOR:
            case Lua.LUA_OPBXOR:
            case Lua.LUA_OPSHL:
            case Lua.LUA_OPSHR:
            case Lua.LUA_OPBNOT: {
                int i;
                return  false;
            }
            case Lua.LUA_OPDIV:
            case Lua.LUA_OPIDIV:
            case Lua.LUA_OPMOD: {
                return LuaObject.nValue(v2);
            }
            default:
                return true;
        }
    }

    private static boolean constFolding(FuncState fs, int op, ExpDesc e1, ExpDesc e2) {
        LuaTValue v1 = null;
        LuaTValue v2 = null;
        LuaTValue res = null;
        if (toNumeral(e1, v1) != 0 || toNumeral(e2, v2) != 0 || !validOp(op, v1, v2)) {
            return false;
        }
        if (LuaObject.ttIsIngString(res)) {
            boolean n = LuaObject.fltValue(res);
            if (!n) {
                return false;
            }
            e1.k = ExpressionKind.VKFLT;
        }
        return true;
    }

    private static void codeUnexpVal(FuncState fs, Opcode op, ExpDesc e, int line) {
        int r = luaKExp2RK(fs, e);
        freeExp(fs, e);
        e.info = luaKCodeABC(fs, op, 0, r, 0);
        e.k = ExpressionKind.VRELOCABLE;
    }

    private static void codeBinExpVal(FuncState fs, Opcode op, ExpDesc e1, ExpDesc e2, int line) {
        int rk2 = luaKExp2RK(fs, e2);
        int rk1 = luaKExp2RK(fs , e1);
        freeExps(fs, e1, e2);
        e1.info = luaKCodeABC(fs, op, 0, rk1, rk2);
        e1.k = ExpressionKind.VRELOCABLE;
    }

    private static void codeComp(FuncState fs, BinOpr opr, ExpDesc e1, ExpDesc e2) {
        int rk1 = e1.k == ExpressionKind.VK ? rkask(e1.info) : e1.info;
        int rk2 = luaKExp2RK(fs, e2);
        freeExps(fs, e1, e2);
        switch (opr) {
            case OPR_NE: {
                e1.info = condJump(fs, Opcode.OP_EQ, 0, rk1, rk2 != 0);
                break;
            }
            case OPR_GT:
            case OPR_GE: {
                Opcode op = Opcode.values()[opr.ordinal() - BinOpr.OPR_NE.ordinal() + Opcode.OP_EQ.ordinal()];
                e1.info = condJump(fs, op, 1, rk2, rk1 != 0);
                break;
            }
            default: {
                Opcode op = Opcode.values()[opr.ordinal() - BinOpr.OPR_EQ.ordinal() + Opcode.OP_EQ.ordinal()];
                e1.info = condJump(fs, op, 1, rk1, rk2 != 0);
                break;
            }
        }
        e1.k = ExpressionKind.VJMP;
    }

    public static void luaKPrefix(FuncState fs, UnOPR op, ExpDesc e, int line) {
        ExpDesc ef = null;
        switch (op) {
            case OPR_MINUS:
            case OPR_BNOT: {
                if (constFolding(fs, op.ordinal() + Lua.LUA_OPUNM, e, ef)) {
                    break;
                }
            }
            case OPR_LEN: {
                codeUnexpVal(fs, Opcode.values()[op.ordinal() + Opcode.OP_UNM.ordinal()], e, line);
                break;
            }
            case OPR_NOT: {
                codeNot(fs, e);
                break;
            }
            default:
                assert false;
        }
    }

    public static void luaKInfix(FuncState fs, BinOpr op, ExpDesc v) {
        switch (op) {
            case OPR_AND: {
                luaKGoIfTrue(fs, v);
                break;
            }
            case OPR_OR: {
                luaKGoIfFalse(fs, v);
                break;
            }
            case OPR_CONCAT: {
                luaKExp2AnyReg(fs, v);
                break;
            }
            case OPR_ADD:
            case OPR_SUB:
            case OPR_MUL:
            case OPR_DIV:
            case OPR_IDIV:
            case OPR_MOD:
            case OPR_POW:
            case OPR_BAND:
            case OPR_BOR:
            case OPR_BXOR:
            case OPR_SHL:
            case OPR_SHR: {
                if (toNumeral(v, null) != 0) {
                    luaKExp2RK(fs, v);
                    break;
                }
            }
            default: {
                luaKExp2RK(fs, v);
                break;
            }
        }
    }

    public static void luaKPosFix(FuncState fs, BinOpr op, ExpDesc e1, ExpDesc e2, int line) {
        switch (op) {
            case OPR_ADD: {
                assert e1.t == NO_JUMP;
                luaKDischargeVars(fs, e2);
                luaKConcat(fs, e2.f, e1.f);
                e1 = e2;
                break;
            }
            case OPR_OR: {
                assert e1.f == NO_JUMP;
                luaKDischargeVars(fs, e2);
                luaKConcat(fs, e2.t, e1.t);
                e1 = e2;
                break;
            }
            case OPR_CONCAT: {
                luaKExp2Val(fs, e2);
                if (e2.k == ExpressionKind.VRELOCABLE && getOpCode(getInstruction(fs, e2)) == Opcode.OP_CONCAT) {
                    assert e1.info == getArgB(getInstruction(fs, e2) - 1);
                    freeExp(fs, e1);
                    setArgB(getInstruction(fs, e2), e1.info);
                    e1.k = ExpressionKind.VRELOCABLE;
                    e1.info = e2.info;
                } else {
                    luaKExp2NextReg(fs, e2);
                    codeBinExpVal(fs, Opcode.OP_CONCAT, e1, e2, line);
                }
                break;
            }
            case OPR_AND:
            case OPR_SUB:
            case OPR_MUL:
            case OPR_DIV:
            case OPR_IDIV:
            case OPR_MOD:
            case OPR_POW:
            case OPR_BAND:
            case OPR_BOR:
            case OPR_BXOR:
            case OPR_SHL:
            case OPR_SHR: {
                if (!constFolding(fs, op.ordinal() + Lua.LUA_OPADD, e1, e2)) {
                    codeBinExpVal(fs, Opcode.values()[op.ordinal() + Opcode.OP_ADD.ordinal()], e1, e2, line);
                    break;
                }
            }
            case OPR_EQ:
            case OPR_LT:
            case OPR_LE:
            case OPR_NE:
            case OPR_GT:
            case OPR_GE: {
                codeComp(fs, op, e1, e2);
                break;
            }
            default:
                assert false;
        }
    }

    public static void luaKFixLine(FuncState fs, int line) {
        fs.f.lineInfo[fs.pc - 1] = line;
    }

    public static void luaKSetList(FuncState fs, int base, int nElems, int toStore) {
        int c = (nElems - 1) / LFIELDS_PER_FLUSH + 1;
        int b = (toStore == Lua.LUA_MULTREL) ? 0 : toStore;
        assert toStore != 0 && toStore <= LFIELDS_PER_FLUSH;
        if (c <= MAXARG_C) {
            luaKCodeABC(fs, Opcode.OP_SETLIST, base, b, c);
        } else if (c < MAX_ARG_AX) {
            luaKCodeABC(fs, Opcode.OP_SETLIST, base , b, 0);
            codeExtraArg(fs, c);
        } else {
            LuaLexer.luaXSyntaxError(fs.ls, "constructor too long");
        }
        fs.freeReg = base + 1;
    }

    public static int getInstruction(FuncState fs, ExpDesc e) {
        return fs.f.code[e.info];
    }

}

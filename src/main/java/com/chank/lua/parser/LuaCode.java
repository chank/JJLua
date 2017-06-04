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
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;

import static com.chank.lua.LuaOpcode.*;

/**
 * @author Chank
 */
public final class LuaCode {

    public static final int MAX_REGS = 255;

    public static final int NO_JUMP = -1;

    public static boolean hasJumps(LuaParser.ExpDesc e) {
        return e.t != e.f;
    }

    private static int toNumeral(final LuaParser.ExpDesc e, LuaTValue v) {
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

    public static void luaKNil(LuaParser.FuncState fs, int from, int n) {
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

    private static int getJump(LuaParser.FuncState fs, int pc) {
        int offset = LuaOpcode.getArgSBX(fs.f.code[pc]);
        if (offset == NO_JUMP) {
            return NO_JUMP;
        } else {
            return (pc + 1) + offset;
        }
    }

    private static void fixJump(LuaParser.FuncState fs, int pc, int dest) {
        int jmp = fs.f.code[pc];
        int offset = dest - (pc + 1);
        assert dest != NO_JUMP;
        if (Math.abs(offset) > LuaOpcode.MAX_ARG_SBX) {
            LuaLexer.luaXSyntaxError(fs.ls, "control structure too long");
        }
        LuaOpcode.setArgSBX(jmp, offset);
    }

    public static void luaKConcat(LuaParser.FuncState fs, int l1, int l2) {
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

    public static int luaKJump(LuaParser.FuncState fs) {
        int jpc = fs.jpc;
        int j;
        fs.jpc = NO_JUMP;
        j = 0;
        luaKConcat(fs, j, jpc);
        return j;
    }

    public static void luaKRet(LuaParser.FuncState fs, int first, int nret) {
    }

    private static int condJump(LuaParser.FuncState fs, OPCode op, int a, int b, int c) {
        return luaKJump(fs);
    }

    public static int luaKGetLabel(LuaParser.FuncState fs) {
        fs.lastTarget = fs.pc;
        return fs.pc;
    }

    private static int getJumpControl(LuaParser.FuncState fs, int pc) {
        int pi = fs.f.code[pc];
        if (pc >= 1 && LuaOpcode.testTMode(LuaOpcode.getOpCode(pi - 1).ordinal())) {
            return pi - 1;
        } else {
            return pi;
        }
    }

    private static boolean patchTestReg(LuaParser.FuncState fs, int node, int reg) {
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

    private static void removeValues(LuaParser.FuncState fs, int list) {
        for (; list != NO_JUMP; list = getJump(fs, list)) {
            patchTestReg(fs, list, NO_REG);
        }
    }

    private static void patchListAux(LuaParser.FuncState fs, int list, int vTarget, int reg, int dTarget) {
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

    private static void dischargeJPC(LuaParser.FuncState fs) {
        patchListAux(fs, fs.jpc, fs.pc, NO_REG, fs.pc);
        fs.jpc = NO_JUMP;
    }

    public static void luaKPatchToHere(LuaParser.FuncState fs, int list) {
        luaKGetLabel(fs);
        luaKConcat(fs, fs.jpc, list);
    }

    public static void luaKPatchList(LuaParser.FuncState fs, int list, int target) {
        if (target == fs.pc) {
            luaKPatchToHere(fs, list);
        } else {
            assert target < fs.pc;
            patchListAux(fs, list, target, NO_REG, target);
        }
    }

    public static void luaKPatchClose(LuaParser.FuncState fs, int list, int level) {
        level++;
        for (; list != NO_JUMP; list = getJump(fs, list)) {
            assert(LuaOpcode.getOpCode(fs.f.code[list]) == LuaOpcode.Opcode.OP_JMP &&
            LuaOpcode.getArgA(fs.f.code[list]) == 0 || LuaOpcode.getArgA(fs.f.code[list]) >= level);
            LuaOpcode.setArgA(fs.f.code[list], level);
        }
    }

    public static int luaKCode(LuaParser.FuncState fs, int i) {
        LuaObject.Proto f = fs.f;
        dischargeJPC(fs);
        f.code[fs.pc] = i;
        f.lineInfo[fs.pc] = fs.ls.lastLine;
        return fs.pc++;
    }

    public static int luaKCodeABC(LuaParser.FuncState fs, LuaOpcode.Opcode o, int a, int b, int c) {
        assert LuaOpcode.getOpMode(o.ordinal()) == LuaOpcode.OpMode.iABC;
        assert LuaOpcode.getBMode(o.ordinal()) != LuaOpcode.OpArgMask.OpArgN || b == 0;
        assert LuaOpcode.getCMode(o.ordinal()) != LuaOpcode.OpArgMask.OpArgN || c == 0;
        assert a <= MAXARG_A && b <= MAXARG_B && c <= MAXARG_C;
        return luaKCode(fs, LuaOpcode.createABC(o.ordinal(), a, b, c));
    }

    public static int luaKCodeABX(LuaParser.FuncState fs, int o, int a, int bc) {
        assert LuaOpcode.getOpMode(o) == OpMode.iABx || LuaOpcode.getOpMode(o) == OpMode.iAsBx;
        assert LuaOpcode.getCMode(o) == OpArgMask.OpArgN;
        assert a <= MAXARG_A && bc < MAX_ARG_BX;
        return luaKCode(fs, LuaOpcode.createABX(o, a, bc));
    }

    private static int codeExtraArg(LuaParser.FuncState fs, int a) {
        assert a <= LuaOpcode.MAX_ARG_AX;
        return luaKCode(fs, LuaOpcode.createAX(Opcode.OP_EXTRAARG.ordinal(), a));
    }

    public static int luaKCodeK(LuaParser.FuncState fs, int reg, int k) {
        if (k <= LuaOpcode.MAX_ARG_BX) {
            return luaKCodeABX(fs, Opcode.OP_LOADK.ordinal(), reg, k);
        } else {
            int p = luaKCodeABX(fs, Opcode.OP_LOADKX.ordinal(), reg, 0);
            codeExtraArg(fs, k);
            return p;
        }
    }

    public static void luaKCheckStack(LuaParser.FuncState fs, int n) {
        int newStack = fs.freeReg + n;
        if (newStack > fs.f.maxStackSize) {
            if (newStack >= MAX_REGS) {
                LuaLexer.luaXSyntaxError(fs.ls, "function or expression needs too many registers");
            }
            fs.f.maxStackSize = (byte)newStack;
        }
    }

    public static void luaKReserveRegs(LuaParser.FuncState fs, int n) {
        luaKCheckStack(fs, n);
        fs.freeReg += n;
    }

    public static void freeReg(LuaParser.FuncState fs, int reg) {
        if (!isk(reg) && reg >= fs.nactvar) {
            fs.freeReg--;
            assert reg == fs.freeReg;
        }
    }

    private static void freeExp(LuaParser.FuncState fs, LuaParser.ExpDesc e) {
        if (e.k == ExpressionKind.VNONRELOC) {
            freeReg(fs, e.info);
        }
    }

    private static void freeExps(LuaParser.FuncState fs, LuaParser.ExpDesc e1, LuaParser.ExpDesc e2) {
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

    private static int addK(LuaParser.FuncState fs, LuaTValue key, LuaTValue v) {
        LuaState l = fs.ls.l;
        LuaObject.Proto f = fs.f;
        return 0;
    }

    public static int luaKStringK(LuaParser.FuncState fs, String s) {
        LuaTValue o = null;
        return addK(fs, o, o);
    }


    public static void luaKSetReturns(LuaParser.FuncState fs, LuaParser.ExpDesc e, int nresults) {
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

    public static int getInstruction(LuaParser.FuncState fs, LuaParser.ExpDesc e) {
        return fs.f.code[e.info];
    }

}

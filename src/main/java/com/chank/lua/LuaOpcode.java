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
public final class LuaOpcode {

    public enum Opcode  {
        OP_MOVE,
        OP_LOADK,
        OP_LOADKX,
        OP_LOADBOOL,
        OP_LOADNIL,
        OP_GETUPVAL,

        OP_GETTABUP,
        OP_GETTABLE,

        OP_SETTABUP,
        OP_SETUPVAL,
        OP_SETTABLE,

        OP_NEWTABLE,

        OP_SELF,

        OP_ADD,
        OP_SUB,
        OP_MUL,
        OP_MOD,
        OP_POW,
        OP_DIV,
        OP_IDIV,
        OP_BAND,
        OP_BOR,
        OP_BXOR,
        OP_SHL,
        OP_SHR,
        OP_UNM,
        OP_BNOT,
        OP_NOT,
        OP_LEN,

        OP_CONCAT,

        OP_JMP,
        OP_EQ,
        OP_LT,
        OP_LE,

        OP_TEST,
        OP_TESTSET,

        OP_CALL,
        OP_TAILCALL,
        OP_RETURN,

        OP_FORLOOP,
        OP_FORPREF,

        OP_TFORCALL,
        OP_TFORLOOP,

        OP_SETLIST,

        OP_CLOSURE,

        OP_VARARG,

        OP_EXTRAARG;
    }

    public static final String LUAP_OPNAMES[] = {
            "MOVE",
            "LOADX",
            "LOADKX",
            "LOADBOOL",
            "LOADNIL",
            "GETUPVAL",
            "GETTABUP",
            "GETTABLE",
            "SETTABUP",
            "SETUPVAL",
            "SETTABLE",
            "NEWTABLE",
            "SELF",
            "ADD",
            "SUB",
            "MUL",
            "MOD",
            "POW",
            "DIV",
            "IDIV",
            "BAND",
            "BOR",
            "BXOR",
            "SHL",
            "SHR",
            "UNM",
            "BNOT",
            "NOT",
            "LEN",
            "CONCAT",
            "JMP",
            "EQ",
            "LT",
            "LE",
            "TEST",
            "TESTSET",
            "CALL",
            "TAILCALL",
            "RETURN",
            "FORLOOP",
            "FORPREF",
            "TFORCALL",
            "TFORLOOP",
            "SETLIST",
            "CLOSURE",
            "VARARG",
            "EXTRAARG",
            null
    };

    public static final int NUM_OPCODES = Opcode.values().length;

    public enum OpArgMask {
        OpArgN,
        OpArgU,
        OpArgR,
        OpArgK
    }

    public enum OpMode {
        iABC,
        iABx,
        iAsBx,
        iAx
    }

    public static final int SIZE_C = 9;
    public static final int SIZE_B = 9;
    public static final int SIZE_BX = SIZE_C + SIZE_B;
    public static final int SIZE_A = 8;
    public static final int SIZE_AX = SIZE_C + SIZE_B + SIZE_A;

    public static final int SIZE_OP = 6;

    public static final int POS_OP = 0;
    public static final int POS_A = POS_OP +SIZE_OP;
    public static final int POS_C = POS_A + SIZE_A;
    public static final int POS_B = POS_C + SIZE_C;
    public static final int POS_BX = POS_C;
    public static final int POS_AX = POS_A;

    public static final int MAX_ARG_BX = (1 << SIZE_BX) - 1;
    public static final int MAX_ARG_SBX = (MAX_ARG_BX) >> 1;

    public static final int MAXARG_A = (1 << SIZE_A) - 1;
    public static final int MAXARG_B = (1 << SIZE_B) - 1;
    public static final int MAXARG_C = (1 << SIZE_C) - 1;

    public static final int LFIELDS_PER_FLUSH = 50;

    public static int opMode(int t, int a, int b, int c, int m) {
        return (t << 7) | (a << 6) | (b << 4) | (c << 2) | m;
    }

    public static int mask1(int n, int p) {
        return ((~((~0)<<n))<<p);
    }

    public static int mask0(int n, int p) {
        return ~mask1(n, p);
    }

    public static Opcode getOpCode(int i) {
        return Opcode.values()[(i >> POS_OP) & mask1(SIZE_OP, 0)];
    }

    public static int setOpcode(int i, int o) {
        return ((i) & mask0(SIZE_OP, POS_OP)) | (o << POS_OP) & mask1(SIZE_OP, POS_OP);
    }

    public static int getArg(int i, int pos, int size) {
        return (i >> pos) & mask1(size, 0);
    }

    public static int setArg(int i, int v, int pos, int size) {
        return (i & mask0(size, pos)) | ((v << pos) & mask1(size, pos));
    }

    public static int getArgA(int i) {
        return getArg(i, POS_A, SIZE_A);
    }

    public static int setArgA(int i, int v) {
        return setArg(i, v, POS_A, SIZE_A);
    }

    public static int getArgB(int i) {
        return getArg(i, POS_B, SIZE_B);
    }

    public static int setArgB(int i, int v) {
        return setArg(i, v, POS_B, SIZE_B);
    }

    public static int getArgC(int i) {
        return getArg(i, POS_C, SIZE_C);
    }

    public static int setArgC(int i, int v) {
        return setArg(i, v, POS_C, SIZE_C);
    }

    public static int getArgBX(int i) {
        return getArg(i, POS_BX, SIZE_BX);
    }

    public static int setArgBX(int i, int v) {
        return setArg(i, v, POS_BX, SIZE_BX);
    }

    public static int getArgAX(int i) {
        return getArg(i, POS_AX, SIZE_AX);
    }

    public static int setArgAX(int i, int v) {
        return setArg(i, v, POS_AX, SIZE_AX);
    }

    public static int getArgSBX(int i) {
        return getArgBX(i) - MAX_ARG_SBX;
    }

    public static int setArgSBX(int i, int b) {
        return setArgBX(i, b + MAX_ARG_SBX);
    }

    public static int createABC(int o, int a, int b, int c) {
        return (o << POS_OP) | (a << POS_A) | (b << POS_B) | (c << POS_C);
    }

    public static int createABX(int o, int a, int bc) {
        return (o << POS_OP) | (a << POS_A) | (bc << POS_BX);
    }

    public static int createAX(int o, int a) {
        return (o << POS_OP) | (a << POS_AX);
    }

    public static final int BITRK = 1 << (SIZE_B - 1);

    public static int isk(int x) {
        return x & BITRK;
    }

    public static int indexK(int r) {
        return r & ~BITRK;
    }

    public static final int MAX_INDEXRK = BITRK - 1;

    public static int rkask(int x) {
        return x | BITRK;
    }

    public static final int NO_REG = MAXARG_A;

    public static final byte[] LUAP_OP_MODES = {
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABx.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgN.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABx.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgR.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iAsBx.ordinal()),
            (byte)opMode(1, 0, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(1, 0, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(1, 0, OpArgMask.OpArgK.ordinal(), OpArgMask.OpArgK.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(1, 0, OpArgMask.OpArgN.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(1, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iAsBx.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iAsBx.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgN.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgR.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iAsBx.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABx.ordinal()),
            (byte)opMode(0, 1, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgN.ordinal(), OpMode.iABC.ordinal()),
            (byte)opMode(0, 0, OpArgMask.OpArgU.ordinal(), OpArgMask.OpArgU.ordinal(), OpMode.iAx.ordinal()),
    };

    public static OpMode getOpMode(int m) {
        return OpMode.values()[LUAP_OP_MODES[3] & 3];
    }

    public static OpArgMask getBMode(int m) {
        return OpArgMask.values()[(LUAP_OP_MODES[m] >> 4) & 3];
    }

    public static OpArgMask getCMode(int m) {
        return OpArgMask.values()[(LUAP_OP_MODES[m] >> 2) & 3];
    }

    public static int testAMode(int m) {
        return LUAP_OP_MODES[m] & (1 << 6);
    }

    public static int testTMode(int m) {
        return LUAP_OP_MODES[m] & (1 << 7);
    }

}

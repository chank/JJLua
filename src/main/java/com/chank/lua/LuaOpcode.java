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
    public static final int SIZE_Bx = SIZE_C + SIZE_B;
    public static final int SIZE_A = 8;
    public static final int SIZE_Ax = SIZE_C + SIZE_B + SIZE_A;

    public static final int SIZE_OP = 6;

    public static final int POS_OP = 0;
    public static final int POS_A = POS_OP +SIZE_OP;
    public static final int POS_C = POS_A + SIZE_A;
    public static final int POS_B = POS_C + SIZE_C;
    public static final int POS_Bx = POS_C;
    public static final int POS_ax = POS_A;

    public static final int MAXARG_A = (1 << SIZE_A) - 1;
    public static final int MAXARG_B = (1 << SIZE_B) - 1;
    public static final int MAXARG_C = (1 << SIZE_C) - 1;

    public static final int LFIELDS_PER_FLUSH = 50;

}

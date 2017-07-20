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
public final class LuaState {
    public static final int EXTRA_STACK = 5;
    public static final int BASIC_STAK_SIZE = 2 * Lua.LUA_MINSTACK;

    public static final int KGC_NORMAL = 0;
    public static final int KGC_EMERGENCY = 1;

    public static final class StringTable {
        public String[] hash;
        public int nuse;
        public int size;
    }

    public static final class CallInfo {
        public LuaTValue func;
        public LuaTValue top;
        public CallInfo previous;
        public CallInfo next;
        public static final class L {
            public LuaTValue base;
            int saveDpc;
        }
        public L l;
        public static final class C {
            int oldErrFunc;
            int ctx;
        }
        public C c;
        int extra;
        short nResults;
        short callStatus;
    }

    public static final int CIST_OAH = 1 << 0;
    public static final int CIST_LUA = 1 << 1;
    public static final int CIST_HOOKED = 1 << 2;
    public static final int CIST_FRESH = 1 << 3;

    public static final int CIST_YPCALL = 1 << 4;
    public static final int CIST_TALL = 1 << 5;
    public static final int CIST_HOOKYIELD = 1 << 6;
    public static final int CIST_LEQ = 1 << 7;
    public static final int CIST_FIN = 1 << 8;

    public int nci;
    public byte status;
    public LuaTValue top;
    public GlobalState lg;
    public CallInfo ci;
    public int oldPC;
    public LuaTValue stackLast;
    public LuaTValue stack;
    public LuaFunc.UpVal  openUpVal;
    public LuaState twups;
    public LuaDo.LuaLongJmp errorJmp;
    public CallInfo baseCI;
    public int errFunc;
    public int stackSize;
    public int baseBhookCount;
    public short nny;
    public short nCCalls;
    public int hookMask;
    public byte allowHook;
}

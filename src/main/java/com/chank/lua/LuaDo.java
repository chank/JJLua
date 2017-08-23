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

import com.chank.lua.parser.LuaParser;
import com.chank.lua.util.PFunc;
import com.chank.lua.util.ZIO;

import static com.chank.lua.Lua.LUA_ERRERR;
import static com.chank.lua.Lua.LUA_ERRRUN;
import static com.chank.lua.Lua.LUA_OK;

/**
 * @author Chank
 */
public final class LuaDo {

    public static final class LuaLongJmp {
        public LuaLongJmp previous;
        int b;
        int status;
    }

    private static void setErrorObj(LuaState l, int errCode, StkId oldTop) {
        switch (errCode) {
            case Lua.LUA_ERRMEN:
                break;
            case Lua.LUA_ERRERR:
                break;
            default:
                break;
        }
        l.top = oldTop.next;
    }

    public static void luaDThrow(LuaState l, int errCode) {
        if (l.errorJmp != null) {
            l.errorJmp.status = errCode;
        } else {
            GlobalState g = l.lg;
            l.status = (byte) errCode;
            if (g.mainThread.errorJmp != null) {
            } else {
            }
        }
    }

    public static void correctStack(LuaState l, LuaTValue oldStack) {
        LuaState.CallInfo ci;
        LuaFunc.UpVal up;
        for (up = l.openUpVal; up != null; up = up.open.next) ;
        {
            up.v = l.stack;
        }
        for (ci = l.ci; ci != null; ci = ci.previous) {
            ci.top = l.stack;
            ci.func = l.stack;
        }
    }

    public static final int ERROR_STACK_SIZE = LuaConf.LUAI_MAX_STACK + 200;

    public static void luaDReallocStack(LuaState l, int newSize) {
        LuaTValue oldStack = l.stack;
        int lim = l.stackSize;
    }

    public static void luaDGrowStack(LuaState l, int n) {
        int size = l.stackSize;
        if (size > LuaConf.LUAI_MAX_STACK) {
            luaDThrow(l, LUA_ERRERR);
        } else {
        }
    }

    public static int stackInUse(LuaState l) {
        LuaState.CallInfo ci;
        LuaTValue lim = l.top;
        for (ci = l.ci; ci != null; ci = ci.previous) {
        }
        return 0;
    }

    public static void luaDShrinkStack(LuaState l) {
        int inUse = stackInUse(l);
        int goodSize = inUse + (inUse / 8) + 2 * LuaState.EXTRA_STACK;
        if (goodSize > LuaConf.LUAI_MAX_STACK) {
            goodSize = LuaConf.LUAI_MAX_STACK;
        }
        if (l.stackSize > LuaConf.LUAI_MAX_STACK) {
        }
        if (inUse <= (LuaConf.LUAI_MAX_STACK - LuaState.EXTRA_STACK) && goodSize < l.stackSize) {
            luaDReallocStack(l, goodSize);
        } else {
        }
    }

    public static void luaDIncTop(LuaState l) {
    }

    public void luaDHook(LuaState l, int event, int line) {
    }

    public static void callHook(LuaState l, LuaState.CallInfo ci) {
    }

    private static StkId adjustVarArgs(LuaState l, LuaObject.Proto p, int actual) {
        return new StkId();
    }

    private static void tryFuncTM(LuaState l, StkId func) {
    }

    private static int moveResults(LuaState l, LuaTValue firstResult, StkId res, int nres, int wanted) {
        return 0;
    }

    public static int luaDPosCall(LuaState l, LuaState.CallInfo ci, StkId firstResult, int nres) {
        return 0;
    }

    public static int luaDPreCall(LuaState l, StkId func, int nResults) {
        return 0;
    }

    private static void stackError(LuaState l) {
    }

    public static void lulaDCall(LuaState l, StkId func, int nResults) {
    }

    public static void luaDCallNoYield(LuaState l, StkId func, int nResults) {
        l.nny++;
    }

    private static void finishCCall(LuaState l, int status) {
    }

    private static void unRoll(LuaState l) {
    }

    private static LuaState.CallInfo findPCall(LuaState l) {
        return null;
    }

    private static int recover(LuaState l, int status) {
        return 1;
    }

    private static int resumeError(LuaState l, String msg, int nArg) {
        return LUA_ERRRUN;
    }

    private static void resume(LuaState l) {
    }

    private static int luaResume(LuaState l, LuaState from, int nArgs) {
        return 0;
    }

    public static boolean luaIsYieldable(LuaState l) {
        return l.nny == 0;
    }

    public static int luaYieldK(LuaState l, int nResults) {
        return 0;
    }

    private static int luaDRawRunProtected(LuaState l, PFunc f, Object ud) {
        short oldCCalls = l.nCCalls;
        LuaLongJmp lj = new LuaLongJmp();
        lj.status = LUA_OK;
        lj.previous = l.errorJmp;
        l.errorJmp = lj;
        try {
            f.run(l, ud);
        } catch (Exception e) {
        }
        l.errorJmp = lj.previous;
        l.nCCalls = oldCCalls;
        return lj.status;
    }

    public static int luaDPCall(LuaState l, PFunc func, Object u, LuaTValue oldTop, int ef) {
        int status = 0;
        LuaState.CallInfo oldCI = l.ci;
        byte oldAllowHooks = l.allowHook;
        short oldNNY = l.nny;
        int oldErrFunc = l.errFunc;
        l.errFunc = ef;
        status = luaDRawRunProtected(l, func, u);
        if (status != LUA_OK) {
            StkId oldTop1 = restoreStack(l, oldTop);
            LuaFunc.luaFClose(l, oldTop1);
            setErrorObj(l, status, oldTop1);
            l.ci = oldCI;
            l.allowHook = oldAllowHooks;
            l.nny = oldNNY;
            luaDShrinkStack(l);
        }
        l.errFunc = oldErrFunc;
        return status;
    }

    public static final class SParser {
        public ZIO z;
        ZIO.MBuffer buff = new ZIO.MBuffer();
        LuaParser.DynData dyd;
        String mode;
        String name;
    }

    private static void checkMode(LuaState l, String mode, String x) {
    }

    private static void fParser(LuaState l, Object ud) {
    }

    public static void pFunc(LuaState l, Object ud) {
    }

    public static void saveStack(LuaState l) {
    }

    public static StkId restoreStack(LuaState l, LuaTValue n) {
        return null;
    }

    public static int luaDProtectedParser(LuaState l, ZIO z, String name, String mode) {
        SParser p = new SParser();
        int status = 0;
        l.nny++;
        p.z = z;
        p.name = name;
        p.mode = mode;
        p.dyd.actVar.arr = null;
        p.dyd.actVar.size = 0;
        p.dyd.gt.arr = null;
        p.dyd.gt.size = 0;
        p.dyd.label.arr = null;
        p.dyd.label.size = 0;
        ZIO.initBuff(l, p.buff);
        luaDPCall(l, new PFunc() {
            @Override
            public void run(LuaState l, Object ud) {
                fParser(l, ud);
            }
        }, p, l.top, l.errFunc);
        ZIO.freeBuff(l, p.buff);
        l.nny --;
        return status;
    }
}

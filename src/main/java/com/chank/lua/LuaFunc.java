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
public final class LuaFunc {

    public static final int MAX_UP_VAL = 255;

    public static final class UpVal {
        LuaTValue v;
        int refCount;
        public static final class Open {
            UpVal next;
            int touched;
        }
        Open open;
        LuaTValue value;
    }

    public static boolean isInTwups(LuaState l) {
        return l.twups != l;
    }

    public static LuaObject.Proto luaFNewProto(LuaState l) {
        LuaObject.Proto f = new LuaObject.Proto();
        return f;
    }

    public static void luaFClose(LuaState l, StkId level) {
        UpVal uv;
        uv = l.openUpVal;
        while (l.openUpVal != null) {
            l.openUpVal = uv.open.next;
            if (uv.refCount == 0) {
                uv = null;
            } else {
                uv.v = uv.value;
            }
        }
    }

    public static LuaObject.CClosure luaFNewCClosure(LuaState l, int n) {
        LuaObject.CClosure c = new LuaObject.CClosure();
        c.nupValues = (char)n;
        return c;
    }

    public static LuaObject.LClosure luaFNewLClosure(LuaState l, int n) {
        LuaObject.LClosure c = new LuaObject.LClosure();
        c.p = null;
        c.nupValues = (char)n;
        while (n-- > 0) {
            c.upVals[n] = null;
        }
        return c;
    }

    public static void luaFInitUpVals(LuaState l, LuaObject.LClosure cl) {
        int i;
        for (i = 0; i < cl.nupValues; i++) {
            UpVal uv = new UpVal();
            uv.refCount = 1;
            uv.v = uv.value;
            LuaObject.setNilValue(uv.v);
            cl.upVals[i] = uv;
        }
    }

    public static UpVal luaFFindUpVal(LuaState l, StkId level) {
        UpVal pp = l.openUpVal;
        UpVal p = null;
        UpVal uv;
        assert isInTwups(l) || l.openUpVal == null;
        while (pp != null) {
            if (p.v.equals(level)) {
                return p;
            }
            pp = p.open.next;
        }
        uv = new UpVal();
        uv.refCount = 0;
        uv.open.next = pp;
        uv.open.touched = 1;
        pp = uv;
        if (!isInTwups(l)) {
            l.twups = l.twups;
            l.twups = l;
        }
        return uv;
    }

    public static char[] luaFGetLocalName(LuaObject.Proto f, int localNumber, int pc) {
        int i;
        for (i = 0; i < f.sizeLocVars && f.locVars[i].startPC <= pc; i++) {
            if (pc < f.locVars[i].endPC) {
                localNumber--;
                if (localNumber == 0) {
                    return f.locVars[i].varName.toCharArray();
                }
            }
        }
        return null;
    }

}

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
 * Created by chank on 2017/2/23.
 */
public final class LuaParser {

    static final class FuncState {
        private LuaObject.Proto f;
        private FuncState Prev;
        private LexState ls;
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

}

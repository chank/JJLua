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
public final class Lua {

    public static final String LUA_VERSION_MAJOR = "5";
    public static final String LUA_VERSION_MINOR = "3";
    public static final int LUA_VERSION_NUM = 503;
    public static final String LUA_VERSION_RELEASE = "4";

    public static final String LUA_VERSION = "Lua " + LUA_VERSION_MAJOR + "." + LUA_VERSION_MINOR;
    public static final String LUA_RELEASE = LUA_VERSION + "." + LUA_VERSION_RELEASE;

    public static final String LUA_SIGNATURE = "Lua";

    public static final int LUA_MULTREL = -1;

    public static final int LUA_OK = 0;
    public static final int LUA_YIELD = 1;
    public static final int LUA_ERRRUN = 2;
    public static final int LUA_ERRSYNTAX = 3;
    public static final int LUA_ERRMEN = 4;
    public static final int LUA_ERRGCMM = 5;
    public static final int LUA_ERRERR = 6;

    public static final int LUA_NONE = -1;
    public static final int LUA_TNIL = 0;
    public static final int LUA_TBOOLEAN = 1;
    public static final int LUA_TLIGHTUSERDATA = 2;
    public static final int LUA_TNUMBER = 3;
    public static final int LUA_TSTRING = 4;
    public static final int LUA_TTABLE = 5;
    public static final int LUA_TFUNCTION = 6;
    public static final int LUA_TUSERDATA = 7;
    public static final int LUA_TTHREAD = 8;
    public static final int LUA_NUMTAGS = 9;

    public static final int LUA_MINSTACK = 20;

    public static final int LUA_RIDX_MAINTHREAD = 1;
    public static final int LUA_RIDX_GLOBALS = 2;
    public static final int LUA_RIDX_LAST = LUA_RIDX_GLOBALS;

    public static final int LUA_OPADD = 0;
    public static final int LUA_OPSUB = 1;
    public static final int LUA_OPMUL = 2;
    public static final int LUA_OPMOD = 3;
    public static final int LUA_OPPOW = 4;
    public static final int LUA_OPDIV = 5;
    public static final int LUA_OPIDIV = 6;
    public static final int LUA_OPBAND = 7;
    public static final int LUA_OPBOR = 8;
    public static final int LUA_OPBXOR = 9;
    public static final int LUA_OPSHL = 10;
    public static final int LUA_OPSHR = 11;
    public static final int LUA_OPUNM = 12;
    public static final int LUA_OPBNOT = 13;

    public static final int LUA_OPEQ = 0;
    public static final int LUA_OPLT = 1;
    public static final int LUA_OPLE = 2;

    public static final int LUA_HOOKCALL = 0;
    public static final int LUA_HOOKRET = 1;
    public static final int LUA_HOOKLINE = 2;
    public static final int LUA_HOOKCOUNT = 3;
    public static final int LUA_HOKTAILCALL = 4;

    public static final int LUA_MASKCALL = 1 << LUA_HOOKCALL;
    public static final int LUA_MASKRET = 1 << LUA_HOOKRET;
    public static final int LUA_MASKLINE = 1 << LUA_HOOKLINE;
    public static final int LUA_MASKCOUNT = 1 << LUA_HOOKCOUNT;

    public static final class LuaDebug {
        public int event;
        public String name;
        public String nameWhat;
        public String what;
        public String source;
        public int currentLine;
        public int lineDefined;
        public int lastLineDefined;
        public char nups;
        public char nParams;
        public char isVarArg;
        public char isTailCall;
        public char[] shortSrc;
    }

}

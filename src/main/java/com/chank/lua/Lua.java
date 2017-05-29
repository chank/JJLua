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

}

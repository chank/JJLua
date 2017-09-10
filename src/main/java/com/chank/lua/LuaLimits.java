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
public final class LuaLimits {

    public static final int CHAR_BIT = 8;
    public static final int SCHAR_MIN = -128;
    public static final int SCHAR_MAX = 127;
    public static final int UCHAR_MAX = 0xFF;

    public static final int CHAR_MIN = SCHAR_MIN;
    public static final int CHAR_MAX = SCHAR_MAX;

    public static final int MB_LEN_MAX = 5;
    public static final int SHRT_MIN = -32768;
    public static final int SHRT_MAX = 0xFFFF;
    public static final int INT_MIN = Integer.MIN_VALUE;
    public static final int INT_MAX = Integer.MAX_VALUE;
    public static final int UNIT_MAX = 0xFFFFFFFF;
    public static final long LONG_MING = Long.MIN_VALUE;
    public static final long LONG_MAX = Long.MAX_VALUE;
    public static final long ULONG_MAX = 0xFFFFFFFFL;
    public static final long LLONG_MAX = Long.MAX_VALUE;
    public static final long LLONG_MIN = Long.MIN_VALUE;
    public static final long ULLONG_MAX = Long.MAX_VALUE;

    public static final int _I8_MIN = 0-127 - 1;
    public static final int _I8_MAX = 0127;
    public static final int _UT8_MAX = 0xFF;

    public static final int _POSIX_ARG_MAX = 4096;
    public static final int _POSIX_CHILD_MAX = 6;
    public static final int _POSIX_LINK_MAX = 8;
    public static final int _POSIX_MAX_CANON = 255;
    public static final int _POSIX_MAX_INPUT = 255;
    public static final int _POSIX_NAME_MAX = 14;
    public static final int _POSIX_NGROUPS_MAX = 0;
    public static final int _POSIX_OPEN_MAX = 16;
    public static final int _POSIX_PATH_MAX = 255;
    public static final int _POSIX_PIPE_BUF = 512;
    public static final int _POSIX_SSIZE_MAX = 32767;
    public static final int _POSIX_STREAM_MAX = 8;
    public static final int _POSIX_TZNAME_MAX = 3;

    public static final int ARG_MAX = 14500;
    public static final int LINK_MAX = 1024;
    public static final int MAX_CANON = _POSIX_MAX_CANON;
    public static final int MAX_INPUT = _POSIX_MAX_INPUT;
    public static final int NAME_MAX = 255;
    public static final int NGROUPS_MAX = 16;
    public static final int OPEN_MAX = 32;
    public static final int PATH_MAX = 512;
    public static final int PIPE_BUF = _POSIX_PIPE_BUF;
    public static final int SSIZE_MAX = _POSIX_SSIZE_MAX;
    public static final int STREAM_MAX = 20;
    public static final int TZNAME_MAX = 10;

    public static final int LUAI_MAXCCALLS = 200;

    public static final int MAX_INT = LuaLimits.INT_MAX;

    public static final int LUA_MIN_BUFFER = 32;
}

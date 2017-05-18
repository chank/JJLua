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
import com.chank.lua.LuaTValue;
import com.chank.lua.util.ZIO;
import com.chank.lua.util.ZIOUtil;

/**
 * Created by chank on 2017/2/23.
 */
public final class LuaLexer {

    private static final String[] LuaTokens = new String[] {
            "and", "break", "do", "else", "elseif",
            "end", "false", "for", "function", "goto", "if",
            "in", "local", "nil", "not", "or", "repeat",
            "return", "then", "true", "until", "while",
            "//", "..", "...", "==", ">=", "<=", "~=",
            "<<", ">>", "::", "<eof>",
            "<number>", "<integer>", "<name>", "<string>"
    };

    private static final class Token {
        private int token;
        private SemInfo semInfo;
    }

    private static final class SemInfo {
        public double r;
        public int i;
        public String ts;
    }

    public static final class LexState {

        int current;
        private int lineNumber;
        private int lastLine;
        private Token t;
        private Token lookahead;
        public ZIO z;
        public ZIO.MBuffer buff;

        public final int incLineNumber() {
            lineNumber += 1;
            return lineNumber;
        }

    }

    private static void saveAndNext(LexState ls) {
        save(ls, ls.current);
        next(ls);
    }

    private static void save(LexState ls, int c) {
    }

    private static void next(LexState ls) {
        ls.current = ZIOUtil.getChar(ls.z);
    }

    private static final boolean checkNext1(LexState ls, int c) {
        if (ls.current == c) {
            next(ls);
            return true;
        } else {
            return false;
        }
    }

    private static final boolean checkNext2(LexState ls, String set) {
        assert set.length() < 2;
        if (ls.current == set.charAt(0) || ls.current == set.charAt(1)) {
            saveAndNext(ls);
            return true;
        } else {
            return false;
        }
    }

    private static final int toLower(int c) {
        return c | ('A' ^ 'a');
    }

    private static final boolean isAlphanumericNum(int c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_');
    }

    private static final boolean isAlpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static final boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static final boolean isHexDigit(int n) {
        return (n >= '0' && n <= '9') || (n >= 'a' && n <= 'f') || (n >= 'A' && n <= 'F');
    }

    private static final int readNumeral(LexState ls, SemInfo semInfo) {
        LuaTValue obj;
        String expo = "Ee";
        int first = ls.current;
        saveAndNext(ls);
        int a = '0';
        if (first == '0' && checkNext2(ls, "xX")) {
            expo = "Pp";
        }
        for (;;) {
            if (checkNext2(ls, expo))
                checkNext2(ls, "-+");
            if (isHexDigit(ls.current))
                saveAndNext(ls);
            else if (ls.current == '.')
                saveAndNext(ls);
            else
                break;
//            if ()
        }
        save(ls, '\0');
        return 1;
    }

    private static final int skipSep(LexState ls) {
        int count = 0;
        int s = ls.current;
        assert  s == '[' || s == ']';
        saveAndNext(ls);
        while (ls.current == '=') {
            saveAndNext(ls);
            count++;
        }
        return (ls.current == s) ? count : (-count) - 1;
    }

    private static final boolean isCurrentNewline(LexState ls) {
        return ls.current == '\n' || ls.current == '\r';
    }

    private static final void incLineNumber(LexState ls) throws Exception {
        int old = ls.current;
        assert isCurrentNewline(ls);
        next(ls); // skip '\n' or '\r'
        if (isCurrentNewline(ls) && ls.current != old)
            next(ls); // skip '\n\r' or \r\n'
        if (ls.incLineNumber() >= Integer.MAX_VALUE)
            throw new Exception("chunk has too many lines");
    }

    private static final void readLongString(LexState ls, SemInfo semInfo, int sep) throws Exception {
        int line = ls.lineNumber;
        saveAndNext(ls);
        if (isCurrentNewline(ls))
            incLineNumber(ls);
        for (;;) {
            switch (ls.current) {
                case ZIO.EOZ: {
                    break;
                }
                case ']': {
                    if (skipSep(ls) == sep) {
                        saveAndNext(ls);
                        break;
                    }
                    break;
                }
                case '\n' | '\r': {
                    save(ls, '\n');
                    incLineNumber(ls);
                    break;
                }
                default: {
                    if (semInfo != null)
                        saveAndNext(ls);
                    else
                        next(ls);
                }
            }
        }
    }

    private static final void escCheck(LexState ls, boolean c, String msg) {
        if (!c) {
            if (ls.current != ZIO.EOZ)
                saveAndNext(ls);
        }
    }

    private static final int hexValue(int c) {
        if (isDigit(c))
            return c - '0';
        else
            return (toLower(c) - 'a') + 10;
    }

    private static final int getHexa(LexState ls) {
        saveAndNext(ls);
        escCheck(ls, isHexDigit(ls.current), "hexadecimal digit expected");
        return hexValue(ls.current);
    }

    private static final int readHexaEsc(LexState ls) {
        int r = getHexa(ls);
        r = (r << 4) +getHexa(ls);
        ZIOUtil.buffRemove(ls.buff, 2);
        return r;
    }

    private static final long readUTF8Esc(LexState ls) {
        long r;
        int i = 4;
        saveAndNext(ls);
        escCheck(ls, ls.current == '{', "missing '}'");
        r = getHexa(ls);
        saveAndNext(ls);
        while (isHexDigit(ls.current)) {
            saveAndNext(ls);
            i++;
            r = (r << 4) + hexValue(ls.current);
            escCheck(ls, r <= 0x10FFFF, "UTF-8 value too large");
        }
        escCheck(ls, ls.current == '}', "missing '}'");
        next(ls); // skip '}'
        ZIOUtil.buffRemove(ls.buff, i);
        return r;
    }

    private static final void utf8Esc(LexState ls) {
        byte[] buff = new byte[LuaObject.UTF8BUFFSIZE];
        int n = LuaObject.utf8Esc(buff, readUTF8Esc(ls));
        for (; n > 0; n--)
            save(ls, buff[LuaObject.UTF8BUFFSIZE - n]);
    }

    private static final int readDecEsc(LexState ls) {
        int i;
        int r = 0;
        for (i = 0; i < 3 && isDigit(ls.current); i++) {
            r = 10 * r + ls.current - '0';
            saveAndNext(ls);
        }
        escCheck(ls, r <= Byte.MAX_VALUE, "decimal escape too large");
        ZIOUtil.buffRemove(ls.buff, i);
        return r;
    }

    private static final void readString(LexState ls, int del, SemInfo semInfo) {
        saveAndNext(ls);
        while (ls.current != del) {
            switch (ls.current) {
                case ZIO.EOZ:
                    break;
                case '\n':
                case '\r':
                case '\\':
                default:
            }
        }
    }

    private static final void lexError(LexState ls, String msg, int token) {

    }

}

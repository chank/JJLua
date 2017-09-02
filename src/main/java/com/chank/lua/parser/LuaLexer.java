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

import static com.chank.lua.util.ZIO.EOZ;

/**
 * @author Chank
 */
public final class LuaLexer {

    private static final String[] LUA_TOKENS = new String[] {
            "and", "break", "do", "else", "elseif",
            "end", "false", "for", "function", "goto", "if",
            "in", "local", "nil", "not", "or", "repeat",
            "return", "then", "true", "until", "while",
            "//", "..", "...", "==", ">=", "<=", "~=",
            "<<", ">>", "::", "<eof>",
            "<number>", "<integer>", "<name>", "<string>"
    };

    static final class Token {
        int token;
        SemInfo semInfo;
    }

    static final class SemInfo {
        public double r;
        public int i;
        public String ts;
    }

    static final int FIRST_RESERVED = 257;

    private static void saveAndNext(LexState ls) {
        save(ls, ls.current);
        next(ls);
    }

    private static void save(LexState ls, int c) {
        ZIO.MBuffer b = ls.buff;
        int buffLen = b.getBuffLen();
        if (buffLen + 1 > b.getBuffSize()) {
            int newSize;
            if (b.getBuffSize() >= Integer.MAX_VALUE / 2) {
                lexError(ls, "lexical element too long", 0);
            }
            newSize = b.getBuffSize() * 2;
            b.resizeBuffer(newSize);
        }
        b.getBuffer()[buffLen++] = (char)c;
    }

    private static void next(LexState ls) {
        ls.current = ZIOUtil.getChar(ls.z);
    }

    private static boolean checkNext1(LexState ls, int c) {
        if (ls.current == c) {
            next(ls);
            return true;
        } else {
            return false;
        }
    }

    private static boolean checkNext2(LexState ls, String set) {
        assert set.length() < 2;
        if (ls.current == set.charAt(0) || ls.current == set.charAt(1)) {
            saveAndNext(ls);
            return true;
        } else {
            return false;
        }
    }

    private static int toLower(int c) {
        return c | ('A' ^ 'a');
    }

    private static boolean isAlphanumericNum(int c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_');
    }

    private static boolean isAlpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isHexDigit(int n) {
        return (n >= '0' && n <= '9') || (n >= 'a' && n <= 'f') || (n >= 'A' && n <= 'F');
    }

    private static int readNumeral(LexState ls, SemInfo semInfo) {
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

    private static int skipSep(LexState ls) {
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

    private static boolean isCurrentNewline(LexState ls) {
        return ls.current == '\n' || ls.current == '\r';
    }

    private static void incLineNumber(LexState ls) throws Exception {
        int old = ls.current;
        assert isCurrentNewline(ls);
        next(ls); // skip '\n' or '\r'
        if (isCurrentNewline(ls) && ls.current != old)
            next(ls); // skip '\n\r' or \r\n'
        if (ls.incLineNumber() >= Integer.MAX_VALUE)
            throw new Exception("chunk has too many lines");
    }

    private static void readLongString(LexState ls, SemInfo semInfo, int sep) throws Exception {
        int line = ls.lineNumber;
        saveAndNext(ls);
        if (isCurrentNewline(ls))
            incLineNumber(ls);
        for (;;) {
            switch (ls.current) {
                case EOZ: {
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

    private static void escCheck(LexState ls, boolean c, String msg) {
        if (!c) {
            if (ls.current != EOZ)
                saveAndNext(ls);
        }
    }

    private static int hexValue(int c) {
        if (isDigit(c))
            return c - '0';
        else
            return (toLower(c) - 'a') + 10;
    }

    private static int getHexa(LexState ls) {
        saveAndNext(ls);
        escCheck(ls, isHexDigit(ls.current), "hexadecimal digit expected");
        return hexValue(ls.current);
    }

    private static int readHexaEsc(LexState ls) {
        int r = getHexa(ls);
        r = (r << 4) +getHexa(ls);
        ZIOUtil.buffRemove(ls.buff, 2);
        return r;
    }

    private static long readUTF8Esc(LexState ls) {
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

    private static void utf8Esc(LexState ls) {
        byte[] buff = new byte[LuaObject.UTF8BUFFSIZE];
        int n = LuaObject.utf8Esc(buff, readUTF8Esc(ls));
        for (; n > 0; n--)
            save(ls, buff[LuaObject.UTF8BUFFSIZE - n]);
    }

    private static int readDecEsc(LexState ls) {
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

    private static void readString(LexState ls, int del, SemInfo semInfo) throws Exception {
        saveAndNext(ls);
        while (ls.current != del) {
            switch (ls.current) {
                case EOZ:
                    lexError(ls, "unfinished string", Reserved.TK_EOS.getValue());
                    break;
                case '\n':
                case '\r':
                    lexError(ls, "unfinished string", Reserved.TK_STRING.getValue());
                    break;
                case '\\':
                    int c;
                    saveAndNext(ls);
                    switch (ls.current) {
                        case 'a': // TODO
                        case 'b':c = '\b'; next(ls); break;
                        case 'f':c = '\f'; next(ls); break;
                        case 'n': c = '\n'; next(ls); break;
                        case 'r': c = '\r'; next(ls); break;
                        case 't': c = '\t'; next(ls); break;
                        case 'v': c = readHexaEsc(ls); next(ls); break;
                        case 'x': c = readHexaEsc(ls); next(ls); break;
                        case 'u': utf8Esc(ls); break;
                        case '\n':case '\r':
                            incLineNumber(ls); c = '\n'; save(ls, c);
                        case '\\':case '\"':case '\'':
                            c = ls.current; next(ls); break;
                        case EOZ: break;
                        case 'z': {
                            next(ls);
                        }
                    }
                default:
            }
        }
        saveAndNext(ls);
    }

    static void luaXSyntaxError(LexState ls, final String msg) {
        lexError(ls, msg, ls.t.token);
    }

    private static void lexError(LexState ls, final String msg, int token) {
    }

    public static int llex(LexState ls, SemInfo semInfo) throws Exception {
        ls.buff.resetBuffer();
        for (;;) {
            switch (ls.current) {
                case '\n': case '\r': {
                    incLineNumber(ls);
                    break;
                }
                case ' ':case '\f':case '\t': {
                    next(ls);
                    break;
                }
                case '-': {
                    next(ls);
                    if (ls.current != '-')
                        return '-';
                    next(ls);
                    if (ls.current == '[') {
                        int sep = skipSep(ls);
                        if (sep >= 0) {
                            readLongString(ls, null, sep);
                            break;
                        }
                    }
                    while (!isCurrentNewline(ls) && ls.current != EOZ) {
                        next(ls);
                    }
                    break;
                }
                case '[': {
                    int sep = skipSep(ls);
                    if (sep >= 0) {
                        readLongString(ls, semInfo, sep);
                        return Reserved.TK_STRING.getValue();
                    } else if (sep != -1) {
                        lexError(ls, "Invalid long string delimiter", Reserved.TK_STRING.getValue());
                    }
                    return '[';
                }
                case '=': {
                    next(ls);
                    if (checkNext1(ls, '=')) {
                        return Reserved.TK_EQ.getValue();
                    } else {
                        return '=';
                    }
                }
                case '<': {
                    next(ls);
                    if (checkNext1(ls, '=')) {
                        return Reserved.TK_LE.getValue();
                    } else if (checkNext1(ls, '<')) {
                        return Reserved.TK_SHL.getValue();
                    } else {
                        return '<';
                    }
                }
                case '>': {
                    next(ls);
                    if (checkNext1(ls, '=')) {
                        return Reserved.TK_GE.getValue();
                    } else if (checkNext1(ls, '>')) {
                        return Reserved.TK_SHR.getValue();
                    } else {
                        return '>';
                    }
                }
                case '/': {
                    next(ls);
                    if (checkNext1(ls, '/')) {
                        return Reserved.TK_IDIV.getValue();
                    } else {
                        return '/';
                    }
                }
                case '~': {
                    next(ls);
                    if (checkNext1(ls, '=')) {
                        return Reserved.TK_NE.getValue();
                    } else {
                        return '~';
                    }
                }
                case ':': {
                    next(ls);
                    if (checkNext1(ls, ':')) {
                        return Reserved.TK_DBCOLON.getValue();
                    } else {
                        return ':';
                    }
                }
                case '"':case '\'': {
                    readString(ls, ls.current, semInfo);
                    return Reserved.TK_STRING.getValue();
                }
                case '.': {
                    saveAndNext(ls);
                    if (checkNext1(ls, '.')) {
                        if (checkNext1(ls, '.')) {
                            return Reserved.TK_DOTS.getValue();
                        } else {
                            return Reserved.TK_CONCAT.getValue();
                        }
                    } else if (!isDigit(ls.current)) {
                        return '.';
                    } else {
                        return readNumeral(ls, semInfo);
                    }
                }
                case '0': case '1': case '2': case '3':case '4':
                case '5': case '6': case '7': case '8': case '9': {
                    return readNumeral(ls, semInfo);
                }
                case ZIO.EOZ: {
                    return Reserved.TK_EOS.getValue();
                }
                default: {
                    if (isAlpha(ls.current)) {
                        do {
                            saveAndNext(ls);
                        } while (isAlphanumericNum(ls.current));
                    }
                }
            }
        }
    }

    public static void luaXNext(LexState ls) throws Exception {
        ls.lastLine = ls.lineNumber;
        if (ls.lookahead.token != Reserved.TK_EOS.getValue()) {
            ls.t = ls.lookahead;
            ls.lookahead.token = Reserved.TK_EOS.getValue();
        } else {
            ls.t.token = llex(ls, ls.t.semInfo);
        }
    }

    public static int luaXLookahead(LexState ls) throws Exception {
        assert (ls.lookahead.token == Reserved.TK_EOS.getValue());
        ls.lookahead.token = llex(ls, ls.lookahead.semInfo);
        return ls.lookahead.token;
    }

}

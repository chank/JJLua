package com.chank.lua.parser;

import com.chank.lua.LuaState;
import com.chank.lua.util.ZIO;

/**
 * Created by chank on 2017/5/22.
 */
public final class LexState {

    int current;
    int lineNumber;
    int lastLine;
    LuaLexer.Token t;
    LuaLexer.Token lookahead;
    LuaParser.FuncState fs;
    LuaState l;
    ZIO z;
    ZIO.MBuffer buff;

    final int incLineNumber() {
        lineNumber += 1;
        return lineNumber;
    }

}

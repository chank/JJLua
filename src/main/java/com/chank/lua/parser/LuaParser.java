package com.chank.lua.parser;

import com.chank.lua.LuaObject;

/**
 * Created by chank on 2017/2/23.
 */
public final class LuaParser {

    protected static final class FuncState {
        private LuaObject.Proto f;
        private FuncState Prev;
        private LuaLexer.LexState ls;
    }
}

package com.chank.lua.parser;

import static com.chank.lua.parser.LuaLexer.FIRST_RESERVED;

/**
 * Created by chank on 2017/5/19.
 */
public enum Reserved {
    TK_AND(FIRST_RESERVED),
    TK_BREAK,
    TK_DO,
    TK_ELSE,
    TK_ELSEIF,
    TK_END,
    TK_FALSE,
    TK_FOR,
    TK_FUNCTION,
    TK_GOTO,
    TK_IF,
    TK_IN,
    TK_LOCAL,
    TK_NIL,
    TK_NOT,
    TK_OR,
    TK_REPEAT,
    TK_RETURN,
    TK_THEN,
    TK_TRUE,
    TK_UNTIL,
    TK_WHILE,
    // Other terminal symbols
    TK_IDIV,
    TK_CONCAT,
    TK_DOTS,
    TK_EQ,
    TK_GE,
    TK_LE,
    TK_NE,
    TK_SHL,
    TK_SHR,
    TK_DBCOLON,
    TK_EOS,
    TK_FLT,
    TK_INT,
    TK_NAME,
    TK_STRING;

    private int value;
    private static int nextValue;

    Reserved() {
        this(Counter.nextValue);
    }

    Reserved(int value) {
        this.value = value;
        Counter.nextValue = value + 1;
    }

    public final int getValue() {
        return value;
    }

    private static final class Counter {
        private static int nextValue = 0;
    }

}

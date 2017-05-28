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
import com.chank.lua.util.ZIO;

/**
 * Created by Chank on 2017/5/22.
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
    LuaObject.Table h;
    LuaParser.DynData dyd;
    String source;
    String envn;

    final int incLineNumber() {
        lineNumber += 1;
        return lineNumber;
    }

}

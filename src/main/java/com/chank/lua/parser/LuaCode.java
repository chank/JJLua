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
import com.chank.lua.LuaTValue;

/**
 * Created by Chank on 2017/5/23.
 */
public final class LuaCode {

    public static final int NO_JUMP = -1;

    public static int luaKStringK(LuaParser.FuncState fs, String s) {
        LuaTValue o = null;
        return addK(fs, o, o);
    }

    private static int addK(LuaParser.FuncState fs, LuaTValue key, LuaTValue v) {
        LuaState l = fs.ls.l;
        LuaObject.Proto f = fs.f;
        return 0;
    }

}

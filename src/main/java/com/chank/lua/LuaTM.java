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
public class LuaTM {

    public static enum TMS {
        TM_INDEX,
        TM_NEWINDEX,
        TM_GC,
        TM_MODE,
        TM_LEN,
        TM_EQ,
        TM_ADD,
        TM_SUB,
        TM_MUL,
        TM_MOD,
        TM_POW,
        TM_DIV,
        TM_IDIV,
        TM_BAND,
        TM_BOR,
        TM_BXOR,
        TM_SHL,
        TM_SHR,
        TM_UNM,
        TM_BNOT,
        TM_LT,
        TM_LE,
        TM_CONCAT,
        TM_CALL,
        TM_N
    }
}

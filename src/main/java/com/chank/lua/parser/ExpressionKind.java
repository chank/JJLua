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

/**
 * Created by Chank on 2017/5/21.
 */
public enum ExpressionKind {
    VVOID(0),
    VNIL(1),
    VTRUE(2),
    VFALSE(3),
    VK(4),
    VKFLT(5),
    VKINT(6),
    VNONRELOC(7),
    VLOCAL(8),
    VUPVAL(9),
    VINDEXED(10),
    VJMP(11),
    VRELOCABLE(12),
    VCALL(13),
    VVARARG(14);

    private int value;

    ExpressionKind(int value) {
        this.value = value;
    }

    public static boolean vkIsVar(int k) {
        return VLOCAL.getValue() <= k && k <= VINDEXED.getValue();
    }

    public static boolean vkIsInreg(int k) {
        return k == VNONRELOC.getValue() || k == VLOCAL.getValue();
    }

    public static boolean hasMultret(int k) {
        return k == VCALL.getValue() || k == VVARARG.getValue();
    }

    public int getValue() {
        return value;
    }

}

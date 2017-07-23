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

import javafx.scene.control.Tab;

/**
 * @author Chank
 */
public final class LuaTable {

    public static final int MAX_A_BITS = LuaLimits.CHAR_BIT - 1;
    public static final int MAX_A_SIZE = 1 << MAX_A_BITS;

    public static final int MAX_H_BITS = MAX_A_BITS - 1;

    public static LuaObject.Node gNode(LuaObject.Table t, int i) {
        return t.node[i];
    }

    public static LuaTValue gVal(LuaObject.Node n) {
        return n.iVal;
    }

    public static int gNext(LuaObject.Node n) {
        return n.iKey.nk.next;
    }

    public static LuaTValue gKey(LuaObject.Node n) {
        return n.iKey.tvk;
    }

    public static LuaObject.TKey.NK wgKey(LuaObject.Node n) {
        return n.iKey.nk;
    }

    public static int hashPow2(LuaObject.Table t, int n) {
        return 0;
    }

    public static int hashInt(LuaObject.Table t, int i) {
        return hashPow2(t, i);
    }

    private static int mainPosition(LuaObject.Table t, LuaTValue key) {
        int type = LuaObject.ttype(key);
        return type;
    }

    private static int arrayIndex(LuaTValue key) {
        return 0;
    }

    private static int findIndex(LuaState l, LuaObject.Table t, StkId key) {
        int i = 0;
        return i;
    }

    public static int luaHNext(LuaState l, LuaObject.Table t, StkId key) {
        int i = findIndex(l, t, key);
        for (; i < t.sizeArray; i++) {
        }
        return 0;
    }

    private static int computeSizes(int[] nums, int pna) {
        int i;
        int twoToI;
        int a = 0;
        int na = 0;
        int optimal = 0;
        for (i = 0, twoToI = 1; pna > twoToI / 2; i++, twoToI *= 2) {
            if (nums[i] > 0) {
                a += nums[i];
                if (a > twoToI / 2) {
                    optimal = twoToI;
                    na = a;
                }
            }
        }
        pna = na;
        return optimal;
    }

    private static int countInt(LuaTValue key, int[] nums) {
        int k = arrayIndex(key);
        if (k != 0) {
            return 1;
        } else {
            return 0;
        }
    }

    private static int numUseArray(LuaObject.Table t, int[] nums) {
        int lg;
        int ttlg;
        int ause = 0;
        int i = 1;
        for (lg = 0, ttlg = 1; lg <= MAX_A_BITS; lg++, ttlg *= 2) {
            int lc = 0;
            int lim = ttlg;
            if (lim > t.sizeArray) {
                lim = t.sizeArray;
                if (i > lim) {
                    break;
                }
            }
            for (; i <= lim; i++) {
                if (LuaObject.ttIsNil(t.array[i - 1])) {
                    lc++;
                }
            }
            nums[lg] += lc;
            ause += lc;
        }
        return ause;
    }

    private static int numUseHash(LuaObject.Table t, int[] nums, int pna) {
        int totalUse = 0;
        int ause = 0;
        int i = 1;
        while (i-- != 0) {
            LuaObject.Node n = t.node[i];
            if (!LuaObject.ttIsNil(gVal(n))) {
                ause += countInt(gKey(n), nums);
                totalUse++;
            }
        }
        pna += ause;
        return totalUse;
    }

    private static void setArrayVector(LuaState l, LuaObject.Table t, int size) {
        int i;
        for (i = t.sizeArray; i < size; i++) {
            LuaObject.setNilValue(t.array[i]);
        }
        t.sizeArray = size;
    }

    private static void setNodeVector(LuaState l, LuaObject.Table t, int size) {
        if (size == 0) {
            t.node = null;
            t.lSizeNode = 0;
            t.lastFree = null;
        } else {
            int i;
            int lSize = 0;
            if (lSize > MAX_A_BITS) {
            }
            size = 0;
            t.node = null;
            for (i = 0; i < size; i++) {
                LuaObject.Node n = gNode(t, i);
                gNext(n);
            }
        }
        t.lSizeNode = 0;
        t.lastFree = gNode(t, size);
    }

    public static void luaHResize(LuaState l, LuaObject.Table t, int naSize, int nhSize) {
        int i;
        int j;
        int oldASize = t.sizeArray;
        int oldHSize = 0;
        LuaObject.Node[] nold = t.node;
        if (naSize > oldASize) {
        }
    }

    public static void luaHResizeArray(LuaState l, LuaObject.Table t, int naSize) {
        int nsize = 0;
        luaHResize(l, t, naSize, nsize);
    }

    private static void rehash(LuaState l, LuaObject.Table t, LuaTValue ek) {
    }

    public static LuaObject.Table luaHNew(LuaState l) {
        return null;
    }

    public static void luaHFree(LuaState l, LuaObject.Table t) {
    }

    private static LuaObject.Node getFreePos(LuaObject.Table t) {
        return null;
    }

    public static LuaTValue luaHNewKey(LuaState l, LuaObject.Table t, LuaTValue key) {
        return null;
    }

    public static LuaTValue luaHGetInt(LuaObject.Table t, int key) {
        return null;
    }

    public static LuaTValue luaHGetShortStr(LuaObject.Table t, String key) {
        return null;
    }

    private static LuaTValue getGeneric(LuaObject.Table t, LuaTValue key) {
        return null;
    }

    public static LuaTValue luaHGetStr(LuaObject.Table t, String key) {
        return null;
    }

    public static LuaTValue luaHGet(LuaObject.Table t, LuaTValue key) {
        return null;
    }

    public static LuaTValue luaHSet(LuaState l, LuaObject.Table t, LuaTValue key) {
        return null;
    }

    public static void luaHSetInt(LuaState l, LuaObject.Table t, int key, LuaTValue value) {
    }

    private static int unboundSearch(LuaObject.Table t, int j) {
        return 0;
    }

    public static int luaHGetN(LuaObject.Table t) {
        return 0;
    }

}

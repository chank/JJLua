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

package com.chank.lua.util;

/**
 * @author Chank
 */
public final class ZIOUtil {

    public static final byte getChar(ZIO z) {
        return 0;
    }

    public static final void buffRemove(ZIO.MBuffer buff, int i) {
        buff.n -= i;
    }

    public static final void resetBuffer(ZIO.MBuffer buff) {
        buff.n = 0;
    }

}

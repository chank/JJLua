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
 * Created by chank on 2017/2/23.
 */
public final class ZIO {

    public static final int EOZ = -1;

    private int n;
    private byte[] p;

    public static final class MBuffer {
        private char[] buffer;
        private int n;
        private int buffSize;

        public void initBuffer() {
            buffer = null;
            buffSize = 0;
        }

        public void buffRemove(MBuffer buff, int i) {
            n -= i;
        }

        public void resetBuffer() {
            n = 0;
        }

        public char[] getBuffer() {
            return buffer;
        }

        public int getBuffSize() {
            return buffSize;
        }

        public int getBuffLen() {
            return n;
        }


        public void resizeBuffer(int size) {
        }

    }

}

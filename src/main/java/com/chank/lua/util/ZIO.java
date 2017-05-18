package com.chank.lua.util;

/**
 * Created by chank on 2017/2/23.
 */
public final class ZIO {

    public static final int EOZ = -1;

    private int n;
    private byte[] p;

    public static final class MBuffer {
        public byte[] buffer;
        public int n;
        public int buffSize;
    }
}

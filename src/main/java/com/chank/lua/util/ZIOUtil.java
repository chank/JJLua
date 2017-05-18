package com.chank.lua.util;

/**
 * Created by chank on 2017/2/23.
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

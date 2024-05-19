package cool.muyucloud.graime.util;

import java.util.Date;

public class Clock {
    private static long OFFSET = 0;

    public static void forward(Number number) {
        OFFSET += number.longValue();
    }

    public static void reset() {
        OFFSET = 0;
    }

    public static long getTime() {
        return new Date().getTime() + OFFSET;
    }
}

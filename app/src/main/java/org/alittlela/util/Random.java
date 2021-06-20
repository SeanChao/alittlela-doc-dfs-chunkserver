package org.alittlela.util;

public class Random {
    public static int randomPort(int start, int end) {
        return start + (new java.util.Random().nextInt(end - start + 1));
    }

    public static int randomPort(int start) {
        return randomPort(start, 65535);
    }
}

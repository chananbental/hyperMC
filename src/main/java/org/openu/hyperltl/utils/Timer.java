package org.openu.hyperltl.utils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Timer {

    private long start;

    public void start() {
        start = System.nanoTime();
    }

    public long stop() {
        return System.nanoTime() - start;
    }

    public static double toMs(long ns) {
        return ns / 1_000_000.0;
    }

    public static <T> T runAndTime(Supplier<T> action, Consumer<Double> logger) {
        Timer timer = new Timer();
        timer.start();
        T t = action.get();
        logger.accept(toMs(timer.stop()));

        return t;
    }
}
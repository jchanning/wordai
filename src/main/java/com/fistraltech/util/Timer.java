package com.fistraltech.util;

/**
 * Simple wall-clock timer that measures elapsed milliseconds between construction and a
 * call to {@link #stop()}.
 *
 * <p>Usage:
 * <pre>{@code
 * Timer t = new Timer();
 * // ... do work ...
 * t.stop();
 * long ms = t.getElapsed();
 * }</pre>
 */
public class Timer {
    private long start;
    private long end;

    public Timer(){
        this.start = System.currentTimeMillis();
    }

    public void stop(){
        this.end = System.currentTimeMillis();
    }

    public long getElapsed(){
        return end - start;
    }
}

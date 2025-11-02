package com.fistraltech.util;

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

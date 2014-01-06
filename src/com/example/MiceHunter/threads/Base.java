package com.example.MiceHunter.threads;

import com.example.MiceHunter.domain.Hunter;

import java.util.List;

/**
 * @author dzmiter
 */
public abstract class Base extends Thread {
    protected long delay;
    protected boolean isActive;
    protected List<String> huntReport;
    protected List<Hunter> hunters;

    public Base(long delay, boolean isActive, List<String> huntReport, List<Hunter> hunters) {
        this.delay = delay;
        this.isActive = isActive;
        this.huntReport = huntReport;
        this.hunters = hunters;
    }

    public void stopThread() {
        isActive = false;
    }

    public void startThread() {
        isActive = true;
    }

    protected abstract void refresh();
}
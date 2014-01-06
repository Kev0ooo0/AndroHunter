package com.example.MiceHunter.threads;

import com.example.MiceHunter.domain.Hunter;

import java.util.List;

/**
 * @author dzmiter
 */
public abstract class Hunting extends Base {

    public Hunting(List<String> huntReport, List<Hunter> hunters) {
        super(60000L, true, huntReport, hunters);
    }

    @Override
    public void run() {
        int i = 0;
        while (isActive) {
            boolean redraw = hunt();
            if (i == 0) {
                getBonus();
                redraw = true;
            }
            // 4 times a day
            i = (i > 21600000L / delay) ? 0 : i + 1;
            if (redraw) {
                refresh();
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected boolean hunt() {
        boolean redraw = false;
        for (Hunter hunter : hunters) {
            String result = hunter.hunt();
            if (result.length() > 0) {
                huntReport.add(result);
                redraw = true;
            }
        }
        return redraw;
    }

    protected void getBonus() {
        for (Hunter hunter : hunters) {
            String result = hunter.getBonus();
            huntReport.add(result);
        }
    }
}
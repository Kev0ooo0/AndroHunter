package com.example.MiceHunter.threads;

import com.example.MiceHunter.domain.Hunter;

import java.util.List;

/**
 * @author dzmiter
 */
public abstract class MarketFraud extends Base {

    public MarketFraud(List<String> huntReport, List<Hunter> hunters) {
        super(1000L, true, huntReport, hunters);
    }

    @Override
    public void run() {
        int i = 0;
        while (isActive) {
            boolean redraw = buySS();
            if (i == 0) {
                redraw = redraw || sellSS();
            }
            i = (i > 3600000L / delay) ? 0 : i + 1;
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

    protected boolean buySS() {
        boolean redraw = false;
        if (hunters.size() > 0) {
            int hunterNumber = (int) (Math.random() * hunters.size());
            Hunter hunter = hunters.get(hunterNumber);
            List<String> result = hunter.buyCheapSS();
            if (result.size() > 0) {
                huntReport.addAll(result);
                redraw = true;
            }
        }
        return redraw;
    }

    protected boolean sellSS() {
        boolean redraw = false;
        for (Hunter hunter : hunters) {
            String result = hunter.sellSS();
            if (result.length() > 0) {
                huntReport.add(result);
                redraw = true;
            }
        }
        return redraw;
    }
}
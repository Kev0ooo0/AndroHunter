package com.example.MiceHunter.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.MiceHunter.R;
import com.example.MiceHunter.domain.Hunter;
import com.example.MiceHunter.threads.Base;
import com.example.MiceHunter.threads.Hunting;
import com.example.MiceHunter.threads.MarketFraud;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HunterActivity extends Activity {

    private static final int REPORT_HUNT_CAPACITY = 200;
    private static final String CONFIG_FILE_PATH = "/Android/mice2012_users.json";
    private Base[] threads = new Base[2];
    private List<String> huntReport;
    private List<Hunter> hunters;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fitButtonPanel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        hunters = new CopyOnWriteArrayList<Hunter>();
        huntReport = new CopyOnWriteArrayList<String>();
        tryToReadUsers();

        final Button exitBtn = (Button) findViewById(R.id.exit);
        final Button runMainBtn = (Button) findViewById(R.id.runMain);
        final Button runSSBtn = (Button) findViewById(R.id.runSS);
        final Button refreshBtn = (Button) findViewById(R.id.refresh);
        fitButtonPanel();

        exitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                System.exit(0);
            }
        });

        runMainBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Base hunterThread = threads[0];
                if (hunterThread == null || !hunterThread.isAlive()) {
                    runMainBtn.setText(R.string.stopMain);
                    hunterThread = new HuntingImpl(huntReport, hunters);
                    hunterThread.start();
                    threads[0] = hunterThread;
                } else {
                    stopThread(hunterThread, runMainBtn, R.string.runMain);
                }
            }
        });

        runSSBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Base ssThread = threads[1];
                if (ssThread == null || !ssThread.isAlive()) {
                    runSSBtn.setText(R.string.stopSS);
                    ssThread = new MarketFraudImpl(huntReport, hunters);
                    ssThread.start();
                    threads[1] = ssThread;
                } else {
                    stopThread(ssThread, runSSBtn, R.string.runSS);
                }
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                hunters.clear();
                tryToReadUsers();
                StringBuilder sb = new StringBuilder("Новые охотники -> ");
                for (Hunter uc : hunters) {
                    sb.append(uc.getName());
                    sb.append(" {");
                    sb.append(uc.getId());
                    sb.append("}, ");
                }
                sb.setLength(sb.length() - 2);
                sb.append(".");
                huntReport.add(sb.toString());
                redrawReport();
            }
        });

    }

    private void stopThread(final Base thread, final Button button, final int buttonText) {
        new Thread() {
            public void run() {
                thread.stopThread();
                runOnUiThread(new Runnable() {
                    public void run() {
                        button.setText(R.string.stopping);
                        button.setEnabled(false);
                    }
                });

                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        button.setText(buttonText);
                        button.setEnabled(true);
                    }
                });
            }
        }.start();
    }

    private void tryToReadUsers() {
        try {
            readUsersFromSD();
        } catch (Exception e) {
            huntReport.add("Exception while reading users -> " + e.getMessage());
        }
    }

    private void readUsersFromSD() throws IOException, ParseException {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        File usersFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + CONFIG_FILE_PATH);
        JSONParser parser = new JSONParser();
        JSONArray users = (JSONArray) parser.parse(new FileReader(usersFile));
        for (Object user : users) {
            JSONObject jsonUser = (JSONObject) user;
            Long id = (Long) jsonUser.get("id");
            String authKey = (String) jsonUser.get("authKey");
            String name = (String) jsonUser.get("name");

            hunters.add(new Hunter(id, authKey, name));
        }
    }

    private void fitButtonPanel() {
        final Button[] buttons = new Button[]{
                (Button) findViewById(R.id.exit),
                (Button) findViewById(R.id.runSS),
                (Button) findViewById(R.id.runMain),
                (Button) findViewById(R.id.refresh)
        };

        for (Button button : buttons) {
            Display display = getWindowManager().getDefaultDisplay();
            button.setWidth(display.getWidth() / buttons.length);
        }
    }

    private void redrawReport() {
        final TextView textField = (TextView) findViewById(R.id.textView);
        if (huntReport.isEmpty()) {
            textField.setText("");
            return;
        }

        while (huntReport.size() > REPORT_HUNT_CAPACITY) {
            huntReport.remove(0);
        }

        StringBuilder reportText = new StringBuilder();
        for (String hunt : huntReport) {
            reportText.append(hunt).append("\n");
        }

        textField.setText(reportText.toString());
    }

    private class MarketFraudImpl extends MarketFraud {
        public MarketFraudImpl(List<String> huntReport, List<Hunter> hunters) {
            super(huntReport, hunters);
        }

        @Override
        protected void refresh() {
            runOnUiThread(new Runnable() {
                public void run() {
                    redrawReport();
                }
            });
        }
    }

    private class HuntingImpl extends Hunting {
        public HuntingImpl(List<String> huntReport, List<Hunter> hunters) {
            super(huntReport, hunters);
        }

        @Override
        protected void refresh() {
            runOnUiThread(new Runnable() {
                public void run() {
                    redrawReport();
                }
            });
        }
    }
}
package com.example.MiceHunter;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HunterActivity extends Activity {

    private static final int REPORT_HUNT_CAPACITY = 100;
    private static final String CONFIG_FILE_PATH = "/Android/mice2012_users.json";
    private static volatile boolean isActive = true;
    private Thread hunterThread;
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

        hunters = new ArrayList<Hunter>();
        huntReport = new CopyOnWriteArrayList<String>();

        tryToReadUsers();
        hunterThread = createMainThread();

        final Button exitBtn = (Button) findViewById(R.id.exit);
        final Button runBtn = (Button) findViewById(R.id.run);
        final Button stopBtn = (Button) findViewById(R.id.stop);
        final Button refreshBtn = (Button) findViewById(R.id.refresh);

        fitButtonPanel();
        stopBtn.setEnabled(false);

        exitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                System.exit(0);
            }
        });

        runBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isActive = true;
                if (!hunterThread.isAlive()) {
                    runBtn.setEnabled(false);
                    hunterThread = createMainThread();
                    hunterThread.start();
                    createSSThread().start();
                }
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                isActive = false;
                stopBtn.setEnabled(false);
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (hunters == null) {
                    hunters = new ArrayList<Hunter>();
                } else {
                    hunters.clear();
                }

                tryToReadUsers();
                StringBuilder sb = new StringBuilder("Users refreshed. New users -> ");
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

    private void tryToReadUsers() {
        try {
            readUsersFromSD();
        } catch (IOException e) {
            huntReport.add("IO exception -> " + e.getMessage());
        } catch (ParseException e) {
            huntReport.add("Parse json exception -> " + e.getMessage());
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
                (Button) findViewById(R.id.run),
                (Button) findViewById(R.id.stop),
                (Button) findViewById(R.id.refresh)
        };

        for (Button button : buttons) {
            Display display = getWindowManager().getDefaultDisplay();
            button.setWidth(display.getWidth() / buttons.length);
        }
    }

    private void toggleButtons(boolean isActive) {
        final Button runBtn = (Button) findViewById(R.id.run);
        final Button stopBtn = (Button) findViewById(R.id.stop);
        final Button refreshBtn = (Button) findViewById(R.id.refresh);

        runBtn.setEnabled(!isActive);
        stopBtn.setEnabled(isActive);
        refreshBtn.setEnabled(!isActive);
    }

    private void redrawReport() {
        final TextView textField = (TextView) findViewById(R.id.textView);
        if (huntReport == null || huntReport.size() == 0) {
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

    private Thread createMainThread() {
        return new Thread() {
            private final long DELAY = 60000L;

            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        toggleButtons(isActive);
                    }
                });

                int i = 0;
                while (isActive) {
                    try {
                        boolean redraw = hunt();
                        if (i == 0) {
                            redraw = redraw || getBonus();
                        }
                        // 4 times a day
                        i = (i > 21600000L / DELAY) ? 0 : i + 1;
                        if (redraw) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    redrawReport();
                                }
                            });
                        }
                        Thread.sleep(DELAY);
                    } catch (Exception e) {
                        huntReport.add("Exception while hunting -> " + e.getMessage());
                    }
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        toggleButtons(isActive);
                    }
                });
            }

            private boolean hunt() {
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

            private boolean getBonus() {
                boolean redraw = false;
                for (Hunter hunter : hunters) {
                    String result = hunter.getBonus();
                    if (result.length() > 0) {
                        huntReport.add(result);
                        redraw = true;
                    }
                }
                return redraw;
            }
        };
    }

    private Thread createSSThread() {
        return new Thread() {
            private final long DELAY = 1000L;

            public void run() {
                int i = 0;
                while (isActive) {
                    try {
                        boolean redraw = randomBuySS();
                        if (i == 0) {
                            redraw = redraw || sellSS();
                        }
                        i = (i > 3600000L / DELAY) ? 0 : i + 1;
                        if (redraw) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    redrawReport();
                                }
                            });
                        }
                        Thread.sleep(DELAY);
                    } catch (Exception e) {
                        huntReport.add("Exception while doing money on market -> " + e.getMessage());
                    }
                }
            }

            private boolean randomBuySS() {
                boolean redraw = false;
                int hunterNumber = (int) (Math.random() * hunters.size());
                Hunter hunter = hunters.get(hunterNumber);
                List<String> result = hunter.buyCheapSS();
                if (result.size() > 0) {
                    huntReport.addAll(result);
                    redraw = true;
                }
                return redraw;
            }

            private boolean sellSS() {
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
        };
    }
}
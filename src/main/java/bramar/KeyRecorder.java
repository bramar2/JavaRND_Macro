package bramar;

import bramar.swing.JSwitchButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bramar.Main.newFont;

public class KeyRecorder {
    private final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    final JFrame frame;
    private JButton color;
    private boolean recording = false;
    private JSwitchButton btnAutomaticDelay;
    private boolean automaticDelay = false;
    private NativeKeyListener listener;
    private final List<String> keys = new ArrayList<>();
    // automatic delay
    private final List<Long> delays = new ArrayList<>();
    private long lastMs = 0;
    KeyRecorder() {
        frame = new JFrame("Key Recorder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(getContent());
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    public void record() {
        recording = true;

        btnAutomaticDelay.setDisabled(true);
        GlobalScreen.addNativeKeyListener(listener = new NativeKeyListener() {
            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}

            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                keys.add(convert(e.getKeyCode()));
                if(automaticDelay) {
                    if(lastMs != 0) {
                        delays.add(System.currentTimeMillis() - lastMs);
                    }else delays.add(0L);
                    lastMs = System.currentTimeMillis();
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {}
        });
        color.setBackground(Color.green);
        if(automaticDelay) {
            lastMs = 0;
            delays.clear();
        }
    }
    private final Map<Integer, String> cCache = new HashMap<>();
    private String convert(int keycode) {
        String s = cCache.getOrDefault(keycode, null);
        if(s != null) return s;
        try {
            String prefix = "VC_";
            for(Field field : NativeKeyEvent.class.getDeclaredFields()) {
                String fn = field.getName();
                if(fn.startsWith(prefix) && (int) field.get(null) == keycode) {
                    String o = fn.substring(3);
                    cCache.put(keycode, o);
                    return o;
                }
            }
        }catch(Exception e1) {
            throw new RuntimeException(e1);
        }
        Main.debug("Keycode (deemed) invalid [" + keycode + "]. JNativehook might not detect: Right shift, numpad-plus, numpad-minus and other built-in non-formal buttons");
        throw new RuntimeException("name not found from keycode (jnh) | keycode=" + keycode);
    }
    public static String prettyPrint(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonElement = JsonParser.parseString(json);
        return gson.toJson(jsonElement);
    }
    private static class SKey {
        String keyname;
        int count;
        SKey(String keyname, int count) {
            this.keyname = keyname;
            this.count = count;
        }
    }
    public void stopRecord() {
        recording = false;
        lastMs = 0;
        if(listener != null) GlobalScreen.removeNativeKeyListener(listener);

        List<SKey> keyList = new ArrayList<>();
        String last = null;
        int lastCount = 0;
        for(String key : keys) {
            if(last != null && !last.equals(key)) {
                keyList.add(new SKey(last, lastCount));
                lastCount = 0;
            }
            last = key;
            lastCount++;
        }
        if(lastCount > 0) keyList.add(new SKey(last, lastCount));

        JSONObject json = new JSONObject();
        JSONArray arr = new JSONArray();
        for(int i = 0; i < keyList.size(); i++) {
            SKey skey = keyList.get(i);
            JSONObject jKey = new JSONObject();
            jKey.put("key", skey.keyname);
            jKey.put("count", skey.count);
            if(automaticDelay) {
                int delay = (int) (long) delays.get(i);
                if(delay != 0) jKey.put("delay", delay);
            }
            arr.add(jKey);
        }
        json.put("keys", arr);
        try {
            File file = new File("record-latest.json");
            if(!file.exists() && !file.createNewFile()) throw new IOException("failed to create record-latest.json (file)");
            String jsonString = prettyPrint(json.toJSONString());

            PrintWriter writer = new PrintWriter(file);
            String timeStr = DTF.format(LocalDateTime.now());
            PrintWriter w2 = new PrintWriter("record-" + timeStr + ".json");
            writer.write(jsonString);
            w2.write(jsonString);
            writer.close();
            w2.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }


        keys.clear();
        delays.clear();
        color.setBackground(Color.red);
        btnAutomaticDelay.setDisabled(false);
    }
    public void openLatest() {
        File file = new File("record-latest.json");
        if(Desktop.isDesktopSupported()) {
            if(!file.exists()) {
                try {
                    new Thread(() ->
                            JOptionPane.showMessageDialog(
                                    null,
                                    "There is no latest JSON record to open",
                                    "Not found",
                                    JOptionPane.ERROR_MESSAGE
                            )
                    ).start();
                    return;
                }catch(Exception e1) {
                    e1.printStackTrace();
                    System.exit(-1);
                }
            }
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to open config");
            }
        }else {
            new Thread(() ->
                    JOptionPane.showMessageDialog(
                            null,
                            "Your platform does not support this operation",
                            "Not supported",
                            JOptionPane.ERROR_MESSAGE
                    )
            ).start();
        }
    }
    public JPanel getContent() {
        JPanel content = new JPanel();
        // setup
        content.setPreferredSize(new Dimension(600, 650));
        content.setLayout(null);
        JLabel l1 = new JLabel("Key Recorder");
        JLabel toggle = new JLabel("Record");
        color = new JButton();
        JButton sw = new JButton("Macro");
        JButton ol = new JButton("Open Latest");
        // toggle automatic delay
        JLabel adLabel = new JLabel("Toggle Automatic Delay");
        JSwitchButton toggleAd = new JSwitchButton(150, 45);
        btnAutomaticDelay = toggleAd;

        l1.setFont(newFont(30));
        toggle.setFont(newFont(40));
        color.addActionListener((e) -> {
            if(recording) stopRecord();
            else record();
        });
        color.setBackground(Color.red);
        sw.addActionListener((e) -> Main.instance().switchGui());
        sw.setFont(newFont(13));
        ol.addActionListener((e) -> openLatest());
        ol.setFont(newFont(20));
        adLabel.setFont(newFont(17));
        toggleAd.addListener((on) -> automaticDelay = on);

        l1.setBounds(10, 0, 275, 50);
        toggle.setBounds(235, 111, 200, 75);
        color.setBounds(200, 175, 200, 150);
        sw.setBounds(475, 615, 125, 35);
        ol.setBounds(225, 400, 150, 60);
        adLabel.setBounds(10, 545, 240, 60);
        toggleAd.setBounds(220, 555, 150, 45);


        content.add(l1);
        content.add(color);
        content.add(toggle);
        content.add(sw);
        content.add(ol);
        content.add(toggleAd);
        content.add(adLabel);

        for(Component c : content.getComponents()) {
            if(c instanceof JButton) {
                c.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        }

        return content;
    }
}

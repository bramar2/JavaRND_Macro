package bramar;

import bramar.objects.Macro;
import bramar.objects.Switch;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Main {

    private static final String CONFIG = "config.json";
    private static final String DF_NAME;
    private static final int DF_STYLE;

    private static Main instance;
    private final Timer timer = new Timer(true);
    public static Main instance() {
        return instance;
    }

    public Timer getTimer() {
        return timer;
    }

    public void halt(long ms) {
        this.ms = System.currentTimeMillis() + ms;
    }
    public void switchGui() {
        visible = !visible;
        if(visible) {
            if(kr != null) kr.frame.setVisible(false);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    frame.setVisible(true);
                    cancel();
                }
            }, 250, 250);
        }else {
            frame.setVisible(false);
            if(kr == null) kr = new KeyRecorder();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    kr.frame.setVisible(true);
                    cancel();
                }
            }, 250, 250);
        }
    }


    static {
        Font df = new JLabel().getFont();
        DF_NAME = df.getFontName();
        DF_STYLE = df.getStyle();
    }
    static Font newFont(int size) {
        return new Font(DF_NAME, DF_STYLE, size);
    }

    public static void debug(String str) {
        if(debugMode) System.out.println("[DEBUG] " + str);
    }

    private static KeyRecorder kr;
    private static final boolean debugMode = false;
    public int delay;
    public final Robot robot = getRobot();
    private long ms = 0;
    private JFrame frame;
    private JPanel content;
    private boolean enabled = false;
    private JButton color;
    private boolean numpadOnly = false;
    private boolean visible = true;
    private final List<Macro> macros = new ArrayList<>();
    // jnativehook
    private final List<Integer> heldKeys = new ArrayList<>();

    // held macro
    private Switch heldSwitch = null;
    private long heldMs;

    private Thread switchThread;

    public Main() throws Exception {
        if(instance == null) instance = this;
        loadJSON();
        setupJNH();
        setupGUI();
    }
    public void setupJNH() {
        try {
            GlobalScreen.registerNativeHook();
            // disable STUPID UNNECESSARY logging
            LogManager.getLogManager().reset();
            Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
            System.out.println("--- GlobalScreen logging has been set to OFF ---");
            //
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {}

                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    int raw = e.getRawCode();

                    heldKeys.add(raw);

                    if(heldSwitch != null) {
                        boolean a = true;
                        if(System.currentTimeMillis() - heldMs < 5000) {
                            int num = -1;
                            boolean t;
                            if((t = (raw >= KeyEvent.VK_0 && raw <= KeyEvent.VK_9)) ||
                                    (raw >= KeyEvent.VK_NUMPAD0 && raw <= KeyEvent.VK_NUMPAD9)) {
                                if(!t || numpadOnly) num = getNumber(raw);
                            }else if(e.getKeyLocation() == NativeKeyEvent.KEY_LOCATION_STANDARD) {
                                // numpad with shift
                                if (raw == KeyEvent.VK_END) num = 1;
                                else if (raw == KeyEvent.VK_DOWN) num = 2;
                                else if (raw == KeyEvent.VK_PAGE_DOWN) num = 3;
                                else if (raw == KeyEvent.VK_LEFT) num = 4;
                                else if (raw == KeyEvent.VK_CLEAR) num = 5;
                                else if (raw == KeyEvent.VK_RIGHT) num = 6;
                                else if (raw == KeyEvent.VK_HOME) num = 7;
                                else if (raw == KeyEvent.VK_UP) num = 8;
                                else if (raw == KeyEvent.VK_PAGE_UP) num = 9;
                                else if (raw == KeyEvent.VK_INSERT) num = 0;
                            }
                            if(num != -1 && heldSwitch.contains(num)) {
                                Main.debug("run()");
                                switchThread = heldSwitch.run(num);
                            }else {
                                Main.debug("!contains num=" + num);
                                a = false;
                            }
                        }
                        if(a) heldSwitch = null;
                    }

                    if(visible && System.currentTimeMillis() >= ms) {
                        if (raw >= KeyEvent.VK_0 && raw <= KeyEvent.VK_9) {
                            if (!numpadOnly) onClick(getNumber(raw), e.getModifiers(), false);
                        } else if (raw >= KeyEvent.VK_NUMPAD0 && raw <= KeyEvent.VK_NUMPAD9) {
                            onClick(getNumber(raw), e.getModifiers(), false);
                        } else if (e.getKeyLocation() == NativeKeyEvent.KEY_LOCATION_STANDARD) {
                            // numpad with shift
                            boolean a = true;
                            while (a) {
                                a = false;
                                int n;
                                if (raw == KeyEvent.VK_END) n = 1;
                                else if (raw == KeyEvent.VK_DOWN) n = 2;
                                else if (raw == KeyEvent.VK_PAGE_DOWN) n = 3;
                                else if (raw == KeyEvent.VK_LEFT) n = 4;
                                else if (raw == KeyEvent.VK_CLEAR) n = 5;
                                else if (raw == KeyEvent.VK_RIGHT) n = 6;
                                else if (raw == KeyEvent.VK_HOME) n = 7;
                                else if (raw == KeyEvent.VK_UP) n = 8;
                                else if (raw == KeyEvent.VK_PAGE_UP) n = 9;
                                else if (raw == KeyEvent.VK_INSERT) n = 0;
                                else break;
                                onClick(n, e.getModifiers(), true);
                            }
                        }
                    }

//                    System.out.println(String.format("%s | %s | '%s' | %s", e.getKeyCode(), raw, NativeKeyEvent.getKeyText(e.getKeyCode()), e.getKeyLocation()));
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {
                    heldKeys.remove((Integer) e.getKeyCode());
                }
            });
        } catch (NativeHookException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }
    public void onClick(int numkey, int modifiers, boolean shift) {
        if(!enabled) return;
        int[] highestSize = new int[] {0};
        List<Macro> macros = this.macros.stream()
                .filter(macro -> {
                    Set<Integer> hold = macro.getHold();
                    if(hold.size() > highestSize[0]) highestSize[0] = hold.size();
                    for(int mask : hold) {
                        if((modifiers & mask) == 0 && !(mask == NativeKeyEvent.SHIFT_MASK && shift)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        if(macros.isEmpty()) return;
        int hs = highestSize[0];
        Macro macro = this.macros.stream().filter(m -> m.getHold().size() == hs).findFirst().get();
        Switch sw = macro.getSwitch(numkey);
        if(sw == null) return;
        if(sw.isSecond()) {
            heldSwitch = sw;
            heldMs = System.currentTimeMillis();
            Main.debug("isSecond()");
        }else switchThread = sw.run();
    }



    public void setupGUI() {
        frame = new JFrame("Macro");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(getContent());
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }
    public void toggleMacro() {
        enabled = !enabled;
        color.setBackground(enabled ? Color.green : Color.red);
        if(!enabled) {
            heldSwitch = null;
            heldMs = 0L;
            if(switchThread != null && switchThread.isAlive()) {
                switchThread.interrupt(); // After disabling, it should stop the macro
            }
        }
    }
    public void loadJSON() {
        File file = new File(CONFIG);
        if(!file.exists()) {
            try {
                Files.copy(Main.class.getResourceAsStream("/sample.json"), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }catch(Exception e1) {
                e1.printStackTrace();
                System.exit(-1);
            }
        }
        StringBuilder str = new StringBuilder();
        try {
            Scanner scan = new Scanner(file);
            while (scan.hasNext())
                str.append(scan.next()).append('\r');
        }catch(FileNotFoundException ignored) {}
        JSONObject json = (JSONObject) JSONValue.parse(str.toString());
        delay = Math.max(BigInteger.valueOf((long) json.get("delay")).intValue(), 0);
        if(json.containsKey("numpad_only")) numpadOnly = (boolean) json.get("numpad_only");
        macros.clear();
        for(Object each : (JSONArray) json.get("macros")) {
            try {
                macros.add(new Macro((JSONObject) each));
            }catch(Exception e1) {
                e1.printStackTrace();
            }
        }
        System.out.println("Macros successfully loaded. Printing them after GlobalScreen logging");
        // Print macros 1 second later using timer (already in the class)
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Macros loaded: " + macros);
            }
        }, 1000);

    }
    public void openConfig() {
        File file = new File(CONFIG);
        if(Desktop.isDesktopSupported()) {
            if(!file.exists()) {
                try {
                    Files.copy(Main.class.getResourceAsStream("/sample.json"), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
        if(content == null) {
            content = new JPanel();
            // setup
            content.setPreferredSize(new Dimension(600, 650));
            content.setLayout(null);
            JLabel l1 = new JLabel("Macro");
            JLabel toggle = new JLabel("Toggle");
            color = new JButton();
            JButton rlj = new JButton("Reload config");
            JButton ocfg = new JButton("Open config");
            JButton sw = new JButton("Key Recorder");

            l1.setFont(newFont(30));
            toggle.setFont(newFont(40));
            color.addActionListener((e) -> toggleMacro());
            color.setBackground(Color.red);
            rlj.addActionListener((e) -> loadJSON());
            rlj.setFont(newFont(20));
            ocfg.addActionListener((e) -> openConfig());
            ocfg.setFont(newFont(20));
            sw.addActionListener((e) -> switchGui());
            sw.setFont(newFont(13));

            l1.setBounds(10, 0, 100, 50);
            toggle.setBounds(235, 111, 200, 75);
            color.setBounds(200, 175, 200, 150);
            rlj.setBounds(200, 450, 200, 100);
            ocfg.setBounds(0, 600, 160, 50);
            sw.setBounds(475, 615, 125, 35);


            content.add(l1);
            content.add(color);
            content.add(toggle);
            content.add(rlj);
            content.add(ocfg);
            content.add(sw);

            for(Component c : content.getComponents()) {
                if(c instanceof JButton) {
                    c.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }
        }
        return content;
    }



    public static void main(String[] args) throws Exception {
        new Main();
    }
    private static Robot getRobot() {
        try {
            Robot rb = new Robot();
            rb.setAutoDelay(2);
            return rb;
        } catch (AWTException e) {
            e.printStackTrace();
        }
        System.exit(-1);
        return null;
    }

    private static final Map<String, Integer> kcCache = new HashMap<>();
    public static int getKeyCode(String str) {
        int i = kcCache.getOrDefault(str, -69);
        if(i != -69) return i;
        try {
            Field[] fields = KeyEvent.class.getDeclaredFields();
            String check = "VK_"+str.toUpperCase();
            for(Field field : fields) {
                if(field.getName().equals(check)) {
                    int o = (int) field.get(null);
                    kcCache.put(str, o);
                    return o;
                }
            }
        }catch(Exception e1) {
            throw new RuntimeException(e1);
        }
        throw new RuntimeException("keycode not found");
    }
    public int getNumber(int kc) {
        for(char c : KeyEvent.getKeyText(kc).toCharArray()) {
            if(Character.isDigit(c)) return Integer.parseInt(""+c);
        }
        return -1;
    }
}

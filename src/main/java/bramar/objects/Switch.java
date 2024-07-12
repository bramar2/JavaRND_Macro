package bramar.objects;

import bramar.Main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;

public class Switch {
    protected final int numkey;
    protected final List<Key> keys;
    private final int estimatedMs;
    // for second number purposes
    protected Map<Integer, List<Key>> sKeys = null;


    public Switch(int numkey, List<Key> keys) {
        this.numkey = numkey;
        this.keys = keys;
        int totalKeys = 0;
        for(Key key : keys) {
            totalKeys += key.count;
        }
        this.estimatedMs = 40 + totalKeys * Main.instance().delay;
    }
    public boolean isSecond() {
        return sKeys != null;
    }
    public boolean contains(int second) {
        return sKeys.containsKey(second);
    }
    public Thread run(int second) {
        Thread thread = new Thread(() -> {
            try {
                releaseKeys();
                Main.debug("run0(robot, " + sKeys.get(second) + ")");
                run0(Main.instance().robot, sKeys.get(second));
            }catch(InterruptedException e) {
                e.printStackTrace();
                System.out.println("Macro interrupted");
            }
        });
        thread.start();
        return thread;
    }
    public Thread run() {
        Thread thread = new Thread(() -> {
            try {
                releaseKeys();
                run0(Main.instance().robot, keys);
            }catch(InterruptedException e) {
                e.printStackTrace();
                System.out.println("Macro interrupted");
            }
        });
        thread.start();
        return thread;
    }



    private void releaseKeys() {
        Main.instance().halt(estimatedMs + 500); // prevent loops
        Robot robot = Main.instance().robot;
        robot.keyRelease(KeyEvent.VK_ALT);
        robot.keyRelease(KeyEvent.VK_SHIFT);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.delay(20);
    }
    private void run0(Robot robot, List<Key> keys) throws InterruptedException {
        int delay = Main.instance().delay;
        boolean a = delay != 0;
        for(Key key : keys) {
            for(int i = 0; i < key.count; i++) {
                robot.keyPress(key.key);
                robot.keyRelease(key.key);
                if(a || key.delay != 0) Thread.sleep(delay + key.delay);
            }
        }
    }

    @Override
    public String toString() {
        return "Switch{" +
                "numkey=" + numkey +
                ", keys=" + keys +
                ", estimatedMs=" + estimatedMs +
                ", sKeys=" + sKeys +
                '}';
    }
}

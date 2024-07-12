package bramar.objects;

import bramar.Main;
import org.jnativehook.NativeInputEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;

public class Macro {
    private static final Map<String, Integer> availableMasks = new HashMap<>();
    static {
        try {
            String suffix = "_MASK";
            int sl = suffix.length();
            for(Field field : NativeInputEvent.class.getDeclaredFields()) {
                String fn = field.getName();
//                System.out.println(fn + ", " + fn.endsWith(suffix));
                if(fn.endsWith(suffix)) {
//                    System.out.println(fn);
                    availableMasks.put(fn.substring(0, fn.length() - sl), (int) field.get(null));
                }
            }
        }catch(Exception e1) {
            throw new RuntimeException(e1);
        }
    }
    private final Set<Integer> hold = new HashSet<>();
    private final Set<Switch> switches = new HashSet<>();

    public Macro(JSONObject json) {
        JSONArray holdStr = (JSONArray) json.get("hold");
        for(Object hold : holdStr) {
            int kc = availableMasks.getOrDefault(hold.toString().toUpperCase(Locale.ROOT), -69);
            if(kc != -69)
                this.hold.add(kc);
        }
        if(hold.size() == 0) throw new IllegalArgumentException("hold param must not be empty");
        JSONArray switchA = (JSONArray) json.get("switches");
        for(Object switchObj : switchA) {
            JSONObject switchj = (JSONObject) switchObj;
            int numkey = BigInteger.valueOf((long) switchj.get("number")).intValue();
            boolean secondNumkey = (boolean) switchj.getOrDefault("second_num", false);
            List<Key> keys = new ArrayList<>();
            Map<Integer, List<Key>> map = new HashMap<>();
            if(!secondNumkey) {
                for (Object keyObj : (JSONArray) switchj.get("keys")) {
                    JSONObject keyj = (JSONObject) keyObj;
                    int keycode = Main.getKeyCode(String.valueOf(keyj.get("key")));
                    int l = BigInteger.valueOf((long) keyj.getOrDefault("count", 1L)).intValue();
                    int delay = BigInteger.valueOf((long) keyj.getOrDefault("delay", 0L)).intValue();
                    int count = Math.max(l, 1);
                    Key key = new Key(keycode, count);
                    key.delay = delay;
                    keys.add(key);
                }
            }else {
                for(Object numObj : (JSONArray) switchj.get("keys")) {
                    JSONObject numj = (JSONObject) numObj;
                    int nk = BigInteger.valueOf((long) numj.get("numkey")).intValue();
                    List<Key> keyList = new ArrayList<>();
                    for(Object keyObj : (JSONArray) numj.get("keys")) {
                        JSONObject keyj = (JSONObject) keyObj;
                        int keycode = Main.getKeyCode(String.valueOf(keyj.get("key")));
                        int l = BigInteger.valueOf((long) keyj.getOrDefault("count", 1L)).intValue();
                        int delay = BigInteger.valueOf((long) keyj.getOrDefault("delay", 0L)).intValue();
                        int count = Math.max(l, 1);
                        Key key = new Key(keycode, count);
                        key.delay = delay;
                        keyList.add(key);
                    }
                    map.put(nk, keyList);
                }
            }
            Switch sw = new Switch(numkey, keys);
            if(!map.isEmpty()) sw.sKeys = map;
            this.switches.add(sw);
        }
    }

    public Set<Integer> getHold() {
        return hold;
    }

    public Switch getSwitch(int numkey) {
        for(Switch sw : switches) {
            if(sw.numkey == numkey) {
                return sw;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Macro{" +
                "hold=" + hold +
                ", switches=" + switches +
                '}';
    }
}

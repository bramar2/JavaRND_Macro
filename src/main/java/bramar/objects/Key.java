package bramar.objects;

public class Key {
    protected final int key, count;
    protected int delay;

    public Key(int key, int count) {
        this.key = key;
        this.count = count;
    }

    @Override
    public String toString() {
        return "Key{" +
                "key=" + key +
                ", count=" + count +
                ", delay=" + delay +
                '}';
    }
}

package bramar.swing;

import bramar.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class JSwitchButton extends JComponent implements MouseListener {
    private static final long serialVersionUID = 1L;
    private static final int MIDDLE_PX = 5;
    private static final boolean MIDDLE_EVEN = MIDDLE_PX % 2 == 0;
    private static final Color BORDER_COLOR = new Color(122, 138, 153);
    private static final Color BORDER_HOVER_COLOR = new Color(108, 121, 136);
    private static final Color BG_COLOR = new Color(201, 218, 235);
    private static final int ANIMATION_MS = 250;
    private static final int ANIMATION_FRAMES = 20;
    private static final List<Color> RG_COLORS = getColorsBetween(Color.red, Color.green, ANIMATION_FRAMES);
    private static final List<Color> GR_COLORS = getColorsBetween(Color.green, Color.red, ANIMATION_FRAMES);
    private static final Timer timer = Main.instance().getTimer();




    private boolean isOn;
    private boolean hovered = false;
    private boolean disabled = false;
    private final List<ClickListener> clickListeners = new ArrayList<>();
    // going animation
    private boolean animation, toLeft = false;
    private int part = 0;

    private static List<Color> getColorsBetween(Color from, Color to, int amount) {
        List<Color> colors = new ArrayList<>();
        int fRed = from.getRed();
        int fGreen = from.getGreen();
        int fBlue = from.getBlue();
        int tRed = to.getRed();
        int tGreen = to.getGreen();
        int tBlue = to.getBlue();

        for(float i = 1; i <= amount; i++) {
            float blending = i / amount;
            float inverse = 1 - blending;

            float red = fRed * blending + tRed * inverse;
            float green = fGreen * blending + tGreen * inverse;
            float blue = fBlue * blending + tBlue * inverse;

            colors.add(new Color(red / 255f, green / 255f, blue / 255f));
        }
        return colors;
    }

    public JSwitchButton(int width, int height) {
        this(width, height, false);
    }
    public JSwitchButton(int width, int height, boolean defaultOn) {
        super();
        if(width <= 2 || height <= 2) throw new IllegalArgumentException("Invalid width/height");
        this.isOn = defaultOn;
        enableInputMethods(true);
        addMouseListener(this);

        setVisible(true);
        setSize(width, height);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    public void toggle() {
        if(disabled) return;
        isOn = !isOn;
        toLeft = isOn; // if it is on (left), it was off (right) so right to left
        animation = true;
        int perFrame = ANIMATION_MS / ANIMATION_FRAMES;
        Main.debug("perFrame=" + perFrame);
        TimerTask task = new TimerTask() {
            int frame = 1;
            @Override
            public void run() {
                if(frame == 0) {
                    part = 0;
                    animation = false;
                    repaint();
                    cancel();
                    return;
                }
                paintAnimation(frame);
                if(frame != ANIMATION_FRAMES) frame++;
                else frame = 0;
            }
        };
        timer.schedule(task, perFrame, perFrame);
    }
    private void paintAnimation(int frame) {
        part = frame;
        repaint();
    }
    public void addListener(ClickListener listener) {
        clickListeners.add(listener);
    }
    public void removeListener(ClickListener listener) {
        clickListeners.remove(listener);
    }

    public boolean isOn() {
        return isOn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int width0 = width - 1;
        int height0 = height - 1;
        int middle = width / 2;

        // base color
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, width, height);
        // border
        int thickness = 2;
        if(hovered) {
            thickness ++;
            g.setColor(BORDER_HOVER_COLOR);
        }else g.setColor(BORDER_COLOR);

        g.fillRect(0, 0, thickness, height0);
        g.fillRect(0, 0, width0, thickness);
        g.fillRect(0, height0 - thickness, width0, thickness);
        g.fillRect(width0 - thickness, 0, thickness, height0);

        g.setColor(Color.black);
        int half = MIDDLE_PX / 2;
        int bLeft = middle-half;
        int bRight = middle + half - 1; // from
        if(MIDDLE_EVEN) {
            g.fillRect(bLeft, 0, half, height-1);
        }else {
            g.fillRect(bLeft, 0, half + 1, height-1);
        }

        int halfWidth0 = width0 / 2 - 1;
        int thickness2 = 2 * thickness;
        if(animation) {
            Color color = (toLeft ? GR_COLORS : RG_COLORS).get(part-1);
            g.setColor(color);
            int diff = (int) (part / (float) ANIMATION_FRAMES * halfWidth0);
            if(toLeft) {
                g.fillRect(bRight - diff, thickness, halfWidth0 - thickness, height0 - thickness2);
            }else {
                g.fillRect(thickness + diff, thickness, bLeft - thickness, height0 - thickness2);
            }
        }else {
            if (isOn) {
                g.setColor(Color.green);
                g.fillRect(thickness, thickness, bLeft - thickness, height0 - thickness2);
            } else {
                g.setColor(Color.red);
                g.fillRect(bRight, thickness, halfWidth0 - thickness, height0 - thickness2);
            }
        }
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if(disabled) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }else {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if(animation) return;
        toggle();
        for(ClickListener listener : clickListeners) {
            listener.onClick(isOn);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        hovered = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hovered = false;
        repaint();
    }
}

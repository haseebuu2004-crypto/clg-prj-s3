package skillswap.ui;

import java.awt.Color;
import java.awt.Font;

final class Theme {
    // Warm, campus-friendly palette with deep ink for dependable contrast.
    static final Color NAVY = new Color(48, 42, 61);
    static final Color NAVY_LIGHT = new Color(171, 68, 62);
    static final Color BLUE = new Color(190, 81, 69);
    static final Color BLUE_PALE = new Color(252, 235, 229);
    static final Color TEAL = new Color(37, 135, 124);
    static final Color GREEN = new Color(49, 131, 95);
    static final Color GREEN_PALE = new Color(232, 244, 235);
    static final Color AMBER = new Color(174, 112, 33);
    static final Color AMBER_PALE = new Color(251, 241, 220);
    static final Color RED = new Color(176, 67, 75);
    static final Color RED_PALE = new Color(250, 232, 232);
    static final Color PURPLE = new Color(105, 82, 142);
    static final Color BG = new Color(248, 245, 241);
    static final Color CARD = new Color(255, 254, 252);
    static final Color TEXT = new Color(49, 45, 56);
    static final Color MUTED = new Color(126, 119, 126);
    static final Color BORDER = new Color(234, 226, 218);

    static Font font(float size, int style) { return new Font("Segoe UI", style, Math.round(size)); }
    private Theme() { }
}

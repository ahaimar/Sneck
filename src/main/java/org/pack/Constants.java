package org.pack;

import java.awt.Color;

public final class Constants {

    private Constants() {
    }

    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;
    public static final String WINDOW_TITLE = "Snake";

    /** Size of one grid cell, in pixels. */
    public static final int TILE_SIZE = 24;

    public static final int BOARD_COLUMNS = 25;
    public static final int BOARD_ROWS = 20;

    public static final int BOARD_WIDTH = BOARD_COLUMNS * TILE_SIZE;
    public static final int BOARD_HEIGHT = BOARD_ROWS * TILE_SIZE;

    /** Vertical space reserved for the score bar. */
    public static final int HUD_HEIGHT = 48;

    public static final int BOARD_X = (WINDOW_WIDTH - BOARD_WIDTH) / 2;
    public static final int BOARD_Y = HUD_HEIGHT;

    /** Simulation advances in fixed slices so behaviour does not depend on frame rate. */
    public static final double FIXED_STEP_SECONDS = 1.0 / 60.0;

    /** A long stall (window drag, debugger) is discarded instead of fast-forwarding the game. */
    public static final double MAX_FRAME_SECONDS = 0.25;

    public static final int SCORE_PER_FOOD = 10;

    public static final Color COLOR_BACKGROUND = new Color(0x0B1020);
    public static final Color COLOR_BOARD = new Color(0x141B33);
    public static final Color COLOR_GRID = new Color(0x1E2745);
    public static final Color COLOR_BOARD_BORDER = new Color(0x2C3A63);

    public static final Color COLOR_SNAKE = new Color(0x4ADE80);
    public static final Color COLOR_SNAKE_HEAD = new Color(0x86EFAC);
    public static final Color COLOR_SNAKE_EYE = new Color(0x0B1020);

    public static final Color COLOR_FOOD = new Color(0xF87171);
    public static final Color COLOR_FOOD_HIGHLIGHT = new Color(0xFCA5A5);

    public static final Color COLOR_TEXT = new Color(0xE2E8F0);
    public static final Color COLOR_TEXT_DIM = new Color(0x94A3B8);
    public static final Color COLOR_TEXT_ACCENT = new Color(0xFBBF24);
    public static final Color COLOR_OVERLAY = new Color(0x0B, 0x10, 0x20, 205);

    public static final String FONT_FAMILY = "SansSerif";
}

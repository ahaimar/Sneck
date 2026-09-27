package org.pack;

/**
 * What a scene is allowed to ask of the thing hosting it.
 *
 * <p>Scenes depend on this instead of on {@link Window} so they can be driven and rendered in a
 * test without a display or an event thread.
 */
public interface GameHost {

    void changeState(GameState state);

    /** Updates the window title, for example to show the live score. */
    void setStateTitle(int score);

    /** The speed the next run will use. The menu is where the player changes it. */
    Difficulty difficulty();

    void setDifficulty(Difficulty difficulty);

    void close();
}

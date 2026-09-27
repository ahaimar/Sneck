package org.pack;

import java.awt.Graphics;

public abstract class Scene {

    /** Called once before the first update, after the scene becomes current. */
    public void onEnter() {
    }

    /** Called once when the scene stops being current. */
    public void onExit() {
    }

    public abstract void update(double deltaTime);

    public abstract void draw(Graphics graphics);
}

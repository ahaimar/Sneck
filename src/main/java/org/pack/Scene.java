package org.pack;

import java.awt.Graphics;

public abstract class Scene {

    public abstract void update(double deltaTime);

    public abstract void draw(Graphics graphics);
}

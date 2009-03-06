package com.goofans.gootool.leveledit.view.render;

import java.awt.*;

import com.goofans.gootool.leveledit.view.LevelDisplay;
import com.goofans.gootool.leveledit.model.Level;
import com.goofans.gootool.leveledit.model.LevelContentsItem;

/**
 * @author David Croft (davidc@goofans.com)
 * @version $Id$
 */
public abstract class Renderer
{
  public abstract void render(Graphics2D g, LevelDisplay levelDisplay, LevelContentsItem item);

  /* Gets the hit-box in display coordinates */
  public abstract Shape getHitBox(LevelDisplay levelDisplay, LevelContentsItem item);
  public abstract Shape getNegativeHitBox(LevelDisplay levelDisplay, LevelContentsItem item);
}
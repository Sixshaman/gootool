package com.goofans.gootool.view;

import net.infotrek.util.TextUtil;

import com.goofans.gootool.ToolPreferences;
import com.goofans.gootool.Controller;
import com.goofans.gootool.GooTool;
import com.goofans.gootool.GooToolResourceBundle;
import com.goofans.gootool.model.Configuration;
import com.goofans.gootool.util.FileNameExtensionFilter;
import com.goofans.gootool.profile.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.text.NumberFormat;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * @author David Croft (davidc@goofans.com)
 * @version $Id$
 */
public final class ProfilePanel implements ActionListener, ViewComponent
{
  private static final Logger log = Logger.getLogger(ProfilePanel.class.getName());

  private JComboBox profilesCombo;
  private JLabel playTime;
  private JLabel levelsPlayed;
  public JPanel rootPanel;
  private JButton refreshButton;
  private JLabel profileName;
  private JLabel flags;
  private JTable levelsTable;
  private JPanel towerPanel;
  private JLabel towerTotalBalls;
  private JLabel towerNodeBalls;
  private JLabel towerStrandBalls;
  private JLabel towerHeight;
  //  private JButton viewTowerButton;
  private JButton saveTowerButton;
  private JButton profileBackupButton;
  private JButton profileRestoreButton;
  private JButton profilePublishButton;

  private static final String CMD_REFRESH = "REFRESH";
  private static final String CMD_PROFILE_CHANGED = "PROFILE_CHANGED";
  private static final String CMD_VIEW_TOWER = "ViewTower";
  private static final String CMD_SAVE_TOWER = "SaveTower";
  private static final String CMD_SAVE_TOWER_THUMB = "SaveTowerThumb";
  private static final String CMD_SAVE_TOWER_FULL = "SaveTowerFull";
  private static final String CMD_SAVE_TOWER_TRANS = "SaveTowerTrans";

  private Profile currentProfile;

  private static final String[] COLUMN_NAMES;
  private final LevelsTableModel levelsModel;
  private TowerRenderer tr;
  private JPopupMenu saveTowerMenu;

  private static final GooToolResourceBundle resourceBundle = GooTool.getTextProvider();
  private static final String BR = "<br>";

  static {
    COLUMN_NAMES = new String[]{
            resourceBundle.getString("profile.column.level"),
            resourceBundle.getString("profile.column.balls"),
            resourceBundle.getString("profile.column.moves"),
            resourceBundle.getString("profile.column.time")
    };
  }


  public ProfilePanel(Controller controller)
  {
    refreshButton.setActionCommand(CMD_REFRESH);
    refreshButton.addActionListener(this);

//    viewTowerButton.setActionCommand(CMD_VIEW_TOWER);
//    viewTowerButton.addActionListener(this);

    saveTowerButton.setActionCommand(CMD_SAVE_TOWER);
    saveTowerButton.addActionListener(this);

    profilesCombo.setActionCommand(CMD_PROFILE_CHANGED);
    profilesCombo.addActionListener(this);

    profileBackupButton.setActionCommand(Controller.CMD_GOOFANS_BACKUP);
    profileBackupButton.addActionListener(controller);

    profileRestoreButton.setActionCommand(Controller.CMD_GOOFANS_RESTORE);
    profileRestoreButton.addActionListener(controller);

    profilePublishButton.setActionCommand(Controller.CMD_GOOFANS_PUBLISH);
    profilePublishButton.addActionListener(controller);

    levelsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    levelsModel = new LevelsTableModel();
    levelsTable.setModel(levelsModel);

    TableColumnModel columnModel = levelsTable.getColumnModel();

    columnModel.getColumn(0).setPreferredWidth(150);
    columnModel.getColumn(1).setPreferredWidth(50);
    columnModel.getColumn(2).setPreferredWidth(50);
    columnModel.getColumn(3).setPreferredWidth(50);

    levelsTable.getTableHeader().setReorderingAllowed(false);

    /* 1.6 only:
      levelsTable.setAutoCreateRowSorter(true);
      */
    try {
      Method setAutoCreateRowSorterMethod = levelsTable.getClass().getMethod("setAutoCreateRowSorter", boolean.class);
      setAutoCreateRowSorterMethod.invoke(levelsTable, true);
    }
    catch (Exception e) {
      log.log(Level.FINE, "No setAutoCreateRowSorter method found or can't execute", e);
    }

    createSaveTowerMenu();

    if (ProfileFactory.isProfileFound()) {
      loadProfiles();
    }
  }

  public void actionPerformed(ActionEvent event)
  {
    String cmd = event.getActionCommand();

    log.fine("cmd " + cmd);

    if (cmd.equals(CMD_REFRESH)) {
      if (!ProfileFactory.isProfileFound()) {
        JOptionPane.showMessageDialog(rootPanel, resourceBundle.getString("profile.error.notfound.message"), resourceBundle.getString("profile.error.notfound.title"), JOptionPane.ERROR_MESSAGE);
        return;
      }

      loadProfiles();
    }
    else if (cmd.equals(CMD_PROFILE_CHANGED) && profilesCombo.getSelectedItem() != currentProfile) {
      currentProfile = getSelectedProfile();
      log.fine("currentProfile = " + currentProfile);

      if (currentProfile != null) {
        profileName.setText(currentProfile.getName());
        playTime.setText(TextUtil.formatTime(currentProfile.getPlayTime()));
        levelsPlayed.setText(String.valueOf(currentProfile.getLevels()));

        Tower t = currentProfile.getTower();

        towerHeight.setText(formatHeight(t.getHeight()));
        towerTotalBalls.setText(MessageFormat.format(resourceBundle.getString("profile.tower.balls.value"), t.getUsedStrandBalls() + t.getUsedNodeBalls(), t.getTotalBalls()));
        towerNodeBalls.setText(String.valueOf(t.getUsedNodeBalls()));
        towerStrandBalls.setText(String.valueOf(t.getUsedStrandBalls()));

        StringBuilder flagInfo = new StringBuilder();
        if (currentProfile.hasFlag(Profile.FLAG_ONLINE)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.online")).append(BR);
        }
        if (currentProfile.hasFlag(Profile.FLAG_GOOCORP_UNLOCKED)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.goocorpunlocked")).append(BR);
        }
        if (currentProfile.hasFlag(Profile.FLAG_GOOCORP_DESTROYED)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.goocorpdestroyed")).append(BR);
        }
        if (currentProfile.hasFlag(Profile.FLAG_WHISTLE)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.whistle")).append(BR);
        }
        if (currentProfile.hasFlag(Profile.FLAG_TERMS)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.terms")).append(BR);
        }
        if (currentProfile.hasFlag(Profile.FLAG_32)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.flag32")).append(BR);
        }
        if (currentProfile.hasFlag(Profile.FLAG_64)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.flag64")).append(BR);
        }
        if (currentProfile.hasFlag(Profile.FLAG_128)) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.flag128")).append(BR);
        }

        if (flagInfo.length() == 0) {
          flagInfo.append(resourceBundle.getString("profile.info.flags.none"));
        }
        flags.setText("<html>" + flagInfo + "</html>");

        levelsModel.fireTableDataChanged();

        towerPanel.removeAll();

        try {
          tr = new TowerRenderer(currentProfile.getTower());
          tr.render();

          BufferedImage thumbImg = tr.getThumbnail();
          JLabel thumb = new JLabel(new ImageIcon(thumbImg));
          thumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//          thumb.setVerticalAlignment(SwingConstants.CENTER);
//          thumb.setHorizontalAlignment(SwingConstants.CENTER);

          Dimension thumbDim = new Dimension(thumbImg.getWidth(), thumbImg.getHeight());
          thumb.setSize(thumbDim);
          thumb.setMinimumSize(thumbDim);
          thumb.addMouseListener(new MouseAdapter()
          {
            @Override
            public void mouseClicked(MouseEvent e)
            {
              showTower();
            }
          });
          towerPanel.add(thumb);
//          viewTowerButton.setEnabled(true);
          saveTowerButton.setEnabled(true);
        }
        catch (IOException e1) {
          log.log(Level.SEVERE, "Unable to render tower", e1);
          towerPanel.add(new JLabel(resourceBundle.getString("profile.tower.error")));
//          viewTowerButton.setEnabled(false);
          saveTowerButton.setEnabled(false);
        }

      }
    }
    else if (cmd.equals(CMD_VIEW_TOWER) && tr != null) {
      showTower();
    }
    else if (cmd.equals(CMD_SAVE_TOWER) && tr != null) {
      saveTowerMenu.show((Component) event.getSource(), 0, saveTowerButton.getHeight());
    }
    else if (cmd.equals(CMD_SAVE_TOWER_THUMB) && tr != null) {
      saveTower(tr.getThumbnail());
    }
    else if (cmd.equals(CMD_SAVE_TOWER_FULL) && tr != null) {
      saveTower(tr.getPretty());
    }
    else if (cmd.equals(CMD_SAVE_TOWER_TRANS) && tr != null) {
      saveTower(tr.getFullSize());
    }
  }

  public Profile getSelectedProfile()
  {
    return (Profile) profilesCombo.getSelectedItem();
  }

  private void saveTower(BufferedImage image)
  {
    JFileChooser chooser = new JFileChooser(ToolPreferences.getMruTowerDir());
    chooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));
    int returnVal = chooser.showSaveDialog(this.rootPanel);

    if (returnVal == JFileChooser.APPROVE_OPTION) {

      File file = chooser.getSelectedFile();

      if (!file.getName().endsWith(".png")) {
        file = new File(file.getParentFile(), file.getName() + ".png");
      }

      if (file.exists()) {
        returnVal = JOptionPane.showConfirmDialog(this.rootPanel,
                MessageFormat.format(resourceBundle.getString("profile.tower.saveimage.overwrite.message"), file.getName()),
                resourceBundle.getString("profile.tower.saveimage.overwrite.title"),
                JOptionPane.YES_NO_OPTION);
        if (returnVal != JOptionPane.YES_OPTION) return;
      }

      log.info("Saving tower to " + file);
      try {
        ImageIO.write(image, "PNG", file);
      }
      catch (IOException e) {
        log.log(Level.WARNING, "Unable to save tower", e);
        JOptionPane.showMessageDialog(this.rootPanel,
                MessageFormat.format(resourceBundle.getString("profile.tower.saveimage.error.message"), e.getLocalizedMessage()),
                resourceBundle.getString("profile.tower.saveimage.error.title"),
                JOptionPane.ERROR_MESSAGE);
      }

      ToolPreferences.setMruTowerDir(chooser.getCurrentDirectory().getPath());
    }
  }

  private void createSaveTowerMenu()
  {
    saveTowerMenu = new JPopupMenu();
    JMenuItem menuItem;
    menuItem = new JMenuItem(resourceBundle.getString("profile.tower.saveimage.fullsize"));
    menuItem.setActionCommand(CMD_SAVE_TOWER_FULL);
    menuItem.addActionListener(this);
    saveTowerMenu.add(menuItem);
    menuItem = new JMenuItem(resourceBundle.getString("profile.tower.saveimage.thumbnail"));
    menuItem.setActionCommand(CMD_SAVE_TOWER_THUMB);
    menuItem.addActionListener(this);
    saveTowerMenu.add(menuItem);
    menuItem = new JMenuItem(resourceBundle.getString("profile.tower.saveimage.transparent"));
    menuItem.setActionCommand(CMD_SAVE_TOWER_TRANS);
    menuItem.addActionListener(this);
    saveTowerMenu.add(menuItem);
  }

  private void showTower()
  {
    // TODO make this a proper gui frame, with scrollbars etc.
    // TODO parent frame
    final JDialog d = new JDialog();
    d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    d.setTitle(resourceBundle.getString("profile.tower.popup.title"));
    BufferedImage prettyImg = tr.getPretty();
    d.setPreferredSize(new Dimension(prettyImg.getWidth(), prettyImg.getHeight()));
    // TODO 1.6
//    d.setIconImage(GooTool.getMainIconImage());
    JLabel jLabel = new JLabel(new ImageIcon(prettyImg));
    jLabel.setBorder(new LineBorder(Color.BLACK));
    d.add(jLabel);
    d.setModal(true);
    d.pack();
    d.setLocationByPlatform(true);

    d.addKeyListener(new KeyAdapter()
    {
      @Override
      public void keyPressed(KeyEvent e)
      {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          d.setVisible(false);
        }
      }
    });

    d.setVisible(true);
  }

  public void loadProfiles()
  {
    profilesCombo.removeAllItems();

    ProfileData profileData;
    try {
      profileData = ProfileFactory.getProfileData();
    }
    catch (IOException e) {
      log.log(Level.SEVERE, "Unable to read profile", e);
      JOptionPane.showMessageDialog(rootPanel, resourceBundle.getString("profile.error.corrupt.message"), resourceBundle.getString("profile.error.corrupt.title"), JOptionPane.ERROR_MESSAGE);
      return;
    }

    for (Profile profile : profileData.getProfiles()) {
      if (profile != null) {
        profilesCombo.addItem(profile);
      }
    }
    profilesCombo.setSelectedItem(profileData.getCurrentProfile());
  }

  private String formatHeight(double height)
  {
    NumberFormat nf = NumberFormat.getNumberInstance();
    return nf.format(height) + " m";
  }

  public void updateViewFromModel(Configuration c)
  {
    boolean enabled = ToolPreferences.isGooFansLoginOk() && ProfileFactory.isProfileFound();

    profileBackupButton.setEnabled(enabled);
    profileRestoreButton.setEnabled(enabled);
    profilePublishButton.setEnabled(enabled);

    if (enabled) {
      profileBackupButton.setToolTipText(resourceBundle.getString("profile.goofans.backup.tooltip"));
      profileRestoreButton.setToolTipText(resourceBundle.getString("profile.goofans.restore.tooltip"));
      profilePublishButton.setToolTipText(resourceBundle.getString("profile.goofans.publish.tooltip"));
    }
    else {
      String tooltip;
      if (!ProfileFactory.isProfileFound()) {
        tooltip = resourceBundle.getString("profile.goofans.disabled.profile.tooltip");
      }
      else {
        tooltip = resourceBundle.getString("profile.goofans.disabled.tooltip");

      }
      profileBackupButton.setToolTipText(tooltip);
      profileRestoreButton.setToolTipText(tooltip);
      profilePublishButton.setToolTipText(tooltip);
    }

  }

  public void updateModelFromView(Configuration c)
  {
  }


  private class LevelsTableModel extends AbstractTableModel
  {
    public int getRowCount()
    {
      return currentProfile != null ? currentProfile.getLevelAchievements().size() : 0;
    }

    public int getColumnCount()
    {
      return 4;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      LevelAchievement levelAchievement = currentProfile.getLevelAchievements().get(rowIndex);

      if (columnIndex == 0) return levelAchievement.getLevelId();
      if (columnIndex == 1) return levelAchievement.getMostBalls();
      if (columnIndex == 2) return levelAchievement.getLeastMoves();
      if (columnIndex == 3) return levelAchievement.getLeastTime();

      return null;
    }

    @Override
    public String getColumnName(int column)
    {
      return COLUMN_NAMES[column];
    }
  }
}

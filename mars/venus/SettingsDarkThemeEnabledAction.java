package mars.venus;

import mars.Globals;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SettingsDarkThemeEnabledAction extends GuiAction {
    public SettingsDarkThemeEnabledAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
        super(name, icon, descrip, mnemonic, accel, gui);
    }

    public void actionPerformed(ActionEvent e) {
        Globals.getSettings().setBooleanSetting(21, !Globals.getSettings().getBooleanSetting(21));
    }
}

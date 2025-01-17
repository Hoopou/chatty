
package chatty.gui.components.settings;

import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ModerationSettings extends SettingsPanel {
    
    public ModerationSettings(final SettingsDialog d) {
        
        JPanel blah = addTitledPanel(Language.getString("settings.section.modInfos"), 0);
        
        JCheckBox showModActions = d.addSimpleBooleanSetting("showModActions");
        JCheckBox showModActionsRestrict = d.addSimpleBooleanSetting("showModActionsRestrict");
        
        SettingsUtil.addSubsettings(showModActions, showModActionsRestrict);
        
        blah.add(showModActions,
                d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST));
        
        blah.add(showModActionsRestrict,
                d.makeGbcSub(0, 1, 3, 1, GridBagConstraints.WEST));
        
        blah.add(d.addSimpleBooleanSetting("showActionBy"),
                d.makeGbc(0, 4, 3, 1, GridBagConstraints.WEST));
        
        blah.add(d.addSimpleBooleanSetting("showAutoMod", "Show messages rejected by AutoMod", ""),
                d.makeGbc(0, 5, 3, 1, GridBagConstraints.WEST));
        
        blah.add(new JLabel("<html><body style='width:300px;'>"
                + "To approve messages open <code>Extra - AutoMod</code>. "
                + "You can also set a custom hotkey to open dialogs (go "
                + "to <code>Hotkeys</code> settings, add a new item and select "
                + "<code>Dialog: AutoMod Dialog</code> as action)."),
                d.makeGbc(1, 6, 2, 1, GridBagConstraints.CENTER));
        
        
        JPanel userInfo = addTitledPanel(Language.getString("settings.section.userDialog"), 1);
        
        userInfo.add(d.addSimpleBooleanSetting(
                "closeUserDialogOnAction"),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        userInfo.add(d.addSimpleBooleanSetting(
                "openUserDialogByMouse"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        userInfo.add(d.addSimpleBooleanSetting(
                "reuseUserDialog"),
                d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));

        userInfo.add(new JLabel(Language.getString("settings.long.clearUserMessages.label")),
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.EAST));
        userInfo.add(d.addComboLongSetting("clearUserMessages", new int[]{-1, 3, 6, 12, 24}),
                d.makeGbc(1, 3, 1, 1, GridBagConstraints.WEST));
        
        HotkeyTextField banReasonsHotkey = new HotkeyTextField(12, null);
        d.addStringSetting("banReasonsHotkey", banReasonsHotkey);
        userInfo.add(d.createLabel("banReasonsHotkey"),
                d.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST));
        userInfo.add(banReasonsHotkey,
                d.makeGbc(1, 4, 1, 1, GridBagConstraints.WEST));
        
        userInfo.add(d.createLabel("banReasonsInfo", true),
                d.makeGbc(0, 5, 2, 1));
    }
    
}

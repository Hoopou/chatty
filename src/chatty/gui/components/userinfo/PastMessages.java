
package chatty.gui.components.userinfo;

import chatty.User;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.JTextArea;

/**
 *
 * @author tduva
 */
public class PastMessages extends JTextArea {
    
    private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("[HH:mm:ss]");
    
    private String currentMessageIdMessage;
    
    public PastMessages() {
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
    }
    
    public String getCurrentMessage() {
        return currentMessageIdMessage;
    }
    
    public void update(User user, String currentMessageId) {
        setText(null);
        setText(makeLines(user, currentMessageId));
    }
    
    private String makeLines(User user, String currentMessageId) {
        StringBuilder b = new StringBuilder();
        if (user.linesCleared()) {
            b.append("<some lines cleared due to user inactivity>\n");
        }
        if (user.maxLinesExceeded()) {
            b.append("<only last ");
            b.append(user.getMaxNumberOfLines());
            b.append(" lines are saved>\n");
        }
        List<User.Message> messages = user.getMessages();
        for (User.Message m : messages) {
            if (m instanceof User.TextMessage) {
                User.TextMessage tm = (User.TextMessage)m;
                if (!StringUtil.isNullOrEmpty(currentMessageId)
                        && currentMessageId.equals(tm.id)) {
                    b.append(">");
                    //singleMessage.setText(SINGLE_MESSAGE_CHECK+" ("+StringUtil.shortenTo(tm.text, 14)+")");
                    currentMessageIdMessage = tm.text;
                }
                b.append(DateTime.format(m.getTime(), TIMESTAMP));
                if (tm.action) {
                    b.append("* ");
                } else {
                    b.append(" ");
                }
                b.append(tm.text);
                b.append("\n");
            }
            else if (m instanceof User.BanMessage) {
                User.BanMessage bm = (User.BanMessage)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                if (bm.duration > 0) {
                    b.append("Timed out (").append(bm.duration).append("s)");
                }
                else {
                    b.append("Banned permanently");
                }
                if (bm.id != null) {
                    b.append(" (single message)");
                }
                if (bm.reason != null && !bm.reason.isEmpty()) {
                    b.append(" [").append(bm.reason).append("]");
                }
                if (bm.by != null) {
                    b.append(" (@").append(bm.by).append(")");
                }
                b.append("\n");
            }
            else if (m instanceof User.UnbanMessage) {
                User.UnbanMessage ubm = (User.UnbanMessage)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                if (ubm.type == User.UnbanMessage.TYPE_UNBAN) {
                    b.append("Unbanned");
                } else if (ubm.type == User.UnbanMessage.TYPE_UNTIMEOUT) {
                    b.append("Timeout removed");
                }
                b.append(" (@").append(ubm.by).append(")");
                b.append("\n");
            }
            else if (m instanceof User.MsgDeleted) {
                User.MsgDeleted md = (User.MsgDeleted)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                b.append("Message deleted: ").append(md.msg);
                if (md.by != null) {
                    b.append(" (@").append(md.by).append(")");
                }
                b.append("\n");
            }
            else if (m instanceof User.SubMessage) {
                User.SubMessage sm = (User.SubMessage)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append("$ ");
                b.append(sm.system_msg);
                if (!sm.attached_message.isEmpty()) {
                    b.append(" [").append(sm.attached_message).append("]");
                }
                b.append("\n");
            }
            else if (m instanceof User.InfoMessage) {
                User.InfoMessage sm = (User.InfoMessage)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append("I ");
                b.append(sm.system_msg);
                if (!sm.attached_message.isEmpty()) {
                    b.append(" [").append(sm.attached_message).append("]");
                }
                b.append("\n");
            }
            else if (m instanceof User.ModAction) {
                User.ModAction ma = (User.ModAction)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                b.append("ModAction: /");
                b.append(ma.commandAndParameters);
                b.append("\n");
            }
            else if (m instanceof User.AutoModMessage) {
                User.AutoModMessage ma = (User.AutoModMessage)m;
                if (!StringUtil.isNullOrEmpty(currentMessageId)
                        && currentMessageId.equals(ma.id)) {
                    b.append(">");
                }
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                b.append("Filtered by AutoMod");
                if (!StringUtil.isNullOrEmpty(ma.reason)) {
                    b.append(" (").append(ma.reason).append(")");
                }
                b.append(": ").append(ma.message);
                b.append("\n");
            }
        }
        // Remove last newline
        if (b.length() > 0 && b.charAt(b.length() - 1) == '\n') {
            b.deleteCharAt(b.length() - 1);
        }
        return b.toString();
    }
    
}

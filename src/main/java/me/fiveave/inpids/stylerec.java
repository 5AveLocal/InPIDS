package me.fiveave.inpids;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static me.fiveave.inpids.main.stylelist;

/// PIDS style record class
class stylerec {
    /// Height of PIDS display
    private final int height;
    /// Width of PIDS display
    private final int width;
    /// List of lines of PIDS display
    private final List<Integer> lines;
    /// Language loop interval of PIDS display
    private final int loopinterval;
    /// Flash interval of PIDS display
    private final int flashinterval;
    /// List of message styles of PIDS display
    private final ArrayList<String> styles = new ArrayList<>();
    /// List of messages of PIDS display
    private final HashMap<String, String> messages = new HashMap<>();
    /// PA broadcasting radius
    private final int paradius;
    /// List of text PAs
    private final HashMap<String, String> textpa = new HashMap<>();

    /// PIDS style record object
    ///
    /// @param pidsstyle PIDS Style specified in stapidslist.yml, fetched from stylelist.yml
    stylerec(String pidsstyle) {
        height = stylelist.dataconfig.getInt(pidsstyle + ".height");
        width = stylelist.dataconfig.getInt(pidsstyle + ".width");
        lines = stylelist.dataconfig.getIntegerList(pidsstyle + ".lines");
        loopinterval = stylelist.dataconfig.getInt(pidsstyle + ".loopinterval");
        flashinterval = stylelist.dataconfig.getInt(pidsstyle + ".flashinterval");
        int paradius1;
        try {
            paradius1 = stylelist.dataconfig.getInt(pidsstyle + ".pa.radius");
        } catch (Exception e) {
            paradius1 = -1;
        }
        paradius = paradius1;
        // Add styles (size = height * width)
        for (int i = 0; i < height * width; i++) {
            styles.add(stylelist.dataconfig.getString(pidsstyle + ".style." + i));
        }
        // Add messages
        ConfigurationSection cs = stylelist.dataconfig.getConfigurationSection(pidsstyle + ".messages");
        Set<String> msgset;
        if (cs != null) {
            msgset = cs.getKeys(false);
            for (String msg : msgset) {
                messages.put(msg, stylelist.dataconfig.getString(pidsstyle + ".messages." + msg));
            }
        }
        // Add text PAs
        ConfigurationSection cs2 = stylelist.dataconfig.getConfigurationSection(pidsstyle + ".pa.text");
        Set<String> patxtset;
        if (cs2 != null) {
            patxtset = cs2.getKeys(false);
            for (String txt : patxtset) {
                textpa.put(txt, stylelist.dataconfig.getString(pidsstyle + ".pa.text." + txt));
            }
        }
    }

    /// @return Height of PIDS display
    int getHeight() {
        return height;
    }

    /// @return Width of PIDS display
    int getWidth() {
        return width;
    }

    /// @return Language loop interval of PIDS display
    int getLoopinterval() {
        return loopinterval;
    }

    /// @return Flash interval (for train arrival warning) of PIDS display
    int getFlashinterval() {
        return flashinterval;
    }

    /// @return List of lines in PIDS display
    List<Integer> getLines() {
        return lines;
    }

    /// @return List of message styles in PIDS display
    ArrayList<String> getStyles() {
        return styles;
    }

    /// @return HashMap of messages in PIDS display
    HashMap<String, String> getMessages() {
        return messages;
    }

    /// @return HashMap of text PAs in PIDS display
    HashMap<String, String> getTextPa() {
        return textpa;
    }

    /// @return PA broadcasting radius
    public int getPaRadius() {
        return paradius;
    }
}

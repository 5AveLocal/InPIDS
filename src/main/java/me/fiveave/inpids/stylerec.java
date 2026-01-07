package me.fiveave.inpids;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

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

    /// PIDS style record object
    ///
    /// @param pidsstyle PIDS Style specified in stapidslist.yml, fetched from stylelist.yml
    stylerec(String pidsstyle) {
        height = stylelist.dataconfig.getInt(pidsstyle + ".height");
        width = stylelist.dataconfig.getInt(pidsstyle + ".width");
        lines = stylelist.dataconfig.getIntegerList(pidsstyle + ".lines");
        loopinterval = stylelist.dataconfig.getInt(pidsstyle + ".loopinterval");
        flashinterval = stylelist.dataconfig.getInt(pidsstyle + ".flashinterval");
        // Add styles (size = height * width)
        for (int i = 0; i < height * width; i++) {
            styles.add(stylelist.dataconfig.getString(pidsstyle + ".style." + i));
        }
        // Add messages
        ConfigurationSection cs = Objects.requireNonNull(stylelist.dataconfig.getConfigurationSection(pidsstyle + ".messages"));
        Set<String> msgset = cs.getKeys(false);
        for (String msg : msgset) {
            messages.put(msg, stylelist.dataconfig.getString(pidsstyle + ".messages." + msg));
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
}

package me.fiveave.inpids;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

import static me.fiveave.inpids.main.stylelist;

class stylerec {
    private final int height;
    private final int width;
    private final List<Integer> lines;
    private final int loopinterval;
    private final int flashinterval;
    private final ArrayList<String> styles = new ArrayList<>();
    private final HashMap<String, String> messages = new HashMap<>();

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

    int getHeight() {
        return height;
    }

    int getWidth() {
        return width;
    }

    int getLoopinterval() {
        return loopinterval;
    }

    int getFlashinterval() {
        return flashinterval;
    }

    List<Integer> getLines() {
        return lines;
    }

    ArrayList<String> getStyles() {
        return styles;
    }

    HashMap<String, String> getMessages() {
        return messages;
    }
}

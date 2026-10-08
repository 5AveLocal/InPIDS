package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.fiveave.inpids.main.*;

/// inpidscarpa sign class
class carpasign extends SignAction {

    @NonNull
    TextComponent getCarPaPlaceholderReplacedString(statimelist stl, String linesys, String location, String style) {
        int thisstaindex = stl.getStaIndex(location);
        List<String> stylelines = pastylelist.dataconfig.getStringList(style + ".text");
        String[] line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).split("\\|");
        String linecolor = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line_color"));
        String[] type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).split("\\|");
        String typecolor = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type_color"));
        String orilinecode = linetypelist.dataconfig.getString(linesys + ".ori_line_code");
        String altlinecode = linetypelist.dataconfig.getString(linesys + ".alt_line_code");
        TextComponent tc = new TextComponent();
        String doordir = pastylelist.dataconfig.getString(style + ".doordir." + stl.getDoorDir().get(thisstaindex));
        ArrayList<String[]> staname = stl.getStaname();
        ArrayList<String> stacode = stl.getStacode();
        ArrayList<String> transfers = stl.getTransfers();
        ArrayList<String> transferplaceholders = new ArrayList<>(translist.dataconfig.getKeys(false));
        int stlsize = stl.getSize();
        int terminusindex = stlsize - 1;
        String[] dest = staname.get(terminusindex);
        // Station counter
        int i = 0;
        boolean stopouterloop = false;
        for (int selindex = i - thisstaindex; selindex < stlsize - thisstaindex; selindex++) {
            for (String s : stylelines) {
                if (s.contains("%sta_" + selindex)) {
                    i = selindex + thisstaindex;
                    stopouterloop = true;
                    break;
                }
            }
            if (stopouterloop) break;
        }
        // Get string from every line in the file
        for (String s : stylelines) {
            int selindex = i - thisstaindex;
            String appendedstr = s;
            boolean append = true;
            // Lines and train types
            // Replacement of specific languages
            for (int langcount = 0; langcount < dest.length; langcount++) {
                appendedstr = appendedstr.replace("%dest_" + langcount, dest[langcount]);
            }
            for (int langcount = 0; langcount < line.length; langcount++) {
                appendedstr = appendedstr.replace("%line_" + langcount, line[langcount]);
            }
            for (int langcount = 0; langcount < type.length; langcount++) {
                appendedstr = appendedstr.replace("%type_" + langcount, type[langcount]);
            }
            // General replacements
            appendedstr = appendedstr
                    .replace("%dest", String.join(" ", dest))
                    .replace("%line_color", linecolor)
                    .replace("%type_color", typecolor)
                    .replace("%line", String.join(" ", line))
                    .replace("%type", String.join(" ", type));
            // Station display
            if (i >= stlsize && (s.contains("%sta_") || s.contains("%trans_") || s.contains("%line_color"))) {
                append = false;
            } else if (i < stlsize) {
                // Replacement of specific languages (format: %<param>_<index>_<lang>)
                for (int langcount = 0; langcount < staname.get(i).length; langcount++) {
                    appendedstr = appendedstr.replace("%sta_" + selindex + "_" + langcount, staname.get(i)[langcount]);
                }
                // General replacements
                String thisstacode = stacode.get(i);
                if (orilinecode != null && altlinecode != null) {
                    thisstacode = thisstacode.replace(orilinecode, altlinecode);
                }
                appendedstr = appendedstr
                        .replace("%sta_code_" + selindex, thisstacode)
                        .replace("%sta_" + selindex, String.join(" ", staname.get(i)))
                        .replace("%trans_" + selindex, String.join(" ", transfers.get(i)));
            }
            // Door direction
            if (doordir != null) {
                appendedstr = appendedstr.replace("%door_dir", doordir);
            }
            /* Color replacement and converting to BaseComponent
               This will break up the String and split it in many BaseComponents */
            ArrayList<BaseComponent> bcal = new ArrayList<>(Arrays.asList(TextComponent.fromLegacyText(colorparser.parseColors(appendedstr))));
            for (int j = 0; j < bcal.size(); j++) {
                BaseComponent bc = bcal.get(j);
                String fstr = TextComponent.toLegacyText(bc); // Convert once into a legacy string
                // Check if this component contains any relevant placeholders
                boolean hasPlaceholder = false;
                for (String transsuffix : transferplaceholders) {
                    if (fstr.contains("%trans_" + transsuffix)) {
                        hasPlaceholder = true;
                        break;
                    }
                }
                // If no placeholders are found, skip processing for efficiency
                if (!hasPlaceholder) continue;
                // Use ComponentBuilder to securely piece together text and events
                ComponentBuilder builder = new ComponentBuilder("");
                // Look for placeholders from left to right chronologically
                while (true) {
                    String earliestSuffix = null;
                    int earliestIndex = -1;
                    String earliestFull = "";
                    // Determine which placeholder appears first in the current segment
                    for (String transsuffix : transferplaceholders) {
                        String transfull = "%trans_" + transsuffix;
                        int index = fstr.indexOf(transfull);
                        if (index != -1 && (earliestIndex == -1 || index < earliestIndex)) {
                            earliestIndex = index;
                            earliestSuffix = transsuffix;
                            earliestFull = transfull;
                        }
                    }
                    // If no more placeholders remain, append the rest of the string and exit
                    if (earliestIndex == -1) {
                        if (!fstr.isEmpty()) {
                            builder.append(TextComponent.fromLegacyText(fstr));
                        }
                        break;
                    }
                    // 1. Append text before the placeholder
                    String before = fstr.substring(0, earliestIndex);
                    if (!before.isEmpty()) {
                        builder.append(TextComponent.fromLegacyText(before));
                    }
                    // 2. Fetch the text and hover configurations
                    String maintext = colorparser.parseColors(translist.dataconfig.getString(earliestSuffix + ".text"));
                    String hovertext = colorparser.parseColors(translist.dataconfig.getString(earliestSuffix + ".hover"));
                    // 3. Build components from legacy texts safely
                    BaseComponent[] replacement = null;
                    if (maintext != null) {
                        replacement = TextComponent.fromLegacyText(maintext);
                    }
                    BaseComponent[] hoverComponents = null;
                    if (hovertext != null) {
                        hoverComponents = TextComponent.fromLegacyText(hovertext);
                    }
                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponents));
                    // CRITICAL: Apply the HoverEvent to ALL subcomponents generated from 'maintext'
                    // Otherwise, if maintext contains color codes, subsequent segments will lose hover.
                    if (replacement != null) {
                        for (BaseComponent comp : replacement) {
                            comp.setHoverEvent(hoverEvent);
                        }
                    }
                    // Append the safely built replacement chunk
                    if (replacement != null) {
                        builder.append(replacement);
                    }
                    // 4. Shrink the remaining string past the processed placeholder
                    fstr = fstr.substring(earliestIndex + earliestFull.length());
                }
                // Pack the built results back into a clean TextComponent container
                TextComponent finalContainer = new TextComponent(builder.create());
                finalContainer.copyFormatting(bc); // Retain original formatting context if needed
                bcal.set(j, finalContainer);
            }
            // Appending and station counting
            if (append) {
                for (BaseComponent bc : bcal) {
                    tc.addExtra(bc);
                }
                tc.addExtra("\n");
                if (s.contains("%trans_")) {
                    i++;
                }
            }
        }
        return tc;
    }

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("inpidscarpa");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            // Train info
            MinecartGroup mg = cartevent.getGroup();
            // Get sign info
            String linesys = cartevent.getLine(2); // linesys includes both line name and train type
            String[] l3 = cartevent.getLine(3).split(" ");
            String location = l3[0]; // Location: station on linesys
            String style = l3[1]; // PA text style (separate file for every status)
            // Getters
            statimelist stl = stlmap.get(linesys);
            if (stl == null) {
                errorLog(new Exception(linesys + ".csv does not exist!"));
                return;
            }
            // Run for each cart, get passengers to play PA
            mg.forEach(m -> inCarPaSystem(m, stl, linesys, location, style));
        }
    }

    private void inCarPaSystem(MinecartMember<?> m, statimelist stl, String linesys, String location, String style) {
        TextComponent tc = getCarPaPlaceholderReplacedString(stl, linesys, location, style);
        // Play announcement to passengers
        for (Entity e : m.getEntity().getPassengers()) {
            if (e instanceof Player p) {
                p.spigot().sendMessage(tc);
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "In-car Passenger Announcer");
            opt.setDescription("Play announcements based off statimelist");
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            e.getPlayer().sendMessage(ChatColor.RED + "Invalid arguments!");
            e.setCancelled(true);
        }
        return true;
    }
}
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.fiveave.inpids.main.*;

/// inpidscarpa sign class
class carpasign extends SignAction {

    // Precompiled patterns to avoid recompiling regex inside loops
    private static final Pattern GENERAL_AND_INDEXED_PATTERN = Pattern.compile("%(dest|line_color|type_color|line|type|sta_code|sta|trans|door_dir)(_-?\\d+)?(_\\d+)?");

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

        // Pre-compute joined string tokens outside the loop to prevent repeated executions
        String joinedDest = String.join(" ", dest);
        String joinedLine = String.join(" ", line);
        String joinedType = String.join(" ", type);

        // Pre-map static replacements for constant O(1) retrieval speed
        Map<String, String> staticReplacements = Map.of(
                "dest", joinedDest,
                "line_color", linecolor,
                "type_color", typecolor,
                "line", joinedLine,
                "type", joinedType
        );

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
            } else {
                // Final calculation of the conditional variables used in the pass
                final int finalI = i;
                final int finalSelindex = selindex;
                String finalThisstacode = (i < stlsize) ? stacode.get(i) : null;
                if (finalThisstacode != null && orilinecode != null && altlinecode != null) {
                    finalThisstacode = finalThisstacode.replace(orilinecode, altlinecode);
                }
                final String stacodeComputed = finalThisstacode;

                Matcher matcher = GENERAL_AND_INDEXED_PATTERN.matcher(appendedstr);

                // Perform single-pass optimization using Java 9+ Matcher.replaceAll
                appendedstr = matcher.replaceAll(matchResult -> {

                    // Case 1: Handle language-indexed or array-indexed placeholders (e.g., %dest_0, %line_1, %type_0)
                    String baseToken = matchResult.group(1);     // e.g., "dest", "sta", "line"
                    String firstIndexStr = matchResult.group(2); // e.g., "_0", "_-1" (optional)
                    String langIndexStr = matchResult.group(3);  // e.g., "_0" (optional lang index)

                    String replacement = null;

                    // Structure 1: Process global array-indexed tokens (%dest_0, %line_1, %type_0)
                    if (firstIndexStr != null && langIndexStr == null) {
                        int idx = Integer.parseInt(firstIndexStr.substring(1));
                        switch (baseToken) {
                            case "dest":
                                if (idx >= 0 && idx < dest.length) replacement = dest[idx];
                                break;
                            case "line":
                                if (idx >= 0 && idx < line.length) replacement = line[idx];
                                break;
                            case "type":
                                if (idx >= 0 && idx < type.length) replacement = type[idx];
                                break;
                        }
                    }

                    // Structure 2: Process station-specific dynamic calculations (independent flow)
                    if (replacement == null && finalI < stlsize) {
                        // 2a. Fully double-indexed placeholders (e.g., %sta_-1_0)
                        if (firstIndexStr != null && langIndexStr != null) {
                            if ("sta".equals(baseToken)) {
                                int targetSelIndex = Integer.parseInt(firstIndexStr.substring(1));
                                int langIdx = Integer.parseInt(langIndexStr.substring(1));
                                if (targetSelIndex == finalSelindex) {
                                    String[] names = staname.get(finalI);
                                    if (langIdx >= 0 && langIdx < names.length) {
                                        replacement = names[langIdx];
                                    }
                                }
                            }
                        }
                        // 2b. Target-indexed specific properties (e.g., %sta_code_0, %sta_0, %trans_0)
                        else if (firstIndexStr != null) {
                            int targetSelIndex = Integer.parseInt(firstIndexStr.substring(1));
                            if (targetSelIndex == finalSelindex) {
                                replacement = switch (baseToken) {
                                    case "sta_code" -> stacodeComputed;
                                    case "sta" -> String.join(" ", staname.get(finalI));
                                    case "trans" -> String.join(" ", transfers.get(finalI));
                                    default -> null;
                                };
                            }
                        }
                    }

                    // Case 3: Handle global static configurations if no indexed parameters match
                    if (replacement == null && firstIndexStr == null && langIndexStr == null) {
                        if (staticReplacements.containsKey(baseToken)) {
                            replacement = staticReplacements.get(baseToken);
                        } else if ("door_dir".equals(baseToken) && doordir != null) {
                            replacement = doordir;
                        }
                    }

                    // Safeguard strings against raw regex symbol evaluation ($ or \) and fallback safely
                    return replacement != null ? Matcher.quoteReplacement(replacement) : matchResult.group(0);
                });
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
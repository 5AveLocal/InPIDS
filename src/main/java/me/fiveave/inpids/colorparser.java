package me.fiveave.inpids;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class colorparser {

    // Cache patterns for performance
    private static final Pattern HEX_HASH_PATTERN =
            Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_HEX_PATTERN =
            Pattern.compile("&x(&[A-Fa-f0-9]){6}");

    /**
     * Parse both RGB formats and ChatColor codes
     * Order: RGB first, then ChatColor
     */
    public static String parseColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Escape \& sequences
        message = message.replace("\\&", "§AMPERSAND§");
        // Escape \& sequences
        message = message.replace("\\\\n", "§BACKSLASH_N§");

        // Parse colors normally
        message = parseHexHash(message);
        message = parseLegacyHex(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        message = message.replace("\\n", "\n");

        // Restore literal &
        message = message.replace("§AMPERSAND§", "&");
        message = message.replace("§BACKSLASH_N§", "\\\\n");

        return message;
    }

    private static String parseHexHash(String message) {
        Matcher matcher = HEX_HASH_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String parseLegacyHex(String message) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String match = matcher.group();
            String replacement = match.replace('&', '§');
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
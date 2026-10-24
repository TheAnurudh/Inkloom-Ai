package com.inkloom.util;

import java.util.regex.Pattern;

/** Cleans user text before it is stored. (SQL injection is already prevented by JPA parameter binding.) */
public final class InputSanitizer {

    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]{0,200}>");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private InputSanitizer() { }

    /** Removes invisible control characters and trims. Keeps normal text/newlines (used for scripts). */
    public static String clean(String in) {
        if (in == null) return null;
        return CONTROL.matcher(in).replaceAll("").trim();
    }

    /** clean() + removes HTML tags like <script>. Used for names, titles, descriptions. */
    public static String plain(String in) {
        if (in == null) return null;
        return HTML_TAGS.matcher(clean(in)).replaceAll("").trim();
    }
}

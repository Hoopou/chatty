
package chatty.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class StringUtil {
    
    /**
     * Tries to turn the given Object into a List of Strings.
     * 
     * If the given Object is a List, go through all items and copy those
     * that are Strings into a new List of Strings.
     * 
     * @param obj
     * @return 
     */
    public static List<String> getStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                if (item instanceof String) {
                    result.add((String)item);
                }
            }
        }
        return result;
    }
    
    public static String join(String[] array) {
        return join(Arrays.asList(array), ",");
    }
    
    public static String join(Object[] array) {
        return join(Arrays.asList(array), ",");
    }
    
    public static String join(Collection<?> items, String delimiter) {
        return join(items, delimiter, -1, -1);
    }
    
    public static String join(Collection<?> items, String delimiter, int start) {
        return join(items, delimiter, start, -1);
    }
    
    public static String join(Collection<?> items, String delimiter, int start, int end) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        start = start > -1 ? start : 0;
        end = end > -1 ? end : items.size();
        
        StringBuilder b = new StringBuilder();
        Iterator<?> it = items.iterator();
        int i = 0;
        while (it.hasNext()) {
            String next = it.next().toString();
            if (i >= start && i < end) {
                b.append(next);
                if (it.hasNext() && i+1 < end) {
                    b.append(delimiter);
                }
            }
            i++;
        }
        return b.toString();
    }
    
    /**
     * Shortens the given {@code input} to the {@code max} length. Only changes
     * the {@code input} if it actually exceeds {@code max} length, but if it
     * does, the returning text is 2 shorter than {@code max} because it also adds
     * ".." where it shortened the text.
     * 
     * Positive {@code max} length values shorten the {@code input} at the end,
     * negative values shorten the {@code input} at the start.
     * 
     * @param input The {@code String} to shorten
     * @param max The maximum length the String should have after this
     * @return The modified {@code String} if {@code input} exceeds the
     * {@code max} length, the original value otherwise
     */
    public static String shortenTo(String input, int max) {
        if (input != null && input.length() > Math.abs(max)) {
            if (max > 2) {
                return input.substring(0, max-2)+"..";
            } else if (max < -2) {
                return ".."+input.substring(input.length() + max + 2 ); // abcd      -3
            } else {
                return "..";
            }
        }
        return input;
    }
    
    public static String shortenTo(String input, int max, int min) {
        if (input != null && input.length() > max) {
            if (min+2 > max) {
                min = max-2;
            }
            if (max > 2) {
                String start = input.substring(0, min);
                String end = input.substring(input.length() - (max - min - 2));
                return start+".."+end;
            } else {
                return "..";
            }
        }
        return input;
    }
    
    public static String trim(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }
    
    public static String nullToString(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }
    
    public static String toLowerCase(String s) {
        return s != null ? s.toLowerCase(Locale.ENGLISH) : null;
    }
    
    public static String firstToUpperCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1);
    }
    
    /**
     * Removes leading and trailing whitespace and removes and duplicate
     * whitespace in the middle. Due to the way it works, it also replaces any
     * whitespace characters that are not a space with a space (e.g. tabs).
     * 
     * @param s The String
     * @see removeDuplicateWhitespace(String text)
     * @return The modified String or null if the given String was null
     */
    public static String trimAll(String s) {
        if (s == null) {
            return s;
        }
        return removeDuplicateWhitespace(s).trim();
    }
    
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    
    /**
     * Replaces all occurences of one or more consecutive whitespace characters
     * with a single space. So it also replaces any whitespace characters that
     * are not a space with a space (e.g. tabs).
     * 
     * @param text
     * @return 
     */
    public static String removeDuplicateWhitespace(String text) {
        return WHITESPACE.matcher(text).replaceAll(" ");
    }
    
    private static final Pattern LINEBREAK_CHARACTERS = Pattern.compile("[\\r\\n]+");
    
    /**
     * Removes any linebreak characters from the given String and replaces them
     * with a space. Consecutive linebreak characters are replaced with only a
     * single space.
     * 
     * @param s The String (can be empty or null)
     * @return The modified String or null if the given String was null
     */
    public static String removeLinebreakCharacters(String s) {
        if (s == null) {
            return null;
        }
        return LINEBREAK_CHARACTERS.matcher(s).replaceAll(" ");
    }
    
    public static String append(String a, String sep, String b) {
        if (a == null || a.isEmpty()) {
            return b;
        }
        if (b == null || b.isEmpty()) {
            return a;
        }
        return a+sep+b;
    }
    
    /**
     * Checks if any of the String arguments is null or empty.
     * 
     * @param input A number of String arguments
     * @return true if at least one of the arguments is null or empty, false
     * otherwise
     */
    public static boolean isNullOrEmpty(String... input) {
        if (input == null) {
            return true;
        }
        for (String s : input) {
            if (s == null || s.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    public static final String UTF8_BOM = "\uFEFF";
    
    /**
     * Remove the UTF-8 BOM from the beginning of the input.
     * 
     * @param input
     * @return 
     */
    public static String removeUTF8BOM(String input) {
        if (input != null && input.startsWith(UTF8_BOM)) {
            return input.substring(1);
        }
        return input;
    }
    
    /**
     * Adds linebreaks to the input, in place of existing space characters, so
     * that each resulting line has the given maximum length. If there is no
     * space character where needed a line may be longer. The added linebreaks
     * don't count into the maximum line length.
     *
     * @param input The intput to modify
     * @param maxLineLength The maximum line length in number of characters
     * @param html If true, a "&lt;br /&gt;" will be added instead of a \n
     * @return 
     */
    public static String addLinebreaks(String input, int maxLineLength, boolean html) {
        if (input == null || input.length() <= maxLineLength) {
            return input;
        }
        String[] words = input.split(" ");
        StringBuilder b = new StringBuilder();
        int lineLength = 0;
        for (int i=0;i<words.length;i++) {
            String word = words[i];
            if (b.length() > 0
                    && lineLength + word.length() > maxLineLength) {
                if (html) {
                    b.append("<br />");
                } else {
                    b.append("\n");
                }
                lineLength = 0;
            } else if (b.length() > 0) {
                b.append(" ");
                lineLength++;
            }
            b.append(word);
            lineLength += word.length();
        }
        return b.toString();
    }
    
    public static String aEmptyb(String value, String a, String b) {
        if (value == null || value.isEmpty()) {
            return a;
        }
        return String.format(b, value);
    }
    
    public static String concats(Object... args) {
        return concat(" ", args);
    }
    
    public static String concat(String sep, Object... args) {
        if (args.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        boolean appended = false;
        for (Object arg : args) {
            if (appended) {
                b.append(sep);
                appended = false;
            }
            if (arg != null) {
                b.append(arg.toString());
                appended = true;
            }
        }
        return b.toString();
    }
    
    public static List<String> split(String input, char splitAt, int limit) {
        return split(input, splitAt, '"', '\\', limit, 1);
    }
    
    public static List<String> split(String input, char splitAt, int limit, int remove) {
        return split(input, splitAt, '"', '\\', limit, remove);
    }
    
    /**
     * Split the given input String by the {@code splitAt} character. Sections
     * enclosed in the {@code quote} character and characters prefixed by the
     * {@code escape} character aren't checked for the {@code splitAt}
     * character.
     * <p>
     * Whether quote/escape characters are removed from the result can be
     * controlled by the {@code remove} value:
     * <ul>
     * <li>0 - don't remove
     * <li>1 - remove from all parts, except result number {@code limit} (if
     * {@code limit} > 0)
     * <li>2 - remove from all parts
     * </ul>
     * 
     * @param input The input to be split
     * @param splitAt The split character
     * @param quote The quote character
     * @param escape The escape character, also used to escape the quote
     * character and itself
     * @param limit Maximum number of parts in the result (0 for high limit)
     * @param remove 0 - don't remove quote/escape characters, 1 - remove from
     * all parts (except result number "limit", if limit > 0), 2 - remove from
     * all parts
     * @return
     */
    public static List<String> split(String input, char splitAt, char quote, char escape, int limit, int remove) {
        if (input == null) {
            return null;
        }
        List<String> result = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        limit = Math.abs(limit);
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            // Add one escaped character
            if (escaped) {
                b.append(c);
                escaped = false;
            }
            // Next character escaped
            else if (c == escape) {
                escaped = true;
                if (remove == 0) {
                    b.append(c);
                }
            }
            // Begin and end quoted section
            else if (c == quote) {
                quoted = !quoted;
                if (remove == 0) {
                    b.append(c);
                }
            }
            // Split character found, ignore if quoted or max count reached
            else if (c == splitAt && !quoted && result.size()+1 < limit) {
                result.add(b.toString());
                b = new StringBuilder();
                if (result.size()+1 == limit && remove < 2) {
                    // Add remaining text without parsing
                    result.add(input.substring(i+1));
                    return result;
                }
            }
            // Nothing special, just add character
            else {
                b.append(c);
            }
        }
        // Add last
        result.add(b.toString());
        return result;
    }
    
    public static final NullComparator NULL_COMPARATOR = new NullComparator();
    
    private static class NullComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o1.compareTo(o2);
        }

    }
    
    public static String stringFromInputStream(InputStream inputStream) {
        return stringFromInputStream(inputStream, "UTF-8");
    }
    
    public static String stringFromInputStream(InputStream inputStream, String charset) {
        try (InputStream input = inputStream) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(charset);
        } catch (IOException ex) {
            return null;
        }
    }
    
    public static String randomString(String[] input) {
        if (input.length == 0) {
            return null;
        }
        return input[ThreadLocalRandom.current().nextInt(input.length)];
    }
    
    public static final void main(String[] args) {
        System.out.println(shortenTo("abcdefghi", 8, 5));
        System.out.println(concats("a", null, "b", null));
    }
    
}
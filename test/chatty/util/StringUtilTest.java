
package chatty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class StringUtilTest {
    
    @Test
    public void testRemoveLinebreakCharacters() {
        assertEquals(StringUtil.removeLinebreakCharacters("abc\r\nabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\rabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\nabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc abc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\r\r\r\r\rabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("abc\r\n\n\r\rabc"), "abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("\nabc abc"), " abc abc");
        assertEquals(StringUtil.removeLinebreakCharacters("\r"), " ");
    }
    
    @Test
    public void testRemoveDuplicateWhitespace() {
        assertEquals(StringUtil.removeDuplicateWhitespace("abc  abc"), "abc abc");
        assertEquals(StringUtil.removeDuplicateWhitespace("abc   abc"), "abc abc");
        assertEquals(StringUtil.removeDuplicateWhitespace("abcabc"), "abcabc");
        assertEquals(StringUtil.removeDuplicateWhitespace("abc abc"), "abc abc");
        assertEquals(StringUtil.removeDuplicateWhitespace("  "), " ");
        assertEquals(StringUtil.removeDuplicateWhitespace(""), "");
    }
    
    @Test
    public void testAppend() {
        assertEquals(StringUtil.append("abc", "|", "abc"), "abc|abc");
        assertEquals(StringUtil.append("abc", "", "abc"), "abcabc");
        assertEquals(StringUtil.append(null, "|", "b"), "b");
        assertEquals(StringUtil.append("", "|", "b"), "b");
        assertEquals(StringUtil.append("abc", "|", null), "abc");
        assertEquals(StringUtil.append("abc", null, "abc"), "abcnullabc");
        assertEquals(StringUtil.append(null, null, null), null);
    }
    
    @Test
    public void testJoin() {
        Collection<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals(StringUtil.join(list, " "), "a b c");
        assertEquals(StringUtil.join(list, ", "), "a, b, c");
        assertEquals(StringUtil.join(list, ", ", 1), "b, c");
        assertEquals(StringUtil.join(list, ", ", 0, 2), "a, b");
        assertEquals(StringUtil.join(list, ", ", -1, 2), "a, b");
        assertEquals(StringUtil.join(list, ", ", 1, 2), "b");
        assertEquals(StringUtil.join(list, ", ", 3), "");
        list.add(" d");
        assertEquals(StringUtil.join(list, ", "), "a, b, c,  d");
    }
    
    @Test
    public void testFirstToUpperCase() {
        assertEquals(StringUtil.firstToUpperCase(""), "");
        assertEquals(StringUtil.firstToUpperCase("a"), "A");
        assertEquals(StringUtil.firstToUpperCase(null), null);
        assertEquals(StringUtil.firstToUpperCase("Abc"), "Abc");
        assertEquals(StringUtil.firstToUpperCase("abc"), "Abc");
        assertEquals(StringUtil.firstToUpperCase(" abc"), " abc");
        
    }
    
    @Test
    public void testSplit() {
        assertEquals(StringUtil.split(null, 'a', 10), null);
        testSplit2(',', 0, "", "");
        testSplit2(',', 0, "abc", "abc");
        testSplit2(',', 0, "\\abc", "abc");
        testSplit2(',', 0, "\\\\abc", "\\abc");
        testSplit2(',', 0, "a,b,c", "a", "b", "c");
        testSplit2(',', 1, "a,b,c", "a,b,c");
        testSplit2(',', 2, "a,b,c", "a", "b,c");
        testSplit2(',', 0, "'a,b',c", "a,b", "c");
        testSplit2(',', 0, "\\'a,b,c", "'a", "b", "c");
        testSplit2(',', 2, "\\'a,b,c'", "'a", "b,c'");
        testSplit2(',', '\'', '\\', 2, 2, "\\'a,b,c'", "'a", "b,c");
        testSplit2(',', 2, "a,b,\\c", "a", "b,\\c");
        testSplit2(',', '\'', '\\', 2, 2, "a,b,\\c", "a", "b,c");
        testSplit2(',', 2, "a\\,b,c", "a,b", "c");
        testSplit2(',', 0, "t|test=abc\\,lol", "t|test=abc,lol");
        
        testSplit2(',', '#', '#', 2, 1, "a,b,c", "a", "b,c");
        testSplit2(',', '#', '#', 2, 1, "a#,b,c", "a,b", "c");
        testSplit2(' ', '-', '$', 2, 1, "abc- -123 -b c-", "abc 123", "-b c-");
        testSplit2(' ', '-', '$', 2, 1, "abc- $-123 -b c-", "abc -123 b", "c-");
        testSplit2(' ', '-', '$', 2, 1, "abc$ 123 -b c-", "abc 123", "-b c-");
        testSplit2(' ', '-', '$', 3, 1, "abc$ 123 -b c-", "abc 123", "b c");
        testSplit2(' ', '-', '$', 0, 1, "abc$ 123 -b c-", "abc 123", "b c");
        testSplit2(' ', '-', '$', 2, 2, "abc$ 123 -b c-", "abc 123", "b c");
        testSplit2(' ', '-', '$', 2, 0, "abc$ 123 -b c-", "abc$ 123", "-b c-");
        testSplit2(' ', '-', '$', 0, 0, "abc$ 123 -b c-", "abc$ 123", "-b c-");
    }
    
    private static void testSplit2(char split, int limit, String input, String... result) {
        testSplit2(split, '\'', '\\', limit, 1, input, result);
    }
    
    private static void testSplit2(char split, char quote, char escape, int limit, int remove, String input, String... result) {
        assertEquals(StringUtil.split(input, split, quote, escape, limit, remove), Arrays.asList(result));
    }
    
}

package com.BilawalAhmed0900;

public class StringUtils
{
    public static String abbreviate(String source, int tillLength, boolean addEllipses)
    {
        if (source == null)
        {
            return null;
        }

        int stringLength = source.length();
        if (tillLength >= stringLength)
        {
            return source;
        }

        if (tillLength < 0)
        {
            throw new IllegalArgumentException("tillLength < 0");
        }

        int tillTruncate = Character.isLowSurrogate(source.charAt(tillLength)) ? tillLength - 1 : tillLength;
        if (addEllipses)
            tillTruncate -= 3;

        return source.substring(0, tillTruncate) + ((addEllipses) ? "..." : "");
    }

    public static String abbreviate(String source, int tillLength)
    {
        return abbreviate(source, tillLength, true);
    }
}

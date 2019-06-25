package com.BilawalAhmed0900;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebSocketHashGenerator
{
    public static String generateHash(String givenHash)
    {
        try
        {
            String hash = givenHash + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(hash.getBytes());

            return new String(Base64.getEncoder().encode(messageDigest.digest()));
        }
        catch (NoSuchAlgorithmException e)
        {
            return "";
        }
    }
}

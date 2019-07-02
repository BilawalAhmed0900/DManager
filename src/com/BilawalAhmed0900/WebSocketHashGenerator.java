package com.BilawalAhmed0900;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/*
    https://tools.ietf.org/html/rfc6455#section-1.3
    https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers
 */
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

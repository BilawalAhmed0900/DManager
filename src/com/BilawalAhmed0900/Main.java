package com.BilawalAhmed0900;

import java.io.IOException;
import java.net.*;

public class Main
{
    private static final int SERVER_SOCKET_PORT = 49152;

    public static void main(String[] args)
    {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_SOCKET_PORT);
            while (true)
            {
                try
                {
                    WebSocket webSocket = new WebSocket(serverSocket.accept());
                    SocketReaderThread socketReaderThread = new SocketReaderThread(webSocket);
                    socketReaderThread.start();
                }
                catch(SocketException e)
                {
                    System.out.println(
                            "Server socket on port "
                            + SERVER_SOCKET_PORT +
                            " is closed or is not bound yet..."
                    );
                    // e.printStackTrace();
                    break;
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

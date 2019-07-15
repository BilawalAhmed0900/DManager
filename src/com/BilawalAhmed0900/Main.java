package com.BilawalAhmed0900;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class Main
{
    private static final int SERVER_SOCKET_PORT = 49152;

    public static void main(String[] args)
    {
        /*
            Cookie Manager for cookie handling of UrlConnection's
         */
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        System.setProperty("awt.useSystemAAFontSettings", "false");

        /*
            Turning off HTTPS SSL security
         */
        try
        {
            HttpsURLConnection.setDefaultSSLSocketFactory(RelaxedSSLContext.getInstance().getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(RelaxedSSLContext.allHostsValid);

            // System.setProperty("jsse.enableSNIExtension", "false");
        }
        catch (KeyManagementException | NoSuchAlgorithmException e)
        {
            JOptionPaneWithFrame.showExceptionBox(e.getMessage(), false);
            return;
        }

        /*
            A Socket sever to allow extension to get attached as a client
         */
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
        }
        catch (IOException e)
        {
            JOptionPaneWithFrame.showExceptionBox(e, false);
        }
    }
}

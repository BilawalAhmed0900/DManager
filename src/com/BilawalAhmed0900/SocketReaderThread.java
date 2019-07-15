package com.BilawalAhmed0900;

/*
    A Thread Class which reads from WebSocket and start a downloading thread
    Just a thread wrapper above WebSocket
 */
public class SocketReaderThread extends Thread
{
    private WebSocket webSocket;

    public SocketReaderThread(WebSocket webSocket)
    {
        this.webSocket = webSocket;
    }

    @Override
    public void run()
    {
        System.out.println("Socket reader thread started...");
        while (true)
        {
            String readString = webSocket.getReadString();
            if (readString.equals("@@__SOCKET__CLOSED__@@")
                    || readString.equals("@@__NOT__FOUND__@@")
                    || readString.equals("@@__OLD_DATA__@@"))
            {
                break;
            }

            UrlConnectionThread urlConnectionThread = new UrlConnectionThread(readString);
            urlConnectionThread.start();
        }

        System.out.println("Socket reader thread ended...");
    }
}

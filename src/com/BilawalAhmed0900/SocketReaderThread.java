package com.BilawalAhmed0900;

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

            DownloaderThread downloaderThread = new DownloaderThread(readString);
            downloaderThread.start();
        }

        System.out.println("Socket reader thread ended...");
    }
}

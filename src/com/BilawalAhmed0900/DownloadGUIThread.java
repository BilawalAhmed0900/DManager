package com.BilawalAhmed0900;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadGUIThread extends Thread
{
    private String filename;
    private HttpURLConnection urlConnection;
    private AtomicLong downloaded;
    private AtomicBoolean hasCompleted;
    private volatile boolean running = true;
    private final int BUFFER_SIZE = 1024;

    public DownloadGUIThread(String filename, HttpURLConnection urlConnection,
                             AtomicLong downloaded, AtomicBoolean hasCompleted)
    {
        this.filename = filename;
        this.urlConnection = urlConnection;
        this.downloaded = downloaded;
        this.hasCompleted = hasCompleted;
    }

    @Override public void run()
    {
        FileOutputStream fileOutputStream = null;
        try
        {
            fileOutputStream = new FileOutputStream(filename);
            InputStream connectionStream = urlConnection.getInputStream();
            byte[] buffer = new byte[BUFFER_SIZE];

            while (running)
            {
                int read = connectionStream.read(buffer);
                if (read == -1)
                {
                    break;
                }

                fileOutputStream.write(buffer, 0, read);
                downloaded.addAndGet(read);
            }
        }
        catch (FileNotFoundException e)
        {
            stopDownloading();
            System.out.printf("Cannot find file \"%s\"\n", filename);
            // e.printStackTrace();
        }
        catch (IOException e)
        {
            stopDownloading();
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (fileOutputStream != null)
                {
                    fileOutputStream.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public synchronized void stopDownloading()
    {
        running = false;
        hasCompleted.set(false);
    }
}

package com.BilawalAhmed0900;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/*
    A Thread class which downloads from a HttpUrlConnection and updates to AtomicLong how much has been read
 */
public class DownloadGUIThread extends Thread
{
    private String filename;
    private HttpURLConnection urlConnection;
    private AtomicLong downloaded;
    private AtomicBoolean hasCompleted;
    private volatile boolean running = true;
    private final int BUFFER_SIZE = 65536;

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
        BufferedOutputStream fileOutputStream = null;
        try
        {
            fileOutputStream = new BufferedOutputStream(new FileOutputStream(filename), 8 * BUFFER_SIZE);
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
            JOptionPaneWithFrame.showExceptionBox(e.getMessage(), false);
        }
        catch (IOException e)
        {
            stopDownloading();
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
                JOptionPaneWithFrame.showExceptionBox(e.getMessage(), false);
            }
        }
    }

    public synchronized void stopDownloading()
    {
        running = false;
        hasCompleted.set(false);
    }
}

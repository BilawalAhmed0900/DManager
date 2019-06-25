package com.BilawalAhmed0900;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class BytesToMiBGiBTiB
{
    public static String normalize(long size)
    {
        final String[] suffix = { "B", "KiB", "MiB", "GiB", "TiB" };
        long tempSize = size;

        int suffixIndex = 0;
        while (tempSize > 0)
        {
            tempSize /= 1024;
            if (suffixIndex == 4)
            {
                break;
            }

            if (tempSize > 0)
            {
                suffixIndex++;
            }
        }

        return String.format("%.2f %s", (double)size / Math.pow(1024, suffixIndex), suffix[suffixIndex]);
    }
}

class GUI extends Thread
{
    private String filename;
    private long filesize;
    private String url;
    private AtomicLong downloaded;
    private DownloadGUIThread[] downloadGUIThreads;

    private JFrame jFrame;
    private JTextArea jTextArea;

    public GUI(String filename, long filesize, String url, AtomicLong downloaded, DownloadGUIThread[] downloadGUIThreads)
    {
        this.filename = new File(filename).getName();
        this.filesize = filesize;
        this.url = url;
        this.downloaded = downloaded;
        this.downloadGUIThreads = downloadGUIThreads;

        jFrame = new JFrame("Downloading \"" + new File(filename).getName() + "\"");
        jFrame.setSize(600, 155);
        jFrame.setBackground(Color.WHITE);
        jFrame.setAlwaysOnTop(true);
        jFrame.setResizable(false);

        JPanel jPanel = new JPanel();
        jPanel.setBackground(Color.WHITE);
        jPanel.setLayout(null);

        jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        jTextArea.setHighlighter(null);
        jTextArea.setBounds(0, 0, 600, 80);
        jPanel.add(jTextArea);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e ->
        {
            for (DownloadGUIThread downloadGUIThread: downloadGUIThreads)
            {
                downloadGUIThread.stopDownloading();
            }
        });
        cancelButton.setSize(100, 28);
        cancelButton.setBounds(480, 83, 100, 28);
        jPanel.add(cancelButton);

        jPanel.setVisible(true);
        jFrame.add(jPanel);

        jFrame.addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e)
            {
                for (DownloadGUIThread downloadGUIThread: downloadGUIThreads)
                {
                    downloadGUIThread.stopDownloading();
                }

                super.windowClosing(e);
            }
        });

        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setVisible(true);
    }

    @Override public void run()
    {
        System.out.println("Started GUI...");
        String filesizeString = ((filesize == -1) ? "(UNKNOWN)" : BytesToMiBGiBTiB.normalize(filesize));
        while (true)
        {
            boolean isWorking = false;
            for (int i = 0; i < downloadGUIThreads.length; i++)
            {
                isWorking = isWorking || downloadGUIThreads[i].isAlive();
            }

            if (!isWorking)
            {
                break;
            }

            SwingUtilities.invokeLater(() ->
                    jTextArea.setText("  URL:                 " + url + "\n" +
                                      "  Filename:          " + filename + "\n" +
                                      "  Filesize:            " + filesizeString + "\n" +
                                      "  Downloaded:    " + BytesToMiBGiBTiB.normalize(downloaded.get()) + "\n" +
                                      "  Progress:          " + ((filesize != -1) ? String.format("%.2f%%", (double)downloaded.get() / (double)filesize * 100.0) : "(UNKNOWN)")));


            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        jFrame.dispose();
        System.out.println("Ended GUI...");
    }
}

public class DownloaderGUI
{
    private String url;
    private String filename;
    private long filesize;
    private List<HttpURLConnection> links;
    private final static int BUFFER_SIZE = 32 * 1024 * 1024;

    public DownloaderGUI(String url, String filename, long filesize, List<HttpURLConnection> links)
    {
        this.url = url;
        this.filename = filename;
        this.filesize = filesize;
        this.links = links;
    }

    public void download()
    {
        ArrayList<String> filePartsName = new ArrayList<>(links.size());
        if (links.size() > 1)
        {
            for (int i = 0, size = links.size(); i < size; i++)
            {
                filePartsName.add(filename + "__TEMP_PART_" + String.format("%02d", i) + "__");
            }
        }
        else
        {
            filePartsName.add(filename + "__TEMP_PART__");
        }

        AtomicLong downloaded = new AtomicLong(0);
        AtomicBoolean hasCompleted = new AtomicBoolean(true);

        final DownloadGUIThread[] downloaderThreads = new DownloadGUIThread[links.size()];
        for (int i = 0, size = links.size(); i < size; i++)
        {
            downloaderThreads[i] = new DownloadGUIThread(filePartsName.get(i), links.get(i), downloaded, hasCompleted);
            downloaderThreads[i].start();
        }

        GUI gui = new GUI(filename, filesize, url, downloaded, downloaderThreads);
        gui.start();

        for (int i = 0, size = links.size(); i < size; i++)
        {
            try
            {
                downloaderThreads[i].join();
            }
            catch (InterruptedException e)
            {
                // e.printStackTrace();
            }
        }

        try
        {
            gui.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        if (hasCompleted.get() == false)
        {
            for (int i = 0, size = links.size(); i < size; i++)
            {
                new File(filePartsName.get(i)).delete();
            }

            WaitingBox waitingBox = new WaitingBox("Connection Error!",
                    String.format("<html>Read Timeout!<br>Connection error occurred while downloading<br>\"%s\"</html>", filename),
                    500, 200);
            waitingBox.waitForClosing();
        }
        else if (links.size() > 1)
        {
            try
            {
                FileOutputStream fileOutputStream = new FileOutputStream(filename);
                byte[] buffer = new byte[BUFFER_SIZE];
                for (int i = 0, size = links.size(); i < size; i++)
                {
                    FileInputStream fileInputStream = new FileInputStream(filePartsName.get(i));
                    while (true)
                    {
                        int read = fileInputStream.read(buffer);
                        if (read == -1)
                        {
                            break;
                        }

                        fileOutputStream.write(buffer, 0, read);
                    }
                    fileInputStream.close();

                    new File(filePartsName.get(i)).delete();
                }
                fileOutputStream.close();
            }
            catch (FileNotFoundException e)
            {
                // e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            WaitingBox waitingBox = new WaitingBox("Downloading completed!",
                    String.format("<html>Downloading completed of<br>\"%s\"</html>", filename),
                    500, 200);
            waitingBox.waitForClosing();
        }
        else if (links.size() == 1)
        {
            File oldFile = new File(filename);
            if (oldFile.exists() && !oldFile.isDirectory())
            {
                oldFile.delete();
            }

            new File(filePartsName.get(0)).renameTo(oldFile);

            WaitingBox waitingBox = new WaitingBox("Downloading completed!",
                    String.format("<html>Downloading completed of<br>\"%s\"</html>", filename),
                    500, 200);
            waitingBox.waitForClosing();
        }

    }
}

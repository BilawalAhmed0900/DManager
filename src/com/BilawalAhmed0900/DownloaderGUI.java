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
    public static String normalize(long size, int precisionDigit)
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

        return String.format("%." + precisionDigit + "f %s",
                             (double)size / Math.pow(1024, suffixIndex), suffix[suffixIndex]);
    }
}

class GUI extends Thread
{
    private String filename;
    private long filesize;
    private String url;
    private AtomicLong downloaded;
    private AtomicBoolean hasCompleted;
    private AtomicBoolean hasJoined;

    private JFrame jFrame;
    private JTextArea jTextArea;
    private JProgressBar jProgressBar;

    public GUI(String filename, long filesize, String url,
               AtomicLong downloaded, DownloadGUIThread[] downloadGUIThreads,
               AtomicBoolean hasCompleted, AtomicBoolean hasJoined, AtomicBoolean hasCancelled)
    {
        this.filename = new File(filename).getName();
        this.filesize = filesize;
        this.url = url;
        this.downloaded = downloaded;
        this.hasCompleted = hasCompleted;
        this.hasJoined = hasJoined;

        jFrame = new JFrame("Downloading \"" + new File(filename).getName() + "\"");
        jFrame.setSize(600, 190);
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

        jProgressBar = new JProgressBar(0, 1000);
        jProgressBar.setBounds(5, 85, 575, 25);
        jProgressBar.setForeground(new Color(0, 194, 49));
        jPanel.add(jProgressBar);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e ->
        {
            for (DownloadGUIThread downloadGUIThread: downloadGUIThreads)
            {
                downloadGUIThread.stopDownloading();
            }

            hasCancelled.set(true);
        });
        cancelButton.setSize(100, 28);
        cancelButton.setBounds(480, 118, 100, 28);
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
                hasCancelled.set(true);

                // super.windowClosing(e);
            }
        });

        // jFrame.pack();
        jFrame.setLocationRelativeTo(null);

        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setVisible(true);
    }

    @Override public void run()
    {
        System.out.println("Started GUI...");
        final Taskbar taskbar = GetTaskBar.getTaskbar();

        jProgressBar.setValue(jProgressBar.getMaximum());
        String filesizeString = ((filesize == -1) ? "(UNKNOWN)" : BytesToMiBGiBTiB.normalize(filesize, 3));

        int currentProgressValue, previousProgressValue = -1;
        while (true)
        {
            boolean isWorking = !hasJoined.get() && hasCompleted.get();
            if (!isWorking)
            {
                break;
            }

            double progressValue = (double)downloaded.get() / (double)filesize * 100.0;
            if (filesize != -1)
            {
                SwingUtilities.invokeLater(() ->
                                                   jProgressBar.setValue((int)Math.round(progressValue * 10)));

            }

            SwingUtilities.invokeLater(() ->
                    jTextArea.setText("  URL:                 " + url + "\n" +
                                      "  Filename:          " + filename + "\n" +
                                      "  Filesize:            " + filesizeString + "\n" +
                                      "  Downloaded:    " + BytesToMiBGiBTiB.normalize(downloaded.get(), 3) + "\n" +
                                      "  Progress:          " + ((filesize != -1) ? String.format("%.2f%%", progressValue) : "(UNKNOWN)")));

            currentProgressValue = (int)Math.round(progressValue);
            if (taskbar != null && filesize != -1 && currentProgressValue != previousProgressValue)
            {
                final int taskBarProgressValue = currentProgressValue;
                SwingUtilities.invokeLater(() -> taskbar.setWindowProgressValue(jFrame, taskBarProgressValue));

            }
            previousProgressValue = currentProgressValue;

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

/*
    A class that invoke a GUI and invoke several threads to download from ArrayList of HttpUrlConnection
 */
public class DownloaderGUI
{
    private String url;
    private String filename;
    private long filesize;
    private List<HttpURLConnection> links;

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

        // User pressed cancel
        AtomicBoolean hasCancelled = new AtomicBoolean(false);

        // Downloading completed without ReadTimeOut
        AtomicBoolean hasCompleted = new AtomicBoolean(true);

        // Parts have been joined or not
        AtomicBoolean hasJoined = new AtomicBoolean(false);

        final DownloadGUIThread[] downloaderThreads = new DownloadGUIThread[links.size()];
        for (int i = 0, size = links.size(); i < size; i++)
        {
            downloaderThreads[i] = new DownloadGUIThread(filePartsName.get(i), links.get(i), downloaded, hasCompleted);
            downloaderThreads[i].start();
        }

        GUI gui = new GUI(filename, filesize, url, downloaded, downloaderThreads, hasCompleted, hasJoined, hasCancelled);
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

        if (hasCompleted.get() == false)
        {
            for (int i = 0, size = links.size(); i < size; i++)
            {
                new File(filePartsName.get(i)).delete();
            }

            if (hasCancelled.get() == false)
            {
                JOptionPaneWithFrame.showMessageDialog(String.format("<html>Read Timeout!<br>Connection error occurred while downloading<br>\"%s\"</html>", filename),
                                                       "Connection Error!",
                                                       JOptionPane.ERROR_MESSAGE,
                                                       null, false);
            }
        }
        else
        {
            if (links.size() > 1)
            {
                try
                {
                    CombineFiles.combine(filePartsName, filename, true);
                }
                catch (SecurityException e)
                {
                    JOptionPaneWithFrame.showExceptionBox(e.getMessage(), false);
                }
                catch (FileNotFoundException e)
                {
                    // e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            else if (links.size() == 1)
            {
                try
                {
                    File oldFile = new File(filename);
                    if (oldFile.exists() && !oldFile.isDirectory())
                    {
                        oldFile.delete();
                    }

                    new File(filePartsName.get(0)).renameTo(oldFile);
                }
                catch (SecurityException e)
                {
                    JOptionPaneWithFrame.showExceptionBox(e.getMessage(), false);
                }
            }

            hasJoined.set(true);
            Object[] buttonText = { "Open", "Open Directory", "Close" };
            int result = JOptionPaneWithFrame.showOptionDialog(String.format("<html>Downloading completed of<br>\"%s\"</html>", filename),
                                                               "Downloading completed!",
                                                               JOptionPane.YES_NO_CANCEL_OPTION,
                                                               JOptionPane.PLAIN_MESSAGE,
                                                               null,
                                                               buttonText,
                                                               buttonText[2]);
            if (result == JOptionPane.YES_OPTION)
            {
                try
                {
                    Desktop.getDesktop().open(new File(filename));
                }
                catch (IOException e)
                {
                    JOptionPaneWithFrame.showExceptionBox(e.getMessage(), false);
                }
            }
            else if (result == JOptionPane.NO_OPTION)
            {
                try
                {
                    BrowseToFile.browseTo(filename);
                }
                catch (IOException e)
                {
                    JOptionPaneWithFrame.showExceptionBox(e.getMessage(), false);
                }

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
    }
}

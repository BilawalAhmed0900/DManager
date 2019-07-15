package com.BilawalAhmed0900;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
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
            tempSize /= 1024L;
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
                             (double)size / Math.pow(1024L, suffixIndex), suffix[suffixIndex]);
    }
}

class DownloaderGUI extends Thread
{
    private String filename;
    private long filesize;
    private String url;
    private AtomicLong downloaded;
    private AtomicBoolean hasCompleted;
    private AtomicBoolean hasJoined;
    private AtomicBoolean isJoining;

    private JFrame jFrame;
    private JTextArea jTextArea;
    private JProgressBar jProgressBar;

    public DownloaderGUI(String filename, long filesize, String url,
                         AtomicLong downloaded, DownloaderBackendThread[] downloaderBackendThreads,
                         AtomicBoolean hasCompleted, AtomicBoolean hasJoined, AtomicBoolean hasCancelled, AtomicBoolean isJoining)
    {
        this.filename = new File(filename).getName();
        this.filesize = filesize;
        this.url = url;
        this.downloaded = downloaded;
        this.hasCompleted = hasCompleted;
        this.hasJoined = hasJoined;
        this.isJoining = isJoining;

        jFrame = new JFrame("Downloading \"" + new File(filename).getName() + "\"");
        jFrame.setSize(600, 210);
        jFrame.setBackground(Color.WHITE);
        jFrame.setAlwaysOnTop(true);
        jFrame.setResizable(false);

        JPanel jPanel = new JPanel();
        jPanel.setBackground(Color.WHITE);
        jPanel.setLayout(null);

        jTextArea = new JTextArea();
        jTextArea.setEditable(false);
        jTextArea.setHighlighter(null);
        jTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jTextArea.setBounds(0, 0, 600, 100);
        jPanel.add(jTextArea);

        jProgressBar = new JProgressBar(0, 1000);
        jProgressBar.setBounds(5, 105, 575, 25);
        jProgressBar.setForeground(new Color(0, 194, 49));
        jPanel.add(jProgressBar);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e ->
                                       {
                                           for (DownloaderBackendThread downloaderBackendThread : downloaderBackendThreads)
                                           {
                                               downloaderBackendThread.stopDownloading();
                                           }

                                           hasCancelled.set(true);
                                       });
        cancelButton.setSize(100, 28);
        cancelButton.setBounds(480, 138, 100, 28);
        jPanel.add(cancelButton);

        jPanel.setVisible(true);
        jFrame.add(jPanel);

        jFrame.addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e)
            {
                for (DownloaderBackendThread downloaderBackendThread : downloaderBackendThreads)
                {
                    downloaderBackendThread.stopDownloading();
                }
                hasCancelled.set(true);

                // super.windowClosing(e);
            }
        });

        // jFrame.pack();
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try
        {
            SwingUtilities.invokeAndWait(() ->
                                         {
                                             jFrame.setLocationRelativeTo(null);
                                             jFrame.setVisible(true);
                                         });
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            e.printStackTrace();
        }

    }

    @Override public void run()
    {
        long previousTime = System.nanoTime(), elapsedTime;
        long previousDownloaded = 0, currentDownloaded;
        System.out.println("Started DownloaderGUI...");
        final Taskbar taskbar = GetTaskBar.getTaskbar();

        try
        {
            SwingUtilities.invokeAndWait(() -> jProgressBar.setValue(jProgressBar.getMaximum()));
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            e.printStackTrace();
        }

        String filesizeString = ((filesize == -1) ? BytesToMiBGiBTiB.normalize(0, 3) : BytesToMiBGiBTiB.normalize(filesize, 3));

        LongAverage longAverage = new LongAverage();
        int currentProgressValue, previousProgressValue = -1;
        while (true)
        {
            boolean isWorking = !hasJoined.get() && hasCompleted.get();
            if (!isWorking)
            {
                break;
            }

            double progressValue = (double)downloaded.get() / (double)filesize * 100.0;
            try
            {
                if (filesize != -1)
                {
                    SwingUtilities.invokeAndWait(() ->
                                                         jProgressBar.setValue((int)Math.round(progressValue * 10)));

                }

                if (isJoining.get())
                {
                    final long fileSizeCompleted = previousDownloaded;
                    SwingUtilities.invokeAndWait(() ->
                     jTextArea.setText(" URL:          " + url + "\n" +
                                               " Filename:     " + filename + "\n" +
                                               " File size:    " + BytesToMiBGiBTiB.normalize(fileSizeCompleted, 3) + "\n" +
                                               " Joined:       " + BytesToMiBGiBTiB.normalize(downloaded.get(), 3) + "\n" +
                                               " Progress:     " + ((filesize != -1) ? String.format("%.2f%%", progressValue) : "100.00%")));

                }
                else
                {
                    elapsedTime = System.nanoTime() - previousTime;
                    currentDownloaded = downloaded.get() - previousDownloaded;
                    final long currentSpeed = currentDownloaded * 1_000_000_000 / elapsedTime;
                    longAverage.put(currentSpeed);

                    SwingUtilities.invokeAndWait(() ->
                     jTextArea.setText(" URL:          " + url + "\n" +
                                               " Filename:     " + filename + "\n" +
                                               " File size:    " + filesizeString + "\n" +
                                               " Downloaded:   " + BytesToMiBGiBTiB.normalize(downloaded.get(), 3) + "(" + BytesToMiBGiBTiB.normalize(longAverage.averageLong(), 0) + "/s)" + "\n" +
                                               " Progress:     " + ((filesize != -1) ? String.format("%.2f%%", progressValue) : "0.00%")));
                    previousTime = System.nanoTime();
                    previousDownloaded = downloaded.get();
                }
            }
            catch (InterruptedException | InvocationTargetException e)
            {
                e.printStackTrace();
            }


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

        try
        {
            SwingUtilities.invokeAndWait(jFrame::dispose);
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        System.out.println("Ended DownloaderGUI...");
    }
}

/*
    A class that invoke a DownloaderGUI and invoke several threads to download from ArrayList of HttpUrlConnection
 */
public class DownloaderThread
{
    private String url;
    private String filename;
    private long filesize;
    private List<HttpURLConnection> links;

    public DownloaderThread(String url, String filename, long filesize, List<HttpURLConnection> links)
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

        // Downloading or joining
        AtomicBoolean isJoining = new AtomicBoolean(false);

        final DownloaderBackendThread[] downloaderThreads = new DownloaderBackendThread[links.size()];
        for (int i = 0, size = links.size(); i < size; i++)
        {
            downloaderThreads[i] = new DownloaderBackendThread(filePartsName.get(i), links.get(i), downloaded, hasCompleted);
            downloaderThreads[i].start();
        }

        DownloaderGUI downloaderGui = new DownloaderGUI(filename, filesize, url, downloaded, downloaderThreads,
                                                        hasCompleted, hasJoined, hasCancelled, isJoining);
        downloaderGui.start();

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
                    isJoining.set(true);
                    downloaded.set(0);
                    CombineFiles.combine(filePartsName, filename, true, downloaded);
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
            downloaderGui.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}

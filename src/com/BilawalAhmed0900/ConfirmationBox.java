package com.BilawalAhmed0900;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class ConfirmationBox
{
    private String url;
    private String fileName;
    private long contentLength;
    private final Object lock = new Object();

    private static final String DOWNLOAD_DIRECTORY = System.getProperty("user.home") + File.separator + "Downloads";

    public ConfirmationBox(String url, String fileName, long contentLength, boolean isVideo, boolean isAudio, boolean isExecutable)
    {
        this.url = url;
        if (isVideo)
        {
            this.fileName = DOWNLOAD_DIRECTORY + File.separator + "Video" + File.separator + fileName;
        }
        else if (isAudio)
        {
            this.fileName = DOWNLOAD_DIRECTORY + File.separator + "Music" + File.separator + fileName;
        }
        else if (isExecutable)
        {
            this.fileName = DOWNLOAD_DIRECTORY + File.separator + "Programs" + File.separator + fileName;
        }
        else
        {
            this.fileName = DOWNLOAD_DIRECTORY + File.separator + fileName;
        }

        this.contentLength = contentLength;
    }

    public ReturnStructure showConfirmationBox()
    {
        ReturnStructure returnStructure = new ReturnStructure();

        JFrame jFrame = new JFrame("Confirmation...");
        jFrame.setSize(600, 130);
        jFrame.setAlwaysOnTop(true);
        jFrame.setResizable(false);

        JPanel jPanel = new JPanel();
        jPanel.setLayout(null);


    /*
        First row of the DownloaderGUI
     */
        JLabel urlLabel = new JLabel("URL: ");
        JTextField urlField = new JTextField(55);
        urlField.setText(url);
        urlField.setEditable(false);

        urlLabel.setBounds(10, 10, 80, 15);
        urlField.setBounds(90, 8, 385, 19);

        jPanel.add(urlLabel);
        jPanel.add(urlField);


    /*
        Second row of the DownloaderGUI
     */
        JLabel filenameLabel = new JLabel("Filename: ");
        JButton browseButton = new JButton("Browse...");
        JTextField filenameField = new JTextField(55);
        filenameField.setText(fileName);

        browseButton.addActionListener(e ->
       {
           JFileChooser jFileChooser = new JFileChooser(DOWNLOAD_DIRECTORY);
           jFileChooser.setSelectedFile(new File(filenameField.getText()));

           int result = jFileChooser.showSaveDialog(jFrame);
           if (result == JFileChooser.APPROVE_OPTION)
           {
               filenameField.setText(jFileChooser.getSelectedFile().getAbsolutePath());
           }
       });

        filenameLabel.setBounds(10, 40, 80, 15);
        filenameField.setBounds(90, 38, 385, 19);
        browseButton.setBounds(480, 36, 100, 23);

        jPanel.add(filenameLabel);
        jPanel.add(filenameField);
        jPanel.add(browseButton);


    /*
        Third row of the DownloaderGUI
     */
        JButton downloadButton = new JButton(((contentLength != -1)
                                             ? String.format("Download: %s", BytesToMiBGiBTiB.normalize(contentLength, 2))
                                             : "Download"));
        downloadButton.addActionListener(e ->
        {
            File file = new File(filenameField.getText()).getParentFile();
            if (file != null)
            {
                file.mkdirs();
                if (file.isDirectory() && file.canRead() && file.canWrite())
                {
                    jFrame.setVisible(false);

                    returnStructure.code = ReturnCode.OK;
                    returnStructure.path = filenameField.getText();
                }
                else
                {
                    JOptionPaneWithFrame.showExceptionBox("Not enough permission to read/write: "
                                                                  + filenameField.getText(),
                                                          true);

                }
            }
            else
            {
                JOptionPaneWithFrame.showExceptionBox("No parent directory for "
                                                              + filenameField.getText(),
                                                      true);
            }


        });
        if (contentLength != -1)
        {
            downloadButton.setBounds(420, 66, 160, 23);
        }
        else
        {
            downloadButton.setBounds(480, 66, 100, 23);
        }
        jPanel.add(downloadButton);

        try
        {
            SwingUtilities.invokeAndWait(() -> jPanel.setVisible(true));
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        jFrame.add(jPanel);

        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        try
        {
            SwingUtilities.invokeAndWait(() ->
             {
                 jFrame.setLocationRelativeTo(null);
                 jFrame.setVisible(true);
                 jFrame.getRootPane().setDefaultButton(downloadButton);
             });
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            e.printStackTrace();
        }



    /*
        When "X" button on top-right/left is clicked
     */
        jFrame.addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e)
            {
                synchronized (lock)
                {
                    SwingUtilities.invokeLater(() ->
                            jFrame.setVisible(false));
                }
            }
        });

    /*
        Waiting thread
     */
        Thread thread = new Thread(() ->
        {
            while (true)
            {
                synchronized (lock)
                {
                    if (!jFrame.isVisible())
                    {
                        break;
                    }
                }

                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        try
        {
            thread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        try
        {
            SwingUtilities.invokeAndWait(jFrame::dispose);
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            e.printStackTrace();
        }

        return returnStructure;
    }
}

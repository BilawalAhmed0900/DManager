package com.BilawalAhmed0900;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class ConfirmationBox
{
    private String url;
    private String fileName;
    private final Object lock = new Object();

    private static final String DOWNLOAD_DIRECTORY = System.getProperty("user.home") + File.separator + "Downloads";

    public ConfirmationBox(String url, String fileName)
    {
        this.url = url;
        this.fileName = DOWNLOAD_DIRECTORY + File.separator + fileName;
    }

    public ReturnStructure showConfirmationBox()
    {
        ReturnStructure returnStructure = new ReturnStructure();

        JFrame jFrame = new JFrame("Confirmation");
        jFrame.setSize(600, 130);
        jFrame.setAlwaysOnTop(true);
        jFrame.setResizable(false);

        JPanel jPanel = new JPanel();
        jPanel.setLayout(null);


    /*
        First row of the GUI
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
        Second row of the GUI
     */
        JLabel filenameLabel = new JLabel("Filename: ");
        JButton browseButton = new JButton("Browse...");
        JTextField filenameField = new JTextField(55);
        filenameField.setText(fileName);

        browseButton.addActionListener(e ->
                SwingUtilities.invokeLater(() ->
                {
                    JFileChooser jFileChooser = new JFileChooser(DOWNLOAD_DIRECTORY);
                    jFileChooser.setSelectedFile(new File(this.fileName));

                    int result = jFileChooser.showSaveDialog(jFrame);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        filenameField.setText(jFileChooser.getSelectedFile().getAbsolutePath());
                    }
                }));

        filenameLabel.setBounds(10, 40, 80, 15);
        filenameField.setBounds(90, 38, 385, 19);
        browseButton.setBounds(480, 36, 100, 23);

        jPanel.add(filenameLabel);
        jPanel.add(filenameField);
        jPanel.add(browseButton);


    /*
        Third row of the GUI
     */
        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e ->
        {
            synchronized (lock)
            {
                SwingUtilities.invokeLater(() ->
                        jFrame.setVisible(false));
            }

            returnStructure.code = ReturnCode.OK;
            returnStructure.path = filenameField.getText();
        });
        downloadButton.setBounds(480, 66, 100, 23);
        jPanel.add(downloadButton);

        jPanel.setVisible(true);
        jFrame.add(jPanel);

        jFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        jFrame.setVisible(true);

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

        jFrame.dispose();
        return returnStructure;
    }
}

package com.BilawalAhmed0900;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/*
    A class to show a "MessageBox" without interrupting the flow of program
    Message can be changed
    Box can be disposed-of on need
 */
public class WaitingBox
{
    private JFrame jFrame;
    private JLabel jMessageLabel;

    public WaitingBox(String title, String message, int width, int height)
    {
        jFrame = new JFrame(title);
        jFrame.setSize(width, height);
        jFrame.setResizable(false);

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridLayout(1, 1));

        jMessageLabel = new JLabel(message);
        jMessageLabel.setHorizontalAlignment(JLabel.CENTER);
        jMessageLabel.setVerticalAlignment(JLabel.CENTER);
        jPanel.add(jMessageLabel);

        jFrame.add(jPanel);

        jFrame.addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e)
            {
                jFrame.dispose();
                jFrame = null;
            }
        });

        jFrame.pack();
        jFrame.setLocationRelativeTo(null);

        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setVisible(true);
    }

    public synchronized void setNewMessage(String message)
    {
        if (jFrame != null)
        {
            SwingUtilities.invokeLater(() ->
                    jMessageLabel.setText(message));
        }
    }

    public synchronized void dispose()
    {
        if (jFrame != null)
        {
            jFrame.dispose();
        }
    }

    public synchronized void waitForClosing()
    {
        while (jFrame != null)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}

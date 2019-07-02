package com.BilawalAhmed0900;

import javax.swing.*;

public class JOptionPaneWithFrame
{
    /*
        https://stackoverflow.com/questions/542844/how-can-i-make-joptionpane-dialogs-show-up-as-a-task-on-the-taskbar
     */
    public static void showMessageDialog(Object message, String title, int messageType, Icon icon)
    {
        JFrame jFrame = new JFrame(title);
        jFrame.setUndecorated( true );
        jFrame.setVisible( true );
        jFrame.setLocationRelativeTo( null );

        JOptionPane.showMessageDialog(jFrame, message, title, messageType, icon);

        jFrame.dispose();
    }

    public static int showOptionDialog(Object message, String title, int optionType, int messageType,
                                       Icon icon, Object[] options, Object initialValue)
    {
        JFrame jFrame = new JFrame(title);
        jFrame.setUndecorated( true );
        jFrame.setVisible( true );
        jFrame.setLocationRelativeTo( null );

        int result = JOptionPane.showOptionDialog(jFrame, message, title,
                                                  optionType, messageType, icon,
                                                  options, initialValue);

        jFrame.dispose();
        return result;
    }

    public static void showExceptionBox(String exceptionMessage)
    {
        showMessageDialog(exceptionMessage, "Exception...",
                          JOptionPane.ERROR_MESSAGE, null);
    }
}

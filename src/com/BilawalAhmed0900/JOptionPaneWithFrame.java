package com.BilawalAhmed0900;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class JOptionPaneWithFrame
{
    /*
        https://stackoverflow.com/questions/542844/how-can-i-make-joptionpane-dialogs-show-up-as-a-task-on-the-taskbar
     */
    public static void showMessageDialog(Object message, String title, int messageType, Icon icon, boolean setOnTop)
    {
        JFrame jFrame = new JFrame(title);
        jFrame.setUndecorated( true );
        jFrame.setVisible( true );
        jFrame.setLocationRelativeTo( null );
        jFrame.setAlwaysOnTop(setOnTop);

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

    public static void showExceptionBox(String exceptionMessage, boolean setOnTop)
    {
        showMessageDialog(exceptionMessage, "Exception...",
                          JOptionPane.ERROR_MESSAGE, null, setOnTop);
    }

    public static void showExceptionBox(Exception e, boolean setOnTop)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);

        e.printStackTrace(printWriter);

        String message = stringWriter.getBuffer().toString();
        showExceptionBox(message, setOnTop);

        printWriter.close();
    }
}

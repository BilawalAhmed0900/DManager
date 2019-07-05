package com.BilawalAhmed0900;

import java.awt.*;

public class GetTaskBar
{
    public static Taskbar getTaskbar()
    {
        try
        {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.PROGRESS_VALUE_WINDOW))
                return taskbar;
        }
        catch (UnsupportedOperationException ignored)
        {
        }

        return null;
    }
}

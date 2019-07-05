package com.BilawalAhmed0900;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class BrowseToFile
{
    public static void browseTo(File file)
            throws IOException
    {
        try
        {
            Desktop.getDesktop().browseFileDirectory(file);
        }
        catch (UnsupportedOperationException e)
        {
            if (System.getProperty("os.name").toLowerCase().contains("win"))
            {
                Runtime.getRuntime().exec("explorer.exe /select,\"" + file.getAbsolutePath() + "\"");
            }
            else if (System.getProperty("os.name").toLowerCase().contains("mac"))
            {
                // Not Tested
                Runtime.getRuntime().exec("open -R \"" + file.getAbsolutePath() + "\"");
            }
            else
            {
                Desktop.getDesktop().open(file.getParentFile());
            }

        }
    }

    public static void browseTo(String filename)
            throws IOException
    {
        File file = new File(filename);
        if (!file.exists() && !file.isDirectory() && file.getParentFile() != null)
        {
            browseTo(file.getParentFile());
        }
        else
        {
            browseTo(file);
        }
    }
}

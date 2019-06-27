package com.BilawalAhmed0900;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CombineFiles
{
    public static final int BUFFER_SIZE = 64 * 1024 * 1024;

    public static void combine(List<String> partsName, String parentName, boolean deleteParts)
            throws IOException
    {
        FileOutputStream fileOutputStream = new FileOutputStream(parentName);
        byte[] buffer = new byte[BUFFER_SIZE];
        for (int i = 0, size = partsName.size(); i < size; i++)
        {
            FileInputStream fileInputStream = new FileInputStream(partsName.get(i));
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

            if (deleteParts) new File(partsName.get(i)).delete();
        }
        fileOutputStream.close();
    }

    public static void combine(String[] partsName, String parentName, boolean deleteParts)
            throws IOException
    {
        combine(Arrays.asList(partsName), parentName, deleteParts);
    }
}

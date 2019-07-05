package com.BilawalAhmed0900;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CombineFiles
{
    public static final int BUFFER_SIZE = 64 * 1024 * 1024;

    public static void combine(List<String> partsName, String parentName, boolean deleteParts, AtomicLong joinedLength)
            throws IOException, SecurityException
    {
        FileOutputStream fileOutputStream = new FileOutputStream(parentName);
        FileInputStream[] fileInputStreamList = new FileInputStream[partsName.size()];

        for (int i = 0, size = partsName.size(); i < size; i++)
        {
            try
            {
                fileInputStreamList[i] = new FileInputStream(partsName.get(i));
            }
            catch (IOException e)
            {
                for (int j = 0; j < i; j++)
                {
                    fileInputStreamList[j].close();
                }

                throw e;
            }
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        for (int i = 0, size = partsName.size(); i < size; i++)
        {
            while (true)
            {
                int read = fileInputStreamList[i].read(buffer);
                if (read == -1)
                {
                    break;
                }

                if (joinedLength != null)
                {
                    joinedLength.addAndGet(read);
                }

                fileOutputStream.write(buffer, 0, read);
            }

            fileInputStreamList[i].close();
            if (deleteParts) new File(partsName.get(i)).delete();
        }

        fileOutputStream.close();
    }

    public static void combine(String[] partsName, String parentName, boolean deleteParts, AtomicLong joinedLength)
            throws IOException
    {
        /*
            This does slow things...
         */
        combine(Arrays.asList(partsName), parentName, deleteParts, joinedLength);
    }
}

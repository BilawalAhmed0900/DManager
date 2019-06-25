package com.BilawalAhmed0900;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloaderThread extends Thread
{
    private String cookieString;
    private Map<String, String> map;
    private final int MAX_CONNECTION = 8;

    public DownloaderThread(String cookieString)
    {
        this.cookieString = cookieString;
        map = new HashMap<>(8);
    }

    private List<HttpURLConnection> openMainConnection(String url, String cookieString)
    {
        List<HttpURLConnection> result = Collections.synchronizedList(new ArrayList<>(MAX_CONNECTION));
        try
        {
            URL url1 = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url1.openConnection();
            httpURLConnection.addRequestProperty("Cookie", cookieString);
            httpURLConnection.setReadTimeout(60 * 1000);
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                result.add(httpURLConnection);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private String getFileName(HttpURLConnection urlConnection)
    {
        String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
        if (contentDisposition != null)
        {
            Pattern pattern = Pattern.compile("filename=\"(.*?)\"");
            Matcher matcher = pattern.matcher(contentDisposition);
            if (matcher.find())
            {
                return matcher.group(1);
            }
        }

        String url = urlConnection.getURL().toString();
        int position = url.lastIndexOf("/");
        String fileName = url.substring(position + 1);
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        return fileName;
    }

    private List<HttpURLConnection> openLinks(String url, String cookieString)
    {
        List<HttpURLConnection> result = Collections.synchronizedList(new ArrayList<>(MAX_CONNECTION));
        for (int i = 0; i < MAX_CONNECTION; i++)
        {
            try
            {
                URL url1 = new URL(url);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url1.openConnection();
                httpURLConnection.addRequestProperty("Cookie", cookieString);
                httpURLConnection.setReadTimeout(60 * 1000);
                httpURLConnection.connect();

                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    result.add(httpURLConnection);
                    if (httpURLConnection.getContentLength() == -1)
                    {
                        break;
                    }
                }
            }
            catch (IOException e)
            {
                // e.printStackTrace();
            }

        }

        return result;
    }

    private List<HttpURLConnection> openLinksAndSeek(String url, String cookieString, int contentLength, int totalLinks)
    {
        int chunkSize = contentLength / totalLinks;
        List<HttpURLConnection> result = Collections.synchronizedList(new ArrayList<>(totalLinks));
        for (int i = 0; i < totalLinks; i++)
        {
            try
            {
                URL url1 = new URL(url);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url1.openConnection();

                String rangeString;
                if (i == totalLinks - 1)
                {
                    rangeString = String.format("%d-%d", chunkSize * i, contentLength);
                }
                else
                {
                    rangeString = String.format("%d-%d", chunkSize * i, (chunkSize * (i + 1)) - 1);
                }

                httpURLConnection.addRequestProperty("Range", "bytes=" + rangeString);
                httpURLConnection.addRequestProperty("Cookie", cookieString);
                httpURLConnection.setReadTimeout(60 * 1000);
                httpURLConnection.connect();

                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL)
                {
                    result.add(httpURLConnection);
                }
                else
                {
                    for (HttpURLConnection urlConnection: result)
                    {
                        urlConnection.disconnect();
                    }

                    return null;
                }
            }
            catch (IOException e)
            {
                for (HttpURLConnection httpURLConnection: result)
                {
                    httpURLConnection.disconnect();
                }

                return null;
            }

        }

        return result;
    }

    @Override
    public void run()
    {
        String[] cookieStringParted = cookieString.substring(1, cookieString.length() - 1).split(",");
        for (String part: cookieStringParted)
        {
            Pattern pattern = Pattern.compile("\"(.*)\":\"*(.*)?\"*");
            Matcher matcher = pattern.matcher(part);
            if (matcher.find())
            {
                if (matcher.group(2).endsWith("\""))
                {
                    map.put(matcher.group(1), matcher.group(2).substring(0, matcher.group(2).length() - 1));
                }
                else
                {
                    map.put(matcher.group(1), matcher.group(2));
                }

            }
        }

        System.out.println(map);
        String finalURL = (map.get("finalUrl").equals("")) ? map.get("url") : map.get("finalUrl");
        List<HttpURLConnection> arrayList = openMainConnection(finalURL, map.get("cookies"));
        if (arrayList.size() != 1)
        {
            return;
        }

        int totalContentLength = arrayList.get(0).getContentLength();
        String fileName = getFileName(arrayList.get(0));

        ReturnStructure returnStructure = (new ConfirmationBox(arrayList.get(0).getURL().toString(), fileName)).showConfirmationBox();
        if (returnStructure.code == ReturnCode.CANCELLED || returnStructure.path.equals(""))
        {
            arrayList.get(0).disconnect();
            arrayList.clear();
            map.clear();
            return;
        }

        if (totalContentLength != -1
                && arrayList.get(0).getHeaderField("Accept-Ranges") != null
                && arrayList.get(0).getHeaderField("Accept-Ranges").equals("bytes"))
        {
            WaitingBox waitingBox = new WaitingBox("Connecting to " + arrayList.get(0).getURL().toString(),
                    "Establishing connection to the server", 300, 100);
            List<HttpURLConnection> seekAbleList = openLinks(finalURL, map.get("cookies"));
            if (seekAbleList.size() != 0)
            {
                int contentLength = seekAbleList.get(0).getContentLength();
                int totalListLength = seekAbleList.size();

                for (HttpURLConnection httpURLConnection: seekAbleList)
                {
                    httpURLConnection.disconnect();
                }
                seekAbleList.clear();

                waitingBox.setNewMessage("Seeking on the server");
                seekAbleList = openLinksAndSeek(finalURL, map.get("cookies"), contentLength, totalListLength);

                for (HttpURLConnection httpURLConnection: arrayList)
                {
                    httpURLConnection.disconnect();
                }
                arrayList.clear();
                arrayList = seekAbleList;
                waitingBox.dispose();
            }
        }

        assert arrayList != null;
        System.out.println("Started downloading...");
        DownloaderGUI downloaderGUI = new DownloaderGUI(arrayList.get(0).getURL().toString(), returnStructure.path, totalContentLength, arrayList);
        downloaderGUI.download();
        System.out.println("Completed downloading...");

        for (HttpURLConnection httpURLConnection: arrayList)
        {
            httpURLConnection.disconnect();
        }
        arrayList.clear();
        map.clear();
    }
}

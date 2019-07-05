package com.BilawalAhmed0900;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    A Thread class which get the string read from WebSocket, sent from extension
    Parse it and open several HttpUrlConnections to its url
 */
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

    /*
        The very first connection to open. Other may even not open depending upon how many server can give us
     */
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
            return null;
        }

        return result;
    }

    /*
        Try from Content-Disposition
        then from url itself, after last "/" and before first "&" or "?" after it
     */
    private String getFileName(HttpURLConnection urlConnection)
    {
        String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
        if (contentDisposition != null)
        {
            Pattern pattern = Pattern.compile("filename=\"(.*?)\"");
            Matcher matcher = pattern.matcher(contentDisposition);
            if (matcher.find())
            {
                /*
                    Some server sends name like this

                    Example.server/File.ext
                 */
                String fileName = matcher.group(1);
                int position = fileName.lastIndexOf("/");
                if (position == -1) return fileName;

                return fileName.substring(position + 1);
            }
        }

        String url = urlConnection.getURL().toString();
        int position = url.lastIndexOf("/");

        int positionAnd = url.indexOf("&", position);
        int positionQuestionMark = url.indexOf("?", position);

        int tillPosition = Math.min(positionAnd, positionQuestionMark);
        if (tillPosition == -1 || tillPosition <= position)
            tillPosition = url.length();

        String fileName = url.substring(position + 1, tillPosition);
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        return fileName;
    }

    /*
        Try to open as many HttpUrlConnection to a Url as possible, up to 8
     */
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
                    if (httpURLConnection.getContentLengthLong() == -1)
                    {
                        break;
                    }
                }
                else
                {
                    break;
                }
            }
            catch (IOException e)
            {
                break;
                // e.printStackTrace();
            }

        }

        return result;
    }

    /*
        Open `totalLinks` seeked `Range=` HttpUrlConnection on a Url
     */
    private List<HttpURLConnection> openLinksAndSeek(String url, String cookieString, long contentLength, int totalLinks)
    {
        long chunkSize = contentLength / totalLinks;
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
        /*
            A dictionary is read
            Remove first { and last } then
            Split by ,
         */
        String[] cookieStringParted = cookieString.substring(1, cookieString.length() - 1).split(",");
        for (String part: cookieStringParted)
        {
            /*
                "filename":"something"

                Last \" is absorbed it (.*), so check for it is endsWith("\"")
             */
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

        /*
            Firefox sends redirected Url in url while chrome in finalUrl
         */
        String finalURL = (map.get("finalUrl").equals("")) ? map.get("url") : map.get("finalUrl");
        List<HttpURLConnection> arrayList = openMainConnection(finalURL, map.get("cookies"));
        System.out.println(arrayList.get(0).getHeaderFields());
        if (arrayList == null)
        {
            JOptionPane.showMessageDialog(null,
                                          "<html>Error while connection to<br>\"" + finalURL + "\"</html>", "Error",
                                          JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        /*
            May not be necessary, but connection may not open even while not throwing an exception
         */
        if (arrayList.size() != 1)
        {
            return;
        }

        long totalContentLength = arrayList.get(0).getContentLengthLong();
        String fileName = getFileName(arrayList.get(0));

        ReturnStructure returnStructure = (new ConfirmationBox(arrayList.get(0).getURL().toString(), fileName,
                                                               (totalContentLength == -1) ? Long.parseLong(map.get("fileSize")) : totalContentLength,
                                                               map.get("isVideo").equals("true"),
                                                               map.get("isAudio").equals("true"))).showConfirmationBox();
        if (returnStructure.code == ReturnCode.CANCELLED || returnStructure.path.equals(""))
        {
            /*
                Cancelled pressed by user
             */
            arrayList.get(0).disconnect();
            arrayList.clear();
            map.clear();
            return;
        }

        File parentDirs = new File(returnStructure.path).getParentFile();
        if (parentDirs != null) parentDirs.mkdirs();

        /*
            totalContentLength == -1 for unknown size
            Even if filesize is less than 1MB, only use first connection

            Only opens more connection if Accept-Ranges is sent by server and it is equal to "bytes"
            Some server sends "Start" instead
         */
        if (totalContentLength != -1
                && totalContentLength > 1024 * 1024
                && arrayList.get(0).getHeaderField("Accept-Ranges") != null
                && arrayList.get(0).getHeaderField("Accept-Ranges").equals("bytes"))
        {
            WaitingBox waitingBox = new WaitingBox("Connecting to " + arrayList.get(0).getURL().toString(),
                    "Establishing connection to the server", 500, 100);
            List<HttpURLConnection> seekAbleList = openLinks(finalURL, map.get("cookies"));
            if (seekAbleList.size() != 0)
            {
                long contentLength = seekAbleList.get(0).getContentLengthLong();
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

        /*
            Even if server sends -1, browser sometime knows the size.
            But we have to make links according to what server sent
            but GUI can be made according to known size
         */
        if (totalContentLength == -1)
            totalContentLength = Long.parseLong(map.get("fileSize"));

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

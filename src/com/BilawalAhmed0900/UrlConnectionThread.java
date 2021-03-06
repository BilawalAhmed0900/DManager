package com.BilawalAhmed0900;

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
public class UrlConnectionThread extends Thread
{
    private String cookieString;
    private Map<String, String> map;
    private final int MAX_CONNECTION = 8;

    public UrlConnectionThread(String cookieString)
    {
        this.cookieString = cookieString;
        map = new HashMap<>(9);
    }

    /*
        The very first connection to open. Other may even not open depending upon how many server can give us
     */
    private List<HttpURLConnection> openMainConnection(String url, String cookieString, String userAgent)
    {
        final int MAX_URL_LENGTH_TO_SHOW = 70;
        List<HttpURLConnection> result = Collections.synchronizedList(new ArrayList<>(MAX_CONNECTION));
        try
        {
            URL url1 = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url1.openConnection();
            httpURLConnection.addRequestProperty("Cookie", cookieString);
            httpURLConnection.setRequestProperty("User-Agent", userAgent);
            httpURLConnection.setReadTimeout(60 * 1000);
            httpURLConnection.connect();

            int resultCode = httpURLConnection.getResponseCode();
            if (resultCode == HttpURLConnection.HTTP_OK)
            {
                result.add(httpURLConnection);
            }
            else
            {
                JOptionPaneWithFrame.showExceptionBox("<html>HTTP code: " + resultCode +
                                                              "<br>Connection to (\"" + StringUtils.abbreviate(url, MAX_URL_LENGTH_TO_SHOW) + "\") failed</html>",
                                                      true);
                return null;
            }
        }
        catch (IOException e)
        {
            JOptionPaneWithFrame.showExceptionBox(e, true);
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
        boolean isVideoOrAudio = false;
        String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
        String contentType = urlConnection.getHeaderField("Content-Type");
        if (contentType.contains("video/") || contentType.contains("audio/"))
            isVideoOrAudio = true;

        /*
            First check for Content-Disposition

            Its syntax is either

            attachment; filename=""
                or
            attachment; filename=
         */
        if (contentDisposition != null && contentDisposition.startsWith("attachment;"))
        {
            Pattern pattern = Pattern.compile("filename=\"(.*?)\"");
            Matcher matcher = pattern.matcher(contentDisposition);
            String extension = isVideoOrAudio ? "." + contentType.substring(6) : "";
            if (matcher.find())
            {
                /*
                    Some server sends name like this

                    Example.server/File.ext
                 */
                String fileName = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                int position = fileName.lastIndexOf("/");
                if (position == -1) return fileName + extension;

                return fileName.substring(position + 1) + extension;
            }

            pattern = Pattern.compile("filename=(.*)");
            matcher = pattern.matcher(contentDisposition);
            if (matcher.find())
            {
                String fileName = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                int position = fileName.lastIndexOf("/");
                if (position == -1) return fileName + extension;

                return fileName.substring(position + 1) + extension;
            }

        }

        /*
            Otherwise get it from URL link
            while cutting of any
            FILENAME?arg=...&arg2=...
         */
        String url = urlConnection.getURL().toString();
        int position = url.lastIndexOf("/");

        int tillPosition = url.indexOf("?", position);
        if (tillPosition == -1 || tillPosition <= position)
            tillPosition = url.length();

        String fileName = url.substring(position + 1, tillPosition);
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        return fileName;
    }

    /*
        Try to open as many HttpUrlConnection to a Url as possible, up to 8
     */
    private List<HttpURLConnection> openLinks(String url, String cookieString, String userAgent)
    {
        List<HttpURLConnection> result = Collections.synchronizedList(new ArrayList<>(MAX_CONNECTION));
        for (int i = 0; i < MAX_CONNECTION; i++)
        {
            try
            {
                URL url1 = new URL(url);

                HttpURLConnection httpURLConnection = (HttpURLConnection)url1.openConnection();
                httpURLConnection.setRequestProperty("User-Agent", userAgent);
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
        Open `totalLinks` seek-ed `Range=` HttpUrlConnection on a Url
     */
    private List<HttpURLConnection> openLinksAndSeek(String url, String cookieString, String userAgent,
                                                     long contentLength, int totalLinks)
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

                httpURLConnection.setRequestProperty("User-Agent", userAgent);
                httpURLConnection.addRequestProperty("Range", "bytes=" + rangeString);
                httpURLConnection.addRequestProperty("Cookie", cookieString);
                httpURLConnection.setReadTimeout(60 * 1000);
                httpURLConnection.connect();

                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL)
                {
                    result.add(httpURLConnection);
                }
                else if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    for (HttpURLConnection httpURLConnection1: result)
                    {
                        httpURLConnection1.disconnect();
                    }
                    result.clear();
                    result.add(httpURLConnection);
                    return result;
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
                    map.put(matcher.group(1), matcher.group(2).substring(0, matcher.group(2).length() - 1)
                            .replace("__COMMA__", ","));
                }
                else
                {
                    map.put(matcher.group(1), matcher.group(2).replace("__COMMA__", ","));
                }

            }
        }

        System.out.println(map);

        /*
            Firefox sends redirected Url in url while chrome in finalUrl
         */
        String finalURL = (map.get("finalUrl").equals("")) ? map.get("url") : map.get("finalUrl");
        List<HttpURLConnection> arrayList = openMainConnection(finalURL, map.get("cookies"), map.get("userAgent"));
        if (arrayList == null)
        {
            return;
        }

        long totalContentLength = arrayList.get(0).getContentLengthLong();
        String fileName = getFileName(arrayList.get(0));
        System.out.println(fileName);
        boolean isAudio = true;
        if (map.get("isAudio").equals("false"))
        {
            isAudio = IsAudio.isAudio(fileName);
        }

        boolean isVideo = true;
        if (map.get("isVideo").equals("false"))
        {
            isVideo = IsVideo.isVideo(fileName);
        }

        boolean isExecutable = IsExecutable.isExecutable(fileName);

        if ((isAudio && isVideo) || (isVideo && isExecutable) || (isAudio && isExecutable))
        {
            JOptionPaneWithFrame.showExceptionBox("File can only be of one type; audio, video or executable",
                                                  true);
            return;
        }

        System.out.println("isAudio = " + isAudio);
        System.out.println("isVideo = " + isVideo);
        System.out.println("isExecutable = " + isExecutable);
        System.out.println(arrayList.get(0).getHeaderFields());
        ReturnStructure returnStructure = (new ConfirmationBox(arrayList.get(0).getURL().toString(), fileName,
                                                               totalContentLength,
                                                               map.get("isVideo").equals("true") || isVideo,
                                                               map.get("isAudio").equals("true") || isAudio,
                                                                isExecutable)).showConfirmationBox();
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

        /*
            totalContentLength == -1 for unknown size
            Even if fileSize is less than 1MB, only use first connection

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
            List<HttpURLConnection> seekAbleList = openLinks(finalURL, map.get("cookies"), map.get("userAgent"));
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
                seekAbleList = openLinksAndSeek(finalURL, map.get("cookies"), map.get("userAgent"),
                                                contentLength, totalListLength);

                if (seekAbleList != null)
                {
                    for (HttpURLConnection httpURLConnection: arrayList)
                    {
                        httpURLConnection.disconnect();
                    }
                    arrayList.clear();
                    arrayList = seekAbleList;
                }
                else
                {
                    for (int i = 1, size = arrayList.size(); i < size; i++)
                    {
                        arrayList.get(i).disconnect();
                    }
                    arrayList = arrayList.subList(0, 1);
                }
            }
            waitingBox.dispose();
        }

        System.out.println("Started downloading...");
        DownloaderThread downloaderThread = new DownloaderThread(arrayList.get(0).getURL().toString(), returnStructure.path, totalContentLength, arrayList);
        downloaderThread.download();
        System.out.println("Completed downloading...");

        for (HttpURLConnection httpURLConnection: arrayList)
        {
            httpURLConnection.disconnect();
        }
        arrayList.clear();
        map.clear();
    }
}

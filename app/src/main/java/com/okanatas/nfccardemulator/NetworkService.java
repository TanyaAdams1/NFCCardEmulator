package com.okanatas.nfccardemulator;

import android.util.Log;

import java.net.Socket;

public class NetworkService {
    private static final String host = "localhost";
    private static final int port = 12345;
    private Socket socket_;
    private boolean isNetworkServiceRunning = false;
    public NetworkService() {
    }

    public boolean connect() {
        if (!isNetworkServiceRunning) {
            try {
                new Thread(()->{
                    try {
                        socket_ = new Socket(host, port);
                        isNetworkServiceRunning = true;
                    } catch (Exception e) {
                        Log.e("NetworkService", "Error while creating the socket: " + e.getMessage());
                        Log.e("NetworkService", "Stack trace: " + Log.getStackTraceString(e));
                    }
                }).start();
            } catch (Exception e) {
                Log.e("NetworkService", "Error while creating the socket: " + e.getMessage());
                Log.e("NetworkService", "Stack trace: " + Log.getStackTraceString(e));
            }
        }
        return isNetworkServiceRunning;
    }

    public boolean disconnect() {
        if (isNetworkServiceRunning) {
            try {
                new Thread(()->{
                    try {
                        socket_.close();
                        isNetworkServiceRunning = false;
                    } catch (Exception e) {
                        Log.e("NetworkService", "Error while closing the socket: " + e.getMessage());
                        Log.e("NetworkService", "Stack trace: " + Log.getStackTraceString(e));
                    }
                }).start();
            } catch (Exception e) {
                Log.e("NetworkService", "Error while closing the socket: " + e.getMessage());
                Log.e("NetworkService", "Stack trace: " + Log.getStackTraceString(e));
            }
        }
        return !isNetworkServiceRunning;
    }

    /* destruct function */
    protected void finalize() {
        if (isNetworkServiceRunning) {
            try {
                new Thread(()->{
                    try {
                        socket_.close();
                        isNetworkServiceRunning = false;
                    } catch (Exception e) {
                        Log.e("NetworkService", "Error while closing the socket: " + e.getMessage());
                        Log.e("NetworkService", "Stack trace: " + Log.getStackTraceString(e));
                    }
                }).start();
            } catch (Exception e) {
                Log.e("NetworkService", "Error while closing the socket: " + e.getMessage());
                Log.e("NetworkService", "Stack trace: " + Log.getStackTraceString(e));
            }
        }
    }

    public boolean isNetworkServiceRunning() {
        return isNetworkServiceRunning;
    }

    public boolean sendCommand(byte[] command) {
        if (!isNetworkServiceRunning) {
            return false;
        }
        try {
            // send the data length first
            socket_.getOutputStream().write(command.length);
            socket_.getOutputStream().write(command);
            Log.d("NetworkService", "Command: " + Utils.toHexString(command));
            return true;
        } catch (Exception e) {
            Log.e("NetworkService", "Error while sending the command: " + e.getMessage());
            return false;
        }
    }

    /**
     * This method was created to wait for the response from the server. It will block until the full
     * response with the specified length is received.
     *
     * @return byte array of the response.
     */
    public byte[] waitForResponse() {
        if (!isNetworkServiceRunning) {
            return null;
        }
        int len;
        // The first byte of the response is the length of the response.
        try {
            len = socket_.getInputStream().read();
        } catch (Exception e) {
            Log.e("NetworkService", "Error while reading the length of the response: " + e.getMessage());
            return null;
        }
        byte[] response = new byte[len];
        int read = 0;
        try {
            while (read < len) {
                read += socket_.getInputStream().read(response, read, len - read);
            }
            Log.d("NetworkService", "Response: " + Utils.toHexString(response));
            return response;
        } catch (Exception e) {
            Log.e("NetworkService", "Error while receiving the response: " + e.getMessage());
            return null;
        }
    }
}
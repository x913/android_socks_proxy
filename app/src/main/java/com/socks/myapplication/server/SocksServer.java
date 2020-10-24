package com.socks.myapplication.server;

import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class SocksServer {

    protected int port;
    protected boolean stopping = false;

    public int getPort() {
        return port;
    }

    public synchronized void start(int listenPort) {
        this.stopping = false;
        this.port = listenPort;
        new Thread(new ServerProcess()).start();
    }

    public synchronized void stop() {
        stopping = true;
    }

    private class ServerProcess implements Runnable {

        @Override
        public void run() {
            Log.d("AAA", "SOCKS server started...");
            try {
                handleClients(port);
                Log.d("AAA", "SOCKS server stopped...");
            } catch (IOException e) {
                Log.d("AAA", "SOCKS server crashed..." + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        protected void handleClients(int port) throws IOException {
            // todo changed
            //final ServerSocket listenSocket = new ServerSocket(port);

            // try without port
            final ServerSocket listenSocket = new ServerSocket();
            listenSocket.bind(new InetSocketAddress("10.5.0.6", 8888));

            listenSocket.setSoTimeout(SocksConstants.LISTEN_TIMEOUT);
            SocksServer.this.port = listenSocket.getLocalPort();
            Log.d("AAA", "SOCKS server listening at port: " + listenSocket.getLocalPort() + " " + listenSocket.getInetAddress());

            while (true) {
                synchronized (SocksServer.this) {
                    if (stopping) {
                        break;
                    }
                }
                handleNextClient(listenSocket);
            }

            try {
                listenSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }

        private void handleNextClient(ServerSocket listenSocket) {
            try {
                final Socket clientSocket = listenSocket.accept();
                clientSocket.setSoTimeout(SocksConstants.DEFAULT_SERVER_TIMEOUT);
                Log.d("AAA", "Connection from : " + Utils.getSocketInfo(clientSocket));
                new Thread(new ProxyHandler(clientSocket)).start();
            } catch (InterruptedIOException e) {
                //	This exception is thrown when accept timeout is expired
            } catch (Exception e) {
                Log.d("AAA", e.getMessage());
            }
        }
    }

}

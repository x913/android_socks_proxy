package com.socks.myapplication.server;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import static com.socks.myapplication.server.Utils.getSocketInfo;

public class ProxyHandler implements Runnable {


    private InputStream m_ClientInput = null;
    private OutputStream m_ClientOutput = null;
    private InputStream m_ServerInput = null;
    private OutputStream m_ServerOutput = null;
    private Object m_lock;
    private Socks4Impl comm = null;

    Socket m_ClientSocket;
    Socket m_ServerSocket = null;
    byte[] m_Buffer = new byte[SocksConstants.DEFAULT_BUF_SIZE];

    public ProxyHandler(Socket clientSocket) {
        m_lock = this;
        m_ClientSocket = clientSocket;
        try {
            m_ClientSocket.setSoTimeout(SocksConstants.DEFAULT_PROXY_TIMEOUT);
        } catch (SocketException e) {
            Log.d("AAA", "Socket Exception during seting Timeout.");
        }
        Log.d("AAA", "Proxy Created.");
    }

    public void setLock(Object lock) {
        this.m_lock = lock;
    }

    public void run() {
        Log.d("AAA", "Proxy Started.");
        setLock(this);

        if (prepareClient()) {
            processRelay();
            close();
        } else {
            Log.d("AAA", "Proxy - client socket is null !");
        }
    }

    public void close() {
        try {
            if (m_ClientOutput != null) {
                m_ClientOutput.flush();
                m_ClientOutput.close();
            }
        } catch (IOException e) {
            // ignore
        }
        try {
            if (m_ServerOutput != null) {
                m_ServerOutput.flush();
                m_ServerOutput.close();
            }
        } catch (IOException e) {
            // ignore
        }

        try {
            if (m_ClientSocket != null) {
                m_ClientSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }

        try {
            if (m_ServerSocket != null) {
                m_ServerSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }

        m_ServerSocket = null;
        m_ClientSocket = null;

        Log.d("AAA", "Proxy Closed.");
    }

    public void sendToClient(byte[] buffer) {
        sendToClient(buffer, buffer.length);
    }

    public void sendToClient(byte[] buffer, int len) {
        if (m_ClientOutput != null && len > 0 && len <= buffer.length) {
            try {
                m_ClientOutput.write(buffer, 0, len);
                m_ClientOutput.flush();
            } catch (IOException e) {
                Log.d("AAA", "Sending data to client");
            }
        }
    }

    public void sendToServer(byte[] buffer, int len) {
        if (m_ServerOutput != null && len > 0 && len <= buffer.length) {
            try {
                m_ServerOutput.write(buffer, 0, len);
                m_ServerOutput.flush();
            } catch (IOException e) {
                Log.d("AAA", "Sending data to server");
            }
        }
    }

    public boolean isActive() {
        return m_ClientSocket != null && m_ServerSocket != null;
    }

    public void connectToServer(String server, int port) throws IOException {

        if (server.equals("")) {
            close();
            Log.d("AAA", "Invalid Remote Host Name - Empty String !!!");
            return;
        }

        m_ServerSocket = new Socket(server, port);
        m_ServerSocket.setSoTimeout(SocksConstants.DEFAULT_PROXY_TIMEOUT);

        Log.d("AAA", "Connected to " + getSocketInfo(m_ServerSocket));
        prepareServer();
    }

    protected void prepareServer() throws IOException {
        synchronized (m_lock) {
            m_ServerInput = m_ServerSocket.getInputStream();
            m_ServerOutput = m_ServerSocket.getOutputStream();
        }
    }

    public boolean prepareClient() {
        if (m_ClientSocket == null) return false;

        try {
            m_ClientInput = m_ClientSocket.getInputStream();
            m_ClientOutput = m_ClientSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            Log.d("AAA", "Proxy - can't get I/O streams!");
            Log.d("AAA", e.getMessage(), e);
            return false;
        }
    }

    public void processRelay() {
        try {
            byte SOCKS_Version = getByteFromClient();

            switch (SOCKS_Version) {
                case SocksConstants.SOCKS4_Version:
                    comm = new Socks4Impl(this);
                    break;
                case SocksConstants.SOCKS5_Version:
                    comm = new Socks5Impl(this);
                    break;
                default:
                    Log.d("AAA", "Invalid SOKCS version : " + SOCKS_Version);
                    return;
            }
            Log.d("AAA", "Accepted SOCKS " + SOCKS_Version + " Request.");

            comm.authenticate(SOCKS_Version);
            comm.getClientCommand();

            switch (comm.socksCommand) {
                case SocksConstants.SC_CONNECT:
                    comm.connect();
                    relay();
                    break;

                case SocksConstants.SC_BIND:
                    comm.bind();
                    relay();
                    break;

                case SocksConstants.SC_UDP:
                    comm.udp();
                    break;
            }
        } catch (Exception e) {
            Log.d("AAA", e.getMessage(), e);
        }
    }

    public byte getByteFromClient() throws Exception {
        while (m_ClientSocket != null) {
            int b;
            try {
                b = m_ClientInput.read();
            } catch (InterruptedIOException e) {
                Thread.yield();
                continue;
            }
            return (byte) b; // return loaded byte
        }
        throw new Exception("Interrupted Reading GetByteFromClient()");
    }

    public void relay() {
        boolean isActive = true;

        while (isActive) {

            //---> Check for client data <---

            int dlen = checkClientData();

            if (dlen < 0) {
                isActive = false;
            }
            if (dlen > 0) {
                logData(dlen, "Cli data");
                sendToServer(m_Buffer, dlen);
            }

            //---> Check for Server data <---
            dlen = checkServerData();

            if (dlen < 0) isActive = false;
            if (dlen > 0) {
                logData(dlen, "Srv data");
                sendToClient(m_Buffer, dlen);
            }

            Thread.yield();
        }
    }

    public int checkClientData() {
        synchronized (m_lock) {
            //	The client side is not opened.
            if (m_ClientInput == null) return -1;

            int dlen;

            try {
                dlen = m_ClientInput.read(m_Buffer, 0, SocksConstants.DEFAULT_BUF_SIZE);
            } catch (InterruptedIOException e) {
                return 0;
            } catch (IOException e) {
                Log.d("AAA", "Client connection Closed!");
                close();    //	Close the server on this exception
                return -1;
            }

            if (dlen < 0) close();

            return dlen;
        }
    }

    public int checkServerData() {
        synchronized (m_lock) {
            //	The client side is not opened.
            if (m_ServerInput == null) return -1;

            int dlen;

            try {
                dlen = m_ServerInput.read(m_Buffer, 0, SocksConstants.DEFAULT_BUF_SIZE);
            } catch (InterruptedIOException e) {
                return 0;
            } catch (IOException e) {
                Log.d("AAA", "Server connection Closed!");
                close();    //	Close the server on this exception
                return -1;
            }

            if (dlen < 0) {
                close();
            }

            return dlen;
        }
    }

    private void logData(final int traffic, final String dataSource) {
        Log.d("AAA", String.format("%s : %s >> <%s/%s:%d> : %d bytes.",
                dataSource,
                getSocketInfo(m_ClientSocket),
                comm.m_ServerIP.getHostName(),
                comm.m_ServerIP.getHostAddress(),
                comm.m_nServerPort, traffic));
    }

    public int getPort() {
        return m_ServerSocket.getPort();
    }

}

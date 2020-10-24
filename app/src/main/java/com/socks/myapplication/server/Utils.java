package com.socks.myapplication.server;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Utils {
    public static InetAddress calcInetAddress(byte[] addr) {
        InetAddress IA;
        StringBuilder sIA = new StringBuilder();

        if (addr.length < 4) {
            Log.d("AAA", "calcInetAddress() - Invalid length of IP v4 - " + addr.length + " bytes");
            return null;
        }

        // IP v4 Address Type
        for (int i = 0; i < 4; i++) {
            sIA.append(byte2int(addr[i]));
            if (i < 3) sIA.append(".");
        }

        try {
            IA = InetAddress.getByName(sIA.toString());
        } catch (UnknownHostException e) {
            return null;
        }

        return IA;
    }

    public static int byte2int(byte b) {
        return (int) b < 0 ? 0x100 + (int) b : b;
    }

    public static int calcPort(byte Hi, byte Lo) {
        return ((byte2int(Hi) << 8) | byte2int(Lo));
    }

    public static String iP2Str(InetAddress IP) {
        return IP == null
                ? "NA/NA"
                : String.format("%s/%s", IP.getHostName(), IP.getHostAddress());
    }

    public static String getSocketInfo(Socket sock) {
        return sock == null
                ? "<NA/NA:0>"
                : String.format("<%s:%d>", Utils.iP2Str(sock.getInetAddress()), sock.getPort());
    }

    public static String getSocketInfo(DatagramPacket DGP) {
        return DGP == null
                ? "<NA/NA:0>"
                : String.format("<%s:%d>", Utils.iP2Str(DGP.getAddress()), DGP.getPort());
    }




}

package com.smartbear.mqttsupport;

import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

public class SSLSocketFactoryWrapper extends SSLSocketFactory {
    private final SSLSocketFactory wrappedFactory;
    private List<SNIServerName> serverNames;

    public SSLSocketFactoryWrapper(SSLSocketFactory factory, List<SNIServerName> serverNames) {
        this.wrappedFactory = factory;
        this.serverNames = serverNames;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        SSLSocket socket = (SSLSocket) wrappedFactory.createSocket(host, port);
        setParameters(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        SSLSocket socket = (SSLSocket) wrappedFactory.createSocket(host, port, localHost, localPort);
        setParameters(socket);
        return socket;
    }


    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) wrappedFactory.createSocket(host, port);
        setParameters(socket);
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) wrappedFactory.createSocket(address, port, localAddress, localPort);
        setParameters(socket);
        return socket;

    }

    @Override
    public Socket createSocket() throws IOException {
        SSLSocket socket = (SSLSocket) wrappedFactory.createSocket();
        setParameters(socket);
        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return wrappedFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return wrappedFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket socket = (SSLSocket) wrappedFactory.createSocket(s, host, port, autoClose);
        setParameters(socket);
        return socket;
    }

    private void setParameters(SSLSocket socket) {
        SSLParameters sslParameters = socket.getSSLParameters();
        sslParameters.setServerNames(serverNames);
        socket.setSSLParameters(sslParameters);
    }
}
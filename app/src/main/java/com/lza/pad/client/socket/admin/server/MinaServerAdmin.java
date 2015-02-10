package com.lza.pad.client.socket.admin.server;

import com.lza.pad.client.socket.admin.codec.CharsetCodeFactory;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Say something about this class
 *
 * @author xiads
 * @Date 15/2/4.
 */
public class MinaServerAdmin {

    private SocketAcceptor acceptor;
    private ServerMessageHandler handler = new ServerMessageHandler();

    private static final int PORT = 8888;

    public void setOnServerIoListener(ServerMessageHandler.OnServerIoListener listener) {
        handler.setOnServerIoListener(listener);
    }

    public boolean start() {
        acceptor = new NioSocketAcceptor();
        DefaultIoFilterChainBuilder filterChain = acceptor.getFilterChain();
        filterChain.addLast("codec", new ProtocolCodecFilter(new CharsetCodeFactory()));

        acceptor.setHandler(handler);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);

        try {
            acceptor.bind(new InetSocketAddress(PORT));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean stop() {
        try {
            acceptor.unbind(new InetSocketAddress(PORT));
            acceptor.getFilterChain().clear();
            acceptor.dispose();
            acceptor = null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}

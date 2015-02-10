package com.lza.pad.client.socket.admin.client;

import com.lza.pad.client.utils.AppLogger;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * Say something about this class
 *
 * @author xiads
 * @Date 15/2/4.
 */
public class ClientMessageHandlerAdapter extends IoHandlerAdapter {

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        AppLogger.e("客户端发生异常：" + cause.getCause() + "," + cause.getMessage());
        listener.onExceptionCaught(session, cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        String content = message.toString();
        AppLogger.e("客户端收到消息：" + content);
        listener.onMessageReceived(session, message);
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        String content = message.toString();
        AppLogger.e("客户端已发送消息：" + content);
        listener.onMessageSent(session, message);
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        listener.onSessionCreated(session);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        listener.onSessionOpened(session);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        listener.onSessionClosed(session);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        listener.onSessionIdle(session, status);
    }

    @Override
    public void inputClosed(IoSession session) throws Exception {

    }

    public interface OnClientIoListener {
        void onSessionCreated(IoSession session);
        void onSessionOpened(IoSession session);
        void onSessionClosed(IoSession session);
        void onSessionIdle(IoSession session, IdleStatus status);
        void onExceptionCaught(IoSession session, Throwable cause);
        void onMessageReceived(IoSession session, Object message);
        void onMessageSent(IoSession session, Object message);
    }

    OnClientIoListener listener = new OnClientIoAdapter();

    public void setOnClientIoListener(OnClientIoListener listener) {
        this.listener = listener;
    }
}

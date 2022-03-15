package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.ConnectionHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections {
    //might be problem here with ConnectionHandler
    ConcurrentHashMap<Integer, ConnectionHandler> useridToUser = new ConcurrentHashMap<>();//maybe problem because we get int from start.
    int connectionId;
    public static class SingeltonHolder{
        private static ConnectionsImpl instance = new ConnectionsImpl();
    }

    public static ConnectionsImpl getInstance(){return ConnectionsImpl.SingeltonHolder.instance;}
    @Override

    //encode and send, send meaning write and flush, should use the send in the connecntions handelrs.
    //TODO the send.
    public boolean send(int connectionId, Object msg) {
        useridToUser.get(connectionId).send(msg);
        return true;
    }

    @Override
    public void broadcast(Object msg) {

    }

    @Override
    //TODO disconnect.
    public void disconnect(int connectionId) {
        useridToUser.remove(connectionId);
    }

    public void addUser(ConnectionHandler handler){
        handler.setConnectionId(connectionId);
        useridToUser.putIfAbsent(connectionId++,handler);
    }

    public int getConnectionId() {
        return connectionId;
    }

    public ConcurrentHashMap<Integer, ConnectionHandler> getUseridToUser() {
        return useridToUser;
    }
}

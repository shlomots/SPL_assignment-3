package bgu.spl.net.srv;

import java.io.Closeable;
import java.io.IOException;

public interface ConnectionHandler<T> extends Closeable{
    int connectionId=0;
    void send(T msg) ;
    public void setConnectionId(int connectionId);
}

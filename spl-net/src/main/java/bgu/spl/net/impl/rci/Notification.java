package bgu.spl.net.impl.rci;

public class Notification extends SandSCommand{
    private short notificationType;
    public Notification(short opcode){
        super(opcode);
    }

    public short getNotificationType() {
        return notificationType;
    }
}

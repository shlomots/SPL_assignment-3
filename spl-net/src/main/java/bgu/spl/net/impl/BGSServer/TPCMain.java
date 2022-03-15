package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args) {
        Server.threadPerClient(
                7777, //port
                () -> new MessagingProtocol(), //protocol factory
                EncoderDecoderBGU::new //message encoder decoder factory
        ).serve();
    }
}

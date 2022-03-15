package bgu.spl.net.impl.rci;

import java.util.LinkedList;

public class AckReply extends SandSCommand{
    LinkedList<short[]> stats = new LinkedList<>();
    public AckReply(short opcode) {
        super(opcode);
    }

    public LinkedList<short[]> getStats() {
        return stats;
    }

    public void setStats(LinkedList<short[]> stats) {
        this.stats = stats;
    }
}

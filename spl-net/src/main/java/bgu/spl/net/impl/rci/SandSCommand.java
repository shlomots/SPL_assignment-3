package bgu.spl.net.impl.rci;


public class SandSCommand{
    private short opcode;
    private short followOrUnfollow;
    private String [] args = new String[4];//this is the maximum we will have to get or return.
    public SandSCommand(short opcode){
        this.opcode=opcode;
    }

    public short getOpcode() {
        return opcode;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }
    public void setArgs0(String args0){
        args[0]=args0;
    }
    public void setArgs1(String args1){
        args[1]=args1;
    }
    public void setArgs2(String args2){
        args[2]=args2;
    }
    public void setArgs3(String args3){
        args[3]=args3;
    }

    public String getArgs0(){
        return args[0];
    }
    public String getArgs1(){
        return args[1];
    }
    public String getArgs2(){
        return args[2];
    }
    public String getArgs3(){
        return args[3];
    }

    public void setOpcode(short opcode) {
        this.opcode = opcode;
    }

}

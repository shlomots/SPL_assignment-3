package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.rci.SandSCommand;
import bgu.spl.net.impl.rci.AckReply;
import bgu.spl.net.impl.rci.ErrorReply;
import bgu.spl.net.impl.rci.Notification;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Vector;
import java.nio.charset.StandardCharsets;

public class EncoderDecoderBGU implements MessageEncoderDecoder<SandSCommand> {
    private short opcode=-1;
    private Vector<Byte> vectorBuffer = new Vector<>();//$better to do as massage buufer
    private int ArgumentIndex=0;
    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
    String[] arguments = new String[3];
    //TODO PM message decoder incoder

    public SandSCommand decodeNextByte(byte nextByte){
        if(opcode==-1){//didnt start reading yet
            lengthBuffer.put(nextByte);
            if(!lengthBuffer.hasRemaining()){//if we still have room in our buffer
                lengthBuffer.flip();
                opcode=lengthBuffer.getShort();//turn the bytes into a short
                lengthBuffer.clear();
                if(opcode==3|opcode==7){//we are not looking for other bytes if it's logout or logstat
                    return decodeToCommand(nextByte);
                }
                //get the buffer ready for the next opcode.
            }

        }else{
            return decodeToCommand(nextByte);
        }
        return null;
    }



    private SandSCommand decodeToCommand(byte nextByte) {
        SandSCommand command = null;//we want it to be null here so we keep iterating till the command is built.
        switch (opcode) {
            /** break and reset after every command.
             *
             */
            //three strings.
            case 1:
            case 2:
            case 6:
                if(nextByte=='\0') {
                    arguments[ArgumentIndex] = byteToString(vectorToarray(vectorBuffer));
                    vectorBuffer.clear();
                    ArgumentIndex++;
                    if(ArgumentIndex==3) {
                        command = new SandSCommand(opcode);
                        command.setArgs0(arguments[0]);
                        command.setArgs1(arguments[1]);
                        command.setArgs2(arguments[2]);
                        reset();
                    }
                }else {
                    vectorBuffer.add(nextByte);
                }
                break;
            //two strings and a short
//            case 2:
//                System.out.println("im here in case 2 hello what now");
//                if(ArgumentIndex==2) {
//                    vectorBuffer.add(nextByte);
//                    arguments[ArgumentIndex]=byteToString(vectorToarray(vectorBuffer));
//                    command=new SandSCommand(opcode);
//                    command.setArgs0(arguments[0]);
//                    command.setArgs1(arguments[1]);
//                    command.setArgs2(arguments[2]);
//                    reset();
//                    System.out.println("finished decoding");
//                }
//                if(nextByte=='\0') {
//                    arguments[ArgumentIndex] = byteToString(vectorToarray(vectorBuffer));
//                    vectorBuffer.clear();
//                    System.out.println(arguments[ArgumentIndex]);
//                    ArgumentIndex++;
//                }else {
//                    vectorBuffer.add(nextByte);
//                    System.out.println( byteToString(vectorToarray(vectorBuffer)));
//                }
//                break;
            //no arguments.
            case 3:
            case 7:
                command = new SandSCommand(opcode);
                reset();
                return command;
            //short and string
            case 4:
                if(ArgumentIndex==0) {//if it's the short
                    vectorBuffer.add(nextByte);
                    arguments[ArgumentIndex] = byteToString(vectorToarray(vectorBuffer));//put short as string, may cause problems.
                    vectorBuffer.clear();
                    ArgumentIndex++;
                }else if(nextByte=='\0') {//if we already manged the short, we are waiting for an end of a String argument.
                    arguments[ArgumentIndex]=byteToString(vectorToarray(vectorBuffer));
                    command=new SandSCommand(opcode);
                    command.setArgs0(arguments[0]);
                    command.setArgs1(arguments[1]);
                    reset();
                }else  {
                    vectorBuffer.add(nextByte);//we didn't reach the end of the argument yet, get the next byte.
                }
                break;
            //one string
            case 5:
            case 8:
            case 12:
                if (nextByte == '\0') {
                    command = new SandSCommand(opcode);
                    command.setArgs0(byteToString(vectorToarray(vectorBuffer)));
                    reset();
                } else {
                    vectorBuffer.add(nextByte);
                }
                break;
        }
        return command;
    }

    @Override
    public byte[] encode(SandSCommand command) {
        byte[] output=null;
        short opcode1=command.getOpcode();//this needs to be the opcode of the massage we handled.
        String[] arguments = command.getArgs();
        if(command instanceof AckReply) {
            short ackOpcode=10;
            output = shortToBytes(ackOpcode);
            output = appendBytes(output,shortToBytes(opcode1));// maybe problem here

            switch (opcode1) {
                case 1:
                case 2:
                case 3:
                case 5:
                case 12:
                    break;
                case 4:
                    output = appendBytes(output,encodeString(command.getArgs0()));
                    byte[] zero = new byte[1];
                    output=appendBytes(output,zero);
                    break;
                //stat and logstat.
                case 7:
                case 8:
                    output= new byte[0];
                    byte[] psik = new byte[1];
                    psik[0]=';';
                    byte[] revah = new byte[1];
                    revah[0]= '\n';
                    //here it's not append because we don't want the ACK 7/8 in the beginning,
                    LinkedList<short[]> statsToOutput = ((AckReply) command).getStats();
                    int counter=1;
                    for(short[] statsOfUser : statsToOutput ){
                        for(int i =0 ; i < statsOfUser.length ; i++){
                            output=appendBytes(output,shortToBytes(statsOfUser[i]));
                        }
                        if(counter==statsToOutput.size()){
                            output=appendBytes(output,psik);
                        }else {
                            output=appendBytes(output,revah);
                            counter++;
                        }

                    }
                    break;
            }
        }else if(command instanceof ErrorReply) {
            short errOpcode=11;
            output=shortToBytes(errOpcode);
            output=appendBytes(output,shortToBytes(opcode1));
            reset();
        }else if (command instanceof Notification) {
            byte[] zero = new byte[1];
            output = shortToBytes(opcode1);//the opcode
            byte[] notificationType= new byte[1];
            notificationType[0]=Byte.parseByte(command.getArgs0());//all this because you told us to give it to you ine one byte
            output = appendBytes(output,notificationType);//the notification type
            output = appendBytes(output,encodeString(command.getArgs1()));//the first string
            output = appendBytes(output,zero);//the zero
            output = appendBytes(output,encodeString(command.getArgs2()));//the second string
            output = appendBytes(output,zero);//the zero
            reset();
        }
        return output;
    }


    private String byteToString(byte[] bytes){
        //decode to String by UTF8
        return new String(bytes,0,bytes.length, StandardCharsets.UTF_8);
    }
    private byte[] vectorToarray(Vector<Byte> v) {
        byte[] bytes = new byte[v.size()];
        for (int i = 0; i < v.size(); i++) {
            bytes[i] = v.get(i);
        }
        return bytes;
    }
    private void reset() {
        opcode=-1;
        ArgumentIndex=0;
        vectorBuffer=new Vector<>();
        arguments=new String[3];
        lengthBuffer.flip();
        lengthBuffer.clear();
    }
    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }
    private byte[] appendBytes(byte[] arr0, byte[] arr1) {
        // Add your code here
        byte[] arr2 = new byte[arr0.length + arr1.length];
        System.arraycopy(arr0, 0, arr2, 0, arr0.length);
        System.arraycopy(arr1, 0, arr2, arr0.length, arr1.length);
        return arr2;
    }
    private byte[] encodeString(String args){
        //return bytes in UTF 8
        return args.getBytes();
    }
    public short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }




}


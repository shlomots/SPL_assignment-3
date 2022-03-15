package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.rci.Notification;
import bgu.spl.net.impl.rci.SandSCommand;
import bgu.spl.net.Database;
import bgu.spl.net.api.bidi.ConnectionsImpl;
import bgu.spl.net.impl.commands.User;
import bgu.spl.net.impl.rci.ErrorReply;
import bgu.spl.net.impl.rci.AckReply;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

public class MessagingProtocol implements BidiMessagingProtocol<SandSCommand> {
    private Database database = Database.getInstance();
    private ConnectionsImpl connections;
    private int connectionId;
    private User user;
    private boolean shouldTerminate=false;



    @Override
    public void start(int connectionId, Connections<SandSCommand> connections) {
        this.connections= (ConnectionsImpl) connections;
        this.connectionId=connectionId;
    }

    @Override
    public void process(SandSCommand message) {
        short commandOpcode = message.getOpcode();
        switch (commandOpcode){
            //register
            case 1:
                //if he already exists, return error.
                if(database.isRegistered(message.getArgs0())){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));//this will encode and send.
                }else {
                    //if he hasn't registered yet or his name isn't taken, register him.
                    user = new User(message.getArgs0(), message.getArgs1(), message.getArgs2());
                    //register is synchronized, make sure it's enough to avoid problems.
                    database.register(message.getArgs0(),user);
                    connections.send(this.connectionId,new AckReply(commandOpcode));//this will encode and send.
                }
                break;
            //login
            case 2:
                //this checks if the username exists.
                user=database.getUserNameToUser().get(message.getArgs0());
                if(!database.getUserNameToUser().containsKey(message.getArgs0())){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));
                }
                //this checks if he past the captcha.
                if(Objects.equals(message.getArgs3(), "0")){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));//this will encode and send.
                    // ugly way to use atomic boolean inside if...this checks if the user is already logged in.
                }else if(user.isLoggedIn().equals("true")){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));
                    //this checks if the password matches.
                }else if(message.getArgs1().equals(database.getUserNameToUser().get(message.getArgs0()))){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));
                }else{
                    //log him in
                    user=database.getUserNameToUser().get(message.getArgs0());
                    user.logIn();
                    user.setCurrID(connectionId);
                    connections.send(this.connectionId,new AckReply(commandOpcode));
                    database.getUserToAwaitedMessages().putIfAbsent(user.getUserName(),new LinkedBlockingQueue<Notification>());
                    //get his messages if he needs to get any.
                    LinkedBlockingQueue<Notification> awaitedMessages = database.getUserToAwaitedMessages().get(user.getUserName());
                    if(awaitedMessages!=null&&!awaitedMessages.isEmpty()){//if there was ever something to send andand something to send right now
                        for(Notification notification : awaitedMessages) {
                        connections.send(this.connectionId, notification);
                        awaitedMessages.remove(notification);
                    }
                    }
                }
                break;


            case 3:
                //first find him if there is something to find.
                User toLogout=database.lookForClientById(connectionId);
                //return error if he doesn't exist or already logged out.
                if(toLogout==null||toLogout.isLoggedIn().equals("false")){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));
                }else {
                    toLogout.logout();
                    connections.send(this.connectionId,new AckReply(commandOpcode));//this will encode and send.
                }
                break;
                //this is from shiri shiri that is shiri
            case 4:
                if (user.isLoggedIn().equals("false")){
                    connections.send(this.connectionId, new ErrorReply(commandOpcode));
                    break;
                }
                if (!database.getUserNameToUser().containsKey(message.getArgs1())) {
                    connections.send(this.connectionId, new ErrorReply(commandOpcode));
                    break;
                }
                User userToFollow = database.getUserNameToUser().get(message.getArgs1()); //find the user to follow/unfollow
                if (user.getBlock().contains(userToFollow))
                    connections.send(this.connectionId, new ErrorReply(commandOpcode));
                else {
                    AckReply ackFollow = new AckReply(commandOpcode);
                    ackFollow.setArgs0(message.getArgs1()); //the username
                    if (message.getArgs0().equals("0")){ //follow
                        if (!user.getFollowing().contains(userToFollow)){
                            user.getFollowing().add(userToFollow); //add to list of following
                            userToFollow.getFollowers().add(user); //add this user to the list of followers in the other user
                            connections.send(this.connectionId, ackFollow);
                        }
                        else {
                            connections.send(this.connectionId, new ErrorReply(commandOpcode)); //the user was already on the list
                        }
                    }
                    else { //unfollow
                        if (user.getFollowing().contains(userToFollow)){
                            user.getFollowing().remove(userToFollow); //remove from list
                            userToFollow.getFollowers().remove(user); //remove this user from the other user follower list
                            connections.send(this.connectionId, ackFollow);
                        }
                        else
                            connections.send(this.connectionId, new ErrorReply(commandOpcode)); //the user was not on the list
                    }
                }
                break;

            case 5:
                user = database.lookForClientById(connectionId);
                //create the notification
                short notiOpcode = 9;
                short type=1;
                Notification postToBeSent = new Notification(notiOpcode);
                postToBeSent.setArgs0("1");
                postToBeSent.setArgs1(user.getUserName());
                postToBeSent.setArgs2(message.getArgs0());
                //save in database
                Timestamp postCurrTime= new Timestamp(System.currentTimeMillis());
                database.getAllMessages().putIfAbsent(postCurrTime,new LinkedBlockingQueue<>());
                database.getAllMessages().get(postCurrTime).add(message);
                //dont send the massage if the sender doesnt exist or logged out.
                if(user==null||user.isLoggedIn().equals(false)){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));
                }else{
                    //now we will find everyone that should be messaged.
                    ArrayList<User> usersToBeNotified = new ArrayList<>();
                    String[] contentSplit=message.getArgs0().split(" ");//split the message to find the @'s.
                    for(String string : contentSplit){
                        if(string.contains("@")){
                            if(database.getUserNameToUser().containsKey(string.substring(1))){
                                usersToBeNotified.add(database.getUserNameToUser().get(string.substring(1)));
                            }
                        }
                    }
                    //send to all the users tagged.
                    if(!usersToBeNotified.isEmpty()){
                        for(User user1 : usersToBeNotified){
                            //do it only if their is no blocking history there.
                            if(!(user.getBlock().contains(user1)|user1.getBlock().contains(user))){
                                if(user1.isLoggedIn().equals("true")){
                                    connections.send(user1.getCurrID(),postToBeSent);
                                }else{
                                    database.getUserToAwaitedMessages().get(user1.getUserName()).add(postToBeSent);
                                }
                            }
                        }
                    }

                    //send to all the users following
                    if(!user.getFollowers().isEmpty()){
                        for(User user1 : user.getFollowers()){
                            //do it only if there is no blocking history there.
                            if(!(user.getBlock().contains(user1)|user1.getBlock().contains(user))){
                                if(user1.isLoggedIn().equals("true")){
                                    connections.send(user1.getCurrID(),postToBeSent);
                                }else{
                                    database.getUserToAwaitedMessages().get(user1.getUserName()).add(postToBeSent);
                                }
                            }
                        }
                    }
                    connections.send(this.connectionId,new AckReply(commandOpcode));//this will encode and send.
                    user.increaseNumOfPosts();
                }

                break;
            case 6:
                user = database.lookForClientById(connectionId);
                User userToSendTo = database.getUserNameToUser().get(message.getArgs0());
                //dont send the massage if the sender doesnt exist or logged out,or the recipient isn't registered
                if(user==null||user.isLoggedIn().equals(false)||!database.isRegistered(userToSendTo.getUserName())){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));
                    //don't send the message if the sender isn't following the recipient.
                }else if(!userToSendTo.getFollowers().contains(user)){
                    connections.send(this.connectionId,new ErrorReply(commandOpcode));
                }
                else{
                    //filter out the words we don't want.
                    String [] splitContent=message.getArgs1().split(" ");
                    for(int i = 0 ; i< splitContent.length ; i++){
                        if(database.getFilteredOut().contains(splitContent[i])){
                            splitContent[i] = "<filtered>";
                        }
                    }
                    //append to the message we want to send.
                    String filteredContent="";
                    for(int i = 0;i<splitContent.length;i++){
                        filteredContent+=splitContent[i]+" ";
                    }
                    filteredContent+=message.getArgs2();//append the date.
                    //create the notification
                    notiOpcode = 9;
                    Notification PMtoBeSent = new Notification(notiOpcode);
                    PMtoBeSent.setArgs0("0");
                    PMtoBeSent.setArgs1(user.getUserName());
                    PMtoBeSent.setArgs2(filteredContent);//may be problem
                    //save in database
                    message.setArgs1(filteredContent);//put in filtered message instead.
                    Timestamp PMCurrTime= new Timestamp(System.currentTimeMillis());
                    database.getAllMessages().putIfAbsent(PMCurrTime,new LinkedBlockingQueue<>());
                    database.getAllMessages().get(PMCurrTime).add(message);
                    //now send the notification
                    if(!(user.getBlock().contains(userToSendTo)|userToSendTo.getBlock().contains(user))){
                        if(userToSendTo.isLoggedIn().equals("true")){
                            connections.send(userToSendTo.getCurrID(),PMtoBeSent);
                        }else{
                            database.getUserToAwaitedMessages().get(userToSendTo.getUserName()).add(PMtoBeSent);
                        }
                    }
                    connections.send(this.connectionId,new AckReply(commandOpcode));//this will encode and send.
                }
                break;
            case 7:
                //check if user is registered and if he is logged in
                if (!database.isRegistered(user.getUserName()) || user.isLoggedIn().equals("false"))
                    connections.send(this.connectionId, new ErrorReply(commandOpcode));
                else {
                    LinkedList<short[]> stats = new LinkedList<>();
                    for (User logUser : database.getUserNameToUser().values()){
                        if (logUser.isLoggedIn().equals("true") & !user.getBlock().contains(logUser)){
                            short[] statOfUser = statForUser(logUser, commandOpcode);
                            stats.add(statOfUser);
                        }
                    }
                    AckReply ackToReply = new AckReply(commandOpcode);
                    ackToReply.setStats(stats);
                    connections.send(this.connectionId, ackToReply);
                }
                break;
            case 8:
                if (!database.isRegistered(user.getUserName()) || user.isLoggedIn().equals("false"))
                    connections.send(this.connectionId, new ErrorReply(commandOpcode));
                else{
                    boolean error = false;
                    LinkedList<short[]> stats = new LinkedList<>();
                    String[] usersArray = message.getArgs0().split("\\|"); //split the list uf users
                    for (int i=0; i<usersArray.length; i++){
                        //there is a user that is not registered or the user is blocked, send error and finish
                        if (!database.isRegistered(usersArray[i]) && user.getBlock().contains(database.getUserNameToUser().get(usersArray[i]))){
                            connections.send(this.connectionId, new ErrorReply(commandOpcode));
                            error = true;
                            break;
                        }
                        short[] statOfUser = statForUser(database.getUserNameToUser().get(usersArray[i]), commandOpcode);
                        stats.add(statOfUser);
                    }
                    if (!error){ //error has not been sent
                        AckReply ackToReply = new AckReply(commandOpcode);
                        ackToReply.setStats(stats);
                        connections.send(this.connectionId, ackToReply);
                    }
                }
                break;

            case 12:
                //check if username exits
                if (!database.isRegistered(message.getArgs0()))
                    connections.send(this.connectionId, new ErrorReply(commandOpcode));
                else {
                    User userToBlock = database.getUserNameToUser().get(message.getArgs0());
                    if (user.getBlock().contains(userToBlock)) //user already blocked this user
                        connections.send(this.connectionId, new ErrorReply(commandOpcode));
                    else {
                        connections.send(this.connectionId, new AckReply(commandOpcode));
                        block(user, userToBlock);
                        block(userToBlock, user);
                    }
                }
                break;
        }
    }

    private short[] statForUser(User user, short opcode){
        short[] output1= new short[6];
        output1[0]=10;
        output1[1]=opcode;
        int age = calculateAge(user.getBirthday());
        output1[2]= (short) age;
        output1[3]=  (short) user.getNumPosts();
        output1 [4]=  (short) user.getFollowers().size();
        output1 [5]=  (short) user.getFollowing().size();
        return output1;
    }
    //calculating user age
    private int calculateAge(String birthday){
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            Date date1 = formatter.parse(birthday);
            Instant instant = date1.toInstant();
            ZonedDateTime zone = instant.atZone(ZoneId.systemDefault());
            LocalDate givenDate = zone.toLocalDate();
            Period period = Period.between(givenDate, LocalDate.now());
            return period.getYears();
        }catch (ParseException e){}
        return -1;
    }

    private void block(User blockingUser, User userToBlock){
        blockingUser.getFollowing().remove(userToBlock); //remove() contains the contains()
        blockingUser.getFollowers().remove(userToBlock);
        blockingUser.getBlock().add(userToBlock);
    }


    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

}

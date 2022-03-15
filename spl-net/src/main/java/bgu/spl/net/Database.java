package bgu.spl.net;

import bgu.spl.net.impl.commands.User;
import bgu.spl.net.impl.rci.Notification;
import bgu.spl.net.impl.rci.SandSCommand;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Database {
    private ConcurrentHashMap<String,User> userNameToUser = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LinkedBlockingQueue<Notification>> UserToAwaitedMessages= new ConcurrentHashMap<>();
    private ConcurrentHashMap<Timestamp,LinkedBlockingQueue<SandSCommand>> allMessages = new ConcurrentHashMap<>();
    private ArrayList<String> filteredOut = new ArrayList<>();//TODO find where to put the words.

    public static class SingeltonHolder{
        private static Database instance = new Database();
    }

    public static Database getInstance(){return SingeltonHolder.instance;}

    public ConcurrentHashMap<String, User> getUserNameToUser() {return userNameToUser;}


    //check if he is registered
    public boolean isRegistered (String username){
        return userNameToUser.containsKey(username);//check is their a username like this
    }

    //register him
    //should be synchronized so we would have a situation where someone thought he could enter a username but then some one else beat him to it.
    public synchronized void register(String usernamre, User user){
        userNameToUser.put(usernamre,user);
        System.out.println(usernamre+" "+ user.getUserName()+user.getBirthday());
    }

    public ConcurrentHashMap<String, LinkedBlockingQueue<Notification>> getUserToAwaitedMessages() {
        return UserToAwaitedMessages;
    }

    public User lookForClientById(int currId){
        User output=null;
        for(User user : userNameToUser.values()){
            //if this is the user with this handlers Id.
            if(user.getCurrID()==currId){
                output=user;
            }
        }
        return output;
    }

    public ConcurrentHashMap<Timestamp, LinkedBlockingQueue<SandSCommand>> getAllMessages() {
        return allMessages;
    }

    public ArrayList<String> getFilteredOut() {
        filteredOut.add("shlomo");
        filteredOut.add("shiri");
        return filteredOut;
    }
}

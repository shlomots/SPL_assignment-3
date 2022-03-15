package bgu.spl.net.impl.commands;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class User {
    private int currID;
    private int numPosts=0;
    private String userName;
    private String password;
    private String birthday;
    private AtomicBoolean loggedIn = new AtomicBoolean(false);
    private ConcurrentLinkedQueue<User> following = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<User> followers = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<User> block = new ConcurrentLinkedQueue<>();


    public User(String userName, String password, String birthday){
        this.userName = userName;
        this.password = password;
        this.birthday = birthday;
    }
    //ugly, need to find better way to use boolean inside if.
    public String isLoggedIn(){
        return loggedIn.toString();
    }
    //set logged in is true

    public void logIn() {
        this.loggedIn.set(true);
    }

    public String getUserName() {
        return userName;
    }

    public void logout(){
        this.loggedIn.set(false);
    }
    //this is so we could check if he is connected while doing the log out.
    public void setCurrID(int currID) {
        this.currID = currID;
    }

    public int getCurrID() {
        return currID;
    }

    public ConcurrentLinkedQueue<User> getFollowers() {
        return followers;
    }

    public ConcurrentLinkedQueue<User> getFollowing() {
        return following;
    }
    public void increaseNumOfPosts(){
        numPosts++;
    }

    public ConcurrentLinkedQueue<User> getBlock() {
        return block;
    }

    public String getBirthday() {
        return birthday;
    }

    public int getNumPosts() {
        return numPosts;
    }
}




package me.chrismarkus.digital_sputnik_clone;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by Chris on 9/26/2017.
 */

public class Server {
    //final Handler handler = new Handler();
    private Thread thread;
    PrintWriter output;
    BufferedReader input;
    OutputStream out;
    Socket s;
    PrintWriter p;
    String address = "192.168.1.1";
    int port = 21;
    int readTimeout = 1000;
    private boolean isStarted = false;
    boolean hardStop = false;
    int downtime = 5000;
    // server status values:
        //0: not started
        //1: started and connected
        //2: started but not connected
    int serverStatus = 0;

    public void startServer() throws Exception{
        if(!isStarted) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    //set-up the socket and try to connect
                    int heartbeatDelay = 4000;
                    long nextHeartbeat = System.currentTimeMillis() + heartbeatDelay;
                    boolean socketException;
                    do{
                        socketException = false;
                        try {
                            s = new Socket(address, 21);
                            s.setSoTimeout(readTimeout);
                            Log.d("Spot", s.getInetAddress()+"");
                        } catch(SocketException se) {
                            socketException = true;
                            try {
                                thread.sleep(downtime);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            //find some way to print out "No Connection"///////////////////
                            setStatus(2);
                            thread = null;
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(System.currentTimeMillis() >= nextHeartbeat){
                            socketException = !heartbeat(s);
                            nextHeartbeat = System.currentTimeMillis() + heartbeatDelay;
                        }
                    } while(!hardStop && !socketException);
                    do{

                    }while(false);

                }
            });
        }
        else{
            if(s.getInetAddress() + "" != "/"+address){

            }
            setStatus(1);
        }
    }

    //sends heartbeat, returns false if response recieved within alotted time, false otherwise
    public boolean heartbeat(Socket s){
        sendMessage("111");
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
            final String st = input.readLine();
            return true;
        }catch(IOException e){
            return false;
        }
    }

    public void sendMessage(String msg){

    }

    public int getStatus(){
        return serverStatus;
    }

    private void setStatus(int status){
        serverStatus = status;
    }

    public boolean killServerAndRestart(){

        return true;
    }

    public boolean getIsStarted(){
        return true;
    }

    public boolean isConnectionAlive(){

        return true;
    }


}

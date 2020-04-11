package org.quain.groupchat.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class SocketService extends Service {
    public static BufferedReader in;
    public static List<String> myMsgList = new ArrayList<>();
    public static String newClientMsg;
    public static String newServerMsg;
    public static PrintWriter out;
    public static String serverHost;
    public static int serverPort = 0;
    public static Socket socket;
    public static boolean socketAliveFlag = false;
    public static Thread socketSubThread;
    public static boolean socketSubThreadRunFlag;
    public static String userName;

    public static String setJSONContent(String msgContent) {
        try {
            JSONObject root = new JSONObject();
            root.put("msgSender", userName);
            root.put("msgContent", msgContent);
            root.put("userConnectionState", true);
            return root.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    public String getJSONContent(String JSONString) {
        try {
            JSONObject root = new JSONObject(JSONString);
            return " --- " + root.getString("msgTime") + " --- " + root.getString("msgSender") + "\n" + root.getString("msgContent") + "\n";
        } catch (JSONException e) {
            return "---------";
        }
    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        socketSubThread = new Thread(new Runnable() {
            public void run() {
                try {
                    SocketService.socket = new Socket();
                    SocketService.socket.connect(new InetSocketAddress(SocketService.serverHost, SocketService.serverPort), 1000);

                    LoginActivity.loginDiaLog.dismiss();
                    SocketService.socketAliveFlag = true;
                    SocketService.newClientMsg = SocketService.userName;
                    sendMsg(SocketService.newClientMsg);
                    MainActivity.mainActivityAliveFlag = true;

                    Intent mainIntent = new Intent(SocketService.this, MainActivity.class);
                    mainIntent.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainIntent);
                    while (SocketService.socketAliveFlag) {
                        SocketService.newServerMsg = SocketService.this.receiveMsg();
                        if (SocketService.newServerMsg == null) {
                            break;
                        }
                        SocketService.myMsgList.add(SocketService.this.getJSONContent(SocketService.newServerMsg));
                    }
                    try {
                        SocketService.out.close();
                        SocketService.in.close();
                        SocketService.socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    SocketService.socketAliveFlag = false;
                    MainActivity.mainActivityAliveFlag = false;
                    Intent loginIntent = new Intent(SocketService.this, LoginActivity.class);
                    loginIntent.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                    SocketService.this.startActivity(loginIntent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SocketService.socketSubThreadRunFlag = false;
                LoginActivity.loginDiaLog.dismiss();
                SocketService.this.stopSelf();
            }
        });
    }

    public void onDestroy() {
        socketAliveFlag = false;
        socketSubThreadRunFlag = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onStart(Intent paramIntent, int paramInt) {
        if (!socketSubThreadRunFlag) {
            socketSubThreadRunFlag = true;
            socketSubThread.start();
        }
    }

    public String receiveMsg() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "----------";
    }

    public void sendMsg(String paramString) {
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            out.println(paramString);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

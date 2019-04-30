package org.quain.groupchat.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
	final int UPDATE_TEXT_FROM_SYSTEM = 0;
	final int UPDATE_TEXT_FROM_SERVER = 1;
	final int ENABLE_BUTTON_CONNECT = 2;
	final int DISABLE_BUTTON_CONNECT = 3;
	final int ENABLE_BUTTON_DISCONNECT = 4;
	final int DISABLE_BUTTON_DISCONNECT = 5;
	final int CLEAR_EDIT_SEND = 6;

	TextView text_content;
	EditText text_host;
	EditText text_port;
	EditText username;
	EditText edit_send;
	Button btn_send;
	Button btn_connect;
	Button btn_disconnect;
	ScrollView text_scroll;

	String host;
	int port;
	Socket socket;

	PrintWriter out;// 字符输出流
	BufferedReader in;// 字符输入流

	Thread subThread;//连接服务器、接收消息线程
	Thread subThread_sendMsg;//发送消息线程

	StringBuilder sb;
	String newServerMsg;
	String newClientMsg;
	String newSystemMsg;

	boolean connectionAliveFlag;

	//定义一个handler对象，用来刷新界面
	private Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
				case UPDATE_TEXT_FROM_SYSTEM:
					sb.append(newSystemMsg.substring(0, newSystemMsg.indexOf("：")));
					sb.append("\n");
					sb.append(newSystemMsg.substring(newSystemMsg.indexOf("：") + 1));
					text_content.setText(sb);
					text_scroll.fullScroll(ScrollView.FOCUS_DOWN);
					sb.append("\n\n");
					text_content.setText(sb);
					text_scroll.fullScroll(ScrollView.FOCUS_DOWN);
					break;
				case UPDATE_TEXT_FROM_SERVER:
					sb.append(newServerMsg.substring(0, newServerMsg.indexOf("：")));
					sb.append("\n");
					sb.append(newServerMsg.substring(newServerMsg.indexOf("：") + 1));
					text_content.setText(sb);
					text_scroll.fullScroll(ScrollView.FOCUS_DOWN);
					sb.append("\n\n");
					text_content.setText(sb);
					text_scroll.fullScroll(ScrollView.FOCUS_DOWN);
					break;
				case ENABLE_BUTTON_CONNECT:
					btn_connect.setEnabled(true);
					username.setEnabled(true);
					text_host.setEnabled(true);
					text_port.setEnabled(true);
					edit_send.setEnabled(false);
					btn_send.setEnabled(false);
					break;
				case DISABLE_BUTTON_CONNECT:
					btn_connect.setEnabled(false);
					username.setEnabled(false);
					text_host.setEnabled(false);
					text_port.setEnabled(false);
					edit_send.setEnabled(true);
					btn_send.setEnabled(true);
					edit_send.requestFocus();
					break;
				case ENABLE_BUTTON_DISCONNECT:
					btn_disconnect.setEnabled(true);
					break;
				case DISABLE_BUTTON_DISCONNECT:
					btn_disconnect.setEnabled(false);
					break;
				case CLEAR_EDIT_SEND:
					edit_send.setText("");
					edit_send.requestFocus();
					break;
				default:
					break;
			}
			return true;
		}
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		text_content = findViewById(R.id.text_content);
		text_host = findViewById(R.id.host);
		text_port = findViewById(R.id.port);
		username = findViewById(R.id.username);
		edit_send = findViewById(R.id.edit_send);
		btn_send = findViewById(R.id.btn_send);
		btn_connect = findViewById(R.id.btn_connect);
		btn_disconnect = findViewById(R.id.btn_disconnect);
		text_scroll = findViewById(R.id.text_scroll);

		sb = new StringBuilder();
		username.setText(android.os.Build.MODEL);
		username.requestFocus();

		// 发送按钮点击事件
		btn_send.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!edit_send.getText().toString().trim().equals("")) {
					// 如果消息框不为空，则发送，trim()删除头尾空白符
					if (socket != null) {// socket存在
						subThread_sendMsg = new Thread(new Runnable() {// 创建新线程
							@Override
							public void run() {
								try {
									newClientMsg = edit_send.getText().toString();
									sendMsg(newClientMsg);
									sendMsgToHandler(CLEAR_EDIT_SEND);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
						subThread_sendMsg.start();
					}
				}
			}
		});

		// 连接按钮点击事件
		btn_connect.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				subThread = new Thread(new Runnable() {
					@Override
					public void run() {
						host = text_host.getText().toString();
						port = Integer.parseInt(text_port.getText().toString());
						try {
							newSystemMsg = getCurrentDate() + "本机：正在连接服务器" + host + ":" + port;
							sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);

							socket = new Socket(host, port);

							newSystemMsg = getCurrentDate() + "本机：连接成功";
							sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);
							sendMsgToHandler(DISABLE_BUTTON_CONNECT);
							sendMsgToHandler(ENABLE_BUTTON_DISCONNECT);
							connectionAliveFlag = true;
							newClientMsg = username.getText().toString();
							sendMsg(newClientMsg);
							while (connectionAliveFlag) {
								if ((newServerMsg = recvMsg()) != null) {
									sendMsgToHandler(UPDATE_TEXT_FROM_SERVER);
								}
							}
						} catch (IOException e) {
							connectionAliveFlag = false;
							newSystemMsg = getCurrentDate() + "本机：连接失败，服务器未响应";
							sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);
							socket = null;
							subThread = null;
						}
					}
				});
				subThread.start();
			}
		});

		// 断开按钮点击事件
		btn_disconnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					out.close();
					in.close();
					socket.close();
					connectionAliveFlag = false;
					subThread = null;
					subThread_sendMsg = null;

					newSystemMsg = getCurrentDate() + "本机：断开连接";
//                    sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);
					sendMsgToHandler(ENABLE_BUTTON_CONNECT);
					sendMsgToHandler(DISABLE_BUTTON_DISCONNECT);
				} catch (Exception e) {
					connectionAliveFlag = false;
					newSystemMsg = "本机：断开连接时发生错误";
					sendMsgToHandler(ENABLE_BUTTON_CONNECT);
					sendMsgToHandler(DISABLE_BUTTON_DISCONNECT);
					sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);
				}
			}
		});

		text_scroll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				text_scroll.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

	public void sendMsg(String msgReadyToSend) {
		try {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			out.println(msgReadyToSend);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String recvMsg() {
		String msgReadyToRecv;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			msgReadyToRecv = in.readLine();
			return msgReadyToRecv;
		} catch (IOException e) {
			connectionAliveFlag = false;
//            newSystemMsg = getCurrentDate() + "本机：与服务器断开连接，消息接收失败\n";
			sendMsgToHandler(ENABLE_BUTTON_CONNECT);
			sendMsgToHandler(DISABLE_BUTTON_DISCONNECT);
			sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);
			return null;
		}
	}

	public void sendMsgToHandler(int MsgType) {
		Message msg = new Message();
		msg.what = MsgType;
		handler.sendMessage(msg);
	}

	public String getCurrentDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return "---" + sdf.format(new Date()) + "--- ";
	}
}
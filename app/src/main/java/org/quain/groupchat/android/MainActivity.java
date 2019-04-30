package org.quain.groupchat.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

	Thread subThread;
	Thread subThread_sendMsg;

	String host;
	int port;
	Socket socket;

	OutputStream os;// 输出流
	OutputStreamWriter osr;
	BufferedWriter bw;
	PrintWriter out;// 打印流
	InputStream is;// 输入流，字节流
	InputStreamReader isr;// 字符流
	BufferedReader in;// 缓冲流

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
					sb.append(newSystemMsg);
					sb.append("\n");
					text_content.setText(sb);
					break;
				case UPDATE_TEXT_FROM_SERVER:
					sb.append(newServerMsg);
					sb.append("\n");
					text_content.setText(sb);
					break;
				case ENABLE_BUTTON_CONNECT:
					btn_connect.setEnabled(true);
					break;
				case DISABLE_BUTTON_CONNECT:
					btn_connect.setEnabled(false);
					break;
				case ENABLE_BUTTON_DISCONNECT:
					btn_disconnect.setEnabled(true);
					break;
				case DISABLE_BUTTON_DISCONNECT:
					btn_disconnect.setEnabled(false);
					break;
				case CLEAR_EDIT_SEND:
					edit_send.setText("");
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
		text_host = findViewById(R.id.txt_host);
		text_port = findViewById(R.id.txt_port);
		username = findViewById(R.id.username);
		edit_send = findViewById(R.id.edit_send);
		btn_send = findViewById(R.id.btn_send);
		btn_connect = findViewById(R.id.btn_connect);
		btn_disconnect = findViewById(R.id.btn_disconnect);

		sb = new StringBuilder();
		username.setText(android.os.Build.MODEL);

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
							newSystemMsg = getCurrentDate() + "本机：正在连接服务器" + host + ":" + port + "\n";
							sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);

							socket = new Socket(host, port);

							newSystemMsg = getCurrentDate() + "本机：连接成功\n";
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
							newSystemMsg = getCurrentDate() + "本机：连接失败，服务器未响应\n";
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

					newSystemMsg = getCurrentDate() + "本机：断开连接\n";
					sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);
					sendMsgToHandler(ENABLE_BUTTON_CONNECT);
					sendMsgToHandler(DISABLE_BUTTON_DISCONNECT);
				} catch (Exception e) {
					connectionAliveFlag = false;
					newSystemMsg = "本机：断开连接时发生错误\n";
					sendMsgToHandler(ENABLE_BUTTON_CONNECT);
					sendMsgToHandler(DISABLE_BUTTON_DISCONNECT);
					sendMsgToHandler(UPDATE_TEXT_FROM_SYSTEM);
				}
			}
		});
	}

	public void sendMsg(String msgReadyToSend) {
		try {
			os = socket.getOutputStream();
			osr = new OutputStreamWriter(os);
			bw = new BufferedWriter(osr);
			out = new PrintWriter(bw, true);
			out.println(msgReadyToSend);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String recvMsg() {
		String msgReadyToRecv;
		try {
			is = socket.getInputStream();
			isr = new InputStreamReader(is, "UTF-8");
			in = new BufferedReader(isr);
			msgReadyToRecv = in.readLine();
			return msgReadyToRecv;
		} catch (IOException e) {
			connectionAliveFlag = false;
			newSystemMsg = getCurrentDate() + "本机：与服务器断开连接，消息接收失败\n";
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
		return "***" + sdf.format(new Date());
	}
}
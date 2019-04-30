package org.quain.groupchat.android;

import android.support.v7.app.AppCompatActivity;
import android.os.*;
import android.view.*;
import android.content.*;
import android.widget.*;

import org.json.JSONObject;

import java.io.*;

import static org.quain.groupchat.android.SocketService.myMsgList;
import static org.quain.groupchat.android.SocketService.socket;
import static org.quain.groupchat.android.SocketService.socketAliveFlag;

public class MainActivity extends AppCompatActivity {
	public static final int UPDATE_TEXT_FROM_SYSTEM = 0;
	public static final int UPDATE_TEXT_FROM_SERVER = 1;
	public static final int CLEAR_EDIT_SEND = 2;

	TextView text_content;
	EditText edit_send;
	Button btn_send;
	ScrollView text_scroll;

	PrintWriter out;// 字符输出流
	Thread subThread_sendMsg;//发送消息线程
	static StringBuilder sb = new StringBuilder();
	static String newServerMsg;
	static String newClientMsg;
	static String newSystemMsg;
	public static boolean mainActivityAliveFlag = false;
	public static Thread refreshMsgListThread;
	//定义一个handler对象，用来刷新界面
	Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
				case UPDATE_TEXT_FROM_SYSTEM:
					sb.append(newSystemMsg);
					text_content.setText(sb);
					sb.append("\n");
					text_content.setText(sb);
					text_scroll.fullScroll(ScrollView.FOCUS_DOWN);
					break;
				case UPDATE_TEXT_FROM_SERVER:
					sb.append(newServerMsg);
					text_content.setText(sb);
					sb.append("\n");
					text_content.setText(sb);
					text_scroll.fullScroll(ScrollView.FOCUS_DOWN);
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
		edit_send = findViewById(R.id.edit_send);
		btn_send = findViewById(R.id.btn_send);
		text_scroll = findViewById(R.id.text_scroll);

		mainActivityAliveFlag = true;

		refreshMsgListThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (mainActivityAliveFlag) {
					try {
						newServerMsg = myMsgList.get(0);
						sendMsgToHandler(UPDATE_TEXT_FROM_SERVER);
						Thread.sleep(50);
						myMsgList.remove(0);
//						System.out.println("显示内容" + newServerMsg + "删除myMsgList第一项");
					} catch (Exception e) {
//						System.out.println("不能删除myMsgList第一项");
					}
				}
//				System.out.println("结束线程结束线程结束线程结束线程结束线程结束线程");
			}
		});
//		System.out.println("运行线程运行线程运行线程运行线程运行线程运行线程");
		refreshMsgListThread.start();


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
									sendMsg(SocketService.setJSONContent(newClientMsg));
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main_titlebar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		Intent serviceIntent = new Intent(MainActivity.this, SocketService.class);
		Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
		switch (id) {
			case R.id.menu_logout:
				closeConnection();
				stopService(serviceIntent);//退出聊天界面，返回登录页面
				mainActivityAliveFlag = false;
				loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(loginIntent);
				return true;
			case R.id.menu_about:
				Toast.makeText(MainActivity.this, "Designed by QuainK.", Toast.LENGTH_SHORT).show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void sendMsgToHandler(int MsgType) {
		Message msg = new Message();
		msg.what = MsgType;
		handler.sendMessage(msg);
	}

	@Override
	public void onBackPressed() {
		mainActivityAliveFlag = false;
		finish();
	}

	public void closeConnection() {
		new Thread(new Runnable() {// 创建新线程
			@Override
			public void run() {
				try {
					JSONObject root = new JSONObject();
					root.put("userConnectionState", false);
					sendMsg(root.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
				socketAliveFlag = false;
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
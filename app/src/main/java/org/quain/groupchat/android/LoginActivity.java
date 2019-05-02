package org.quain.groupchat.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
	public static ProgressDialog loginDiaLog;
	Button btn_login;
	EditText txt_host;
	EditText txt_port;
	EditText txt_username;

	public void onBackPressed() {
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		txt_host = findViewById(R.id.host);
		txt_port = findViewById(R.id.port);
		txt_username = findViewById(R.id.username);
		btn_login = findViewById(R.id.btn_login);
		txt_username.setText(Build.MODEL);
		txt_username.requestFocus();
		if (!SocketService.socketAliveFlag) {
			btn_login.setOnClickListener(new View.OnClickListener() {
				public void onClick(View paramAnonymousView) {
					SocketService.serverHost = txt_host.getText().toString();
					SocketService.serverPort = Integer.parseInt(txt_port.getText().toString());
					SocketService.userName = txt_username.getText().toString();

					loginDiaLog = new ProgressDialog(LoginActivity.this);
					loginDiaLog.setTitle("正在登录");
					loginDiaLog.setMessage("正在登录用户 " + SocketService.userName);
					LoginActivity.loginDiaLog.show();
					Intent serviceIntent = new Intent(LoginActivity.this, SocketService.class);
					startService(serviceIntent);
				}
			});
		} else {
			startActivity(new Intent(this, MainActivity.class));
			finish();
		}
	}
}
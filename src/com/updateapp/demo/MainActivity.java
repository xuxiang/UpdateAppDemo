package com.updateapp.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.updateappdemo.R;

public class MainActivity extends Activity implements OnClickListener {
	
	public static final String downUrl = "http://www.appchina.com/market/d/877993/cop.baidu_0/com.code.search.apk";
	
	private Button updateButton = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		updateButton = (Button)findViewById(R.id.updateButton);
		updateButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if(view == updateButton){
			Intent intent = new Intent(this,UpdateLoadService.class);
			intent.putExtra("downUrl",downUrl);						
			startService(intent);
		}
	}

}

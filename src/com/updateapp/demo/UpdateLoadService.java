package com.updateapp.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.RemoteViews;

import com.example.updateappdemo.R;

/**   
* @ClassName UpdateLoadService.java 
* @Description 下载更新服务
* @author 徐湘
* @date 2015年10月12日 下午4:57:48 
*/ 
public class UpdateLoadService extends Service {

	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private PendingIntent mPendingIntent;
	private RemoteViews mRemoteViews = null;
	private static final int DOWNLOADING = 0;
	private static final int NETWORK_ERR = 1;
	private static final int DOWNLOAD_ERR = 2;
	private static final int DOWNLOAD_OK = 3;
	private static final int NOTIFICATION_ID = 111;

	private String mURL = "";// 下载新版apk路径
	private String mApkName = "update.apk";// 新版apk保存名字

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("deprecation")
	public void onCreate() {
		try {
			super.onCreate();
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotification = new Notification();
			mRemoteViews = new RemoteViews(getPackageName(), R.layout.update_load);
			Intent intent = new Intent();
			mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
			mNotification.tickerText = "准备下载更新...";
			mNotification.icon = android.R.drawable.stat_sys_download;
			mNotification.setLatestEventInfo(UpdateLoadService.this, "提示", "准备下载 ...", mPendingIntent);
		} catch (Exception e) {
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			mNotification.contentIntent = mPendingIntent;
			mNotificationManager.cancel(NOTIFICATION_ID);
			mNotificationManager.notify(NOTIFICATION_ID, mNotification);
			mURL = intent.getStringExtra("downUrl");
			new DownLoadThread().start();
		} catch (Exception e) {
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	private class DownLoadThread extends Thread {
		public void run() {
			super.run();
			try {
				download();
			} catch (IOException e) {
				handler.sendEmptyMessage(DOWNLOAD_ERR);
			}
		}
	}

	private void download() throws IOException {
		try {
			URL url = new URL(mURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(10 * 1000);
			conn.setRequestProperty("User-agent", "Mozilla/4.0");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-type", "application/vnd.android.package-archive");
			int total = conn.getContentLength();
			int code = 0;
			try {
				code = conn.getResponseCode();
			} catch (Exception e) {
				code = 0;
			}

			mNotificationManager.cancel(NOTIFICATION_ID);
			if (code != 200) {
				handler.sendEmptyMessage(NETWORK_ERR);
			} else {
				if (total == -1) {
					handler.sendEmptyMessage(NETWORK_ERR);
					return;
				}
				InputStream fin = conn.getInputStream();
				File file = new File(getApkFilePath(mApkName));
				if (file.exists()) { // delete old one
					file.delete();
				}
				FileOutputStream fos = new FileOutputStream(file);
				try {
					int len = -1;
					int init = 0;
					int lastPercent = 0;
					byte buffer[] = new byte[1024];

					while ((len = fin.read(buffer)) != -1) {
						fos.write(buffer, 0, len);
						init += len;
						int pct = (int) ((float) init / (float) total * 100);

						if (pct - lastPercent >= 1) {
							Message msg = handler.obtainMessage();
							msg.what = DOWNLOADING;
							msg.obj = pct + "";
							handler.sendMessage(msg);
							lastPercent = pct;
						}
					}
					fos.flush();
					handler.sendEmptyMessage(DOWNLOAD_OK);
				} catch (FileNotFoundException e) {
					throw new FileNotFoundException();
				} finally {
					try {
						fin.close();
						fos.close();
					} catch (IOException e) {
						throw new IOException();
					}
				}
			}
		} catch (Exception e) {
		}
	}

	private String getApkFilePath(String fileName) {
		String temp = Environment.getExternalStorageDirectory().toString() + "/tem";
		File file = new File(temp);
		if (!file.exists()) {
			file.mkdirs();
		}
		return temp + "/" + fileName;
	}

	private Handler handler = new Handler() {
		@SuppressWarnings("deprecation")
		public void handleMessage(Message msg) {
			try {
				if (DOWNLOADING == msg.what) {
					if (null != msg.obj) {
						mNotification.tickerText = "正在下载更新...";
						mNotification.icon = android.R.drawable.stat_sys_download;
						mNotification.contentView = mRemoteViews;
						mNotification.contentView.setTextViewText(R.id.load_percent, msg.obj + "%");
						mNotification.contentView.setProgressBar(R.id.load_progress, 100,
								Integer.valueOf((String) msg.obj), false);
						Intent intent0 = new Intent();
						mNotification.contentIntent = PendingIntent.getActivity(UpdateLoadService.this, 
										NOTIFICATION_ID, intent0, PendingIntent.FLAG_UPDATE_CURRENT);
						mNotificationManager.notify(NOTIFICATION_ID, mNotification);
					}
				} else {
					switch (msg.what) {
					case NETWORK_ERR:
						mNotification.tickerText = "网络错误，请稍后再试�?";
						mNotification.icon = android.R.drawable.stat_sys_warning;
						Intent intent0 = new Intent();
						mNotification.contentIntent = PendingIntent.getActivity(UpdateLoadService.this, 0,
										intent0, 0);
						mNotification.setLatestEventInfo(
								UpdateLoadService.this, "提示", "网络错误，请稍后再试 ?", mPendingIntent);
						mNotificationManager.cancel(NOTIFICATION_ID);
						mNotificationManager.notify(NOTIFICATION_ID, mNotification);
						break;

					case DOWNLOAD_ERR:
						mNotification.tickerText = "下载更新失败，请稍后再试?";
						mNotification.icon = android.R.drawable.stat_sys_warning;
						Intent intent = new Intent();
						mNotification.contentIntent = PendingIntent
								.getActivity(UpdateLoadService.this, 0, intent, 0);
						mNotification.setLatestEventInfo(
								UpdateLoadService.this, "提示", "下载更新失败，请稍后再试?", mPendingIntent);
						mNotificationManager.cancel(NOTIFICATION_ID);
						mNotificationManager.notify(NOTIFICATION_ID, mNotification);
						break;

					case DOWNLOAD_OK:
						mNotification.tickerText = "更新成功，点击进行安装！";
						mNotification.icon = android.R.drawable.stat_sys_download_done;
						Intent intent1 = new Intent();
						intent1.setAction(android.content.Intent.ACTION_VIEW);
						intent1.setDataAndType(
								Uri.parse("file://" + getApkFilePath(mApkName)),
								"application/vnd.android.package-archive");
						intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						mPendingIntent = PendingIntent.getActivity(
								UpdateLoadService.this, 0, intent1, 0);
						mNotification.contentIntent = mPendingIntent;
						mNotification.setLatestEventInfo(UpdateLoadService.this, "提示",
								"下载更新完成，请点击进行安装?", mPendingIntent);
						mNotificationManager.cancel(NOTIFICATION_ID);
						mNotificationManager.notify(NOTIFICATION_ID, mNotification);

						break;
					default:
						break;

					}
					UpdateLoadService.this.stopSelf();
				}
			} catch (NumberFormatException e) {
			}
		}
	};

}

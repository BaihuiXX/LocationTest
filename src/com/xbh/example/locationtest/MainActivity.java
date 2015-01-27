package com.xbh.example.locationtest;

import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends Activity {
	protected static final int SHOW_LOCATION = 0;
	private String TAG = "MainActivity";
	Context mContext;
	
	private TextView longitudeTv;
	private TextView latitudeTv;
	private TextView locationNameTv;

	private LocationManager locationManager;
	private String provider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		if (savedInstanceState == null) {
//			getFragmentManager().beginTransaction()
//					.add(R.id.container, new PlaceholderFragment()).commit();
//		}
		
		mContext = getApplicationContext();
		longitudeTv = (TextView) findViewById(R.id.tv_longitude);
		latitudeTv = (TextView) findViewById(R.id.tv_latitude);
		locationNameTv = (TextView) findViewById(R.id.tv_location_name);
		
		//获取位置管理类
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		//获取所有的位置提供器
		List<String> providers = locationManager.getProviders(true);
		Log.d(TAG, "provider size = " + providers.size());
		for (int i = 0; i < providers.size(); ++i) {
			Log.d(TAG, "provider[" + i + "]=" + providers.get(i));
		}
		
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			provider = LocationManager.GPS_PROVIDER;
		} else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			provider = LocationManager.NETWORK_PROVIDER;
			//provider = "network";
		} else {
			Toast.makeText(mContext, "Three is no provider to use!", Toast.LENGTH_SHORT).show();
			return;
		}		
		
		Location location = locationManager.getLastKnownLocation(provider);
		if (location != null) {
			showLocation(location); //将位置信息显示在页面中
		} else {
			Toast.makeText(mContext, "location = null", Toast.LENGTH_SHORT).show();
			
		}
		Log.d(TAG, "provider=" + provider);
		//设置位置变化更新时间、距离、监听类
		while (location == null) {
			locationManager.requestLocationUpdates(provider, 5000, 2, locationListener);
		}
		//locationManager.requestLocationUpdates(provider, 5000, 2, locationListener);
	}
	
	protected void onDestroy() {
		super.onDestroy();
		if (locationManager != null) {
			//关闭程序时将监听器移除
			locationManager.removeUpdates(locationListener);
		}
	}

	/**
	 * 将位置信息显示在页面中
	 * @param location
	 */
	private void showLocation(final Location location) {
		// TODO Auto-generated method stub
		longitudeTv.setText("Longitude:" + location.getLongitude());
		latitudeTv.setText("Latitude:" + location.getLatitude());
		Log.d(TAG, "Longitude:" + location.getLongitude());
		Log.d(TAG, "Latitude:" + location.getLatitude());
		
		new Thread (new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					//组装反向地理编码的接口地址
					StringBuilder url = new StringBuilder();
					url.append("http://maps.googleapis.com/maps/api/geocode/json?latlng=");
					url.append(location.getLatitude());
					url.append(",");
					url.append(location.getLongitude());
					url.append("&sensor=false");
					Log.d(TAG, "url=" + url.toString());
					
					HttpClient httpClient = new DefaultHttpClient();
					HttpGet httpGet = new HttpGet(url.toString());
					//在请求消息头中指定语言，保证服务器会返回中文数据
					httpGet.addHeader("Accept-Language","zh-CN");
					
					HttpResponse httpResponse = httpClient.execute(httpGet);
					Log.d(TAG, "response code=" + httpResponse.getStatusLine().getStatusCode());
					//发送、接收都成功：200
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						HttpEntity entity = httpResponse.getEntity();
						String response = EntityUtils.toString(entity, "utf-8");
						Log.d(TAG, "response="+response);
						JSONObject jsonObject = new JSONObject(response);
						//获取result节点下的位置信息
						JSONArray resultArray = jsonObject.getJSONArray("results");
						if (resultArray.length() > 0) {
							JSONObject subObject = resultArray.getJSONObject(0);
							//获取格式化后的位置信息
							String address = subObject.getString("formatted_address");
							Message msg = new Message();
							msg.what = SHOW_LOCATION;
							msg.obj = address;
							handler.sendMessage(msg);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}).start();
	}
	
	/**
	 * 收到子线程获取的位置信息后显示在ui上
	 */
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case SHOW_LOCATION:
				String address = (String) msg.obj;
				Log.d(TAG, "address name="+address);
				locationNameTv.setText(address);
				break;
			default:
				break;
			}
		}
	};
	
	/**
	 * 位置信息变化监听类
	 */
	LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			//更新当前设备的位置信息
			showLocation(location);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
		
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

//	/**
//	 * A placeholder fragment containing a simple view.
//	 */
//	public static class PlaceholderFragment extends Fragment {
//
//		public PlaceholderFragment() {
//		}
//
//		@Override
//		public View onCreateView(LayoutInflater inflater, ViewGroup container,
//				Bundle savedInstanceState) {
//			View rootView = inflater.inflate(R.layout.fragment_main, container,
//					false);
//			return rootView;
//		}
//	}

}

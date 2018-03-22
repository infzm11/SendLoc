package com.example.sendloc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.unstoppable.submitbuttonview.SubmitButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private static final String SENT_SMS_ACTION = "demo_sms_send_action";
    private static final String KEY_PHONENUM = "phone_num";

    //用户手中手机的手机号码
    public  String userPhoneNum="+8618633211746";
    //车上手机的手机号码
    public  String carPhoneNum="+8615230827851";

    private LocationManager locationManager;
    private String locationProvider;
    private ImageView sendMs;
    Location location;
    String locationStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        setContentView(R.layout.activity_main);
        sendMs = (ImageView) findViewById(R.id.sendMs);


        //定位
        MainActivityPermissionsDispatcher.findLocWithPermissionCheck(MainActivity.this);
        sendMs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,SecondActivity.class);
                startActivity(intent);

                //车内手机的电话号码
                String phone = carPhoneNum;
                //用户的经纬度信息
                String content = locationStr;
                MainActivityPermissionsDispatcher.sendSMSWithPermissionCheck(MainActivity.this,phone,content);
            }
        });

        mReceiver = new SMSSendResultReceiver();
        IntentFilter filter = new IntentFilter(SENT_SMS_ACTION);
        registerReceiver(mReceiver, filter);

        //读取信息
        smsReceiver= new SmsReceiver();
        IntentFilter filter1 = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter1);

    }
    @NeedsPermission({Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION})
    public void findLoc(){
        //获取地理位置管理器
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            //如果是GPS
            locationProvider = LocationManager.GPS_PROVIDER;
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            //如果是Network
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
            return;
        }
        //获取Location
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.
                ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "onCreate: 没有位置权限 ");
            return;
        }
         location = locationManager.getLastKnownLocation(locationProvider);
        locationManager.requestLocationUpdates(locationProvider, 30000, 50,
                locationListener);
        if(location != null){
            locationStr = "维度：" + location.getLatitude() +"\n"
                    + "经度：" + location.getLongitude();
            Log.i("lat,lon",locationStr);
        }
    }
    /**
     * 方位改变时触发，进行调用
     */
    private final LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            MainActivity.this.location=location;
            locationStr = "维度：" + location.getLatitude() +"\n"
                    + "经度：" + location.getLongitude();
        }
        public void onProviderDisabled(String provider) {
        }
        public void onProviderEnabled(String provider) {
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };
    /**
     * 直接调用短信接口发短信    如果群发可以循环调用
     * @param phoneNumber 手机号码
     * @param message 短信内容
     */
    @NeedsPermission(Manifest.permission.SEND_SMS)
    public void sendSMS(String phoneNumber,String message){
        //获取短信管理器
        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
        //拆分短信内容（手机短信长度限制）
        List<String> divideContents = smsManager.divideMessage(message);
        Intent itSend = new Intent(SENT_SMS_ACTION);
        itSend.putExtra(KEY_PHONENUM, phoneNumber);
        PendingIntent sentPI = PendingIntent.getBroadcast(getApplicationContext(), 0,
                itSend, PendingIntent.FLAG_UPDATE_CURRENT);
        for (String text : divideContents) {
            smsManager.sendTextMessage(phoneNumber, null, text, sentPI, null);
        }
    }
    SMSSendResultReceiver mReceiver;
    public class SMSSendResultReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (SENT_SMS_ACTION.equals(intent.getAction())) {
                String phoneNum = intent.getStringExtra(KEY_PHONENUM);
                switch(getResultCode())
                {
                    case Activity.RESULT_OK:
                        // 发送成功
                        Toast.makeText(context, "Send Message to "+phoneNum+" success!",
                                Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    default:
                        // 发送失败
                        Toast.makeText(context, "Send Message to "+phoneNum+" fail!",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }

        }
    }

    SmsReceiver smsReceiver;

    public class SmsReceiver extends BroadcastReceiver{

        @Override

        public void onReceive(Context context, Intent intent) {
            MainActivityPermissionsDispatcher.readAndReceiveSmsWithPermissionCheck(MainActivity.this,
                    context,intent);
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    @NeedsPermission({Manifest.permission.READ_SMS,Manifest.permission.RECEIVE_SMS})
    public void readAndReceiveSms(Context context,Intent intent){
        Bundle bundle = intent.getExtras();
        SmsMessage msg = null;
        if (null != bundle) {
            Object[] smsObj = (Object[]) bundle.get("pdus");
            for (Object object : smsObj) {
                msg = SmsMessage.createFromPdu((byte[])object,intent.getStringExtra("format"));
                Date date = new Date(msg.getTimestampMillis());
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String receiveTime = format.format(date);
                System.out.println("number:" + msg.getOriginatingAddress()
                        + "   body:" + msg.getDisplayMessageBody() + "  time:"
                        + receiveTime);

                //在这里写自己的逻辑
                //msg.getOriginatingAddress()是发信人的电话号码，需要修改
                if (msg.getOriginatingAddress().equals(userPhoneNum)){
                }

            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        if(smsReceiver !=null){
            unregisterReceiver(smsReceiver);
        }
    }
}

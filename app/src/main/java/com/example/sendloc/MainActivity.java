package com.example.sendloc;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String SENT_SMS_ACTION = "demo_sms_send_action";
    private static final String KEY_PHONENUM = "phone_num";
    //can总线返回的信息标准
    public static final String CAN_INFO_RIGTH="123";
    //用户手中手机的手机号码
    public static final String userPhoneNum="+8618633211746";
    //车上手机的手机号码
    public static final String carPhoneNum="+8615230827851";
    //用户的地理位置
    private String userLoc;
    private LocationManager locationManager;
    private String locationProvider;
    private Button sendMs;
    public TextView tv;
    String locationStr;
    //是否收到来自用户的短信
    private int userReceive=0;
    //是否收到来自车的短信
    private int carReceive=0;
    //是否接收到can总线的信息(0表示没有接收到，1代表接收到了）
    private int canReceive=0;
    //can总结返回来的信息
    private String canInfo="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendMs = (Button) findViewById(R.id.sendMs);
        tv = (TextView) findViewById(R.id.tv);
        //定位
        findLoc();
        sendMs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //车内手机的电话号码
                String phone = userPhoneNum;
                //用户的经纬度信息
                String content = carPhoneNum;
                sendSMS(phone,content);
            }
        });

        if(!TextUtils.isEmpty(canInfo)){
            if(canInfo.equals(CAN_INFO_RIGTH)){
                Toast.makeText(this,"信息成功发送",Toast.LENGTH_LONG);
            }else {
                //车内手机的电话号码
                String phone = userPhoneNum;
                //用户的经纬度信息
                String content = carPhoneNum;
                sendSMS(phone,content);
            }
            if(canReceive==1){
                //用户手机的电话号码
                String phone ="18633211746";
                //can总线的信息
                String content = canInfo;
                sendSMS(phone,content);
                canReceive=0;
            }
        }
        //信息发送成败监控
        mReceiver = new SMSSendResultReceiver();
        IntentFilter filter = new IntentFilter(SENT_SMS_ACTION);
        registerReceiver(mReceiver, filter);

        //读取信息
        smsReceiver= new SmsReceiver();
        IntentFilter filter1 = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter1);

    }
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
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "onCreate: 没有权限 ");
            return;
        }
        Location location = locationManager.getLastKnownLocation(locationProvider);
        if(location != null){
            locationStr = "维度：" + location.getLatitude() +"\n"
                    + "经度：" + location.getLongitude();
            Log.i("lat,lon",locationStr);
        }
    }
    /**
     * 直接调用短信接口发短信    如果群发可以循环调用
     * @param phoneNumber
     * @param message
     */
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
                        Toast.makeText(context, "Send Message to "+phoneNum+" success!", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    default:
                        // 发送失败
                        Toast.makeText(context, "Send Message to "+phoneNum+" fail!", Toast.LENGTH_LONG).show();
                        break;
                }
            }

        }
    }
    SmsReceiver smsReceiver;
    public class SmsReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            SmsMessage msg = null;
            if (null != bundle) {
                Object[] smsObj = (Object[]) bundle.get("pdus");
                for (Object object : smsObj) {
                    msg = SmsMessage.createFromPdu((byte[]) object);
                    Date date = new Date(msg.getTimestampMillis());//时间
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String receiveTime = format.format(date);
                    System.out.println("number:" + msg.getOriginatingAddress()
                            + "   body:" + msg.getDisplayMessageBody() + "  time:"
                            + msg.getTimestampMillis());

                    //在这里写自己的逻辑
                    //msg.getOriginatingAddress()是发信人的电话号码，需要修改
                    if (msg.getOriginatingAddress().equals(userPhoneNum)){
                        userLoc=msg.getDisplayMessageBody();
                    }
                    if(msg.getOriginatingAddress().equals(carPhoneNum)){
                        carReceive=1;
                        canInfo=msg.getDisplayMessageBody();
                    }

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

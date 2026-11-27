package dev.fieldsql.app;
import android.app.*;
import android.os.*;
import android.content.*;
public final class ConnectionService extends Service {
 public int onStartCommand(Intent i,int flags,int id){
   String channel="active-connections"; NotificationManager m=getSystemService(NotificationManager.class);
   if(Build.VERSION.SDK_INT>=26)m.createNotificationChannel(new NotificationChannel(channel,"数据库连接",NotificationManager.IMPORTANCE_LOW));
   Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);
   PendingIntent p=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
   startForeground(7,n.setSmallIcon(R.drawable.ic_fieldsql).setContentTitle("FieldSQL · 数据库连接中").setContentText("点按返回工作台；断开连接后停止").setContentIntent(p).setOngoing(true).build());
   return START_NOT_STICKY;
 }
 public IBinder onBind(Intent i){return null;}
 public void onTimeout(int startId,int foregroundServiceType){stopSelf(startId);}
}

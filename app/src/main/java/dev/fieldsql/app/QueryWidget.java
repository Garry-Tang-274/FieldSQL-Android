package dev.fieldsql.app;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
public final class QueryWidget extends AppWidgetProvider{
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,QueryWidget.class));m.notifyAppWidgetViewDataChanged(ids,R.id.widget_profiles);}
 public void onUpdate(Context c,AppWidgetManager m,int[] ids){
   for(int id:ids){
     RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
     v.setTextViewText(R.id.widget_label,"数据库连接  →");
     Intent open=new Intent(c,MainActivity.class).putExtra("connections",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
     v.setOnClickPendingIntent(R.id.widget_label,PendingIntent.getActivity(c,17,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
     v.setRemoteAdapter(R.id.widget_profiles,new Intent(c,WidgetList.class));
     v.setPendingIntentTemplate(R.id.widget_profiles,PendingIntent.getActivity(c,18,open,PendingIntent.FLAG_UPDATE_CURRENT|(android.os.Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0)));
     m.updateAppWidget(id,v);
   }
 }
}

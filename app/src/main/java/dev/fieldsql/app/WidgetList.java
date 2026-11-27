package dev.fieldsql.app;
import android.content.Intent;
import android.widget.*;
import org.json.*;
import java.util.*;
public final class WidgetList extends RemoteViewsService {
 public RemoteViewsFactory onGetViewFactory(Intent intent){
   return new RemoteViewsFactory(){
     final List<String> lines=new ArrayList<>();
     public void onCreate(){onDataSetChanged();}
     public void onDataSetChanged(){lines.clear();try{JSONObject all=new JSONObject(new Vault(WidgetList.this).read("profiles","{}"));Iterator<String> it=all.keys();while(it.hasNext()){JSONObject p=all.getJSONObject(it.next());lines.add(p.optString("name")+"  ·  "+p.optString("host")+"  /  "+p.optString("engine").toUpperCase(Locale.ROOT));}}catch(Exception ignored){}}
     public void onDestroy(){lines.clear();}
     public int getCount(){return lines.size();}
     public RemoteViews getViewAt(int n){if(n<0||n>=lines.size())return null;RemoteViews v=new RemoteViews(getPackageName(),R.layout.widget_row);v.setTextViewText(R.id.widget_profile_name,lines.get(n));v.setOnClickFillInIntent(R.id.widget_profile_name,new Intent());return v;}
     public RemoteViews getLoadingView(){return null;}public int getViewTypeCount(){return 1;}public long getItemId(int n){return n;}public boolean hasStableIds(){return false;}
   };
 }
}

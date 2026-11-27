package dev.fieldsql.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import java.util.*;

final class LocalDb extends BaseDb {
    private final SQLiteDatabase db;
    LocalDb(SQLiteDatabase d){super("sqlite");db=d; db.setForeignKeyConstraintsEnabled(true);}
    public List<String> tables(){
        List<String> r=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type IN ('table','view') AND substr(name,1,7) != 'sqlite_' AND name != 'android_metadata' ORDER BY name COLLATE NOCASE",null)){
            while(c.moveToNext())r.add(c.getString(0));
        } return r;
    }
    public Result describe(String table){
        List<List<String>> r=new ArrayList<>();
        try(Cursor c=db.rawQuery("PRAGMA table_info("+q(table)+")",null)){
            while(c.moveToNext())r.add(Arrays.asList("column",c.getString(1),c.getString(2),c.getInt(3)==0?"YES":"NO",
                 c.getInt(5)>0?c.getString(5):null,c.getString(4)));
        }
        try(Cursor c=db.rawQuery("PRAGMA index_list("+q(table)+")",null)){
            while(c.moveToNext()){
                List<String> cols=new ArrayList<>();String idx=c.getString(1);
                try(Cursor i=db.rawQuery("PRAGMA index_info("+q(idx)+")",null)){while(i.moveToNext())cols.add(i.isNull(2)?"(expression)":i.getString(2));}
                r.add(Arrays.asList("index",idx,c.getInt(2)!=0?"UNIQUE":"INDEX",null,null,String.join(", ",cols)));
            }
        }return new Result(descCols(),r,-1,false);
    }
    public Result execute(String sql,int n){
        limit(n);sql=validSql(sql);
        SqlText text=new SqlText(sql,"sqlite");sql=text.single;
        try(Cursor c=db.rawQuery(sql,null)){
            if(c.getColumnCount()>0){
                if(text.words.contains("RETURNING") && (text.words.contains("INSERT")||text.words.contains("UPDATE")||text.words.contains("DELETE")||text.words.contains("REPLACE")))
                    throw new IllegalArgumentException("本地 SQLite 编辑请去掉 RETURNING，再单独 SELECT 查看写入结果");
                List<String> names=Arrays.asList(c.getColumnNames()); List<List<String>> rows=new ArrayList<>(),types=new ArrayList<>();
                while(rows.size()<n && c.moveToNext()){
                    List<String> row=new ArrayList<>(),rt=new ArrayList<>();
                    for(int i=0;i<names.size();i++){int t=c.getType(i);rt.add(t==Cursor.FIELD_TYPE_INTEGER||t==Cursor.FIELD_TYPE_FLOAT?"N":"S");row.add(t==Cursor.FIELD_TYPE_NULL?null:t==Cursor.FIELD_TYPE_BLOB?bytes(c.getBlob(i)):clip(c.getString(i)));}rows.add(row);types.add(rt);
                }
                return new Result(names,rows,-1,rows.size()==n && c.moveToNext(),types);
            }
        }
        String op=text.words.isEmpty()?"":text.words.get(0);
        if(op.equals("INSERT")||op.equals("REPLACE")||op.equals("DELETE")||op.equals("UPDATE")){
            try(SQLiteStatement stmt=db.compileStatement(sql)){return Result.count(stmt.executeUpdateDelete());}
        }
        db.execSQL(sql);return Result.count(-1);
    }
    public int update(String table, Map<String,String> values,Map<String,String> key) throws Exception {
        List<String> keys=checkEdits(table,values,key),cols=new ArrayList<>(values.keySet());
        try(SQLiteStatement s=db.compileStatement(updateSql(table,cols,keys))){
            int i=1;for(String col:cols){String v=values.get(col);if(v==null)s.bindNull(i++);else s.bindString(i++,v);}
            for(String k:keys)s.bindString(i++,key.get(k));return s.executeUpdateDelete();
        }
    }
    public void close(){db.close();}
}


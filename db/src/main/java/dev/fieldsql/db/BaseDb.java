package dev.fieldsql.db;

import java.util.*;

abstract class BaseDb implements Database {
    static final int MAX_ROWS=10000, MAX_CELL=32768;
    final String dialect;
    BaseDb(String d){dialect=d;}
    static void limit(int n){if(n<1||n>MAX_ROWS)throw new IllegalArgumentException("行数须在 1–10000 之间");}
    String q(String name){
        if(name==null||name.isEmpty()||name.indexOf(0)>=0) throw new IllegalArgumentException("无效的标识符");
        String c=dialect.equals("mysql")?"`":"\"";
        return c+name.replace(c,c+c)+c;
    }
    static String clip(String s){if(s!=null&&s.length()>MAX_CELL)throw new IllegalArgumentException("单元格超过 32768 字符。请选择部分列，或用 substr() 选取片段");return s;}
    static String bytes(byte[] b){
        int n=Math.min(b.length,128); char[] out=new char[n*2],hex="0123456789ABCDEF".toCharArray();
        for(int i=0;i<n;i++){out[2*i]=hex[(b[i]&255)>>>4];out[2*i+1]=hex[b[i]&15];}
        return "⟪BLOB:"+b.length+" bytes · 0x"+new String(out)+(b.length>n?"…":"")+"⟫";
    }
    static List<String> descCols(){return Arrays.asList("kind","name","type","nullable","primary","detail");}
    final List<String> checkEdits(String table,Map<String,String> values, Map<String,String> key) throws Exception{
        if(values==null||values.isEmpty()||key==null||key.isEmpty())
            throw new IllegalArgumentException("编辑一行需要新值和完整的非空主键");
        for(String v:key.values())if(v==null||v.startsWith("⟪BLOB:"))throw new IllegalArgumentException("主键必须完整；BLOB 预览不能作为主键"); for(String v:values.values())if(v!=null&&v.startsWith("⟪BLOB:"))throw new IllegalArgumentException("二进制预览不能作为编辑值，请使用 SQL");
        Set<String> columns=new HashSet<>(),pk=new HashSet<>();
        for(List<String> r:describe(table).rows) if("column".equals(r.get(0))){
            columns.add(r.get(1));if(r.get(4)!=null)pk.add(r.get(1));
        }
        if(!columns.containsAll(values.keySet())||!pk.equals(key.keySet()))
            throw new IllegalArgumentException("字段不存在或未提供完整主键");
        return new ArrayList<>(key.keySet());
    }
    final String updateSql(String table, List<String> columns,List<String> keys){
        List<String> sets=new ArrayList<>(),where=new ArrayList<>();
        for(String v:columns)sets.add(q(v)+"=?");for(String k:keys)where.add(q(k)+"=?");
        return "UPDATE "+q(table)+" SET "+String.join(", ",sets)+" WHERE "+String.join(" AND ",where);
    }
    public Result browse(String table,int offset,int n) throws Exception {
        limit(n); if(offset<0)throw new IllegalArgumentException("偏移须非负数");
        TreeMap<Integer,String> pk=new TreeMap<>();
        for(List<String> r:describe(table).rows)if("column".equals(r.get(0)) && r.get(4)!=null)pk.put(Integer.parseInt(r.get(4)),q(r.get(1)));
        return execute("SELECT * FROM "+q(table)+(pk.isEmpty()?"":" ORDER BY "+String.join(",",pk.values()))+" LIMIT "+(n+1)+" OFFSET "+offset,n);
    }
    static String validSql(String sql){
        if(sql==null || sql.trim().isEmpty()) throw new IllegalArgumentException("请输入 SQL");
        return sql.trim();
    }
}


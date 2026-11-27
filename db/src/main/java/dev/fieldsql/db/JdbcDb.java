package dev.fieldsql.db;

import java.sql.*;
import java.util.*;
import com.jcraft.jsch.Session;

final class JdbcDb extends BaseDb {
    final Connection conn; final Session ssh;
    interface Op<T>{T run()throws SQLException;}
    interface OpenAux {Connection open()throws Exception;}
    final OpenAux openAux; final long connectionId;
    private int timeoutSeconds=60;
    static final java.util.concurrent.ScheduledThreadPoolExecutor TIMER=new java.util.concurrent.ScheduledThreadPoolExecutor(
        2,r->{Thread t=new Thread(r,"fieldsql-query-deadline");t.setDaemon(true);return t;});
    static{TIMER.setRemoveOnCancelPolicy(true);}
    JdbcDb(String d,Connection c,Session s,OpenAux aux)throws SQLException{
        super(d);conn=c;ssh=s;openAux=aux;
        if(d.equals("postgres")){connectionId=0;setQueryTimeoutSeconds(60);}
        else {try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT CONNECTION_ID()")){r.next();connectionId=r.getLong(1);}}
    }
    public void setQueryTimeoutSeconds(int seconds)throws SQLException{
        if(seconds<1||seconds>3600)throw new IllegalArgumentException("查询限时须在 1–3600 秒之间");
        if(dialect.equals("postgres")){try(Statement s=conn.createStatement()){s.execute("SET statement_timeout = "+(seconds*1000));}}
        timeoutSeconds=seconds;
    }
    /** MySQL cancels on a separate authenticated connection. Use the same TLS host and SSH
     * session as the query, not the socket's loopback address chosen by the generic JDBC timer. */
    private <T>T guarded(Op<T> op)throws SQLException{
        if(dialect.equals("postgres"))return op.run();
        final Object guard=new Object();final boolean[] state={true,false};
        java.util.concurrent.ScheduledFuture<?> f=TIMER.schedule(()->{
            synchronized(guard){if(!state[0])return;}
            try(Connection ctrl=openAux.open()){
                synchronized(guard){if(!state[0])return;
                    state[1]=true;
                    try(Statement q=ctrl.createStatement()){q.execute("KILL QUERY "+connectionId);}
                }
            }catch(Exception e){
                synchronized(guard){if(!state[0])return;state[1]=true;
                    // If the control connection cannot reach the server, stop waiting on the actual query.
                    if(ssh!=null)ssh.disconnect();
                    try{((org.mariadb.jdbc.MariaDbConnection)conn).abort(Runnable::run);}catch(Exception ignored){}
                }
            }
        },timeoutSeconds,java.util.concurrent.TimeUnit.SECONDS);
        try{T result=op.run();
            synchronized(guard){state[0]=false;if(state[1])throw new SQLTimeoutException("已达到查询限时；执行写操作时请先核实数据");}
            return result;
        }finally{synchronized(guard){state[0]=false;}f.cancel(false);}
    }
    private static String displayType(int sqlType,int precision){
        switch(sqlType){
            case Types.TINYINT:case Types.SMALLINT:case Types.INTEGER:case Types.BIGINT:
            case Types.NUMERIC:case Types.DECIMAL:case Types.FLOAT:case Types.REAL:case Types.DOUBLE:return "N";
            case Types.BOOLEAN:return "B";
            case Types.BIT:return precision==1?"B":"S";
            default:return "S";
        }
    }
    static Result collect(ResultSet rs,int n)throws SQLException{
        ResultSetMetaData md=rs.getMetaData();int w=md.getColumnCount();List<String> names=new ArrayList<>();
                int[] sqlTypes=new int[w];String[] columnTypes=new String[w];
        for(int i=1;i<=w;i++){names.add(md.getColumnLabel(i));int t=sqlTypes[i-1]=md.getColumnType(i);
            columnTypes[i-1]=displayType(t,t==Types.BIT?md.getPrecision(i):0);}
        List<List<String>> rows=new ArrayList<>(),types=new ArrayList<>();
        while(rows.size()<n && rs.next()){List<String> r=new ArrayList<>(),rt=new ArrayList<>();
            for(int i=1;i<=w;i++){
                int t=sqlTypes[i-1]; if(t==Types.BINARY||t==Types.BLOB||t==Types.LONGVARBINARY||t==Types.VARBINARY){byte[] b=rs.getBytes(i);r.add(b==null?null:bytes(b));}
                else r.add(clip(rs.getString(i)));
                rt.add(r.get(i-1)==null?"S":columnTypes[i-1]);
            }rows.add(r);types.add(rt);
        }
        return new Result(names,rows,-1,rows.size()==n&&rs.next(),types);
    }
    public Result execute(String sql,int n)throws SQLException{
        limit(n);String single=new SqlText(validSql(sql),dialect).single;return guarded(()->runSql(single,n));
    }
    private Result runSql(String sql,int n)throws SQLException {
        try(Statement s=conn.createStatement()){s.setMaxRows(n+1);
            if(s.execute(sql)){try(ResultSet rs=s.getResultSet()){return collect(rs,n);}}
            return Result.count(s.getUpdateCount());
        }
    }
    public List<String> tables()throws SQLException{return guarded(this::listTables);}
    private List<String> listTables()throws SQLException{
        String sql=dialect.equals("postgres")?
            "SELECT c.relname FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace s ON c.relnamespace=s.oid WHERE s.nspname=current_schema() AND c.relkind IN ('r','p','v','m','f') ORDER BY c.relname":
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME";
        List<String> names=new ArrayList<>();try(Statement s=conn.createStatement();ResultSet r=s.executeQuery(sql)){while(r.next())names.add(r.getString(1));}return names;
    }
    public Result describe(String table)throws SQLException {return guarded(()->tableDetails(table));}
    private Result tableDetails(String table)throws SQLException {
        boolean pg=dialect.equals("postgres");
        String columns=pg?
            "SELECT a.attname, pg_catalog.format_type(a.atttypid,a.atttypmod), CASE WHEN a.attnotnull THEN 'NO' ELSE 'YES' END, pk.pos, pg_get_expr(d.adbin,d.adrelid) FROM pg_catalog.pg_attribute a JOIN pg_catalog.pg_class c ON c.oid=a.attrelid JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace LEFT JOIN pg_catalog.pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum LEFT JOIN LATERAL (SELECT k.pos FROM pg_catalog.pg_index ix,unnest(ix.indkey) WITH ORDINALITY k(num,pos) WHERE ix.indrelid=c.oid AND ix.indisprimary AND k.num=a.attnum AND k.pos<=ix.indnkeyatts) pk ON true WHERE n.nspname=current_schema() AND c.relname=? AND a.attnum>0 AND NOT a.attisdropped ORDER BY a.attnum":
            "SELECT c.COLUMN_NAME,c.COLUMN_TYPE,c.IS_NULLABLE,pk.ORDINAL_POSITION,c.COLUMN_DEFAULT FROM information_schema.COLUMNS c LEFT JOIN information_schema.KEY_COLUMN_USAGE pk ON pk.TABLE_SCHEMA=c.TABLE_SCHEMA AND pk.TABLE_NAME=c.TABLE_NAME AND pk.COLUMN_NAME=c.COLUMN_NAME AND pk.CONSTRAINT_NAME='PRIMARY' WHERE c.TABLE_SCHEMA=DATABASE() AND c.TABLE_NAME=? ORDER BY c.ORDINAL_POSITION";
        String indexes=pg?
            "SELECT i.relname, CASE WHEN ix.indisunique THEN 'UNIQUE' ELSE 'INDEX' END, pg_get_indexdef(i.oid) FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace JOIN pg_catalog.pg_index ix ON ix.indrelid=c.oid JOIN pg_catalog.pg_class i ON i.oid=ix.indexrelid WHERE n.nspname=current_schema() AND c.relname=? ORDER BY i.relname":
            "SELECT INDEX_NAME,CASE WHEN MIN(NON_UNIQUE)=0 THEN 'UNIQUE' ELSE 'INDEX' END, GROUP_CONCAT(COALESCE(COLUMN_NAME,'(expression)') ORDER BY SEQ_IN_INDEX SEPARATOR ', ') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? GROUP BY INDEX_NAME ORDER BY INDEX_NAME";
        List<List<String>> rows=new ArrayList<>();
        try(PreparedStatement s=conn.prepareStatement(columns)){s.setString(1,table);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(Arrays.asList("column",r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));}}
        try(PreparedStatement s=conn.prepareStatement(indexes)){s.setString(1,table);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(Arrays.asList("index",r.getString(1),r.getString(2),null,null,r.getString(3)));}}
        return new Result(descCols(),rows,-1,false);
    }
    public int update(String table,Map<String,String> values,Map<String,String> key)throws Exception{
        List<String> keys=checkEdits(table,values,key),cols=new ArrayList<>(values.keySet());
        try(PreparedStatement s=conn.prepareStatement(updateSql(table,cols,keys))){
            int i=1;for(String col:cols)s.setString(i++,values.get(col));for(String k:keys)s.setString(i++,key.get(k));return guarded(s::executeUpdate);
        }
    }
    public void close() throws SQLException{try{conn.close();}finally{if(ssh!=null)ssh.disconnect();}}
}





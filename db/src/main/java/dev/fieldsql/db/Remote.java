package dev.fieldsql.db;

import com.jcraft.jsch.*;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

final class Remote {
    static String host(String h) throws Exception{
        if(h==null||h.trim().isEmpty())throw new IllegalArgumentException("请输入主机名");h=h.trim();
        if(h.startsWith("[")&&h.endsWith("]"))h=h.substring(1,h.length()-1);
        if(h.contains(":")){
            if(!h.matches("[A-Fa-f0-9:.]+"))throw new IllegalArgumentException("IPv6 地址无效");
            InetAddress.getByName(h);return h;
        }
        String s=IDN.toASCII(h,IDN.USE_STD3_ASCII_RULES);
        if(s.length()>253||s.isEmpty())throw new IllegalArgumentException("主机名无效");return s;
    }
    static void port(int p){if(p<1||p>65535)throw new IllegalArgumentException("端口须在 1–65535 之间");}
    static Database open(String kind,String host,int port,String database,String user,String password,boolean tls,Databases.SshConfig sc)throws Exception{
        String k=kind==null?"":kind.toLowerCase(Locale.ROOT);boolean pg=k.equals("postgres")||k.equals("postgresql");
        if(!pg&&!k.equals("mysql")&&!k.equals("mariadb"))throw new IllegalArgumentException("请选择 PostgreSQL、MySQL 或 MariaDB");
        String dest=host(host);port(port);
        if(database==null||database.isEmpty()||database.matches("(?s).*[\\p{Cntrl}/?#%;].*")||user==null||user.isEmpty())
            throw new IllegalArgumentException("请输入数据库和用户名；库名不能包含路径或 URL 控制符");
        String jdbcHost=dest.contains(":")?"["+dest+"]":dest;
        Properties p=new Properties();p.setProperty("user",user);p.setProperty("password",password==null?"":password);
        p.setProperty("connectTimeout",pg?"10":"10000");p.setProperty("socketTimeout",pg?"3615":"3615000");
        String url;
        if(pg){url="jdbc:postgresql://"+jdbcHost+":"+port+"/"+URLEncoder.encode(database,"UTF-8").replace("+","%20");
            p.setProperty("sslmode",tls?"verify-full":"disable");
            if(tls)p.setProperty("sslfactory","org.postgresql.ssl.DefaultJavaSSLFactory"); // Android/Java trust store
            p.setProperty("stringtype","unspecified");p.setProperty("gssEncMode","disable");
        }else{url="jdbc:mariadb://"+jdbcHost+":"+port+"/"+database;
            p.setProperty("useSsl",Boolean.toString(tls));p.setProperty("trustServerCertificate","false");
            p.setProperty("disableSslHostnameVerification","false");p.setProperty("allowLocalInfile","false");
            p.setProperty("allowMultiQueries","false");p.setProperty("allowPublicKeyRetrieval","false");p.setProperty("autoReconnect","false");
        }
        Session session=null;Connection c=null;
        try{
            if(sc!=null)session=ssh(sc);
            final Session route=session;
            Connector driver=pg?new org.postgresql.Driver()::connect:
                    (address,settings)->org.mariadb.jdbc.MariaDbConnection.newConnection(org.mariadb.jdbc.UrlParser.parse(address,settings),null);
            // Use the same connector factory as MariaDB Driver.connect. Its static DriverManager
            // registration requires java.sql.DriverAction, which Android does not provide.
            JdbcDb.OpenAux dial=()->openSocket(driver,url,p,route,dest,port);
            c=dial.open();
            JdbcDb result=new JdbcDb(pg?"postgres":"mysql",c,route,dial);session=null;c=null;return result;
        }finally{try{if(c!=null)c.close();}finally{if(session!=null)session.disconnect();}}
    }
    private interface Connector {Connection connect(String url,Properties props)throws SQLException;}
    private static Connection openSocket(Connector driver,String url,Properties original,Session session,String dest,int port)throws Exception{
        Properties p=new Properties();p.putAll(original);
        int listener=-1;Connection c=null;
        try{
            if(session!=null){listener=session.setPortForwardingL("127.0.0.1",0,dest,port,null,10000);
                TunnelSocketFactory.PORT.set(listener);p.setProperty("socketFactory",TunnelSocketFactory.class.getName());}
            c=driver.connect(url,p);if(c==null)throw new SQLException("驱动未接受数据库地址");
            if(listener!=-1){session.delPortForwardingL("127.0.0.1",listener);listener=-1;}
            Connection result=c;c=null;return result;
        }finally{
            TunnelSocketFactory.PORT.remove();
            try{if(c!=null)c.close();}finally{if(listener!=-1)try{session.delPortForwardingL("127.0.0.1",listener);}catch(JSchException ignored){}}
        }
    }
    private static Session ssh(Databases.SshConfig s)throws Exception{
        String h=host(s.host);port(s.port);
        if(s.user==null||s.user.isEmpty()||s.privateKey==null||s.privateKey.length==0)throw new IllegalArgumentException("需要 SSH 用户名和私钥");
        String fp=s.hostKeySha256==null?"":s.hostKeySha256.trim();
        if(!fp.startsWith("SHA256:"))throw new IllegalArgumentException("请填入管理员提供的 SHA256:… 主机公钥指纹");
        byte[] expected=Base64.getDecoder().decode(fp.substring(7));if(expected.length!=32)throw new IllegalArgumentException("SSH 主机指纹不是 SHA-256");
        JSch js=new JSch();
        js.setHostKeyRepository(new HostKeyRepository(){
            public int check(String ignored,byte[] key){try{return MessageDigest.isEqual(expected,MessageDigest.getInstance("SHA-256").digest(key))?OK:CHANGED;}catch(Exception e){return CHANGED;}}
            public void add(HostKey k,UserInfo u){throw new UnsupportedOperationException();}
            public void remove(String h,String t){}public void remove(String h,String t,byte[] k){}
            public String getKnownHostsRepositoryID(){return "Pinned SSH host key";}
            public HostKey[] getHostKey(){return new HostKey[0];}public HostKey[] getHostKey(String h,String t){return getHostKey();}
        });
        byte[] key=s.privateKey.clone(),pass=s.keyPassphrase==null?null:s.keyPassphrase.clone();Session session=null;
        try{
            js.addIdentity("fieldsql-session",key,null,pass);session=js.getSession(s.user,h,s.port);
            session.setConfig("StrictHostKeyChecking","yes");session.setConfig("PreferredAuthentications","publickey");session.setDaemonThread(true);
            session.setServerAliveInterval(30000);session.setServerAliveCountMax(2);session.connect(12000);return session;
        }catch(Exception e){if(session!=null)session.disconnect();throw e;}
        finally{Arrays.fill(key,(byte)0);if(pass!=null)Arrays.fill(pass,(byte)0);js.removeAllIdentity();}
    }
}




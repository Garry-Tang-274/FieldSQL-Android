package dev.fieldsql.db;

import javax.net.SocketFactory;
import java.net.*;
import java.io.*;
/** Constructed by each driver during synchronous connect. No DNS lookup or direct destination fallback. */
public final class TunnelSocketFactory extends SocketFactory {
    static final ThreadLocal<Integer> PORT=new ThreadLocal<>();
    private final int port;
    public TunnelSocketFactory(){Integer p=PORT.get();if(p==null)throw new IllegalStateException("SSH 转发未开启");port=p;}
    public Socket createSocket()throws IOException{Socket s=new Socket();try{s.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port),10000);return s;}catch(IOException e){s.close();throw e;}}
    public Socket createSocket(String h,int p)throws IOException{return createSocket();}
    public Socket createSocket(String h,int p,InetAddress a,int l)throws IOException{return createSocket();}
    public Socket createSocket(InetAddress h,int p)throws IOException{return createSocket();}
    public Socket createSocket(InetAddress h,int p,InetAddress a,int l)throws IOException{return createSocket();}
}

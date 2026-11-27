package dev.fieldsql.db;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/** Strict reader: never ignore an unexpected PEM block or trailing bytes attached to a certificate. */
final class PrivateCa {
    static final int MAX_BYTES=256*1024;
    private static final String BEGIN="-----BEGIN CERTIFICATE-----", END="-----END CERTIFICATE-----";
    private PrivateCa(){}

    static byte[] normalize(byte[] raw)throws CertificateException{
        List<X509Certificate> certs=parse(raw);
        Base64.Encoder encoder=Base64.getMimeEncoder(64,new byte[]{10});
        StringBuilder pem=new StringBuilder();
        for(X509Certificate cert:certs)
            pem.append(BEGIN).append('\n').append(encoder.encodeToString(cert.getEncoded())).append('\n').append(END).append('\n');
        byte[] out=pem.toString().getBytes(StandardCharsets.US_ASCII);
        if(out.length>MAX_BYTES)throw new CertificateException("规范化后的 CA 证书包不能超过 256 KiB");
        return out;
    }
    static List<X509Certificate> parse(byte[] raw)throws CertificateException{
        if(raw==null||raw.length==0||raw.length>MAX_BYTES)
            throw new CertificateException("请选择 1–262144 字节的 X.509 CA 证书（PEM 或 DER）");
        CertificateFactory cf=CertificateFactory.getInstance("X.509");
        List<X509Certificate> certs=new ArrayList<>();
        if(raw[0]==0x30){certs.add(fromDer(raw,cf));return certs;}
        int off=raw.length>=3 && raw[0]==(byte)0xef&&raw[1]==(byte)0xbb&&raw[2]==(byte)0xbf?3:0;
        String text=new String(raw,off,raw.length-off,StandardCharsets.US_ASCII);
        if(text.contains("PRIVATE KEY"))throw new CertificateException("所选文件含有私钥。请只导入云数据库提供的 CA 公钥证书");
        for(int i=0;i<text.length();){
            if(white(text.charAt(i))){i++;continue;}
            if(!text.startsWith(BEGIN,i))throw new CertificateException("证书包包含非 CERTIFICATE 内容；请选择 PEM 或 DER CA 证书");
            int start=i+BEGIN.length(), end=text.indexOf(END,start);
            if(end<0)throw new CertificateException("PEM CA 证书缺少结束标记");
            StringBuilder b64=new StringBuilder(end-start);
            for(int j=start;j<end;j++){char c=text.charAt(j);if(!white(c))b64.append(c);}
            byte[] bytes;
            try{bytes=Base64.getDecoder().decode(b64.toString());}
            catch(IllegalArgumentException e){throw new CertificateException("PEM CA 证书的 Base64 数据无效",e);}
            certs.add(fromDer(bytes,cf));i=end+END.length();
        }
        if(certs.isEmpty())throw new CertificateException("文件中没有 CA 证书");
        return certs;
    }
    private static boolean white(char c){return c==' '||c=='\t'||c=='\r'||c=='\n';}
    private static X509Certificate fromDer(byte[] data,CertificateFactory cf)throws CertificateException{
        ByteArrayInputStream in=new ByteArrayInputStream(data);
        X509Certificate c=(X509Certificate)cf.generateCertificate(in);
        if(in.available()!=0||!Arrays.equals(c.getEncoded(),data))throw new CertificateException("DER 文件须仅含一张完整的 X.509 证书");
        if(c.getBasicConstraints()<0)throw new CertificateException("所选的是终端证书，不是 CA 签发机构证书");
        boolean[] ku=c.getKeyUsage();
        if(ku!=null&&(ku.length<=5||!ku[5]))throw new CertificateException("这张 CA 证书没有签发证书的权限");
        return c;
    }
}

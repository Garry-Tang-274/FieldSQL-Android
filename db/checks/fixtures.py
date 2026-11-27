"""Disposable Windows integration fixture. Uses only its own random ports, data and processes.
Args: probe directory; expects extracted pg/bin, maria/**/bin, pinned JDBC jars and compiled Java classes.
"""
import base64,datetime,hashlib,ipaddress,os,pathlib,secrets,select,socket,subprocess,sys,tempfile,threading,time
import paramiko
from cryptography import x509
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID,ExtendedKeyUsageOID

b=pathlib.Path(sys.argv[1]).resolve()
# MySQL/Windows parses non-ASCII binary paths incorrectly. Map only this test directory,
# check the device before removing it and keep all filesystem writes under the project.
real_b=b; drive=None
if not str(b).isascii():
    import ctypes,string
    used=ctypes.windll.kernel32.GetLogicalDrives()
    for ch in reversed(string.ascii_uppercase[8:]):
        if not (used & (1<<(ord(ch)-65))):
            flag=subprocess.run(['subst',ch+':',str(b)],capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW)
            if flag.returncode==0:drive=ch+':';b=pathlib.Path(drive+'/');break
t=pathlib.Path(tempfile.mkdtemp(prefix='it-',dir=b))
procs=[]; files=[]
def proc(args, **kw):return subprocess.run([str(x) for x in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,**kw)
def run(args,**kw):
    r=proc(args,**kw)
    if r.returncode:raise RuntimeError(r.stdout.decode(errors='replace'))
def start(args,log):
    f=open(t/log,'wb');files.append(f);p=subprocess.Popen([str(x) for x in args],stdout=f,stderr=f,creationflags=subprocess.CREATE_NO_WINDOW)
    procs.append(p)
def free():
    with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
pg,my,oracle=free(),free(),free();pw=secrets.token_hex(20)
pgbin=b/'pg'/'bin';mdb=next((b/'maria').rglob('mariadbd.exe')).parent; dat=t/'pgdata';md=t/'mariadata'
suite=True
def ready(port):
    for _ in range(200):
        try:
            with socket.create_connection(('127.0.0.1',port),timeout=.2):return
        except OSError:time.sleep(.1)
    raise RuntimeError('fixture not ready on '+str(port)+'; '+str(t))
def certs():
    ca=rsa.generate_private_key(public_exponent=65537,key_size=2048);leaf=rsa.generate_private_key(public_exponent=65537,key_size=2048)
    intermediate=rsa.generate_private_key(public_exponent=65537,key_size=2048);other=rsa.generate_private_key(public_exponent=65537,key_size=2048)
    now=datetime.datetime.now(datetime.timezone.utc);cn=lambda n:x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,n)])
    def make(pub,name,issuer='FieldSQL QA Root'):
        return x509.CertificateBuilder().subject_name(cn(name)).issuer_name(cn(issuer)).public_key(pub.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(days=1)).not_valid_after(now+datetime.timedelta(days=2))
    cr=make(ca,'FieldSQL QA Root').add_extension(x509.BasicConstraints(ca=True,path_length=1),critical=True).sign(ca,hashes.SHA256())
    ci=make(intermediate,'FieldSQL QA Issuing CA').add_extension(x509.BasicConstraints(ca=True,path_length=0),critical=True).sign(ca,hashes.SHA256())
    cw=make(other,'FieldSQL Unrelated CA','FieldSQL Unrelated CA').add_extension(x509.BasicConstraints(ca=True,path_length=0),critical=True).sign(other,hashes.SHA256())
    sv=make(leaf,'localhost','FieldSQL QA Issuing CA').add_extension(x509.BasicConstraints(ca=False,path_length=None),True).add_extension(x509.SubjectAlternativeName([x509.DNSName('localhost'),x509.IPAddress(ipaddress.ip_address('10.0.2.2'))]),False).add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]),False).sign(intermediate,hashes.SHA256())
    for path,obj in [('ca.pem',cr),('wrong-ca.pem',cw),('leaf.pem',sv)]: (t/path).write_bytes(obj.public_bytes(serialization.Encoding.PEM))
    (t/'ca.der').write_bytes(cr.public_bytes(serialization.Encoding.DER))
    (t/'server.pem').write_bytes(sv.public_bytes(serialization.Encoding.PEM)+ci.public_bytes(serialization.Encoding.PEM))
    (t/'server.key').write_bytes(leaf.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.TraditionalOpenSSL,serialization.NoEncryption()))
    run(['keytool','-importcert','-noprompt','-alias','fieldsql-qa','-file',t/'ca.pem','-keystore',t/'trust.p12','-storepass',pw])
class Proxy(paramiko.ServerInterface):
    def __init__(self):self.dest={}
    def get_allowed_auths(self,u):return 'publickey'
    def check_auth_publickey(self,u,k):return paramiko.AUTH_SUCCESSFUL if u=='qa' and k==client else paramiko.AUTH_FAILED
    def check_channel_direct_tcpip_request(self,n,origin,dest):
        if dest[0] not in ('localhost','127.0.0.1') or dest[1] not in (pg,my,oracle):return paramiko.OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED
        self.dest[n]=dest;return paramiko.OPEN_SUCCEEDED
def serve(s):
    def one(c):
        tr=paramiko.Transport(c);tr.add_server_key(server);policy=Proxy()
        try:
            tr.start_server(server=policy)
            def relay(channel):
                try:
                    with socket.create_connection(policy.dest[channel.chanid],5) as dst:
                        while True:
                            read,_,_=select.select([dst,channel],[],[],35)
                            if not read:break
                            if channel in read:
                                x=channel.recv(65536)
                                if not x:break
                                dst.sendall(x)
                            if dst in read:
                                x=dst.recv(65536)
                                if not x:break
                                channel.sendall(x)
                except (OSError,EOFError,paramiko.SSHException):pass
                finally:channel.close()
            while tr.is_active():
                channel=tr.accept(2)
                if channel is not None:threading.Thread(target=relay,args=(channel,),daemon=True).start()
        except (OSError,EOFError,paramiko.SSHException):pass
        finally:tr.close()
    while True:
        try:c,_=s.accept()
        except OSError:return
        threading.Thread(target=one,args=(c,),daemon=True).start()
try:
    certs();(t/'pw').write_text(pw+'\n',encoding='ascii')
    run([pgbin/'initdb','--no-locale','--encoding=UTF8','--username=fieldsql','--auth-host=scram-sha-256','--auth-local=trust','--pwfile',t/'pw','-D',dat])
    run([mdb/'mariadb-install-db.exe','--datadir='+str(md),'--password='+pw,'--port='+str(my)])
    for n in ('server.pem','server.key','ca.pem'):
        import shutil;shutil.copyfile(t/n,dat/n)
    start([pgbin/'postgres','-D',dat,'-h','127.0.0.1','-p',pg,'-c','ssl=on','-c','ssl_cert_file=server.pem','-c','ssl_key_file=server.key'],'pg.log')
    start([mdb/'mariadbd','--no-defaults','--basedir='+str(mdb.parent),'--datadir='+str(md),'--bind-address=127.0.0.1','--port='+str(my),'--ssl-ca='+str(t/'ca.pem'),'--ssl-cert='+str(t/'server.pem'),'--ssl-key='+str(t/'server.key'),'--console'],'maria.log')
    ready(pg);ready(my)
    run([mdb/'mariadb','--no-defaults','-h','127.0.0.1','-P',my,'-uroot','-e','CREATE DATABASE fieldsql_qa CHARACTER SET utf8mb4'],env={**os.environ,'MYSQL_PWD':pw})
    obin=next((b/'mysql8').rglob('mysqld.exe')).parent; sql8=t/'mysql8data'
    # Keep all extracted and generated files inside the fixture. The init file sets the password before the listener accepts clients.
    run([obin/'mysqld','--no-defaults','--initialize-insecure','--basedir='+str(obin.parent),'--datadir='+str(sql8),'--console'])
    init=t/'init.sql';init.write_text("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+pw+"';\nCREATE DATABASE fieldsql_qa CHARACTER SET utf8mb4;\n",encoding='ascii')
    start([obin/'mysqld','--no-defaults','--no-monitor','--basedir='+str(obin.parent),'--datadir='+str(sql8),'--bind-address=127.0.0.1','--port='+str(oracle),'--mysqlx=0','--init-file='+str(init),'--ssl-ca='+str(t/'ca.pem'),'--ssl-cert='+str(t/'server.pem'),'--ssl-key='+str(t/'server.key'),'--console'],'mysql8.log')
    ready(oracle)
    server=paramiko.RSAKey.generate(2048);client=paramiko.RSAKey.generate(2048);client.write_private_key_file(str(t/'ssh.pem'));fp='SHA256:'+base64.b64encode(hashlib.sha256(server.asbytes()).digest()).decode().rstrip('=')
    listener=socket.socket();listener.bind(('127.0.0.1',0));listener.listen(8);threading.Thread(target=serve,args=(listener,),daemon=True).start()
    cp=os.pathsep.join(str(b/f) for f in ('classes','android-compile.jar','jsch-2.28.8.jar','mariadb-java-client-2.7.15.jar','postgresql-42.7.15.jar'))
    run(['javac','-encoding','UTF-8','-cp',cp,'-d',b/'classes',pathlib.Path(__file__).with_name('RemoteSmoke.java')])
    res=proc(['java','-Djavax.net.ssl.trustStore='+str(t/'trust.p12'),'-Djavax.net.ssl.trustStorePassword='+pw,'-cp',cp,'RemoteSmoke',pg,my,oracle,listener.getsockname()[1],t/'ssh.pem',fp,'hold' if '--hold' in sys.argv else 'run'],env={**os.environ,'FIELD_TEST_PASS':pw},timeout=120)
    print(res.stdout.decode('utf-8',errors='replace'))
    if res.returncode:raise RuntimeError('Java integration failed')
    if '--hold' in sys.argv:
        import json
        # Bind only to host loopback. Android emulator reaches it via 10.0.2.2.
        ui=t/'ui.json'; ui.write_text(json.dumps({'host':'10.0.2.2','ssl':True,'ca':str(real_b/t.name/'ca.pem'),'wrongCa':str(real_b/t.name/'wrong-ca.pem'),'password':pw,'postgres':{'port':pg,'user':'fieldsql','database':'postgres'},'mariadb':{'port':my,'user':'root','database':'fieldsql_qa'},'mysql':{'port':oracle,'user':'root','database':'fieldsql_qa'},'sshPort':listener.getsockname()[1],'sshUser':'qa','sshKey':str(real_b/t.name/'ssh.pem'),'sshHostPin':fp,'stop':str(real_b/t.name/'STOP')}),encoding='utf-8')
        print('ANDROID FIXTURES READY: '+str(real_b/t.name/'ui.json'),flush=True)
        deadline=time.monotonic()+3600
        while time.monotonic()<deadline and not (t/'STOP').exists():time.sleep(.5)
finally:
    # Oracle mysqld on Windows runs a monitor plus a child server. Tell the server to
    # exit before reaping its own launcher; terminating only the launcher leaves the child behind.
    for index,name in ((2,'mysql'),(1,'mariadb')):
        if len(procs)>index and procs[index].poll() is None:
            cmd,port=((obin/'mysql',oracle) if index==2 else (mdb/'mariadb',my))
            try:
                proc([cmd,'--no-defaults','-h','127.0.0.1','-P',port,'-uroot','-e','SHUTDOWN'],env={**os.environ,'MYSQL_PWD':pw},timeout=6)
                procs[index].wait(timeout=10)
            except (subprocess.TimeoutExpired,OSError):pass
    if procs and procs[0].poll() is None:
        try:proc([pgbin/'pg_ctl','-D',dat,'-m','fast','-w','-t','6','stop'],timeout=9)
        except (subprocess.TimeoutExpired,OSError):pass
    for p in procs:
        if p.poll() is None:p.terminate()
    for p in procs:
        try:p.wait(timeout=7)
        except subprocess.TimeoutExpired:p.kill();p.wait()
    for f in files:f.close()
    # Preserve fixture logs only. Never traverse outside this run unique directory.
    import shutil
    for name in ('pg.log','maria.log','mysql8.log'):
        if (t/name).is_file():shutil.copyfile(t/name,b/('last-'+name))
    if t.resolve().parent==real_b and t.name.startswith('it-'):shutil.rmtree(t,ignore_errors=True)
    if drive and pathlib.Path(drive+'/').resolve()==real_b:subprocess.run(['subst',drive,'/d'],capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW)









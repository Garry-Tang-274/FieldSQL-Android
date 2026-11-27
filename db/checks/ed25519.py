"""Use an existing owned Windows QA fixture (ui.json); test BC/Ed25519 without a new DB service."""
import base64,hashlib,io,json,os,pathlib,secrets,select,socket,subprocess,sys,tempfile,threading
import paramiko
from cryptography.hazmat.primitives import serialization as ser
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
u=pathlib.Path(sys.argv[1]).resolve();cfg=json.loads(u.read_text('utf-8'));base=u.parent.parent
sshpass=secrets.token_hex(18);pw=cfg['password'];pg=cfg['postgres']['port']
def private():return Ed25519PrivateKey.generate()
def pem(k,enc):return k.private_bytes(ser.Encoding.PEM,ser.PrivateFormat.OpenSSH,enc)
sk=private();uk=private()
host=paramiko.Ed25519Key.from_private_key(io.StringIO(pem(sk,ser.NoEncryption()).decode()))
client=paramiko.Ed25519Key.from_private_key(io.StringIO(pem(uk,ser.NoEncryption()).decode()))
pin='SHA256:'+base64.b64encode(hashlib.sha256(host.asbytes()).digest()).decode().rstrip('=')
class Policy(paramiko.ServerInterface):
    def get_allowed_auths(self,u):return 'publickey'
    def check_auth_publickey(self,u,k):return paramiko.AUTH_SUCCESSFUL if u=='qa' and k==client else paramiko.AUTH_FAILED
    def check_channel_direct_tcpip_request(self,id,origin,dest):
        return paramiko.OPEN_SUCCEEDED if dest==('localhost',pg) else paramiko.OPEN_FAILED_ADMINISTRATIVELY_PROHIBITED
def serve(server):
    c,_=server.accept();tr=paramiko.Transport(c);tr.add_server_key(host)
    try:
        tr.start_server(server=Policy());channel=tr.accept(15)
        if not channel:return
        with socket.create_connection(('127.0.0.1',pg),5) as s:
            while True:
                read,_,_=select.select([channel,s],[],[],10)
                if not read:break
                if channel in read:
                    x=channel.recv(65536)
                    if not x:break
                    s.sendall(x)
                if s in read:
                    x=s.recv(65536)
                    if not x:break
                    channel.sendall(x)
    finally:tr.close()
with tempfile.TemporaryDirectory(dir=base,prefix='ed-key-') as tmp,socket.socket() as listener:
    p=pathlib.Path(tmp)/'key';p.write_bytes(pem(uk,ser.BestAvailableEncryption(sshpass.encode())))
    listener.bind(('127.0.0.1',0));listener.listen(1);threading.Thread(target=serve,args=(listener,),daemon=True).start()
    cp=os.pathsep.join(str(base/f) for f in ('classes','android-compile.jar','jsch-2.28.8.jar','bcprov-jdk15to18-1.87.jar','mariadb-java-client-2.7.15.jar','postgresql-42.7.15.jar'))
    subprocess.run(['javac','-encoding','UTF-8','-cp',cp,'-d',str(base/'classes'),str(pathlib.Path(__file__).with_name('Ed25519Smoke.java'))],check=True)
    subprocess.run(['java','-Djdk.util.jar.enableMultiRelease=false','-Djavax.net.ssl.trustStore='+str(u.parent/'trust.p12'),'-Djavax.net.ssl.trustStorePassword='+pw,'-cp',cp,'Ed25519Smoke',str(listener.getsockname()[1]),str(pg),pin,str(p)],env={**os.environ,'FIELD_TEST_PASS':pw,'FIELD_TEST_KEYPASS':sshpass},timeout=25,check=True)

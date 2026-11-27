"""Make fresh leaf certificates, including matching CN deliberately paired with absent/incorrect SAN."""
import datetime,ipaddress,os,pathlib,subprocess,tempfile
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import rsa
module=pathlib.Path(__file__).resolve().parents[1];probe=module/'build/probe';key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
name=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'db.example.test')]);now=datetime.datetime.now(datetime.timezone.utc)
def write(p,san):
    cert=x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(days=1)).not_valid_after(now+datetime.timedelta(days=2))
    if san is not None:cert=cert.add_extension(x509.SubjectAlternativeName([san]),False)
    p.write_bytes(cert.sign(key,hashes.SHA256()).public_bytes(serialization.Encoding.DER))
with tempfile.TemporaryDirectory(dir=probe,prefix='san-only-') as tmp:
    p=pathlib.Path(tmp)
    for title,san in {'absent':None,'mismatch':x509.DNSName('other.example.test'),'numericdns':x509.DNSName('10.0.2.2'),'ip':x509.IPAddress(ipaddress.ip_address('10.0.2.2')),'v6':x509.IPAddress(ipaddress.ip_address('2001:db8::1')),'wild':x509.DNSName('*.example.test')}.items():write(p/(title+'.der'),san)
    cp=os.pathsep.join([str(probe/'classes'),str(probe/'postgresql-42.7.15.jar')])
    subprocess.run(['javac','-encoding','UTF-8','-cp',cp,'-d',str(probe/'classes'),str(module/'checks/HostnameSanChecks.java')],check=True)
    subprocess.run(['java','-cp',cp,'dev.fieldsql.db.HostnameSanChecks',str(p)],check=True)

"""Prepare pinned desktop fixtures and Java QA classes. All downloaded files stay under db/build/probe."""
import pathlib,subprocess,sys,tarfile,urllib.request,zipfile,os
module=pathlib.Path(__file__).resolve().parent.parent
b=module/'build/probe'; b.mkdir(exist_ok=True,parents=True)
base='https://repo.maven.apache.org/maven2/'
items=[
'org/postgresql/postgresql/42.7.15/postgresql-42.7.15.jar',
'org/mariadb/jdbc/mariadb-java-client/2.7.15/mariadb-java-client-2.7.15.jar',
'com/github/mwiede/jsch/2.28.8/jsch-2.28.8.jar',
'org/bouncycastle/bcprov-jdk15to18/1.87/bcprov-jdk15to18-1.87.jar',
'com/google/android/android/4.1.1.4/android-4.1.1.4.jar',
'io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/17.10.0/embedded-postgres-binaries-windows-amd64-17.10.0.jar',
'ch/vorburger/mariaDB4j/mariaDB4j-db-winx64/10.11.5-fix1/mariaDB4j-db-winx64-10.11.5-fix1.jar',
'https://cdn.mysql.com/archives/mysql-8.4/mysql-8.4.7-winx64.zip']
jars=[]
for x in items:
    name=x.split('/')[-1] if not x.startswith('com/google/android/') else 'android-compile.jar'
    target=b/name
    if not target.is_file():
        temp=target.with_suffix('.download')
        urllib.request.urlretrieve(x if x.startswith('https:') else base+x,temp)
        temp.replace(target)
    if 'embedded-postgres' in x:
        with zipfile.ZipFile(target) as z:z.extract('postgres-windows-x86_64.txz',b/'pg')
        with tarfile.open(b/'pg/postgres-windows-x86_64.txz') as t:t.extractall(b/'pg',filter='data')
    elif 'mariaDB4j' in x:
        with zipfile.ZipFile(target) as z:z.extractall(b/'maria')
    elif x.endswith('.zip'):
        with zipfile.ZipFile(target) as z:
            names=[n for n in z.namelist() if any(s in n for s in ['/share/','/lib/plugin/','/bin/mysqld.exe','/bin/mysql.exe','/lib/private/']) or ('/bin/' in n and n.endswith('.dll'))]
            z.extractall(b/'mysql8',members=names)
    else:jars.append(str(target))
(b/'classes').mkdir(exist_ok=True)
subprocess.run(['javac','-encoding','UTF-8','-cp',os.pathsep.join(jars),'-d',str(b/'classes'),*[str(x) for x in (module/'src/main/java').rglob('*.java')]],check=True)
print('Ready:',b)

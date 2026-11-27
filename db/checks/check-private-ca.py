"""Use ui.json from the dedicated fixtures.py --hold run. No unrelated database is used."""
import json,os,pathlib,subprocess,sys
file=pathlib.Path(sys.argv[1]).resolve()
if file.name!='ui.json' or not file.parent.name.startswith('it-') or file.parent.parent.name!='probe':raise ValueError('expected db/build/probe/it-*/ui.json')
c=json.loads(file.read_text('utf-8'));run=file.parent;probe=run.parent;pw=c['password']
cp=os.pathsep.join(str(probe/f) for f in ('classes','android-compile.jar','jsch-2.28.8.jar','bcprov-jdk15to18-1.87.jar','mariadb-java-client-2.7.15.jar','postgresql-42.7.15.jar'))
subprocess.run(['javac','-encoding','UTF-8','-cp',cp,'-d',str(probe/'classes'),str(pathlib.Path(__file__).with_name('PrivateCaSmoke.java'))],check=True)
for mode in ('custom','system'):
    trust=['-Djavax.net.ssl.trustStore='+str(run/'trust.p12'),'-Djavax.net.ssl.trustStorePassword='+pw] if mode=='system' else []
    argv=['java','-Duser.language=en','-Duser.country=US',*trust,'-cp',cp,'PrivateCaSmoke',str(run),*[str(c[k]['port']) for k in ('postgres','mariadb','mysql')],str(c['sshPort']),c['sshHostPin'],mode]
    # Do not put ephemeral passwords in source or echo the JVM command line on failure.
    result=subprocess.run(argv,env={**os.environ,'FIELD_TEST_PASS':pw},timeout=120)
    if result.returncode:sys.exit(result.returncode)

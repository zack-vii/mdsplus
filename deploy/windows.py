import subprocess,os,sys

class InstallationPackage(object):
    """Provides exists,build,test and deploy methods"""
    def __init__(self,info):
        self.info=info
        self.info['workspace']=os.environ['WORKSPACE']

    def exists(self):
        """Check to see if install kit for this release already exist."""
        for arch in ('x86_64','x86'):
            self.info['arch']=arch
            kit="/repository/Windows/%(flavor)s/%(arch)s/MDSplus%(rflavor)s-%(major)d.%(minor)d-%(release)d.exe" % self.info
            try:
                os.stat(kit)
            except:
                print("%s not found" % kit)
                sys.stdout.flush()
                return False
        return True

    def build(self):
        """Build MDSplus from the sources and install into a 'flavor' directory"""
        status = subprocess.Popen("""
set -e
rm -Rf %(workspace)s/%(flavor)s
mkdir -p %(workspace)s/%(flavor)s
cd ..
./configure --host=x86_64-w64-mingw32 --build=x86_64-redhat-linux-gnu --target=x86_64-w64-mingw32 \
        --prefix=%(workspace)s/%(flavor)s --exec-prefix=%(workspace)s/%(flavor)s \
        --libdir=%(workspace)s/%(flavor)s/bin_x86_64 \
        --bindir=%(workspace)s/%(flavor)s/bin_x86_64 --enable-mdsip_connections --with-labview=$LABVIEW_DIR \
        --with-jdk=$JDK_DIR --with-idl=$IDL_DIR --with-java_target=6 --with-java_bootclasspath=$(pwd)/rt.jar
make clean
make
make install
./configure --host=i686-w64-mingw32 --build=i686-redhat-linux-gnu --target=i686-w64-mingw32 \
        --prefix=%(workspace)s/%(flavor)s --exec-prefix=%(workspace)s/%(flavor)s \
        --libdir=%(workspace)s/%(flavor)s/bin_x86 \
        --bindir=%(workspace)s/%(flavor)s/bin_x86 --enable-mdsip_connections --with-labview=$LABVIEW_DIR \
        --with-jdk=$JDK_DIR --with-idl=$IDL_DIR --with-java_target=6 --with-java_bootclasspath=$(pwd)/rt.jar
make clean
make
make install
pushd %(workspace)s/%(flavor)s
makensis -DMAJOR=%(major)d -DMINOR=%(minor)d -DRELEASE=%(release)d -DFLAVOR=%(rflavor)s -NOCD \
        -DOUTDIR=%(workspace)s/%(flavor)s %(workspace)s/mdsplus%(rflavor)s-%(major)d.%(minor)d-%(release)d/deploy/mdsplus.nsi
echo mdsplus | signcode -spc /mnt/scratch/mdsplus/mdsplus.spc \
         -v /mnt/scratch/mdsplus/mdsplus.pvk \
         -a sha1 \
         -$ individual \
         -n MDSplus  \
         -i http://www.mdsplus.org/ \
         -t http://timestamp.verisign.com/scripts/timestamp.dll \
         -tr 10
""" % self.info,shell=True).wait()
        if status != 0:
            raise Exception("Error building windows kit for package mdsplus%(rflavor)s.%(major)d.%(minor)d-%(release)d.exe" % self.info)
        print("Done building mdsplus%(rflavor)s-%(major)d.%(minor)d-%(release)d.exe" % self.info)

    def test(self):
        return

    def deploy(self):
        """Deploy release to repository"""
        if subprocess.Popen("""
rsync -a %(workspace)s/%(flavor)s/MDSplus%(rflavor)s-%(major)d.%(minor)d-%(release)d.exe /repository/Windows/%(flavor)s/
""" % self.info,shell=True).wait() != 0:
            raise Exception("Error deploying %(flavor)s release to repository" % self.info)

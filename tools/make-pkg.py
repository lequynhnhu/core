import sys, os, re, getopt, glob
from zipfile import ZipFile
import xml.etree.ElementTree as ET
import tempfile
from Queue import Queue
from threading import Thread
# external library (pip install plumbum)
from plumbum import local, ProcessExecutionError, local_machine, FG, BG
from plumbum.utils import delete
# from setuptools (pip install setuptools)
from distutils.version import LooseVersion
# in bundle.py
from bundle import Bundle, Artifact

debug = False

nsmap = { "mvn": "http://maven.apache.org/POM/4.0.0" }
def findall(domroot, path):
    return domroot.findall(path, namespaces=nsmap)

local.encoding = "cp949"
if "JAVA_HOME" not in local.env.keys():
    local.env["JAVA_HOME"] = r"C:\Java\jdk_current"

inc_bundles = [
        ("org.araqne", "araqne-ipojo", "1.1.0"),
    ]
exc_bundles = [
        "org.osgi:org.osgi.core",
        "org.araqne:araqne-api",
        "com.jcraft:jsch",
        "org.apache.sshd:sshd-core",
        "org.araqne:araqne-cron",
        "org.araqne:araqne-codec",
        "org.araqne:araqne-confdb",
        "org.slf4j:slf4j-api",
        "org.slf4j:slf4j-simple",
        "junit:junit",
        "org.bouncycastle:bcprov-jdk16",
        "org.apache.felix:org.apache.felix.framework",
        "org.apache.felix:org.apache.felix.ipojo.annotations",
        "org.apache.felix:org.apache.felix.ipojo.metadata",
        "org.apache.felix:org.osgi.compendium",
        "org.apache.felix:org.osgi.core",
        "org.apache.felix:org.osgi.foundation",
        "org.apache.felix:javax.servlet",
    ]

#mvn_path = "/cygdrive/c/Java/jdk_current/bin/mvn"
mvn_path = "mvn.bat"

if sys.platform == "cygwin":
    abspath = lambda x: local["cygpath.exe"]["-wa"][x]().strip()
else:
    abspath = os.path.abspath

def get_dom_text(dom, path, idx):
    ret = findall(dom, path)
    if ret:
        return ret[idx].text
    else:
        return None

def get_group_id(dom):
    ret = findall(dom, "./mvn:groupId")
    if ret:
        return ret[0].text
    else:
        ret = findall(dom, "./mvn:parent/mvn:groupId")
        if ret:
            return ret[0].text
    return None

def get_user_dir():
    if sys.platform == "cygwin": 
        return cpath[os.environ["USERPROFILE"]]().strip()
    else:
        return os.environ["USERPROFILE"].strip()

def get_repo_dir(user_dir):
    return os.path.normpath(os.path.join(user_dir, ".m2/repository"))

def make_local_jar(gid, aid, ver):
    user_dir = get_user_dir()
    repo_dir = get_repo_dir(user_dir)
    bundle_dir = os.path.join(repo_dir, gid.replace(".", os.path.sep), aid)
    return os.path.join(bundle_dir, ver, aid + "-" + ver + ".jar")

def get_latest(group_id, arti_id):
    user_dir = get_user_dir()
    repo_dir = get_repo_dir(user_dir)

    bundle_dir = os.path.join(repo_dir, group_id.replace(".", os.path.sep), arti_id)
    versions = []
    for dir in glob.glob(bundle_dir + "/*"):
        if os.path.isdir(dir):
            versions.append(os.path.basename(dir))
    versions = sorted(versions, key=LooseVersion, reverse=True)
    return bundle_dir, versions[0]

def get_dep(gid, aid, ver):
    user_dir = get_user_dir()
    repo_dir = get_repo_dir(user_dir)

    bundle_dir = os.path.join(repo_dir, gid.replace(".", os.path.sep), aid)
    return os.path.join(bundle_dir, ver, aid + "-" + ver + ".dep")

def get_bundle(gid, aid, ver):
    return Bundle(gid, aid, None, ver, "compile", make_local_jar(gid, aid, ver))

def read_jar(jar):
    symline = None
    verline = None
    for line in ZipFile(jar).open("META-INF/MANIFEST.MF"):
        if "Bundle-SymbolicName" in line:
            symline = line.strip()
        if "Bundle-Version" in line:
            verline = line.strip()
    symname = None
    version = None
    if symline:
        symname = symline.split(":")[1].strip().split(";")[0]
    if verline:
        version = verline.split(":")[1].strip().split(";")[0]
    return symname, version

bundle_info = {}
def get_symbolic_name(jar):
    if bundle_info.has_key(jar):
        return bundle_info[jar][0]
    symname, version = read_jar(jar)
    if symname:
        bundle_info[jar] = (symname, version)
    return symname

def get_bundle_version(jar):
    if bundle_info.has_key(jar):
        return bundle_info[jar][1]
    symname, version = read_jar(jar)
    if version:
        bundle_info[jar] = (symname, version)
    return version

q = Queue()
def worker():
    while True:
        runnable, item = q.get()
        runnable(*item)
        q.task_done()

def make_dep(pom, outf):
    try: 
        mvn_dep_list = local[mvn_path]["-o", "dependency:list", "-DincludeScope=runtime", "-DoutputAbsoluteArtifactFilename=true"]
        f = mvn_dep_list["-f", pom, "-DoutputFile=" + abspath(outf)]()
        return f
    except WindowsError as e:
        print os.strerror(e.errno)
    except ProcessExecutionError as e:
        print e.argv
        print e.stdout.encode("utf-8")
    return None

if __name__ == "__main__":
    try:
        opts, args = getopt.getopt(sys.argv[1:], "f:so:dcpj:")
    except getopt.GetoptError as err:
        print str(err)
        sys.exit(2)

    leaf_bundle_strs = []
    skip_make_dep = False
    outputfile = None
    output = sys.stdout
    make_parent = False
    thread_pool_size = 4

    for opt in opts:
        if opt[0] == "-f":
            for line in open(opt[1]):
                if line.strip():
                    leaf_bundle_strs.append(line.strip())
        if opt[0] == "-s":
            skip_make_dep = True
        if opt[0] == "-o":
            outputfile = opt[1]
        if opt[0] == "-d":
            debug = True
        if opt[0] == "-p":
            make_parent = True
        if opt[0] == "-j":
            thread_pool_size = int(opt[1])

    for i in range(thread_pool_size):
        t = Thread(target=worker)
        t.daemon = True
        t.start()

    if outputfile:
        if not os.path.exists(os.path.dirname(outputfile)) and make_parent:
            os.mkdir(os.path.dirname(outputfile))
        output = open(outputfile, "w")

    for arg in args:
        leaf_bundle_strs.append(arg.split(":"))

    leaf_bundles = [x.split(":") for x in leaf_bundle_strs]

    for lb in leaf_bundles:
        if len(lb) == 2:
            lb.append(get_latest(lb[0], lb[1])[1])

    outfiles = []
    outcnt = 0
    artifacts = {}

    for lb in leaf_bundles:
        outfiles.append(get_dep(lb[0], lb[1], lb[2]))
        bundle = get_bundle(lb[0], lb[1], lb[2])
        artifacts[bundle.artifact] = bundle

    for outfile in outfiles:
        for line in open(outfile):
            dep = Bundle.parse(line.strip())
            if dep is not None:
                if dep.artifact not in artifacts:
                    if str(dep.artifact) not in exc_bundles and get_symbolic_name(dep.jar) is not None:
                        artifacts[dep.artifact] = dep
                else:
                    if dep.version > artifacts[dep.artifact].version:
                        artifacts[dep.artifact] = dep

    for a in inc_bundles:
        artifacts[Artifact(a[0], a[1])] = Bundle(
            a[0], a[1], None, a[2], "compile", make_local_jar(a[0], a[1], a[2]))

    # make output
    print >> output, "[bundle]"
    max_symname_len = 0
    for a in artifacts.keys():
        max_symname_len = max(max_symname_len, len(get_symbolic_name(artifacts[a].jar)))
    for a in sorted(artifacts.keys()):
        print >> output, ("{0:" + str(max_symname_len+1) + "s} {1:15s}").format(
            get_symbolic_name(artifacts[a].jar), get_bundle_version(artifacts[a].jar))
    print >> output, ""

    print >> output, "[start]"
    for a in sorted(artifacts.keys()):
        print >> output, get_symbolic_name(artifacts[a].jar)
    print >> output, ""

    print >> output, "[maven]"
    max_gid_len = 0
    max_aid_len = 0
    for a in sorted(artifacts.keys()):
        max_gid_len = max(max_gid_len, len(artifacts[a].artifact.gid))
        max_aid_len = max(max_aid_len, len(artifacts[a].artifact.aid))
    for a in sorted(artifacts.keys()):
        afct = artifacts[a]
        fmtstr = ("{0:" + str(max_gid_len + 1) + "s} {1:" + str(max_aid_len + 1) + "s} {2:15s}")
        print >> output, fmtstr.format(afct.artifact.gid, afct.artifact.aid, afct.version)






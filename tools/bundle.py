from distutils.version import LooseVersion

class Artifact:
    def __init__(self, groupId, artifactId):
        self.gid = groupId
        self.aid = artifactId

    def __str__(self):
        return ":".join([self.gid, self.aid])

    def __hash__(self):
        return hash(str(self))

    def __cmp__(self, o):
        if self.gid == o.gid:
            return cmp(self.aid, o.aid)
        else:
            return cmp(self.gid, o.gid)

class Bundle:
    def __init__(self, groupId, artifactId, type, version, scope, jar):
        self.artifact = Artifact(groupId, artifactId)
        self.type = type
        self.version = version
        self.scope = scope
        self.jar = jar

    def __str__(self):
        return ":".join([self.artifact.gid, self.artifact.aid, str(self.version), self.scope])

    def __hash__(self):
        return hash(":".join([self.artifact.gid, self.artifact.aid, str(self.version)]))

    def __eq__(self, o):
        return self.artifact == o.artifact and self.version == o.version

    def __cmp__(self, o):
        if self.artifact == o.artifact:
            return cmp(self.version, o.version)
        else:
            return cmp(self.artifact, o.artifact)
        
    @classmethod
    def parse(cls, line):
        try: 
            s = line.split(":", 5)
            return Bundle(s[0], s[1], s[2], LooseVersion(s[3]), s[4], s[5])
        except:
            return None

// Writes tmp/line-resize/cp.txt: a javac classpath for compile checks outside Gradle (the dev client's classes untouched).
const fs = require('fs');
const lines = fs.readFileSync('build/moddev/clientLegacyClasspath.txt', 'utf8').split(/\r?\n/).filter(Boolean);
const cache = process.env.USERPROFILE.split('\\').join('/') + '/.gradle/caches/modules-2/files-2.1/';
const find = (group, artifact, version) => {
    const dir = cache + group + '/' + artifact + '/' + version;
    for (const hash of fs.readdirSync(dir)) {
        const jar = dir + '/' + hash + '/' + artifact + '-' + version + '.jar';
        if (fs.existsSync(jar)) return jar;
    }
    throw new Error(artifact);
};
const junit = fs.readFileSync('gradle.properties', 'utf8').match(/junit_version\s*=\s*(\S+)/)[1];
const extra = ['build/classes/java/main', 'build/classes/java/test', 'build/resources/main', 'build/moddev/artifacts/neoforge-21.1.219-merged.jar',
    find('org.junit.jupiter', 'junit-jupiter-api', junit), find('org.apiguardian', 'apiguardian-api', '1.1.2'), find('org.opentest4j', 'opentest4j', '1.3.0'), find('org.jetbrains', 'annotations', '24.1.0')];
fs.writeFileSync('tmp/line-resize/cp.txt', extra.concat(lines).join(';'));
console.log('junit ' + junit + ', entries ' + (extra.length + lines.length));

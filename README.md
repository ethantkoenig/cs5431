[![Build Status](https://travis-ci.com/ethantkoenig/cs5431.svg?token=HMpuCAbooS74SzbHVpfA&branch=master)](https://travis-ci.com/ethantkoenig/cs5431)
[![codecov](https://codecov.io/gh/ethantkoenig/cs5431/branch/master/graph/badge.svg?token=osjK4B44Ty)](https://codecov.io/gh/ethantkoenig/cs5431)

# cs5431

Group Project for CS 5431

### Building the project

To simply compile the project, run 

```$ ./gradlew assemble```

To run the project, run

```$ ./gradlew distTar```

then unpack the compressed archive at `build/distributions/cs5431.tar`. Inside the uncompressed
directory, run

```$ bin/cs5431 <args>```

To fully build the project (compile, test, and run all checks), run

```$ ./gradlew build```

Before issuing a pull request or pushing to master, please ensure that `./gradlew build` succeeds.

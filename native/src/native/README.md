## JNI wrapper (calling Rust from Scala)

This folder contains the glue code needed for calling Rust code from Scala. The code is compiled into a dynamic library which is then shipped as a single file inside the JAR file of this sbt project.

### Header files

The header files in the `jni` folder can be generated using the `sbt` command `javah`, and are only there to show what the API should look like. Since the header files are checked in to git it should be easy to see if something has changed since the last commit.

### Useful sbt commands

- `native / test`
- `javah`

## Contributing

### The GitHub repository

To contribute you need to fork the master-branch of the project. To make changes I would recommend that you make many small commits rather than one big at the end, since it will make it easy to see which commit introduced what (assuming that you provide good commit-messages). When you are done with a feature you can create a pull-request. Please note that you can still add commits to an active pull-request, which might be needed to fix merge conflicts.

### Building the project

This project uses [sbt](https://www.scala-sbt.org/1.x/docs/index.html) as the build tool. The project can be directly imported into [IntelliJ](https://www.jetbrains.com/idea/) since it supports sbt, but the project itself does not in any way depend on any tools other than sbt.

### Tests

At the moment this project hardly contains any tests (unit tests), but that will hopefully improve as time goes on. That being said, there is support for unit tests so you are welcome to write tests for your code. That will make sure that the code you write do what it was designed to do not only now, but also in the future.

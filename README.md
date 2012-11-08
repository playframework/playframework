# Play 2.0 

Play 2.0 is a high productivity Java and Scala Web application framework, integrating all components and API needed for modern Web application development. 

It is based on a lightweight stateless Web friendly architecture and features predictable and minimal resources consumption (CPU, Memory, Threads) for highly scalable applications thanks to its reactive model based on Iteratee IO.

http://www.playframework.org

## Installing

You first need to build Play:

```bash
$ cd framework/
$ ./build
> build-repository
```

Then, for convenience, add the framework installation directory to your system PATH.  
On UNIX systems, it would be something like:

```bash
export PATH=$PATH:/path/to/play2.0
```

On windows systems you'll need to set it in the global environment variables.

> If you’re on UNIX, make sure that the play script is executable (otherwise do a chmod a+x play).

## Getting started

Enter any existing Play 2.0 application directory and use the `play` command to launch the development console:

```bash
$ cd ~/workspace/myApplication
$ play
```

You can also directly use `play run` to run the application:

```bash
$ cd ~/workspace/myApplication
$ play run
```

Use `play new yourNewApplication` to create a new application:

```bash
$ cd ~/workspace
$ play new myNewApplication
```

Once the application is created, use it as any existing application:

```bash
$ cd myNewApplication
$ play
```

## Running the sample applications

There are several samples applications included in the `samples/` directory. For example, to run the included Scala Hello World application:

```bash
$ cd ~/workspace/play2.0/samples/scala/helloworld/
$ play run
```
> The application will be available on port 9000.

## Issues tracker

Report issues at https://play.lighthouseapp.com/projects/82401-play-20/overview.

## Contributors

Check for all contributors at https://github.com/playframework/Play20/contributors.

## Licence

This software is licensed under the Apache 2 license, quoted below.

Copyright 2012 Typesafe (http://www.typesafe.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

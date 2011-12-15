# Play 2.0 RC1

Play 2.0 is a Java and Scala Web application framework (http://www.playframework.org/2.0).

## Installing

For convenience, you should add the framework installation directory to your system PATH. On UNIX systems will be something like:

```bash
export PATH=$PATH:/path/to/play20
```

On windows systems you'll need to set it in the global environment variables.

> If you’re on UNIX, make sure that the play script is executable (otherwise do a chmod a+x play).

## Getting started

Enter any existing Play 2.0 application directory and use the `play` command to launch the development console:

```bash
$ cd /dev/myApplication
$ play
```

You can also directly use `play run` to run the application:

```bash
$ cd /dev/myApplication
$ play run
```

Use `play new yourNewApplication` to create a new application:

```bash
$ cd /dev
$ play new myNewApplication
```

Once the application is created, use it as any existing application:

```bash
$ cd myNewApplication
$ play
```

## Running the sample applications

There are several samples applications included in the `samples/` directory. For example, to run the included ZenTask sample application:

```bash
$ cd samples/scala/zentasks/
$ play run
```
> The application will be available on port 9000. On first run, it will notify you that database evolutions are required. Click "Apply this script now" and you're away! 

## Documentation

The temporary documentation is available at https://github.com/playframework/Play20/wiki.

## Contributors

Check for all contributors at https://github.com/playframework/Play20/contributors.

## Licence

This software is licensed under the Apache 2 license, quoted below.

Copyright 2011 Zenexity (http://www.zenexity.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
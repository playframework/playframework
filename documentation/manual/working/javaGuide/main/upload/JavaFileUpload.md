<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Handling file upload

## Uploading files in a form using `multipart/form-data`

The standard way to upload files in a web application is to use a form with a special `multipart/form-data` encoding, which allows mixing of standard form data with file attachments. Please note: the HTTP method for the form has to be POST (not GET).

Start by writing an HTML form:

```
@import helper._
@form(action = routes.Application.upload, 'enctype -> "multipart/form-data") {

    <input type="file" name="picture">

    <p>
        <input type="submit">
    </p>

}
```

Now let’s define the `upload` action:

@[syncUpload](code/JavaFileUpload.java)

### Testing the file upload

You can also write an automated JUnit test to your `upload` action:

@[testSyncUpload](code/JavaFileUploadTest.java)

Basically, we are creating a `Http.MultipartFormData.FilePart` that is required by `RequestBuilder` method `bodyMultipart`. Besides that, everything else is just like [[unit testing controllers|JavaTest#Unit-testing-controllers]].

## Direct file upload

Another way to send files to the server is to use Ajax to upload files asynchronously from a form. In this case, the request body will not be encoded as `multipart/form-data`, but will just contain the plain file contents.

@[asyncUpload](code/JavaFileUpload.java)

### Writing a custom multipart file part body parser

The multipart upload specified by [`MultipartFormData`](api/java/play/mvc/BodyParser.MultipartFormData.html) takes uploaded data from the request and puts into a TemporaryFile object.  It is possible to override this behavior so that `Multipart.FileInfo` information is streamed to another class, using the `DelegatingMultipartFormDataBodyParser` class:

@[customfileparthandler](code/JavaFileUpload.java)

Here, `akka.stream.javadsl.FileIO` class is used to create a sink that sends the `ByteString` from the Accumulator into a `java.io.File` object, rather than a TemporaryFile object.
 
Using a custom file part handler also means that behavior can be injected, so a running count of uploaded bytes can be sent elsewhere in the system.


## Cleaning up temporary files

Uploading files uses a [`TemporaryFile`](api/java/play/libs/Files.TemporaryFile.html) API which relies on storing files in a temporary filesystem.  All [`TemporaryFile`](api/java/play/libs/Files.TemporaryFile.html) references come from a [`TemporaryFileCreator`](api/java/play/libs/Files.TemporaryFileCreator.html) trait, and the implementation can be swapped out as necessary, and there's now an [`atomicMoveWithFallback`](api/java/play/libs/Files.TemporaryFile.html#temporaryFileCreator--) method that uses `StandardCopyOption.ATOMIC_MOVE` if available.

Uploading files is an inherently dangerous operation, because unbounded file upload can cause the filesystem to fill up -- as such, the idea behind [`TemporaryFile`](api/java/play/libs/Files.TemporaryFile.html) is that it's only in scope at completion and should be moved out of the temporary file system as soon as possible.  Any temporary files that are not moved are deleted. 

However, under [certain conditions](https://github.com/playframework/playframework/issues/5545), garbage collection does not occur in a timely fashion.  As such, there's also a [`play.api.libs.Files.TemporaryFileReaper`](api/scala/play/api/libs/Files$$DefaultTemporaryFileReaper.html) that can be enabled to delete temporary files on a scheduled basis using the Akka scheduler, distinct from the garbage collection method.

The reaper is disabled by default, and is enabled through configuration of `application.conf`:

```
play.temporaryFile {
  reaper {
    enabled = true
    initialDelay = "5 minutes"
    interval = "30 seconds"
    olderThan = "30 minutes"
  }
}
```

The above configuration will delete files that are more than 30 minutes old, using the "olderThan" property.  It will start the reaper five minutes after the application starts, and will check the filesystem every 30 seconds thereafter.  The reaper is not aware of any existing file uploads, so protracted file uploads may run into the reaper if the system is not carefully configured.

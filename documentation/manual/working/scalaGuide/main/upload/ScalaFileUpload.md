<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Handling file upload

## Uploading files in a form using multipart/form-data

The standard way to upload files in a web application is to use a form with a special `multipart/form-data` encoding, which lets you mix standard form data with file attachment data. 

> **Note:** The HTTP method used to submit the form must be `POST` (not `GET`).

Start by writing an HTML form:

@[file-upload-form](code/scalaguide/templates/views/uploadForm.scala.html)

Add a CSRF token to the form, unless you have the [[CSRF filter|ScalaCsrf]] disabled. The CSRF filter checks the multi-part form in the order the fields are listed, so put the CSRF token before the file input field. This improves efficiency and avoids a token-not-found error if the file size exceeds `play.filters.csrf.body.bufferSize`.

Now define the `upload` action using a `multipartFormData` body parser:

@[upload-file-action](code/ScalaFileUpload.scala)

The `ref` attribute give you a reference to a `TemporaryFile`. This is the default way the `multipartFormData` parser handles file upload.

> **Note:** As always, you can also use the `anyContent` body parser and retrieve it as `request.body.asMultipartFormData`.

At last, add a `POST` router

@[application-upload-routes](code/scalaguide.upload.fileupload.routes)

## Direct file upload

Another way to send files to the server is to use Ajax to upload the file asynchronously in a form. In this case the request body will not have been encoded as `multipart/form-data`, but will just contain the plain file content.

In this case we can just use a body parser to store the request body content in a file. For this example, let’s use the `temporaryFile` body parser:

@[upload-file-directly-action](code/ScalaFileUpload.scala)

## Writing your own body parser

If you want to handle the file upload directly without buffering it in a temporary file, you can just write your own `BodyParser`. In this case, you will receive chunks of data that you are free to push anywhere you want.

If you want to use `multipart/form-data` encoding, you can still use the default `multipartFormData` parser by providing a `FilePartHandler[A]` and using a different Sink to accumulate data.  For example, you can use a `FilePartHandler[File]` rather than a TemporaryFile by specifying an `Accumulator(fileSink)`:

@[upload-file-customparser](code/ScalaFileUpload.scala)

## Cleaning up temporary files

Uploading files uses a [`TemporaryFile`](api/scala/play/api/libs/Files$$TemporaryFile.html) API which relies on storing files in a temporary filesystem, accessible through the `ref` attribute.  All [`TemporaryFile`](api/scala/play/api/libs/Files$$TemporaryFile.html) references come from a [`TemporaryFileCreator`](api/scala/play/api/libs/Files$$TemporaryFileCreator.html) trait, and the implementation can be swapped out as necessary, and there's now an [`atomicMoveWithFallback`](api/scala/play/api/libs/Files$$TemporaryFile.html#atomicMoveWithFallback\(to:java.nio.file.Path\):play.api.libs.Files.TemporaryFile) method that uses `StandardCopyOption.ATOMIC_MOVE` if available.

Uploading files is an inherently dangerous operation, because unbounded file upload can cause the filesystem to fill up -- as such, the idea behind [`TemporaryFile`](api/scala/play/api/libs/Files$$TemporaryFile.html) is that it's only in scope at completion and should be moved out of the temporary file system as soon as possible.  Any temporary files that are not moved are deleted. 

However, under [certain conditions](https://github.com/playframework/playframework/issues/5545), garbage collection does not occur in a timely fashion.  As such, there's also a [`play.api.libs.Files.TemporaryFileReaper`](api/scala/play/api/libs/Files$$DefaultTemporaryFileReaper.html) that can be enabled to delete temporary files on a scheduled basis using the Akka scheduler, distinct from the garbage collection method.

The reaper is disabled by default, and is enabled through `application.conf`:

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

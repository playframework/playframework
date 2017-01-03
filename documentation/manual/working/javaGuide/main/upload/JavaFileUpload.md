<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Handling file upload

## Uploading files in a form using `multipart/form-data`

The standard way to upload files in a web application is to use a form with a special `multipart/form-data` encoding, which allows mixing of standard form data with file attachments. Please note: the HTTP method for the form has to be POST (not GET).

Start by writing an HTML form:

```
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
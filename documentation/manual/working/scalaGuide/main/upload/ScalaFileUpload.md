<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Handling file upload

## Uploading files in a form using multipart/form-data

The standard way to upload files in a web application is to use a form with a special `multipart/form-data` encoding, which lets you mix standard form data with file attachment data. Please note: the HTTP method used to submit the form must be POST (not GET). 

Start by writing an HTML form:

@[file-upload-form](code/scalaguide/templates/views/uploadForm.scala.html)


Now define the `upload` action using a `multipartFormData` body parser:

@[upload-file-action](code/ScalaFileUpload.scala)


The `ref` attribute give you a reference to a `TemporaryFile`. This is the default way the `mutipartFormData` parser handles file upload.

> **Note:** As always, you can also use the `anyContent` body parser and retrieve it as `request.body.asMultipartFormData`.

At last, add a POST router

@[application-upload-routes](code/scalaguide.upload.fileupload.routes)


## Direct file upload

Another way to send files to the server is to use Ajax to upload the file asynchronously in a form. In this case the request body will not have been encoded as `multipart/form-data`, but will just contain the plain file content.

In this case we can just use a body parser to store the request body content in a file. For this example, let’s use the `temporaryFile` body parser:

@[upload-file-directly-action](code/ScalaFileUpload.scala)

## Writing your own body parser

If you want to handle the file upload directly without buffering it in a temporary file, you can just write your own `BodyParser`. In this case, you will receive chunks of data that you are free to push anywhere you want.

If you want to use `multipart/form-data` encoding, you can still use the default `mutipartFormData` parser by providing your own `PartHandler[FilePart[A]]`. You receive the part headers, and you have to provide an `Iteratee[Array[Byte], FilePart[A]]` that will produce the right `FilePart`.

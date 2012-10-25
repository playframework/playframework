# Handling file upload

## Uploading files in a form using `multipart/form-data`

The standard way to upload files in a web application is to use a form with a special `multipart/form-data` encoding, which allows to mix standard form data with file attachments. Please note: the HTTP method for the form have to be POST (not GET). 

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

```
public static Result upload() {
  MultipartFormData body = request().body().asMultipartFormData();
  FilePart picture = body.getFile("picture");
  if (picture != null) {
    String fileName = picture.getFilename();
    String contentType = picture.getContentType(); 
    File file = picture.getFile();
    return ok("File uploaded");
  } else {
    flash("error", "Missing file");
    return redirect(routes.Application.index());    
  }
}
```

## Direct file upload

Another way to send files to the server is to use Ajax to upload files asynchronously from a form. In this case, the request body will not be encoded as `multipart/form-data`, but will just contain the plain file contents.

```
public static Result upload() {
  File file = request().body().asRaw().asFile();
  return ok("File uploaded");
}
```

> **Next:** [[Accessing an SQL database | JavaDatabase]]
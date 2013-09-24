<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Common template use cases

Templates, being simple functions, can be composed in any way you want. Below are a few examples of some common scenarios.

## Layout

Let’s declare a `views/main.scala.html` template that will act as a main layout template:

```html
@(title: String)(content: Html)
<!DOCTYPE html>
<html>
  <head>
    <title>@title</title>
  </head>
  <body>
    <section class="content">@content</section>
  </body>
</html>

```

As you can see, this template takes two parameters: a title and an HTML content block. Now we can use it from another `views/Application/index.scala.html` template:

```html
@main(title = "Home") {
    
  <h1>Home page</h1>
    
}
```

> **Note:** You can use both named parameters (like `@main(title = "Home")` and positional parameters, like `@main("Home")`. Choose whichever is clearer in a specific context.

Sometimes you need a second page-specific content block for a sidebar or breadcrumb trail, for example. You can do this with an additional parameter:

```html
@(title: String)(sidebar: Html)(content: Html)
<!DOCTYPE html>
<html>
  <head>
    <title>@title</title>
  </head>
  <body>
    <section class="content">@content</section>
    <section class="sidebar">@sidebar</section>
  </body>
</html>
```

Using this from our ‘index’ template, we have:

```html
@main("Home") {
  <h1>Sidebar</h1>

} {
  <h1>Home page</h1>

}
```

Alternatively, we can declare the sidebar block separately:

```html
@sidebar = {
  <h1>Sidebar</h1>
}

@main("Home")(sidebar) {
  <h1>Home page</h1>

}
```


## Tags (they are just functions right?)

Let’s write a simple `views/tags/notice.scala.html` tag that displays an HTML notice:

```html
@(level: String = "error")(body: (String) => Html)
 
@level match {
    
  case "success" => {
    <p class="success">
      @body("green")
    </p>
  }

  case "warning" => {
    <p class="warning">
      @body("orange")
    </p>
  }

  case "error" => {
    <p class="error">
      @body("red")
    </p>
  }
    
}
```

And now let’s use it from another template:

```html
@import tags._
 
@notice("error") { color =>
  Oops, something is <span style="color:@color">wrong</span>
}
```

## Includes

Again, there’s nothing special here. You can just call any other template you like (or in fact any other function, wherever it is defined):

```html
<h1>Home</h1>
 
<div id="side">
  @common.sideBar()
</div>
```

## moreScripts and moreStyles equivalents
> **Next:** [[HTTP form submission and validation | ScalaForms]]
To define old moreScripts or moreStyles variables equivalents (like on Play! 1.x) on a Scala template, you can define a variable in the main template like this :

```html
@(title: String, scripts: Html = Html(""))(content: Html)

<!DOCTYPE html>

<html>
    <head>
        <title>@title</title>
        <link rel="stylesheet" media="screen" href="@routes.Assets.at("stylesheets/main.css")">
        <link rel="shortcut icon" type="image/png" href="@routes.Assets.at("images/favicon.png")">
        <script src="@routes.Assets.at("javascripts/jquery-1.7.1.min.js")" type="text/javascript"></script>
        @scripts
    </head>
    <body>
        <div class="navbar navbar-fixed-top">
            <div class="navbar-inner">
                <div class="container">
                    <a class="brand" href="#">Movies</a>
                </div>
            </div>
        </div>
        <div class="container">
            @content
        </div>
    </body>
</html>
```

And on an extended template that need an extra script : 

```html
@scripts = {
    <script type="text/javascript">alert("hello !");</script>
}

@main("Title",scripts){

   Html content here ...

}

```

And on an extended template that not need an extra script, just like this :

```html
@main("Title"){

   Html content here ...

}
```

> **Next:** [[Custom formats | JavaCustomTemplateFormat]]
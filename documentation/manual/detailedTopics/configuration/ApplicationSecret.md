# The Application Secret

Play uses a secret key for a number of things, including:

* Signing session cookies and CSRF tokens
* Built in encryption utilities

It is configured in `application.conf`, with the property name `play.crypto.secret`, and defaults to `changeme`.  As the default suggests, it should be changed for production.

> When started in prod mode, if Play finds that the secret is not set, or if it is set to `changeme`, Play will throw an error.

## Best practices

Anyone that can get access to the secret will be able to generate any session they please, effectively allowing them to log in to your system as any user they please.  Hence it is strongly recommended that you do not check your application secret into source control.  Rather, it should be configured on your production server.  This means that it is considered bad practice to put the production application secret in `application.conf`.

One way of configuring the application secret on a production server is to pass it as a system property to your start script.  For example:

```bash
/path/to/yourapp/bin/yourapp -Dplay.crypto.secret="QCY?tAnfk?aZ?iwrNwnxIlR6CTf:G3gf:90Latabg@5241AB`R5W:1uDFN];Ik@n"
```

This approach is very simple, and we will use this approach in the Play documentation on running your app in production mode as a reminder that the application secret needs to be set.  In some environments however, placing secrets in command line arguments is not considered good practice.  There are two ways to address this.

### Environment variables

The first is to place the application secret in an environment variable.  In this case, we recommend you place the following configuration in your `application.conf` file:

    play.crypto.secret="changeme"
    play.crypto.secret=${?APPLICATION_SECRET}

The second line in that configuration sets the secret to come from an environment variable called `APPLICATION_SECRET` if such an environment variable is set, otherwise, it leaves the secret unchanged from the previous line.

This approach works particularly well for cloud based deployment scenarios, where the normal practice is to set passwords and other secrets via environment variables that can be configured through the API for that cloud provider.

### Production configuration file

Another approach is to create a `production.conf` file that lives on the server, and includes `application.conf`, but also overrides any sensitive configuration, such as the application secret and passwords.

For example:

    include "application"

    play.crypto.secret="QCY?tAnfk?aZ?iwrNwnxIlR6CTf:G3gf:90Latabg@5241AB`R5W:1uDFN];Ik@n"

Then you can start Play with:

```bash
/path/to/yourapp/bin/yourapp -Dconfig.file=/path/to/production.conf
```

## Generating an application secret

Play provides a utility that you can use to generate a new secret.  Run `play-generate-secret` in the Play console.  This will generate a new secret that you can use in your application.  For example:

```
[my-first-app] $ play-generate-secret
[info] Generated new secret: QCYtAnfkaZiwrNwnxIlR6CTfG3gf90Latabg5241ABR5W1uDFNIkn
[success] Total time: 0 s, completed 28/03/2014 2:26:09 PM
```

## Updating the application secret in application.conf

Play also provides a convenient utility for updating the secret in `application.conf`, should you want to have a particular secret configured for development or test servers.  This is often useful when you have encrypted data using the application secret, and you want to ensure that the same secret is used every time the application is run in dev mode.

To update the secret in `application.conf`, run `play-update-secret` in the Play console:

```
[my-first-app] $ play-update-secret
[info] Generated new secret: B4FvQWnTp718vr6AHyvdGlrHBGNcvuM4y3jUeRCgXxIwBZIbt
[info] Updating application secret in /Users/jroper/tmp/my-first-app/conf/application.conf
[info] Replacing old application secret: play.crypto.secret="changeme"
[success] Total time: 0 s, completed 28/03/2014 2:36:54 PM
```

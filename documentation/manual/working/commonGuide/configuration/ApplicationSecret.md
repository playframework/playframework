<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# The Application Secret

Play uses a secret key for a number of things, including:

* Signing session cookies and CSRF tokens
* Built in encryption utilities

It is configured in `application.conf`, with the property name `play.http.secret.key`, and defaults to `changeme`.  As the default suggests, it should be changed for production.

> **Note:** On startup, if Play finds that the secret is not set, or if it is set to `changeme`, or if the application secret is too short, Play will throw an error.

## Best practices

Anyone that can get access to the secret will be able to generate any session they please, effectively allowing them to log in to your system as any user they please.  Hence it is strongly recommended that you do not check your application secret into source control.  Rather, it should be configured on your production server.  This means that it is considered bad practice to put the production application secret in `application.conf`.

One way of configuring the application secret on a production server is to pass it as a system property to your start script.  For example:

```bash
/path/to/yourapp/bin/yourapp -Dplay.http.secret.key='QCY?tAnfk?aZ?iwrNwnxIlR6CTf:G3gf:90Latabg@5241AB`R5W:1uDFN];Ik@n'
```

This approach is very simple, and we will use this approach in the Play documentation on running your app in production mode as a reminder that the application secret needs to be set.  In some environments however, placing secrets in command line arguments is not considered good practice.  There are two ways to address this.

### Environment variables

The first is to place the application secret in an environment variable.  In this case, we recommend you place the following configuration in your `application.conf` file:

    play.http.secret.key="changeme"
    play.http.secret.key=${?APPLICATION_SECRET}

The second line in that configuration sets the secret to come from an environment variable called `APPLICATION_SECRET` if such an environment variable is set, otherwise, it leaves the secret unchanged from the previous line.

This approach works particularly well for cloud based deployment scenarios, where the normal practice is to set passwords and other secrets via environment variables that can be configured through the API for that cloud provider.

### Production configuration file

Another approach is to create a `production.conf` file that lives on the server, and includes `application.conf`, but also overrides any sensitive configuration, such as the application secret and passwords.

For example:

    include "application"

    play.http.secret.key="QCY?tAnfk?aZ?iwrNwnxIlR6CTf:G3gf:90Latabg@5241AB`R5W:1uDFN];Ik@n"

Then you can start Play with:

```bash
/path/to/yourapp/bin/yourapp -Dconfig.file=/path/to/production.conf
```

## Requirements for an application secret

The application secret configuration `play.http.secret.key` is checked for a minimum length, dependent on the algorithm used to sign the session and flash cookie. By default that algorithm is HS256, which requires at least 256 bits. Therefore if the key has fewer bits an error is thrown and the configuration is invalid.  For HS256, you can resolve this error by setting the secret to at least 32 bytes of completely random input, such as `head -c 32 /dev/urandom | base64` or by the application secret generator, using `playGenerateSecret` or `playUpdateSecret` as explained below.

Also, as explained above, the application secret is used as the key for ensuring that a Play session cookie is valid, i.e. has been generated by the server as opposed to spoofed by an attacker.  However, the secret only specifies a string, and does not determine the amount of entropy in that string.  Anyhow, it is possible to put an upper bound on the amount of entropy in the secret simply by measuring how short it is: if the secret is eight characters long, that is at most 64 bits of entropy, which is insufficient by modern standards.

## Generating an application secret

Play provides a utility that you can use to generate a new secret.  Run `playGenerateSecret` in the Play console.  This will generate a new secret that you can use in your application.  For example:

```
[my-first-app] $ playGenerateSecret
[info] Generated new secret: QCYtAnfkaZiwrNwnxIlR6CTfG3gf90Latabg5241ABR5W1uDFNIkn
[success] Total time: 0 s, completed 28/03/2014 2:26:09 PM
```

## Updating the application secret in application.conf

Play also provides a convenient utility for updating the secret in `application.conf`, should you want to have a particular secret configured for development or test servers.  This is often useful when you have encrypted data using the application secret, and you want to ensure that the same secret is used every time the application is run in dev mode.

To update the secret in `application.conf`, run `playUpdateSecret` in the Play console:

```
[my-first-app] $ playUpdateSecret
[info] Generated new secret: B4FvQWnTp718vr6AHyvdGlrHBGNcvuM4y3jUeRCgXxIwBZIbt
[info] Updating application secret in /Users/jroper/tmp/my-first-app/conf/application.conf
[info] Replacing old application secret: play.http.secret.key="changeme"
[success] Total time: 0 s, completed 28/03/2014 2:36:54 PM
```
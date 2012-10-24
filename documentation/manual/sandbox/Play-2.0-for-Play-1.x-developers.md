# Suggested Play 2.0 alternatives for Play 1.x features

<table>
<tr>
  <th>Feature</th>
  <th>Play 1.x</th>
  <th>Play 2.0</th>
</tr>
<tr>
  <td colspan="3">Templates</td>
</tr>
<tr>
  <td>Iterate</td>
  <td>#{list items:collection, as:'var' } ...$var...    #{/list}</td>
  <td>@for(var &lt;- collection) { ... @var ... }</td>
</tr>
<tr>
  <td>Iteration index</td>
  <td>#{list items:collection, as:'var' } ...$var_index...    #{/list}</td>
  <td>@for((var,index) &lt;- collection.zipWithIndex) { ...@index @var ... }</td>
</tr>
<tr>
  <td>Relative URL</td>
  <td>@{controller.method}</td>
  <td>@routes.Controller.method()</td>
</tr>
<tr>
  <td>Absolute URL</td>
  <td>@@{controller.method}</td>
  <td> @routes.Controller.method().absoluteURL()</td>
</tr>
<tr>
  <td>Secure URL</td>
  <td>@{Admin.login().secure()}</td>
  <td>@routes.Admin.login() .absoluteURL(secure = true)</td>
</tr>
<tr>
  <td>Asset URL</td>
  <td>@{'/public/images/logo_16x16.png'}</td>
  <td>@routes.Assets.at("images/logo_16x16.png")</td>
</tr>
<tr>
  <td>Messages (i18n)</td>
  <td>&{'key.for.message'}</td>
  <td>@Messages("key.for.message")</td>
</tr>
<tr>
  <td colspan="3">Controller</td>
</tr>
<tr>
  <td>Configuration</td>
  <td>Play.configuration.getProperty("log.level");</td>
  <td>Play.application().configuration() .getString("log.level");</td>
</tr>
<tr>
  <td>Application Path</td>
  <td>play.Play.applicationPath;</td>
  <td>play.Play.application().path();</td>
</tr>
<tr>
  <td>Run level</td>
  <td>play.Play.configuration .get("application.mode").equals("DEV");</td>
  <td>Play.application().isDev()</td>
</tr>
<tr>
  <td>HTTP Parameters</td>
  <td>params.get("name");</td>
  <td>DynamicForm form = form().bindFromRequest();
  form.get("name");</td>
</tr>
<tr>
  <td>Reverse Router</td>
  <td>Router.reverse("Users.activateUser") .add("activationKey",user.getActivationUUID());</td>
  <td>controllers.routes.Users .activateUser(user.getActivationUUID());</td>
</tr>
<tr>
  <td>Forward to View</td>
  <td>renderTemplate("Clients/showClient.html", id, client);</td>
  <td>ok(view.html.Clients .showClient( id, client ));</td>
</tr>
<tr>
  <td colspan="3">Outside the Controller scope</td>
</tr>
<tr>
  <td>Request</td>
  <td>play.mvc.Http.Request.current()</td>
  <td>Request request = play.mvc.Http.Context .current().request()</td>
</tr>
<tr>
  <td>Response</td>
  <td>play.mvc.Http.Response.current()</td>
  <td>Response response = play.mvc.Http.Context .current().response()</td>
</tr>
<tr>
  <td>Session</td>
  <td>play.mvc.Http.Session.current()</td>
  <td>Session session = play.mvc.Http.Context .current().session()</td>
</tr>
<tr>
  <td>Flash</td>
  <td>play.mvc.Http.Flash.current()</td>
  <td>Flash flash = play.mvc.Http.Context .current().flash()</td>
</tr>
<tr>
<td colspan="3">Misc</td>
<tr>
  <td>Job at start</td>
  <td>@OnApplicationStart</td>
  <td><a href="http://www.playframework.org/documentation/2.0/JavaGlobal">Global.onStart</td>
</tr>
</tr>
</table>
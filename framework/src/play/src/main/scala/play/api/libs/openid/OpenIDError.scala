package play.api.libs.openid

sealed abstract class OpenIDError(val id: String, val message: String) extends Throwable

object Errors {
  object MISSING_PARAMETERS extends OpenIDError("missing_parameters", """The OpenID server omitted parameters in the callback.""")
  object AUTH_ERROR extends OpenIDError("auth_error", """The OpenID server failed to verify the OpenID response.""")
  object AUTH_CANCEL extends OpenIDError("auth_cancel", """OpenID authentication was cancelled.""")
  object BAD_RESPONSE extends OpenIDError("bad_response", """Bad response from the OpenID server.""")
  object NO_SERVER extends OpenIDError("no_server", """The OpenID server could not be resolved.""")
  object NETWORK_ERROR extends OpenIDError("network_error", """Couldn't contact the server.""")
}


# Contains the actual business logic of the main page
define ["page", "sumWebSocket"], (page, sumWebSocket) ->

  ws = undefined

  bind = () ->
    page.showLogin()
    page.hideConnecting()
    page.hideSum()

    page.onConnectClicked () ->

      page.hideLogin()
      page.showConnecting()

      ws = new sumWebSocket(page.getPassword())

      ws.onConnected () ->
        page.hideConnecting()
        page.showSum()

      ws.onResult (result) ->
        page.setSumResult(result)

      ws.onClose () ->
        page.showLogin()
        page.hideSum()
        page.hideConnecting()

    page.onSubmitSum () ->
      values = page.getSumValues().split(/[, ]+/)
      valueInts = values.map((v) -> parseInt(v))
        .filter((v) -> !isNaN(v))
      ws.sum(valueInts)

    page.onDisconnectClicked () ->
      ws.disconnect()
      page.hideSum()
      page.showLogin()

  {
    bind: bind
  }

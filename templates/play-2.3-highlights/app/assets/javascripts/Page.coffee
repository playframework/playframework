define ["jquery"], ($) ->
  {
    showLogin: -> $("#login").show()

    showConnecting: -> $("#connecting").show()

    showSum: -> $("#sum").show()

    hideLogin: -> $("#login").hide()

    hideConnecting: -> $("#connecting").hide()

    hideSum: -> $("#sum").hide()

    onConnectClicked: (cb) -> $("#connect").click(cb)

    onDisconnectClicked: (cb) -> $("#disconnect").click(cb)

    onSubmitSum: (cb) -> $("#submitSum").click(cb)

    getPassword: -> $("#password").val()

    getSumValues: -> $("#sumValues").val()

    setSumResult: (result) -> $("#sumResult").text(result)
  }

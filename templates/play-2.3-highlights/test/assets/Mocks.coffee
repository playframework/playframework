class MockPage
  login: true
  connecting: true
  sum: true
  password: ""
  sumValues: ""

  showLogin: -> @login = true
  showConnecting: -> @connecting = true
  showSum: -> @sum = true
  hideLogin: -> @login = false
  hideConnecting: -> @connecting = false
  hideSum: -> @sum = false
  onConnectClicked: (cb) -> @connectClicked = cb
  onDisconnectClicked: (cb) -> @disconnectClicked = cb
  onSubmitSum: (cb) -> @submitSum = cb
  getPassword: -> @password
  getSumValues: -> @sumValues
  setSumResult: (result) -> @sumResult = result

class MockSumWebSocketFactory

  self = undefined

  class SumWebSocket
    constructor: (password) -> self.password = password
    onConnected: (cb) -> self.connected = cb
    onClose: (cb) -> self.close = cb
    disconnect: () -> self.disconnected = true
    sum: (values) -> self.values = values
    onResult: (cb) -> self.result = cb

  MockSumWebSocket: () -> SumWebSocket
  constructor: () ->
    self = @

exports.MockPage = MockPage
exports.MockSumWebSocketFactory = MockSumWebSocketFactory
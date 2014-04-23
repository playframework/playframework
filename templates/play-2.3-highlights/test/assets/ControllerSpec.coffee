mocks = require("./Mocks")

# Setup Squire to mock the page and colorService
runTest = (test) ->
  (done) ->
    injector = new Squire()
    page = new mocks.MockPage()
    webSocket = new mocks.MockSumWebSocketFactory()
    injector.mock("page", page)
    injector.mock("sumWebSocket", webSocket.MockSumWebSocket)

    injector.require ["./javascripts/Controller"], (main) ->
      test(main, {
        page: page,
        sumWebSocket: webSocket
      }, done)

describe "the controller", () ->

  it "should ensure only login div is shown on bind", runTest (main, deps, done) ->
    main.bind()
    assert deps.page.login
    assert !deps.page.connecting
    assert !deps.page.sum
    done()

  it "should hide the login div and show connecting when connect clicked", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    assert !deps.page.login
    assert deps.page.connecting
    assert !deps.page.sum
    done()

  it "should connect with the entered password when connect clicked", runTest (main, deps, done) ->
    main.bind()
    deps.page.password = "foo"
    deps.page.connectClicked()
    assert.equal deps.sumWebSocket.password, "foo"
    done()

  it "should hide the connecting div and show the sum div when connected", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    deps.sumWebSocket.connected()
    assert !deps.page.login
    assert !deps.page.connecting
    assert deps.page.sum
    done()

  it "should hide the sum div and show the login dev when closed", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    deps.sumWebSocket.connected()
    deps.sumWebSocket.close()
    assert deps.page.login
    assert !deps.page.connecting
    assert !deps.page.sum
    done()

  it "should disconnect the web socket when disconnect clicked", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    deps.sumWebSocket.connected()
    deps.page.disconnectClicked()
    assert deps.sumWebSocket.disconnected
    assert deps.page.login
    assert !deps.page.connecting
    assert !deps.page.sum
    done()

  it "should send the entered values when sum clicked", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    deps.page.sumValues = "1,2,3"
    deps.page.submitSum()
    assert.deepEqual deps.sumWebSocket.values, [1,2,3]
    done()

  it "should let the entered values be separated by commas or spaces", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    deps.page.sumValues = "1, 2  3,,5"
    deps.page.submitSum()
    assert.deepEqual deps.sumWebSocket.values, [1,2,3,5]
    done()

  it "should ignore values that aren't numbers", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    deps.page.sumValues = "1,foo,3"
    deps.page.submitSum()
    assert.deepEqual deps.sumWebSocket.values, [1,3]
    done()

  it "should render the sum result when returned", runTest (main, deps, done) ->
    main.bind()
    deps.page.connectClicked()
    deps.sumWebSocket.result(6)
    assert.equal deps.page.sumResult, 6
    done()

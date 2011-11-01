var websocket; 
var count =0;
$(document).ready(function(){

websocket = new WebSocket("ws://localhost:9000/sockets/reversed-echo");
var $responses = $("#responses");
    websocket.onmessage = function(evt){count=count+1; $responses.text(count)};
setInterval("websocket.send('hey there!')",1);
});
const express = require('express');
const http = require('http');
const url = require('url');
const WebSocket = require('ws');

const app = express();

app.use(function (req, res) {
  res.send({ msg: "hello" });
});

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

var players = [0,0,0,0];
var grid = new Array(5); // linhas
for (var i = 0; i < 5; i++) {
  grid[i] = new Array(16); // colunas
}

wss.broadcast = function broadcast(data) {
  wss.clients.forEach(function each(client) {
    if (client.readyState === WebSocket.OPEN) {
      client.send(data);
    }
  });
};

wss.on('connection', function connection(ws) {
  const location = url.parse(ws.upgradeReq.url, true);
  // You might use location.query.access_token to authenticate or share sessions
  // or ws.upgradeReq.headers.cookie (see http://stackoverflow.com/a/16395220/151312)

  ws.on('message', function incoming(message) {
    console.log('received: %s', message);
    var msg = JSON.parse(message);

    if (msg.type == 'user login') {
      var userName = msg.data;
      console.log('logou...');
      var playerId = players.indexOf(0);
      ws.id = playerId;
      var x, y;
      do {
        x = Math.floor(Math.random() * 4) + 1; // coluna: 1..4 (1a tela somente)
        y = Math.floor(Math.random() * 5) + 1; // linha: 1..5

      } while (grid[y-1][x-1] !== undefined);
      grid[y-1][x-1] = playerId;
      players[playerId] = {id: playerId, name: userName, x: x, y: y};
      console.log('playerId: ' + playerId + ' x: ' + x + ' y: ' + y);
      ws.x = x;
      ws.y = y;

      // envia os usuários já logados
      var list = [];
      for (var i=0; i<playerId; i++) {
        list.push(players[i]);
      }
      if (list.length > 0) {
        ws.send(JSON.stringify({type: 'onUserJoined', data: list}));
      }

      //ws.send({type: 'loginOk', data: players[playerId]});
      wss.broadcast(JSON.stringify({type: 'onUserJoined', data: [players[playerId]]}));
    } // if (msg.type == 'user login') {

    if (msg.type == 'move') {
      var direction = msg.data;
      var t1 = new Date().getTime();
      var playerId = ws.id;
      var canMove = true;
      var x = ws.x - 1;
      var y = ws.y - 1;
      switch (direction[0]) {
        case "L": x--; break;
        case "U": y--; break;
        case "D": y++; break;
        case "R": x++; break;
      }
      if ( (x >= 0) && (x <=15) && (y >= 0) && (y <= 4) && (grid[y][x] === undefined) ) {
        grid[ws.y-1][ws.x-1] = undefined;
        grid[y][x] = playerId;
        players[playerId].x = x+1;
        players[playerId].y = y+1;
        wss.broadcast(JSON.stringify({type: 'onMove', data: {id: playerId, xFrom: ws.x, yFrom: ws.y, xTo: x+1, yTo: y+1}})); // para todo mundo inclusive o "socket"
        ws.x = x+1;
        ws.y = y+1;
      }

      var t2 = new Date().getTime();
      console.log('Miliseg: ' + (t2- t1));
    } // if (msg.type == 'move') {

  });

  ws.on('close', function (code, reason) {
    console.log('desconectou...');
    players[ws.id] = 0;
    grid[ws.y-1][ws.x-1] = undefined;
    wss.broadcast(JSON.stringify({type: 'user left', data: {id: ws.id}}));
  });

});

server.listen(3000, function listening() {
  console.log('Listening on %d', server.address().port);
});

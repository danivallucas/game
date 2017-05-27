// Setup basic express server
var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io')(server);
var port = process.env.PORT || 3000;
var crypto = require('crypto');
var base64url = require('base64url');

function token(size) {
  return base64url(crypto.randomBytes(size));
}

server.listen(port, function () {
  console.log('Server listening at port %d', port);
});

// Routing
app.use(express.static(__dirname + '/public'));

var COLUMNS_COUNT = 4; // 4, 8, 12, 16
var MAX_PILL_COUNT = COLUMNS_COUNT / 4;
var MAX_FOOD_COUNT = 4;
var started = false;
var pillCount = 0;
var foodCount = 0;
var score = [0, 0];
var players = []; // propriedades col e row -> 1-index
var bases = [];

if (COLUMNS_COUNT == 4) {
  bases = [{type: 'base',  id: 0, team:  0, flag: 100, points: 0, col: 1,  row: 1},
           {type: 'base',  id: 1, team: -1, flag: 0,   points: 0, col: 3,  row: 2},
           {type: 'base',  id: 2, team: -1, flag: 0,   points: 0, col: 2,  row: 4},
           {type: 'base',  id: 3, team:  1, flag: 100, points: 0, col: 4,  row: 5}];
}

if (COLUMNS_COUNT == 16) {
  bases = [{type: 'base',  id: 0, team:  0, flag: 100, points: 0, col: 1,  row: 1},
           {type: 'base',  id: 1, team: -1, flag: 0,   points: 0, col: 6,  row: 5},
           {type: 'base',  id: 2, team: -1, flag: 0,   points: 0, col: 11, row: 1},
           {type: 'base',  id: 3, team:  1, flag: 100, points: 0, col: 16, row: 5}];
}

// novo grid
var grid = new Array(5); // linhas -> 0-index
for (var row = 0; row < 5; row++) {
  grid[row] = new Array(COLUMNS_COUNT); // colunas -> 0-index
  for (var col = 0; col < COLUMNS_COUNT; col++) {
    grid[row][col] = {type: 'group', id: -1, team: -1, members: []}; // objeto default é grupo
  }
}
// insere bases
for (var i = 0; i < bases.length; i++) {
  grid[bases[i].row-1][bases[i].col-1] = {type: 'base', id: bases[i].id, team: bases[i].team, members: []};
}
// insere fences

if (COLUMNS_COUNT == 4) {
  grid[3-1][2-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[3-1][3-1] = {type: 'fence', id: -1, team: -1, members: []};
}

if (COLUMNS_COUNT == 16) {
  grid[4-1][2-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[4-1][3-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[2-1][4-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[2-1][6-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[2-1][7-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[3-1][7-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[2-1][8-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[4-1][9-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[3-1][10-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[4-1][10-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[4-1][11-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[4-1][13-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[2-1][14-1] = {type: 'fence', id: -1, team: -1, members: []};
  grid[2-1][15-1] = {type: 'fence', id: -1, team: -1, members: []};
}

function newPill () {
  var timeRand = 30000 + (Math.floor(Math.random() * 30000));
  setTimeout(function(){ newPill(); }, timeRand);

  if (pillCount == MAX_PILL_COUNT) { return; }
  pillCount++;

  var col, row;
  do {
    col = Math.floor(Math.random() * COLUMNS_COUNT) + 1; // coluna: 1..COLUMNS_COUNT
    row = Math.floor(Math.random() * 5) + 1; // linha: 1..5
  } while ( (grid[row-1][col-1].type != 'group') || (grid[row-1][col-1].length > 0) );

  grid[row-1][col-1] = {type: 'pill', id: -1, team: -1, members: []};
  io.emit('onNewMapItems', [{type: 'cell', col: col, row: row, cell: grid[row-1][col-1], flag: 0}]); // para todo mundo inclusive o "socket"
}

function canAttack(col, row, team) { // 0-index
  if ( (col < 0) || (col > (COLUMNS_COUNT-1)) || (row < 0) || (row > 4) ) { return false; }
  if (grid[row][col].type == 'fence') { return false; }
  if ( (grid[row][col].type == 'group') && (grid[row][col].team != -1) && (grid[row][col].team == team) ) { return false; }
  if ( (grid[row][col].type == 'base') && (grid[row][col].members.length > 0) && (grid[row][col].team == team) ) { return false; }
  return true;
}

function canMove(col, row, team) { // 0-index
  if ( (col < 0) || (col > (COLUMNS_COUNT-1)) || (row < 0) || (row > 4) ) { return false; }
  if (grid[row][col].type == 'fence') { return false; }
  if ( (grid[row][col].type == 'group') && (grid[row][col].team != -1) && (grid[row][col].team != team) ) { return false; }
  if ( (grid[row][col].type == 'base') && (grid[row][col].members.length > 0) && (grid[row][col].team != team) ) { return false; }
  return true;
}

function removeMember(col, row, id) {
  var index;
  var members = grid[row][col].members;
  for (var i=0; i<members.length; i++) {
    if (members[i].id == id) {
      index = i;
    }
  }
  grid[row][col].members.splice(index, 1);
  if (grid[row][col].type == 'group') {
    grid[row][col].team = (grid[row][col].members.length == 0) ? -1 : grid[row][col].team; // se vazia a célula passa a não pertencer a nenhum time
  }
}

function spawn(playerId) {
  var player = players[playerId];
  var team = player.team;

  // procura uma base desse time para nascer
  var baseId = -1;
  if (team == 0) {
    for (var i=0; i<4; i++) {
      if (bases[i].team == team) {
        baseId = i;
        break;
      }
    }
  }
  if (team == 1) {
    for (var i=3; i>=0; i--) {
      if (bases[i].team == team) {
        baseId = i;
        break;
      }
    }
  }
  if (baseId == -1) {
    setTimeout(function(){ spawn(playerId); }, 100); // tenta de novo
    return;
  }
  player.life = 3;
  player.col = bases[baseId].col;
  player.row = bases[baseId].row;
  grid[player.row-1][player.col-1].members.push({id: playerId});
  io.emit('onLifeChanged', {id: playerId, life: players[playerId].life}); // para todo mundo inclusive o "socket"
  io.emit('onNewMapItems', [{type: 'cell', col: player.col, row: player.row, cell: grid[player.row-1][player.col-1], flag: bases[baseId].flag}]);
}

function baseManager() {
  var scoreChanged = false;
  for (var i=0; i<4; i++) {
    col = bases[i].col;
    row = bases[i].row;
    if (bases[i].team == -1) { // base sem time
      bases[i].flag += grid[row-1][col-1].members.length;
      bases[i].flag = Math.max(0, Math.min(100, bases[i].flag)); // entre 0 e 100
      if (bases[i].flag == 100) { // base totalmente capturada
        bases[i].team = grid[row-1][col-1].team;
        bases[i].points = 0;
        console.log('Base totalmente capturada: ' + bases[i].id);
        io.emit('onBaseChanged', bases[i]);
      }
    } else { // base pertence a um time
      bases[i].points++;
      if (bases[i].points == 200) {
        score[bases[i].team] += 10; // 10 pontos a cada 10 segundos em que a base pertence a um time
        bases[i].points = 0;
        scoreChanged = true;
      }
    }
  }
  scoreChanged && io.emit('onScoreChanged', {scoreA: score[0], scoreB: score[1]});
  setTimeout(function(){ baseManager(); }, 100);
}

function start() {
  if (started) { return; }
  var timeRand = 30000 + (Math.floor(Math.random() * 30000));
  setTimeout(function(){ newPill(); }, timeRand);
  setTimeout(function(){ baseManager(); }, 100);
  started = true;
}

io.on('connection', function (socket) {
  console.log('connection: ' + socket.id);

  socket.on('playerLogin', function (playerName, emoji) {
    console.log('playerLogin: ' + playerName + ' emoji: ' + emoji);
    // tenta pegar um id disponível na lista de jogadores
    //players[socket.playerId] = {type: 'player', id: socket.playerId, name: playerName, token: token(4), onLine: true, life: 3, team: team, col: col, row: row};
    socket.playerId = (players.indexOf(undefined) != -1) ? players.indexOf(undefined) : players.push(undefined) - 1;
    var team = socket.playerId % 2; // team 0 = pares / team 1 = ímpares
    var timeLastMove = Date.now() - 5000; // já nasce podendo se mover
    players[socket.playerId] = {type: 'player', id: socket.playerId, name: playerName, emoji: emoji, token: token(4), onLine: true, team: team, life: 0, energy: 5*5000, timeLastMove: timeLastMove};
    sendLoginResult(socket);
    socket.broadcast.emit('onNewMapItems', [players[socket.playerId]]);
    spawn(socket.playerId);
    start();
  });

  socket.on('playerReconnect', function (id, token) {
    console.log('');
    console.log('playerReconnect: id=' + id + ' token=' + token);
    // Ver se esse id existe ou se é um id antigo que se perdeu em um restart do servidor
    if ( (players[id] == undefined) || (players[id].token != token) ) {
      console.log('Id inválido!');
      socket.emit('onInvalidId');
      return;
    }

    socket.playerId = id;
    players[socket.playerId].onLine = true;
    sendLoginResult(socket);
    socket.broadcast.emit('onPlayerOnline', players[socket.playerId]);
  });

  socket.on('move', function (direction) {
    console.log('move: ' + direction + ' socket.playerId: ' + socket.playerId);
    var colFrom = players[socket.playerId].col - 1;
    var rowFrom = players[socket.playerId].row - 1;
    var colTo = colFrom;
    var rowTo = rowFrom;
    switch (direction[0]) {
      case "L": colTo--; break;
      case "U": rowTo--; break;
      case "D": rowTo++; break;
      case "R": colTo++; break;
    }

    if (!canMove(colTo, rowTo, players[socket.playerId].team)) {
      socket.emit('onCantMove', direction);
      return;
    }

    // processa célula de origem
    removeMember(colFrom, rowFrom, socket.playerId);

    // processa célula de destino
    if (grid[rowTo][colTo].type == 'pill') {
      players[socket.playerId].life = 3;
      io.emit('onLifeChanged', {id: socket.playerId, life: players[socket.playerId].life}); // para todo mundo inclusive o "socket"
      io.emit('onRemoveMapItem', {type: 'pill', id: grid[rowTo][colTo].id}); // para todo mundo inclusive o "socket"
    }

    if (grid[rowTo][colTo].type == 'base') {
      var baseId = grid[rowTo][colTo].id;
      if (bases[baseId].team != players[socket.playerId].team) { // entrou em uma base que era do outro time
        bases[baseId].team = -1; // remove a bandeira
        bases[baseId].flag = 0;
        bases[baseId].points = 0;
        io.emit('onBaseChanged', bases[baseId]);
      }
    }

    grid[rowTo][colTo].type = (grid[rowTo][colTo].type == 'base') ? 'base' : 'group';
    grid[rowTo][colTo].members.push({id: socket.playerId}) ;
    grid[rowTo][colTo].team = players[socket.playerId].team;

    var flag = 0; // se a célula de origem ou a de destino for base, envia o status da flag
    if (grid[rowFrom][colFrom].type == 'base') {
      var baseId = grid[rowFrom][colFrom].id;
      flag = bases[baseId].flag;
    }
    if (grid[rowTo][colTo].type == 'base') {
      var baseId = grid[rowTo][colTo].id;
      flag = bases[baseId].flag;
    }

    io.emit('onMove', {id: socket.playerId, colFrom: colFrom+1, rowFrom: rowFrom+1, cellFrom: grid[rowFrom][colFrom], colTo: colTo+1, rowTo: rowTo+1, cellTo: grid[rowTo][colTo], flag: flag}); // para todo mundo inclusive o "socket"
    players[socket.playerId].col = colTo+1;
    players[socket.playerId].row = rowTo+1;
    players[socket.playerId].energy -= 5000;
    players[socket.playerId].energy = Math.max(0, players[socket.playerId].energy);
    players[socket.playerId].timeLastMove = Date.now();
  });

  socket.on('attack', function (direction) {
    console.log('attack: ' + direction + ' socket.playerId: ' + socket.playerId);
    var colTo = players[socket.playerId].col - 1;
    var rowTo = players[socket.playerId].row - 1;
    switch (direction[0]) {
      case "L": colTo--; break;
      case "U": rowTo--; break;
      case "D": rowTo++; break;
      case "R": colTo++; break;
    }

    if (!canAttack(colTo, rowTo, players[socket.playerId].team)) {
      socket.emit('onCantAttack', direction);
      return;
    }

    io.emit('onAttack', {id: socket.playerId, colFrom: players[socket.playerId].col, rowFrom: players[socket.playerId].row, colTo: colTo+1, rowTo: rowTo+1});
    setTimeout(function(){ attack(colTo+1, rowTo+1); }, 500);
  });

  socket.on('playerLeave', function () {
    console.log('playerLeave: ' + socket.playerId);
    socket.broadcast.emit('onRemoveMapItem', {type: 'player', id: socket.playerId});
    removeMember(players[socket.playerId].col-1, players[socket.playerId].row-1, socket.playerId);
    players[socket.playerId] = undefined;
  });

  socket.on('disconnect', function () {
    console.log('disconnect: ' + socket.playerId);
    if ( (socket.playerId === undefined) || (players[socket.playerId] == undefined) ) { return }
    players[socket.playerId].onLine = false;
    socket.broadcast.emit('onPlayerOffline', players[socket.playerId]);
  });

});

function sendLoginResult(socket) {
  //var timeToMove = Math.max(0, 5000 - (Date.now() - players[socket.playerId].timeLastMove));
  players[socket.playerId].energy += Math.floor((Date.now() - players[socket.playerId].timeLastMove)/1000)*1000;
  players[socket.playerId].energy = Math.min(5*5000, players[socket.playerId].energy);
  socket.emit('onLoginOk', {COLUMNS_COUNT: COLUMNS_COUNT, id: players[socket.playerId].id, token: players[socket.playerId].token, energy: players[socket.playerId].energy});
  // send MapItems
  var list = [];
  list = list.concat(bases.filter(Boolean)); // bases
  list = list.concat(players.filter(Boolean)); // players
  for (var row = 0; row < 5; row++) {
    for (var col = 0; col < COLUMNS_COUNT; col++) {
      if ( (grid[row][col].type != 'group') || (grid[row][col].members.length>0) ) {
        var flag = 0; // se essa célula for base, envia o status da flag
        if (grid[row][col].type == 'base') {
          var baseId = grid[row][col].id;
          flag = bases[baseId].flag;
        }
        list.push({type: 'cell', col: col+1, row: row+1, cell: grid[row][col], flag: flag}); // cells
      }
    }
  }

  (list.length > 0) && socket.emit('onNewMapItems', list);
}

function attack(col, row) { // 1-index
  if ( (grid[row-1][col-1].type != 'base') && (grid[row-1][col-1].type != 'group') ) { return; }
  var members = [];
  members = members.concat(grid[row-1][col-1].members); // faz uma cópia
  for (i=0; i<members.length; i++) {
    var playerId = members[i].id;
    players[playerId].life--;
    if (players[playerId].life == 0) { // morreu
      score[(players[playerId].team+1)%2] += 5; // soma 5 pontos para o time adversário
      io.emit('onScoreChanged', {scoreA: score[0], scoreB: score[1]});
      console.log('---> id: ' + playerId + ' morreu!');
      removeMember(col-1, row-1, playerId);
      var flag = 0; // se essa célula for base, envia o status da flag
      if (grid[row-1][col-1].type == 'base') {
        var baseId = grid[row-1][col-1].id;
        flag = bases[baseId].flag;
      }
      io.emit('onDied', {id: playerId, col: col, row: row, cell: grid[row-1][col-1], flag: flag}); // para todo mundo inclusive o "socket"
      //setTimeout(function(){ spawn(playerId); }, 3000); // não funciona!!! a variável playerId é modificada antes dos 3seg... são feitas n chamadas so spawn todas com o último valor atribuído ao playerId
      setTimeout(function(id){ spawn(id); }, 3000, playerId); // playerId passado como parâmetro resolve o problema acima
    }
    io.emit('onLifeChanged', {id: playerId, life: players[playerId].life}); // para todo mundo inclusive o "socket"
  }
}

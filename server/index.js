var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io')(server);
var port = process.env.PORT || 3000;
var crypto = require('crypto');
var base64url = require('base64url');
var googleMapsClient = require('@google/maps').createClient({ key: 'AIzaSyCdjD_nesJlYD6hURCSLbxMmc6YIbAV1zo' });
var config = require('./config');
var flags = require('./flags');
var tools = require('./tools');

const conf = config.generalParams;
var metersToLat = tools.metersToLat;
var metersToLng = tools.metersToLng;
var getDist = tools.getDist;
var rotatePoint = tools.rotatePoint;
Math.toDegrees = tools.toDegrees;
Math.toRadians = tools.toRadians;

server.listen(port, function () {
  console.log('Server listening at port %d', port);
});

app.use(express.static(__dirname + '/public'));

var playerList = [];
var foodList = [];
var bombList = [];
var energyBallList = [];
var flagList = flags.flagList;

// http://www.jstips.co/en/javascript/picking-and-rejecting-object-properties/
// retorna apenas os campos "keys" do objeto
function pick (obj, keys) {
    return keys.map(k => k in obj ? {[k]: obj[k]} : {}).reduce((res, o) => Object.assign(res, o), {});
}

// retorna os outros campos exceto "keys" do objeto
function reject (obj, keys) {
    const vkeys = Object.keys(obj).filter(k => !keys.includes(k));
    return pick(obj, vkeys);
}
// **************************************************************************

function token (size) {
  return base64url(crypto.randomBytes(size));
}

function setFlagEnergyAndPoints(flag) {
  var population = Math.max(flag.population, conf.CITY_POPULATION_MIN); //limita a população no MIN
  flag.points = population / conf.CITY_POPULATION_MIN * conf.CITY_POINTS_MIN;
  population = Math.min(population, conf.CITY_POPULATION_MAX); //limita a população no MAX
  flag.energy = Math.floor( conf.CITY_ENERGY_MIN + (population - conf.CITY_POPULATION_MIN) / (conf.CITY_POPULATION_MAX - conf.CITY_POPULATION_MIN) * (conf.CITY_ENERGY_MAX - conf.CITY_ENERGY_MIN) );
}

function getFlagByPlaceId(placeId) {
  for (var i = 0; i < flagList.length; i++)
    if (flagList[i].placeId == placeId)
      return flagList[i];
  return undefined;
}

// callback de  googleMapsClient.reverseGeocode
function newFlag(err, response) {
  if (!err) {
    console.log('newFlag.......');
    var r = response.json.results[0];
    if (getFlagByPlaceId(r.place_id) != undefined) return; // cidade já cadastrada
    var loc = r.geometry.location;
    var vp = r.geometry.viewport;
    var dist = getDist(vp.northeast.lat, vp.northeast.lng, vp.southwest.lat, vp.southwest.lng);
    var percent = dist > conf.CITY_PORTVIEW_DIAGONAL_MAX ? 1 : dist/conf.CITY_PORTVIEW_DIAGONAL_MAX;
    population = Math.floor(Math.pow(70, percent-1) * conf.CITY_POPULATION_MAX);
    flagList.push({id: ++flags.lastId, type: 'city', fmtAddress: r.formatted_address, population: population, lat: loc.lat, lng: loc.lng, placeId: r.place_id,  viewport: {northeast: {lat: vp.northeast.lat, lng: vp.northeast.lng}, southwest: {lat: vp.southwest.lat, lng: vp.southwest.lng}}, energy: 0, wall: 0, playerId: -1});
    console.log('newFlag....... inserido!');
    var flag = getFlagByPlaceId(r.place_id);
    setFlagEnergyAndPoints(flag);
    console.log('newFlag....... points: ' + flag.points);
    io.emit('onNewFlag', [flag]);
  }
}

function insideCity(lat, lng) {
  for (var i = 0; i < flagList.length; i++)
    if ( (lat > flagList[i].viewport.southwest.lat) && (lat < flagList[i].viewport.northeast.lat) &&
         (lng > flagList[i].viewport.southwest.lng) && (lng < flagList[i].viewport.northeast.lng) )
         return true;
  return false;
}

function getDuration(type, playerEnergy, lat1, lng1, lat2, lng2) {
  var distance = getDist(lat1, lng1, lat2, lng2);
  var vel;
  if (type == 'direct') {
    vel = conf.DIRECT_VEL;
  } else { // driving
    var energyRange = conf.MAX_ESTIMATED_ENERGY - conf.START_ENERGY;
    var percentEnergy = (playerEnergy - conf.START_ENERGY)/energyRange;
    var velRange = conf.DRIVING_MAX_VEL - conf.DRIVING_MIN_VEL;
    vel = conf.DRIVING_MIN_VEL + Math.floor(Math.pow(0.00001, percentEnergy) * velRange);
  }
  return distance*60*60/vel;
}

function calcLeg(playerEnergy, leg) {
  leg.totalDuration = 0;
  for (var i = 0; i < leg.pointList.length; i++) {
    var duration = 0;
    if (i < (leg.pointList.length - 1)) {
      duration = getDuration(leg.type, playerEnergy, leg.pointList[i].lat, leg.pointList[i].lng, leg.pointList[i+1].lat, leg.pointList[i+1].lng);
      leg.pointList[i].duration = duration;
    }
    leg.totalDuration += duration;
  }
  leg.now = Date.now();
  leg.endTime = leg.now + leg.totalDuration;
}

function newFood() {

  function count(list, flag) { // list pode ser foodList ou playerList
    var result = 0;
    for (var i = 0; i < list.length; i++) {
      if (list[i] == undefined) continue;
      if ( (list[i].status != undefined) && (list[i].status != 'in') ) continue;
      if ( (list[i].lat > flag.viewport.southwest.lat) && (list[i].lat < flag.viewport.northeast.lat) &&
           (list[i].lng > flag.viewport.southwest.lng) && (list[i].lng < flag.viewport.northeast.lng) )
        result++
    }
    return result;
  }

  function hit(lat, lng, list) {
    for (var i = 0; i < list.length; i++) {
      if (getDist(lat, lng, list[i].lat, list[i].lng) < list[i].energy)
        return true;
    }
    return false;
  }

  for (var i = 0; i < flagList.length; i++) {
    var flag = flagList[i];
    if (count(foodList, flag) >= count(playerList, flag) * conf.FOOD_MAX_PER_PLAYER) continue;
    var latRand, lngRand;
    do {
      deltaLat = flag.viewport.northeast.lat - flag.viewport.southwest.lat;
      deltaLng = flag.viewport.northeast.lng - flag.viewport.southwest.lng;
      latRand = Math.random() * deltaLat + flag.viewport.southwest.lat;
      lngRand = Math.random() * deltaLng + flag.viewport.southwest.lng;
    } while (hit(latRand, lngRand, playerList)); // enquanto estiver pegando ponto dentro de algum player
    var foodId = (foodList.indexOf(undefined) != -1) ? foodList.indexOf(undefined) : foodList.push(undefined) - 1;
    var rand = Math.random();
    var foodType;
    if (rand < 0.35) foodType = 0; // Probabilidades:  = 100%
    if ( (rand >= 0.35) && (rand < 0.57) ) foodType = 1;
    if ( (rand >= 0.57) && (rand < 0.69) ) foodType = 2;
    if ( (rand >= 0.69) && (rand < 0.78) ) foodType = 3;
    if ( (rand >= 0.78) && (rand < 0.85) ) foodType = 4;
    if ( (rand >= 0.85) && (rand < 0.90) ) foodType = 5;
    if ( (rand >= 0.90) && (rand < 0.94) ) foodType = 6;
    if ( (rand >= 0.94) && (rand < 0.97) ) foodType = 7;
    if ( (rand >= 0.97) && (rand < 0.99) ) foodType = 8;
    if (rand >= 0.99) foodType = 9;
    foodList[foodId] = {id: foodId, type: foodType, lat: latRand, lng: lngRand, energy: conf.FOOD_ENERGY[foodType]};
    io.emit('onNewFood', [foodList[foodId]]); // para todo mundo

  }
  setTimeout(function(){ newFood(); }, conf.NEW_FOOD_TIME);
}

function onPlayerStop(player) {
  if (!insideCity(player.lat, player.lng)) // fez o spawn fora de todas as cidades cadastradas
    googleMapsClient.reverseGeocode({latlng: [player.lat, player.lng], result_type: ['locality']}, newFlag); // tenta cadastrar uma nova cidade para esse local

  console.log('onPlayerStop');
  // Bombs
  var energyChanged = false;
  var boom = false;
  for (var i = 0; i < bombList.length; i++) {
    if (bombList[i] == undefined) continue;
    var bomb = bombList[i];
    if ( ( (player.energy+1) - bomb.energy) >= getDist(player.lat, player.lng, bomb.lat, bomb.lng)) {
      // (player.energy+1) --> a energia + 1 metro (para garantir que conseguirá capturar um item de mesmo tamanho
      // independente de pequenas variações no cálculo do getDist)
      boom = true;
      io.emit('onRemoveBomb', {id: bomb.id});
      bombList[i] = undefined;
    }
  }
  if (boom) { // se foi atingido por uma ou mais bombas
    if (player.energy < conf.ENERGY_BALL_DEFAULT_ENERGY * 2) { // morre! muito pequeno para se dividir em 2 para criar energyBall(s)
      player.status = 'out';
      clearTimeout(player.scheduledMove);
      io.emit('onPlayerOut', {id: player.id, status: player.status});
      return;
    }
    // suficientemente grande para se dividir em 2 para criar energyBall(s)
    var energyBallCount = Math.floor((player.energy/2)/conf.ENERGY_BALL_DEFAULT_ENERGY); // 30 de energia para cada bola a ser gerada
    var deltaDegree = 360/energyBallCount;
    var latBall = player.lat + metersToLat(player.lat, player.lng, player.energy); // a 1a vai ao Norte
    var lngBall = player.lng;
    var newEnergyBallList = [];
    for (var i = 0; i < energyBallCount; i++) {
      // ver se algum outro player captura essa nova energyBall
      var captured = false;
      for (var j = 0; j < playerList.length; j++) {
        if (playerList[j] == undefined) continue;
        if (playerList[j].id == player.id) continue;
        if (playerList[j].status != 'in') continue;
        var player2 = playerList[j];
        if ( (player2.energy - conf.ENERGY_BALL_DEFAULT_ENERGY) >= getDist(latBall, lngBall, player2.lat, player2.lng)) {
          player2.energy += conf.ENERGY_BALL_DEFAULT_ENERGY;
          io.emit('onEnergyChange', {id: player2.id, energy: player2.energy});
          captured = true;
          break;
        }
      }
      if (captured) continue;
      // não foi capturada por nenhum outro player -> cria no mapa
      var energyBallId = (energyBallList.indexOf(undefined) != -1) ? energyBallList.indexOf(undefined) : energyBallList.push(undefined) - 1;
      energyBallList[energyBallId] = {id: energyBallId, type: 0, lat: latBall, lng: lngBall, energy: conf.ENERGY_BALL_DEFAULT_ENERGY};
      newEnergyBallList.push(energyBallList[energyBallId]);
      var latLng = rotatePoint(latBall, lngBall, player.lat, player.lng, deltaDegree);
      latBall = latLng.lat;
      lngBall = latLng.lng;
    }
    io.emit('onNewEnergyBall', newEnergyBallList); // para todo mundo
    player.energy = Math.floor(player.energy/2);
    energyChanged = true;
  }
  // Outros players
  for (var i = 0; i < playerList.length; i++) {
    if (playerList[i] == undefined) continue;
    if (playerList[i].id == player.id) continue;
    if (playerList[i].status != 'in') continue;
    var player2 = playerList[i];
    // É capturado por outro
    if ( (player2.energy - player.energy) >= getDist(player.lat, player.lng, player2.lat, player2.lng)) {
      player2.energy += player.energy;
      player.status = 'out';
      clearTimeout(player.scheduledMove);
      io.emit('onEnergyChange', {id: player2.id, energy: player2.energy});
      io.emit('onPlayerOut', {id: player.id, status: player.status});
      return;
    }
    // Captura outros
    if ( (player.energy - player2.energy) >= getDist(player.lat, player.lng, player2.lat, player2.lng)) {
      player.energy += player2.energy;
      energyChanged = true;
      player2.status = 'out';
      clearTimeout(player2.scheduledMove);
      io.emit('onPlayerOut', {id: player2.id, status: player2.status});
    }
  }
  // Captura foods
  for (var i = 0; i < foodList.length; i++) {
    if (foodList[i] == undefined) continue;
    var food = foodList[i];
    if ( ( (player.energy+1) - food.energy) >= getDist(player.lat, player.lng, food.lat, food.lng)) {
      player.energy += food.energy;
      energyChanged = true;
      io.emit('onRemoveFood', {id: food.id});
      foodList[i] = undefined;
    }
  }
  // Captura energyBalls
  for (var i = 0; i < energyBallList.length; i++) {
    if (energyBallList[i] == undefined) continue;
    var energyBall = energyBallList[i];
    if ( ( (player.energy+1) - energyBall.energy) >= getDist(player.lat, player.lng, energyBall.lat, energyBall.lng)) {
      player.energy += energyBall.energy;
      energyChanged = true;
      io.emit('onRemoveEnergyBall', {id: energyBall.id});
      energyBallList[i] = undefined;
    }
  }
  if (energyChanged) {
    io.emit('onEnergyChange', {id: player.id, energy: player.energy});
  }
  // Captura flags
  for (var i = 0; i < flagList.length; i++) {
    var flag = flagList[i];
    if ( ( (player.energy+1) - flag.energy) >= getDist(player.lat, player.lng, flag.lat, flag.lng)) {
      flag.playerId = player.id;
      player.flagPoints += flag.points;
      io.emit('onFlagCaptured', {flagId: flag.id, playerId: player.id, flagPoints: player.flagPoints});
    }
  }
}

function checkMoving() {
  for (var i = 0; i < playerList.length; i++) {
    var player = playerList[i];
    if ( (player == undefined) || (player.status != 'moving') ) continue;
    var now = Date.now();
    var leg = player.legList[0];
    if (now < leg.endTime) continue;
    // Fim do movimento de uma leg
    player.lastMove = leg.endTime; // liberar a próxima montagem de rota somente após WAIT_AFTER_LEG miliseg
    var lastPoint = leg.pointList[leg.pointList.length-1];
    player.lat = lastPoint.lat;
    player.lng = lastPoint.lng;
    player.status = 'in';
    io.emit('onLegFinished', {id: player.id, status: player.status, lat: player.lat, lng: player.lng, timeToNewRoute: conf.WAIT_AFTER_LEG});
    // remove esta leg e agenda a próxima, se houver
    player.legList.splice(0, 1);
    if (player.legList.length > 0)
      player.scheduledMove = setTimeout(function(param){ newScheduledMove(param); }, conf.WAIT_AFTER_LEG, player); // colocar 1 min ??

    onPlayerStop(player);
  }
  setTimeout(function(){ checkMoving(); }, 100);
}

function newScheduledMove(player) {
  console.log("newScheduledMove - player.id: " + player.id + " legList.length: " + player.legList.length);
  calcLeg(player.energy, player.legList[0])
  player.status = 'moving';
  io.emit('onMove', {id: player.id, energy: player.energy, legList: player.legList});
}

io.on('connection', function (socket) {
  console.log('connection: ' + socket.id);

  socket.on('test', function (tag) {
    console.log('test: ' + tag);
    io.emit('onTest', 'trace 01');
  });

  socket.on('createPlayer', function (playerName, emoji) {
      console.log('createPlayer: ' + playerName + ' emoji: ' + emoji);
      // tenta pegar um id disponível na lista de jogadores
      socket.playerId = (playerList.indexOf(undefined) != -1) ? playerList.indexOf(undefined) : playerList.push(undefined) - 1;
      playerList[socket.playerId] = {id: socket.playerId, name: playerName, emoji: emoji, token: token(4), onLine: true, status: 'out', lat: -100, lng: -200, energy: conf.START_ENERGY, flagPoints: 0, legList: [], scheduledMove: undefined, lastMove: 0, lastBomb: 0}; // statu=in/out/moving
      socket.emit('onCreatePlayerResult', pick(playerList[socket.playerId], ['id', 'token']));
      socket.broadcast.emit('onNewPlayer', [reject(playerList[socket.playerId], ['token', 'scheduledMove'])]);
  });

  socket.on('login', function (id, token) {
      console.log('');
      console.log('login: id=' + id + ' token=' + token);
      // Ver se esse id existe ou se é um id antigo que se perdeu em um restart do servidor
      if ( (playerList[id] == undefined) || (playerList[id].token != token) ) {
        console.log('Id inválido!');
        socket.emit('onLogin', {id: id, resultCode: 1}); //1=Erro: Usuário ou token inválidos
        return;
      }

      socket.playerId = id;
      var player = playerList[socket.playerId];
      player.onLine = true;
      var timeToNewRoute = Math.max((player.lastMove + conf.WAIT_AFTER_LEG) - Date.now(), 0);
      var timeToNewBomb = Math.max((player.lastBomb + conf.WAIT_AFTER_BOMB) - Date.now(), 0);
      socket.emit('onLogin', {id: socket.playerId, resultCode: 0, playerStatus: player.status, timeToNewRoute: timeToNewRoute, timeToNewBomb: timeToNewBomb, conf: conf}); //0=Login Ok
      socket.emit('onNewPlayer', playerList.filter(Boolean).map(player => reject(player, ['scheduledMove'])));
      socket.emit('onNewFood', foodList.filter(Boolean));
      socket.emit('onNewBomb', bombList.filter(Boolean));
      socket.emit('onNewFlag', flagList);
      // legs...
      for (var i = 0; i < playerList.length; i++) {
        player = playerList[i];
        if ( (player == undefined) || (player.status != 'moving') ) continue;
        var leg = player.legList[0];
        leg.now = Date.now(); // só atualiza o now!!!
        socket.emit('onMove', {id: player.id, energy: player.energy, legList: player.legList});
      }

      socket.broadcast.emit('onLogin', {id: socket.playerId, resultCode: 0}); //0=Login Ok
  });

  socket.on('spawn', function (lat, lng) {
      console.log('spawn: ' + lat + ' / ' + lng);
      var player = playerList[socket.playerId];
      if (player.status != 'out') return;
      player.status = 'in';
      player.lat = lat;
      player.lng = lng;
      player.energy = 10; //conf.START_ENERGY;
      io.emit('onSpawn', {id: player.id, status: player.status, lat: player.lat, lng: player.lng, energy: player.energy});
      onPlayerStop(player);
      if (!insideCity(lat, lng)) // fez o spawn fora de todas as cidades cadastradas
        googleMapsClient.reverseGeocode({latlng: [lat, lng], result_type: ['locality']}, newFlag); // tenta cadastrar uma nova cidade para esse local
  });

  socket.on('move', function (type, route, legCount, totalRouteDistance) {
      console.log('move - player.id: ' + socket.playerId + ' type: ' + type); // direct/normal
      var player = playerList[socket.playerId];
      if (player.status != 'in') return;
      if (type == 'direct') {
        var energyToGo = player.energy - (legCount * conf.DIRECT_UNIT_COST) - conf.START_ENERGY;
        var maxDist = energyToGo * conf.DIRECT_MAX_DIST;
        if ( totalRouteDistance > maxDist ) return;
        player.energy -= ( (legCount * conf.DIRECT_UNIT_COST) + Math.floor(energyToGo*(totalRouteDistance/maxDist)) );
        io.emit('onEnergyChange', {id: player.id, energy: player.energy});
      }

      var basicLegList = JSON.parse(route);
      for (var i = 0; i < basicLegList.length; i++) {
        var basicLeg = basicLegList[i];
        var leg = {type: type, pointList: [], totalDuration: 0, endTime: 0, now: 0};
        for (var j = 0; j < basicLeg.pointList.length; j++) {
          var basicPoint = basicLeg.pointList[j];
          var point = {lat: basicPoint.lat, lng: basicPoint.lng, duration: 0};
          leg.pointList.push(point);
        }
        if (i == 0)
          calcLeg(player.energy, leg); // calcula totalDuration e endTime de acordo com a energia e hora atuais
        player.legList.push(leg);
      }
      player.status = 'moving';
      io.emit('onMove', {id: player.id, energy: player.energy, legList: player.legList});
  });

  socket.on('throwBomb', function (lat, lng) {
      console.log('throwBomb - player.id: ' + socket.playerId + ' lat: ' + lat + ' lng: ' + lng);
      var player = playerList[socket.playerId];
      if (player.status != 'in') return;
      // verifica limites
      var energyToThrow = player.energy - conf.BOMB_UNIT_COST - conf.START_ENERGY;
      var maxDist = energyToThrow * conf.BOMB_MAX_DIST
      var dist = getDist(lat, lng, player.lat, player.lng);
      if ( dist > maxDist ) return;
      // cria no mapa
      var bombId = (bombList.indexOf(undefined) != -1) ? bombList.indexOf(undefined) : bombList.push(undefined) - 1;
      bombList[bombId] = {id: bombId, type: 0, lat: lat, lng: lng, energy: conf.BOMB_DEFAULT_ENERGY};
      io.emit('onNewBomb', [bombList[bombId]]); // para todo mundo
      player.lastBomb = Date.now();
      player.energy -= ( conf.BOMB_UNIT_COST + Math.floor(energyToThrow*(dist/maxDist)) );
      io.emit('onEnergyChange', {id: player.id, energy: player.energy});
      // verifica se afeta algum player
      for (var j = 0; j < playerList.length; j++) {
        if (playerList[j] == undefined) continue;
        if (playerList[j].status != 'in') continue;
        var player2 = playerList[j];
        if ( (player2.energy - conf.BOMB_DEFAULT_ENERGY) >= getDist(lat, lng, player2.lat, player2.lng)) {
          onPlayerStop(player2);
          break;
        }
      }
  });

  socket.on('disconnect', function () {
    console.log('disconnect: ' + socket.playerId);
    if (socket.playerId == undefined) return;
    var player = playerList[socket.playerId];
    player.onLine = false;
    socket.broadcast.emit('onLogout', {id: socket.playerId});
  });

  socket.on('stop', function (lat, lng) {
      console.log('stop');
      var player = playerList[socket.playerId];
      player.legList.length = 0; // clear
      if (player.status == 'moving') { // pode já estar parado no final de uma leg
        player.status = 'in';
        player.lastMove = Date.now();
        player.lat = lat;
        player.lng = lng;
        socket.broadcast.emit('onStop', {id: socket.playerId, lat: player.lat, lng: player.lng});
        onPlayerStop(player);
      } else { // está parado no final de uma leg -> apenas cancelar o agendamento do reinício da rota
          clearTimeout(player.scheduledMove);
      }
  });

});

// configura energia e pontos das flags fixas (capitais)
for (var i = 0; i < flagList.length; i++)
  setFlagEnergyAndPoints(flagList[i]);

// Inicia os processos automáticos
newFood();
checkMoving();

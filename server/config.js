module.exports = {
  generalParams: {
    // Player
    SPAWN_AREA: 3000, // 3000, // (raio em metros) área limite a partir da última posição conhecida do aparelho
    START_ENERGY: 10, //10, // (raio em metros) energia inicial do player
    MAX_ESTIMATED_ENERGY: 5000, //5000, // (metros) energia máxima estimada (os maiores players alcançarão uma energia próxima a essa)
    NORMAL_MIN_VEL: 30, //30, // (km/h) velocidade normal mínima (player próximo a MAX_ESTIMATED_ENERGY)
    NORMAL_MAX_VEL: 300, //300; // (km/h) velocidade normal máxima (player com START_ENERGY)
    TURBO_MAX_DIST: 1000, // (x*player.energy) alcance do turbo (porcentagem da energia do player)
    TURBO_VEL: 1200, //1200; // (km/h) fixa em 1.000 km/h (avião)
    WAIT_AFTER_LEG: 15000, //15000; // (miliseg) tempo de espera após comletar cada trecho NORMAL
    WAIT_AFTER_TURBO_LEG: 30000, //30000; // (miliseg) tempo de espera após comletar cada trecho via TURBO

    // Food
    FOOD_MAX_PER_PLAYER: 10, // (unid) qtde máxima de foods em uma viewport de uma flag por players que estejam dentro dela
    FOOD_ENERGY: [10, 20, 30, 40, 60, 80, 100, 120, 150, 200],
    NEW_FOOD_TIME: 5000, //5000, // tenta criar nova food a cada 'n' miliseg

    // EnergyBall
    ENERGY_BALL_DEFAULT_ENERGY: 30, //30, // em metros de raio
    ENERGY_BALL_EXPIRE: 10*60*1000, // 10*60*1000, // (miliseg) desaparece depois de um tempo

    // Bomb
    BOMB_DEFAULT_ENERGY: 10, // em metros de raio
    WAIT_AFTER_BOMB: 15000, //15000; // (miliseg) tempo de espera após jogar cada bomba
    BOMB_UNIT_COST: 10, // custo de energia por bomba
    BOMB_MAX_DIST: 10, // (x*player.energy) alcance de uma bomba (porcentagem da energia do player)
    BOMB_EXPIRE: 10*60*1000, // 10*60*1000, // (miliseg) desaparece depois de um tempo

    // Flag
    CITY_PORTVIEW_DIAGONAL_MIN: 1000, // (metros) distância entre os pontos northeast e southwest da menor cidade
    CITY_PORTVIEW_DIAGONAL_MAX: 85000, // (metros) distância entre os pontos northeast e southwest da maior cidade
    CITY_POPULATION_MIN: 180000,  // (nro habit) qtde mínima de habitantes considerada para a menor cidade (aprox CITY_PORTVIEW_DIAGONAL_MIN)
    CITY_POPULATION_MAX: 12000000, // (nro habit) qtde máxima de habitantes considerada para a maior cidade (aprox CITY_PORTVIEW_DIAGONAL_MAX -> SP)
    CITY_ENERGY_MIN: 50, // (metros) energia necessária para capturar a menor cidade
    CITY_ENERGY_MAX: 3000, // (metros) energia necessária para capturar a maior cidade
    CITY_POINTS_MIN: 100 // (pts) a menor cidade capturada gera essa qtde de pontos
  }
}

'use strict';

var mongoose = require('mongoose');

var User = require('../models/userModel');

var mongoUser = 'game_admin',
    mongoPass = '123',
    mongoHost = 'localhost',
    mongoPort = '27017',
    mongoDB = 'game',
    mongoUri = 'mongodb://' + mongoUser + ':' + mongoPass + '@' + mongoHost + ':' + mongoPort + '/' + mongoDB;

module.exports = {
    init: function () {
        mongoose.Promise = Promise;
        mongoose.connect(mongoUri, function (err) {
            if (err) {
                console.log('Failed to connect in MongoDB');
                console.log(err.stack);
                process.exit(1);
            }
            console.log('Successfully connection in MongoDB');
        });
    }
};

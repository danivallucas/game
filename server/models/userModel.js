'use strict';

var mongoose = require('mongoose');
var Schema = mongoose.Schema;

var UserSchema = new Schema({
    name: {
        type: String,
        required: true
    },
    username: {
        type: String,
        required: true,
        index: {
            unique: true
        }
    }
});

module.exports = UserSchema;

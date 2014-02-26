var mongodb = require('../common/db');

function User(user) {
  this.name = user.name;
  this.password = user.password;
};

module.exports = User;

User.prototype.save = function(callback) {
  var user = {
    name: this.name,
    password: this.password
  };
  mongodb.open(function (err, db) {
    if (err) {
      return callback(err);
    }
    // 读取users集合
    db.collection('users', function(err, collection) {
      if (err) {
        mongodb.close();
        return callback(err);
      }
      collection.ensureIndex('name', {unique: true});
      collection.insert(user, {safe: true}, function (err, user) {
        mongodb.close();
        callback(err, user);
      });
    });
  });
};

User.get = function(username, callback) {
  console.log('get user ' + username);
  mongodb.open(function (err, db) {
    if (err) {
      console.log('error occurs when open mongodb', err);
      return callback(err);
    }
    db.collection('users', function (err, collection) {
      if (err) {
        console.log('error occurs when get collection', err);
        mongodb.close();
        return callback(err);
      }
      collection.findOne({name: username}, function (err, doc) {
        console.log('find document', doc, err);
        mongodb.close();
        if (doc) {
          var user = new User(doc);
          callback(err, user);
        } else {
          callback(err, null);
        }
      });
    });
  });
};

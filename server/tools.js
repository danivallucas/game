module.exports = {

  toDegrees: function(rad) {
    return rad*(180/Math.PI);
  },

  toRadians: function(deg) {
    return deg * (Math.PI/180);
  },

  metersToLat: function (lat, lng, meters) {
    return (meters / 6378100) * (180 / Math.PI);
  },

  metersToLng: function (lat, lng, meters) {
    return (meters / 6378100) * (180 / Math.PI) / Math.cos(lat * Math.PI/180);
  },

  getDist: function (lat1, lng1, lat2, lng2) {
    var earth_radius = 6378100; // (km = 6378.1)
    var radianLat1 = lat1 * ( Math.PI  / 180 );
    var radianLng1 = lng1 * ( Math.PI  / 180 );
    var radianLat2 = lat2 * ( Math.PI  / 180 );
    var radianLng2 = lng2 * ( Math.PI  / 180 );
    var diffLat =  ( radianLat1 - radianLat2 );
    var diffLng =  ( radianLng1 - radianLng2 );
    var sinLat = Math.sin( diffLat / 2  );
    var sinLng = Math.sin( diffLng / 2  );
    var a = Math.pow(sinLat, 2.0) + Math.cos(radianLat1) * Math.cos(radianLat2) * Math.pow(sinLng, 2.0);
    var distance = earth_radius * 2 * Math.asin(Math.min(1, Math.sqrt(a)));
    return distance;
  },

  rotatePoint: function (lat1, lng1, lat2, lng2, degree) {
    var lng =  lng2  + (Math.cos(Math.toRadians(degree)) * (lng1 - lng2) - Math.sin(Math.toRadians(degree)) * (lat1 - lat2) / Math.abs(Math.cos(Math.toRadians(lat2))));
    var lat = lat2 + (Math.sin(Math.toRadians(degree)) * (lng1 - lng2) * Math.abs(Math.cos(Math.toRadians(lat2))) + Math.cos(Math.toRadians(degree)) * (lat1 - lat2));
    return {lat: lat, lng: lng};
  }

}

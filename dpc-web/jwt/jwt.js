function generateJwtTest(env, clientToken, privateKey, keyId) {
  var dt = new Date().getTime();
  var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = (dt + Math.random() * 16) % 16 | 0;
    dt = Math.floor(dt / 16);
    return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });

  var data = {
    "iss": clientToken.trim(),
    "sub": clientToken.trim(),
    "aud": env.trim() + "/v1/Token/auth",
    "exp": Math.round(new Date().getTime() / 1000) + 300,
    "iat": Math.round(Date.now()),
    "jti": uuid
  };

  var secret = privateKey.trim();

  const header = {
    "alg": 'RS384',
    "kid": keyId
  }

  var sPayload = JSON.stringify(data);
  var sJWT = KJUR.jws.JWS.sign("RS384", header, sPayload, secret);

  return { sPayload: sPayload, sJWT: sJWT }
};

module.exports = generateJwtTest;
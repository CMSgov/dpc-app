const generateJWT = () => {
  // User input
  var clientToken, env, secret, keyId, unsigned, jwt;
  clientToken = document.getElementById("clientToken").value.trim();
  env = document.getElementById("environment").value.trim();
  secret = document.getElementById("privateKey").value.trim();
  keyId = document.getElementById("keyId").value.trim();

  // Output
  unsigned = document.getElementById("unsignedJWT");
  jwt = document.getElementById("JWT");

  var dt = new Date().getTime();
  var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      var r = (dt + Math.random() * 16) % 16 | 0;
      dt = Math.floor(dt / 16);
      return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });

  var data = {
      "iss": clientToken,
      "sub": clientToken,
      "aud": env + "/v1/Token/auth",
      "exp": Math.round(new Date().getTime() / 1000) + 300,
      "iat": Math.round(Date.now()),
      "jti": uuid,
  };

  const header = {
      'alg': 'RS384',
      'kid': keyId,
  }

  var sPayload = JSON.stringify(data);
  var sJWT = KJUR.jws.JWS.sign("RS384", header, sPayload, secret);

  try {
    KJUR.jws.JWS.verifyJWT(sJWT, `-----BEGIN PUBLIC KEY-----MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEApwlL/kFlrYMvpc3ksbSsXEYWvBf5qIXnDAeHp0Tj+U6ItZJbjp2uelQUtDfPiMl8N2w4fjBH44J0x8Cp9dk5GkHntzSluhiJiTvf9E4Kqfv/gw2Q+o9lMqTvKcX9x+P9hn8/2c7BFm4LN7WBKbEf
    r52mp97a1qA69kJGbiyAT2MlTlcfLuAh8E3XAJPPtgkDu+0xUOgIb26xoEYZMy4E
    /3CZw1yOjd2sF/p87RdojsKSsHhObjIUwby6Q2RuAmSEg382jzkoqfOyhYcViJMa
    jdOmj+4OSus89dfg12hsRfF8Gs/NW7jIH3v8TXcwzUXeU29Vh4JvoCladuUuHcRC
    b1yy3jQgC0FMoMdLgW2MoMM1LseeUtlIdQ0MfRtfXJ7axsZLJ7KSlvSZIA7SrYTz
    IT0Y6HODWi1dl6tw+fDvDjnsCMULgvgFd1PS9sPyYmbGnb1/gSaDLVbo+HyzhcCO
    dgaXMSBDqIcqAyb2qXVrpyR0pyl6YQyWBo6qkLQjnAGU7kd6AIINZ2b6qtOELLB2
    3VMrODf46vTqMqsNpxXI3EqfVLkZcb1IQ4jrWdRVfVYZtE+3WQGLX9TQVCmZqVy2
    yO2uPwGcSGLyNafspG4sICXi4HxQmyJt/FA967o3A1t/7AZv/ICKJ8KrvAvzoihs
    7FBKpdfdfG7GFVjDWZoowD8CAwEAAQ==
    -----END PUBLIC KEY-----`,sPayload)
  }

  unsigned.innerHTML = JSON.stringify(data, null, 2);
  jwt.innerHTML = sJWT;

}

module.exports = generateJWT;
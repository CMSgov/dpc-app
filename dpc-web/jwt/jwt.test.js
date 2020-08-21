/* libraries */
var jwtDecode = require('jwt-decode');
var KJUR = require('jsrsasign').KJUR;
window.KJUR = KJUR;

/* functions */
const generateJwtTest = require('./jwt');

/* unchanged variables */
var env = 'https://sandbox.dpc.cms.gov';

test('generates an unsigned JWT and JWT with user input', () => {
  var clientToken = 'W3sidiI6MiwibCI6Imh0dHA6LyMiIsImkiOiIxZWIxZGM5OS0zYTlmLTQwNTYtYjA5Y2MiLCJjIjpbeyJpNjQiOiJaSEJqWDIxaFkyRnliMjl1WDNabGNuTnBiMjRnUFNBeSJ9LHsiaTY0IjoiWlhod2FYSmxjeUE5SURJd01qRXRNRGd0TVRoVU1qTTZNVFE2TWpVdU5qQXpNemt3V2cifSx7Imk2NCI6ImIzSm5jlqOXdQY2NOMlRBdHIxb2kyeG5McGk2RUZMSDZuNjZ3IiwidjY0IjoiMUkwTFlrZVRyU21yMHhHV3dSckM1TEZteU9GNThDUi1zbjcxUEQwMW5sWktCZjNZUXRSVnUxWUp6Q2ZQNFJlbGNlbVJRUU9ucUZOOC1zWmJCNEFKYm9ReGRycG5IalNWIn1dLCJzNjQiOiJveXdLVmZVMkt6YmtjbXlNSE9Cb1lvUWUwc1NhZEZYdzFNc1E2dHd1a0lrIn1d';
  var privateKey = `Zjrh0cRWzKKRM\n+QG2AmK7s2ODfdm8hlux8VqTtIZrPbtG4vrYCdorl8FLuWIbwfFUPCA9gJwsHIX+\ni+yFlBXm+ENXnQB/xTVADUa94lceYTi8Le8Y1b6BXe6obPpmN2VJOj+ej95pwk6U\nH6rYIPO6ehHKPzyze6dIYtBGE5JTTzfExO6Q9cufgXnZIAfxAkvT782E21d2az3l\nc9KAbSTC6lpEn6+4DW5Es+buu4o+GB0lrwTzUGyG4yrLrUJS6DlN2e0GjtepcgMgA2/1cb5FnbRTOANREy/3FH\n0LwW940XkUvyYUtU3i3I+S8T0+S4Bm+Nd0m0w5FJszoMc45OpXz9p6HkipF+tpyP\njPgQQ08MucwyRl7SiOmEFKnGYH9mGLufr9KmhQz1s3Jf1z4iEMqvGlpXJFI0Glc7\ntSvzgcp4MnvYiUtpo6PIMFvDQQcJCLjBhwlwXxLmxvnAyA+UtNCxqtwUEQMh`;
  var keyId = 'fd38276d-786a-49ec-9987-5e7b258e77cf';

  const result = generateJwtTest(env, clientToken, privateKey, keyId);

  var sPayload = JSON.parse(result.sPayload);
  var sJWT = result.sJWT;

  expect(sPayload.iss).toEqual(clientToken);
  expect(sPayload.sub).toEqual(clientToken);
  expect(sPayload.aud).toContain(env);
  expect(sJWT).not.toBe('');
});

test('decoded JWT matches user input', () => {
  var clientToken = 'W3sidiI6MiwibCI6Imh0dHA6Ly9sb2NhbGhvc3Q6MzAwMiIsImkiOiIxYtYjA5OC03NGFkMzNCJjIjpbeyJpNjQiOiJaSEJqWDIxaFkyRnliMjl1WDNaNOMlRBdHIxb2kyeG5McGk2RUZMSDZuNjZ3IiwidjY0IjoiMUkwTFlrZVRyU21yMHhHV3dSckM1TEZteU9GNThDUi1zbjcxUEQwMW5sWktCZjNZUXRSVnUxWUp6Q2ZQNFJlbGNlbVJRUU9ucUZOOC1zWmJCNEFKYm9ReGRycG5IalNWIn1dLCJzNjQiOiJveXdLVmZVMkt6YmtjbXlNSE9Cb1lvUWUwc1NhZEZYdzFNc1E2dHd1a0lrIn1d';
  var privateKey = `\nMIIJKQIBAAKCAgEAtVT4NBbthWNNwcQDlmX1dQBiwFq0uUtx4NaremA\ntSvzgcp4MnvYiUtpo6PIMFvDQQcJCLjBhwlwXxLmxvnAyA+UtNCxqtwUEQMh`;
  var keyId = 'fd38276d-786a-49ec-9987-5e7b258e77cf';

  const result = generateJwtTest(env, clientToken, privateKey, keyId);

  var decoded = jwtDecode(result.sJWT);
  var decoded_header =  jwtDecode(result.sJWT, {header: true});

  expect(decoded_header.alg).toEqual('RS384');
  expect(decoded_header.kid).toEqual(keyId);
  expect(decoded.iss).toEqual(clientToken);
  expect(decoded.sub).toEqual(clientToken);
  expect(decoded.aud).toEqual(env + '/v1/Token/auth');
});

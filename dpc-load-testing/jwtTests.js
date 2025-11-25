import { check, fail } from 'k6';
import encoding from 'k6/encoding';
import { buildJwt,
	 exportPublicKey,
	 generateKey,
	 importKey,
	 jwtHeader,
	 jwtPayload,
	 makeJwt,
	 signatureSnippet } from './generate-jwt.js';

export const options = {
  thresholds: {
    checks: ['rate===1'],
  },
  scenarios: {
    testSnippet: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testSnippet" },
    testBuildJwt: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testBuildJwt" },
    testMakeJwt: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testMakeJwt" },
    testGenerateKey: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testGenerateKey" },
    testExportPublicKey: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: "testExportPublicKey" },
  }
}

const testJWK = {"kty":"RSA","n":"rDZ5Tlygr5Nj4boh7gVwjBLTNDHSKdeSAngQOMBmL-YDIyJwsNJOKiNOlJnuRtHDLXoq4IFhTrySduC7S3wmC-yGs3lrxYbSIM7T0TnefW321caoJLO76MMpCEI0OUuZ-n4RTnjPplmIErLCHLT-0ssxaR1bLF9NT8RwRyZndDpsAgxr6pkgnvhlKGsJs_IEezF5qrt4MTAv_GF_ueyEd2ZZZuWyjlQYki0dj3cOxWxR2TG4FTPbFyo2sYU9hR_H7EglqpKhTBaeZGZ0sky4xLzvt6ReHXkRGlQhV4x5JNknzpl21sV0oc0vxnh0hFJn_EdS2ZEtBDuYZY6vijBm9TcIXzNqiMcIDJs7JI81QwACjoOJoUV2kmMxdS4WH-Z2N4sZ7TdLbowITEC8mA-FF6ZCZE6SrVM3ebDkfT-cGh-qa2fj1fk5nZVrh6KpRXk7jrG-DcAcBimE_DUJwzbVzgpc_2-73LQr4YCevWQz81xsedszxvwcgGPiGo4hM0v3mvgJGClp7p2ekQz5XvSFe7yxG2Byfza11ndyr7APcwuLMCYCUX3mEUcn3qd_78RclJkfSM0w6eUR_D2DZdl_TmA8fp6HSMeVGfkebc8zn-XOCn5XZQcUFcouaKgct10iHVpvzgqi2aEae7b38o1WBi8vnDOD2aJWx4ZpgDZcjB0","e":"AQAB","q":"233MrFf_pzzWBKN8O2P-S1cq0z-nsm5cc88J6Lj6983AV0nr9jfJizknMFF5ZjWqzneS3k7-7QtFuFmxrKZC1wHx6Vm7Hb6TZKAhOKk2WK6i6G-7B-cu7YNQ8i-a9xxMDC52gZ6D8rBHjp2ZBs4gJqFmHg12Px0tkXCNrrzAf9WwB4k-TeHO9Olw78LKurTzJXvyCkMHMJufSQxiq0QjVmqLAly6BeC89CndsnsMv-pc9jdJY9FFpY73F4P4JUQi0tKQ20a82krozG-0_SIB7U_aUaEQ8Wsgql1Wx-ieePBXnGY3S5nVx-WLt2HLevYB7LS0mnH_fENdQdts7-aGVQ","dp":"TSvoSC4OxAVHzuHFyyBknoQxAvJi_0Hg9QL-LAazwRnX9Q8ZrJJnCne-ZogZEpGpqp3fYLC2w_9suNx2w_xXeYw4j3ODqQaINNAqM-BkzCB1-DCF5H-6LKz2eB8_037V7nBrTtw1U7hQogVy0liW-KYM0b3sADysfuelCeqZkc0vpQFqEtekLLw6q94FwajdqulwprZm0uZKcM-k3p4GU-H8lJPAGNFTpBp1FBsnFlEh1hF6mfY59IwyhxzQdemwNYonaQfUdjm-USTVLdhC4YBekyBJqLVZuZD2T8Iwt0Q28tkfV5YhiIEX8NfN068armFhwpMqVpssXkzmKfA0YQ","dq":"LgwNRVbMzpQHvj9iD7-hmMK3vEvNbbSTdsRHqugr1pL2uBlNBIGdqbSa0Rs2oce-hsQ1LqhZ-Pb1cIB4suTKfvrsem0VwV6641Box89QFHjGuoa-MSFCChgTgkuUciUgmymgOP89ZUuwJ25OrUvPGOuhVcqzNPh1F98KCk-U_vygimQzivlFhsH2rIYsK9FMdV-0XozDVGJBn3yWsqF_3GBLu3oimJsWaQ7jcM851IVhbVIZ9nPv7ZS5p9hK1d6eg6YjN612b0Fp54HD8ZsR1mx7O8bTnCJCXxtn2lal8G9Tof8buKOUYvR4REei_a_gpX0fqosh8zratLdI-mddZQ","qi":"Wb3H-wv6q_efw6xaa0-WwPl9ul6Tv0WgqL45ghVg4mgPtMu_mnQRictI7n6taUnouNgcJnJ0DKZskMkZ9cEHhY3Jeu8C7b-rFhQmTTHyIZVWV3jfLfZ7si3DLK8a6SOWTHuLAf0qsTruTEcYDQwO_90e4F7Vx6KzEXgdKASXJH7L2rm0Kpz89Z0-Z3NLnrz-gVYM_wbJCrbg-FnXCOe2_ckNl7OWqb1LtIfXtwhngfdQV0Pupt-yGZSF3N7S1gS74VDS_5yPfhnzcVOz07fZtbIRzSPJK-TE8vKixhlYGZHeY97kRVObhB_d_-rFWk56RcdR_IceUtjBa0TNOP9JAg","d":"KW46jWBp9QuNzEy6JHgRDVXrE2PIBrJ6yVbGukyJWQ0qZI3A1D1oOil_2m7U_RsybFK3lYby2N0zlF95F84wng5DktKLm7wVefigS3XzYbQundwa8CeAdd1b1gWNaFpYm0OwmLMma8Qe4Ta5zeu6YY6qXrpGCL0Nzbq581gZLARxHyYLYWeBClXCinCD51N6ggPTXECZSCYqaTqwnpN5A_uu1vs8U5MGTUaG1dDEEDmA0HKucYVl-GL6wQcH16rfb2glDa4UmYhYh1mByjJ8gjJdLo5f6RHLDE1Arz4XmAaZVdIkXcOnzVReoc4xY_MqWq5zD24miR3oSkiDUUjXP9KihaSliWWwQwLeSC0Bq9HOCCIsENvYNNTk9Kkcl_YXKDF0_WYrRL2N2LqE_3tzIWZuXQTZM_KC-uPrUeJpmsfWc2YIKCO7UTfluiyd3DLj4MSPd9fAKmpmJf4E7vTWeEOlLTQOTYz9B0Cc2fM9BHp9zn12A0IZAylsG0DuPmGEFrYvdPuTbNlajcAYQqufQJcM-Q3KMQfGr-1kuyEApEsph5boM9lWmaY5YbEoSWqqW-lLE5wnLCmrXHKS3nnxBUqf0IJm5AWhbQwES7SWgzJvSs0G0qg17AW3qsWxrpmUyGYI7R4j-LKbt7N0ZMBRs7dw1-Cw880xBxTUTWKfFBk","p":"yNt-4e4YgLZPLkEyRheAyCwUFLGINg48Vr4cYC_SiDXvelumpp1BkHpqxkcp36ge3oa1xIulR1o_cZh1IEfae79HfVZJaOcqNLVdncZCajHhGXumQgTyqk4xvzwQnsBJ_JEzHIC2rSMPB6ql64nVoyrL_ArgOF8gxgKPu9aODfoTyVqjSAn-f8fKuhmjCBQqAcfAuTBUCBqmkE7zvdK_PI6AqApLr_s_IUl_ZU3o_0-WVqejEdx_J2NCXKIYWMH2o4GTSc2g5nvBe7NygWCCuOf3agIALDxf9qyCThYL477qEiNnGTqIDv4DB3CDOlODiGX-qFS4E-bRC1vpnwhmqQ"};

export async function testSnippet() {
  const expected = "ZCocE4vMg4azRxwhnejXSUcgRGH1LR64iYJ4e/FeeNlWhVYBXYFJfyhzkuQkuYvXIKnVKbfiB1cbZ5oNhU8J5OWqaF6HlvsNK7ES1Eq9LK9tuXE6v0sIpImlg0EVx+RUE5Xaq6dOi7ToVK3mXpkd/gF2WNYBAUMmZ0RR9LlpaRrvbJVgI0XsAsgq7j5pKZiCV88AuaXoTYYG5iloBXiGRsRG2iUeDWBIEkf0Dz9TaXeLqQVywi4bxrVErXWoqsFx1xKczJ6y6Wq7i9bwmdXkWBj/ZGU7FCiHbCLa+Sr5R0P+qqpFY2HmGXsagcnG0sJgII/iHWTVYoV2CH7O4gfSbdVx1YAxB3XyBj6r9iwPG9zSAEchZLUyAm7ocS4bunSZhDATyJ1MohqaxuoNxWc/OKf2iRH97W7WYOUEgStXEKHEdT56FRRGxf9kwTCUioCoXMEU0V8EsyLQSlqU5a3TjE0kjqP2Uq6HJHynegRXlpDQMNhERGcJo+vtfVkhlHrb5JE9D7DZkymcSy1OQrWINbjKp/Rtto3B7TR7+rCJXMSl/VZwA3m7RcXOcSa/nymyMVkRbEcVMF3DNOu5ifEB1aYspxZuYJVSb8p8sHTwtYec9lwS9IoH7cZ4Br0gw2P3EdQPi8lxjeGJ38AEZ40W1EhEpfeejtSTvpP0TA3xnbI=";

  const key = await importKey(testJWK);
  const snippet = await signatureSnippet(key);

  const checkSnippet = check(
    snippet,
    { "matches expected snippet": snippet => snippet == expected }
  )
  if (!checkSnippet) {
    console.log("Snippet signature doesn't match");
  }
}

export async function testBuildJwt() {
  const expected = ["eyJhbGciOiJSUzI1NiIsImtpZCI6IjVmODZkMGQxLTdhNTQtNDFjNy04ZmYyLTEzM2Q4NTVkZTAyYSIsInR5cCI6IkpXVCJ9",
		    "eyJpc3MiOiJjbGllbnRfdG9rZW4iLCJzdWIiOiJjbGllbnRfdG9rZW4iLCJhdWQiOiJodHRwczovL2Rldi5kcGMuY21zLmdvdi9hcGkvdjEvVG9rZW4vYXV0aCIsImV4cCI6MTAsImp0aSI6Imp0aSJ9",
		    "iNRKlOaTRVlCHvlUaVx5V5C3lbRp6RMbG7MCSLlIDF-BHv9gIBYwqXGU19uf6tferqUaBqQeCSodvBxSVMZeL_g-1XN4VJ7iU7kfdH1PYiuciuT5RfBx-XEye6_umIyudt_YZlO2gRzzQLEEIMj17zueC9nHWuioNszYuZAHOFpq0T2taov5apnWCBQ2aki0MaW09LwRdZFG8p8YAXuEFl74hq2zPCdRnfob_MiuGHQxtclcXSxpCzwIy4fqUD8DWOaYvR0kv3M-fWRT2iQM4YklbYuPPHS2Q1bY6dMuebwmywqWD0-C8OHluRo8i9pfT5WjCTjBp6Y7_i8CZhc2lsu1cFdHL6b3YGqg4zjoUUn9s_p7saIxDAe3GzJWWdfmdx8bcCdmW2OJfbVftglu1Ng6UpIkm0Ya_lcwsObnqWFd0L8fv8SnD9JcL76sovOPGswkwNGJMrwsBcIt2Vjnki8JT0I437FpFkzFWHUqVC2dU4oukr5oYErBmi8fY15JCs2MpCLLXvQf_63brm5LzA1H4OnvkkQ2LsQNDJ0ujU8Rlc9ep6j2Q80SbIPrQq24KwoAVDTrJldlhp1rDxVDqrR4ZlCiqtpRImTw1ngzYL5YQ_vryV2vAohPnRze_YLeBZSj1TTi2zDIF_E5WEQUq6_bnOAK9sU8RFo_4xqlaVE"];
  const headerData = jwtHeader("5f86d0d1-7a54-41c7-8ff2-133d855de02a");
  const payloadData = jwtPayload("https://dev.dpc.cms.gov/api/v1", 'client_token', 10, 'jti');
  
  const key = await importKey(testJWK);
  const jwt = await buildJwt(payloadData, key, headerData);

  const checkJwt = check(
    jwt,
    { "matches expected jwt": jwt => jwt == expected.join(".") }
  )
  if (!checkJwt) {
    console.log("Jwt doesn't match");
    return;
  }
}

export async function testMakeJwt() {
  const key = await importKey(testJWK);
  const jwt = await makeJwt('clientToken', 'public-key', key);

  const checkJwt = check(
    jwt,
    { "returns three pieces": jwt => jwt.split('.').length == 3 }
  )
  if (!checkJwt) {
    console.log("Jwt doesn't have three pieces");
    return;
  }
}
export async function testGenerateKey() {
  const key = await generateKey();

  const checkKey = check(
    key,
    {
      'private key generated': key => key['privateKey'],
      'public key generated': key => key['publicKey'],
    }
  );
  if (!checkKey) {
    console.log("Key doesn't generate");
    return;
  };
}

export async function testExportPublicKey() {
  const expectedStart = "-----BEGIN PUBLIC KEY-----\n";
  const expectedEnd = "\n-----END PUBLIC KEY-----";
  
  const key = await generateKey();

  const publicKey = await exportPublicKey(key['publicKey']);

  const checkPublicKey = check(
    publicKey,
    {
      'public key starts with begin key': publicKey => publicKey.startsWith(expectedStart),
      'public key ends with end key': publicKey => publicKey.endsWith(expectedEnd),
      'public key has three parts': publicKey => publicKey.split("\n").length == 3
    }
  );
}

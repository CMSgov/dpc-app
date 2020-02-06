// This is a manifest file that'll be compiled into application.js, which will include all the files
// listed below.
//
// Any JavaScript/Coffee file within this directory, lib/assets/javascripts, or any plugin's
// vendor/assets/javascripts directory can be referenced here using a relative path.
//
// It's not advisable to add code directly here, but if you do, it'll appear at the bottom of the
// compiled file. JavaScript code in this file should be added after the last require_* statement.
//
// Read Sprockets README (https://github.com/rails/sprockets#sprockets-directives) for details
// about supported directives.
//
//= require svg4everybody
//= require @okta/okta-signin-widget/dist/js/okta-sign-in
//= require accordions
//= require rails-ujs
//= require activestorage
//= require_tree ./utils
//= require_tree .

svg4everybody();


// Load OktaSignIn Widget on login page
var loginDiv = document.getElementById("widget");
if(typeof(loginDiv) != 'undefined'){
  var signIn = new OktaSignIn(
    {
      baseUrl: 'https://dev-785497.okta.com',
      redirectUri: 'https://localhost:3000/users/auth/oktaoauth/callback',
      authParams: {
        display: 'page',
        pkce: true
      }
    }
  );

  signIn.renderEl(
    // Assumes there is an empty element on the page with an id of 'osw-container'
    {el: '#widget'},

    function success(res) {
      // The properties in the response object depend on two factors:
      // 1. The type of authentication flow that has just completed, determined by res.status
      // 2. What type of token the widget is returning

      // The user has started the password recovery flow, and is on the confirmation
      // screen letting them know that an email is on the way.
      if (res.status === 'FORGOT_PASSWORD_EMAIL_SENT') {
        // Any followup action you want to take
        return;
      }

      // The user has started the unlock account flow, and is on the confirmation
      // screen letting them know that an email is on the way.
      if (res.status === 'UNLOCK_ACCOUNT_EMAIL_SENT') {
        // Any followup action you want to take
        return;
      }

      // The user has successfully completed the authentication flow
      if (res.status === 'SUCCESS') {
        // Legacy response
        res.session.setCookieAndRedirect(
          '/internal/auth/oktaoauth'
          // '{{Okta URL}}/oauth2/{authz server ID}/v1/authorize?client_id={{clientId}}&response_type=id_token token&response_mode=fragment&scope={{oidcScopes}}&redirect_uri={{redirectUri}}&state={{state}}&nonce={{$guid}}'
        );

        // Okta URL = your Okta org domain, e.g. https://mycompany.okta.com
        // authz server ID = the GUID of the Authorization Server processing your request (and minting tokens)
        // clientId = the Client ID of your OIDC app in Okta
        // oidcScopes = the desired scopes you wish to request
        // redirectUri = the location to which the token(s) will be returned upon successful authorization

        // OIDC response
        // Response includes a user object:
        //   user: {
          //   id: "..."
          //   passwordChanged: "2020-02-06T18:48:55.000Z"
          //   profile: {
          //     firstName: "Shelby"
          //     lastName: "Switzer"
          //     locale: "en"
          //     login: "shelbyswitzer@gmail.com"
          //     timeZone: "America/Los_Angeles"
          //   }
        //   }
        // We would need to grab these data elements and do a login request to our own application,
        // unless we can have the widget redirect the user directly to our login path

        // If the widget is configured for OIDC with a single responseType, the
        // response will be the token.
        // i.e. authParams.responseType = 'id_token':
        // console.log(res.claims);
        // signIn.tokenManager.add('my_id_token', res);

        // // If the widget is configured for OIDC with multiple responseTypes, the
        // // response will be an array of tokens:
        // // i.e. authParams.responseType = ['id_token', 'token']
        // signIn.tokenManager.add('my_id_token', res[0]);
        // signIn.tokenManager.add('my_access_token', res[1]);

        // return;
      }

    },

    function error(err) {
      // This function is invoked with errors the widget cannot recover from:
      // Known errors: CONFIG_ERROR, UNSUPPORTED_BROWSER_ERROR
    }
  );
}

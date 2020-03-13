# frozen_string_literal: true

module OauthSupport
  def set_omniauth_request_env(provider)
    request.env["omniauth.auth"] = OmniAuth.config.mock_auth[provider]
  end

  def mock_oktaoauth(params={})
    OmniAuth.config.mock_auth[:oktaoauth] = OmniAuth::AuthHash.new(okta_oauth_payload(params))
  end

  def okta_oauth_payload(uid: '11r5ysph7s90zebsv22', name: 'Nemo Lee',
    email: 'n@example.com', roles: ['DPC_AppRole_Admin'], loa: '3')
    {
      "provider"=>"oktaoauth",
      "uid"=>uid,
      "info"=>{"name"=>name, "email"=>email, "first_name"=>"Nemo", "last_name"=>"Lee", "image"=>nil},
      "credentials"=>{
        "token"=> "kJiiGCA",
        "expires_at"=>1584107945,
        "expires"=>true
        },
      "extra"=> {
        "raw_info"=>{
          "sub"=>uid,
          "name"=>name,
          "locale"=>"en-US",
          "email"=>email,
          "preferred_username"=>"NNMO",
          "given_name"=>"Nemo",
          "middle_name"=>"",
          "family_name"=>"Lee",
          "zoneinfo"=>"America/Los_Angeles",
          "updated_at"=>1581350278,
          "email_verified"=>true,
          "SourceType"=>"EUA",
          "Roles"=> roles,
          "LOA"=> loa
        },
        "id_token"=> "kJiiGCA",
        "id_info"=>{
          "ver"=>1,
          "scp"=>["profile", "openid", "email"],
          "sub"=>"NNMO"
        }
      }
    }
  end
end

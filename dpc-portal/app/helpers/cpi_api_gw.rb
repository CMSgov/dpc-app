require 'oauth2'
cms_idm_url = 'https://impl.idp.idm.cms.gov/'
access_token_url = '/oauth2/aus2151jb0hszrbLU297/v1/token'

class CPIAPIGatewayHelper
    attr_accessor :token

    def initializer(client_id, client_secret)
        @client = OAuth2::Client.new(client_id, client_secret,
                                     site: cms_idm_url,
                                     token_url: access_token_url)
        fetch_token
    end


    private
    
    def fetch_token
        @token = @client.client_credentials.get_token
    end
end

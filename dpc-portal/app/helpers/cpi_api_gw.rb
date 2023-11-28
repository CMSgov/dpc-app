require 'oauth2'
require 'aws-sdk'

cms_idm_url = 'https://impl.idp.idm.cms.gov/'
access_token_url = '/oauth2/aus2151jb0hszrbLU297/v1/token'


class CPIAPIGatewayHelper
    attr_accessor :token

    def initializer()
        fetch_client_credentials => {client_id:, client_secret:}
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


def fetch_client_credentials
    client = Aws::SSM::Client.new(
        region: 'us-east-1',
    )
    res = client.get_parameters({
        names: ['/dpc/dev/web/cpi_api_gw_client_id', '/dpc/dev/web/cpi_api_gw_client_secret'],
        with_decryption: true
    })

    {
        client_id: res.parameters[0].value,
        client_secret: res.parameters[0].value
    }
end

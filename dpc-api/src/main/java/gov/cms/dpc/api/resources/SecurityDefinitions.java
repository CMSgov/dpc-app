package gov.cms.dpc.api.resources;

import io.swagger.annotations.ApiKeyAuthDefinition;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;

@SwaggerDefinition(
        securityDefinition = @SecurityDefinition(
                apiKeyAuthDefinitions = {
                        @ApiKeyAuthDefinition(key = "apiKey", name = "Authorization", in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER, description = "Ensure that your api key is prefixed with `Bearer: `")
                }
        )
)
class SecurityDefinitions {
}

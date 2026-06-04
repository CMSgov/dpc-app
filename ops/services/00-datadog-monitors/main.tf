terraform {
  required_providers {
    datadog = {
      source  = "DataDog/datadog"
      version = "~>4.4"
    }
  }
}

provider "datadog" {
  api_key = sensitive(module.standards.ssm.datadog.api_key.value)
  app_key = sensitive(module.standards.ssm.datadog.application_key.value)
  api_url = "https://api.ddog-gov.com"
}

locals {
  defaults   = yamldecode(file("config/defaults.yml"))
  env_config = yamldecode(file("config/${var.env}.yml"))
  default_tags = module.standards.default_tags
  service      = "datadog-monitors"

  monitor_config = {
    for key in distinct(concat(keys(local.defaults), keys(local.env_config))) :
    key => try(
      # Attempt map merge (works if both values are map/object-typed)
      merge(
        lookup(local.defaults, key, {}),
        lookup(local.env_config, key, {})
      ),
      # Fallback to scalar: env wins, then default
      lookup(local.env_config, key, lookup(local.defaults, key, null))
    )
  }
}

module "standards" {
  source    = "github.com/CMSgov/cdap//terraform/modules/standards?ref=cbf179cb8c6707c92ad475560a54c061d00f75ff"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = "dpc"
  env          = var.env
  root_module  = "https://github.com/CMSgov/dpc-ops/tree/ops/services/00-datadog-monitors/"
  service      = local.service
  ssm_root_map = { datadog = "/dpc/${var.env}/datadog/cicd/" }
}

module "common_datadog_monitors" {
  source = "github.com/CMSgov/cdap//terraform/modules/datadog_monitors?ref=945fbd644cc8d239bdf3f3a3a7241fb6066a0f55"

  app            = "dpc"
  env            = var.env
  monitor_config = local.monitor_config
}
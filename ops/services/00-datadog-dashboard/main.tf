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
  default_tags = module.standards.default_tags
  service      = "datadog-dashboard"
}

module "standards" {
  source    = "github.com/CMSgov/cdap//terraform/modules/standards?ref=cbf179cb8c6707c92ad475560a54c061d00f75ff"
  providers = { aws = aws, aws.secondary = aws.secondary }

  app          = "dpc"
  env          = "test"
  root_module  = "https://github.com/CMSgov/dpc-ops/tree/ops/services/00-datadog-dashboard/"
  service      = local.service
  ssm_root_map = { datadog = "/dpc/test/datadog/cicd/" }
}

module "datadog_dashboard" {
  source       = "github.com/CMSgov/cdap//terraform/modules/datadog_dashboard?ref=cbf179cb8c6707c92ad475560a54c061d00f75ff"
  app          = module.standards.app
  name_rewrite = "DPC"
  runbook_url  = "https://thisisatest.cdap.internal.cms.gov"

  custom_widgets = [
    {
      # Standard timeseries showing average CPU utilization across all ECS clusters for the application
      type         = "timeseries"
      title        = "TEST DYNAMIC WIDGET ecs.cpuutilization"
      query        = "avg:aws.ecs.cpuutilization{application:${module.standards.app}, $env} by {clustername}" #include $env for filtering provided by default dashboard template
      display_type = "line"
    },
    {
      # A big number widget showing the total number of running services across all clusters
      type      = "query_value"
      title     = "Total Running Services"
      query     = "avg:aws.ecs.service.running{application:${module.standards.app}, $env}" #include $env for filtering provided by default dashboard template
      precision = 0
    },
    {
      # A ranked list of the top s3 buckets by object count for the application
      type  = "toplist"
      title = "Top s3 Buckets by Object Count"
      query = "avg:aws.s3.number_of_objects{application:${module.standards.app}, $env} by {bucketname}" #include $env for filtering provided by default dashboard template
    }
  ]

  # Opt-out of unused default infrastructure widgets
  enable_default_widgets = {
    lambda = true
    aurora = true
    sns    = true
    alb    = true
    s3     = true
    ecs    = true
  }
}

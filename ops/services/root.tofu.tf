# This root tofu.tf is symlink'd to by all per-env Terraservices. Changes to this tofu.tf apply to
# _all_ Terraservices, so be careful!

variable "env" {
  description = "The application environment (dev, test, sandbox, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "test", "sandbox", "prod"], var.env)
    error_message = "Valid value for env is dev, test, sandbox, or prod."
  }
}

variable "region" {
  default  = "us-east-1"
  nullable = false
  type     = string
}

variable "secondary_region" {
  default  = "us-west-2"
  nullable = false
  type     = string
}

locals {
  app            = "dpc"
  env            = var.env
  service_prefix = "${local.app}-${local.env}"

  state_buckets = {
    dev     = "dpc-dev-tfstate-20250409165915907400000001"
    test    = "dpc-test-tfstate-20250410145524530000000001"
    sandbox = "dpc-sandbox-tfstate-20250416202240532700000001"
    prod    = "dpc-prod-tfstate-20250411204900543700000001"
  }
}

provider "aws" {
  region = var.region
}

provider "aws" {
  alias  = "secondary"
  region = var.secondary_region
}

terraform {
  backend "s3" {
    bucket       = local.state_buckets[local.env]
    key          = "ops/services/${local.service}/tofu.tfstate"
    use_lockfile = true
    region       = var.region
  }
}

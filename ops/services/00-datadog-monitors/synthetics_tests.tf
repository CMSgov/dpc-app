resource "datadog_synthetics_test" "certificate_expiration" {
  name      = "A check for whether the ${module.standards.env}.dpc.cms.gov certificate is expiring in 30 days or less."
  type      = "api"
  subtype   = "ssl"
  status    = "live"
  message   = "TODO"
  locations = ["aws:${module.standards.primary_region.region}"]
  tags      = [for k, v in module.standards.default_tags : "${k}:${v}"]

  request_definition {
    host = "${module.standards.env}.dpc.cms.gov"
    port = "443"
  }

  assertion {
    type     = "certificate"
    operator = "isInMoreThan"
    target   = 30
  }

  options_list {
    tick_every = 86400 # Run once per day
  }
}

resource "datadog_synthetics_test" "api_uptime" {
  count = contains(["staging", "prod"], module.standards.env) ? 1 : 0

  name      = "An uptime test on https://${module.standards.env}.dpc.cms.gov/api/v1/metadata"
  type      = "api"
  subtype   = "http"
  status    = "live"
  message   = "TODO"
  locations = ["aws:${module.standards.primary_region.region}"]
  tags      = [for k, v in module.standards.default_tags : "${k}:${v}"]

  request_definition {
    method = "GET"
    url    = "https://${module.standards.env}.dpc.cms.gov/api/v1/metadata"
  }

  request_headers = {
    Content-Type = "application/json"
  }

  assertion {
    type     = "statusCode"
    operator = "is"
    target   = "200"
  }

  assertion {
    type     = "receivedMessage"
    operator = "contains"
    target   = "CapabilityStatement"
  }

  options_list {
    tick_every = 3600
    retry {
      count    = 2
      interval = 300
    }
    monitor_options {
      renotify_interval = 120
    }
  }
}

resource "datadog_synthetics_test" "web_uptime" {
  count = contains(["staging"], module.standards.env) ? 1 : 0

  name      = "An uptime test on https://${module.standards.env}.dpc.cms.gov"
  type      = "api"
  subtype   = "http"
  status    = "live"
  message   = "TODO"
  locations = ["aws:${module.standards.primary_region.region}"]
  tags      = [for k, v in module.standards.default_tags : "${k}:${v}"]

  request_definition {
    method = "GET"
    url    = "https://${module.standards.env}.dpc.cms.gov"
  }

  request_headers = {
    Content-Type = "application/json"
  }

  assertion {
    type     = "statusCode"
    operator = "is"
    target   = "200"
  }

  assertion {
    type     = "receivedMessage"
    operator = "contains"
    target   = "Data at the Point of Care"
  }

  options_list {
    tick_every = 3600
    retry {
      count    = 2
      interval = 300
    }
    monitor_options {
      renotify_interval = 120
    }
  }
}

resource "datadog_synthetics_test" "admin_uptime" {
  count = contains(["staging"], module.standards.env) ? 1 : 0

  name      = "An uptime test on https://${module.standards.env}.dpc.cms.gov/admin"
  type      = "api"
  subtype   = "http"
  status    = "live"
  message   = "TODO"
  locations = ["aws:${module.standards.primary_region.region}"]
  tags      = [for k, v in module.standards.default_tags : "${k}:${v}"]

  request_definition {
    method = "GET"
    url    = "https://${module.standards.env}.dpc.cms.gov/admin"
  }

  request_headers = {
    Content-Type = "application/json"
  }

  assertion {
    type     = "statusCode"
    operator = "is"
    target   = "200"
  }

  assertion {
    type     = "receivedMessage"
    operator = "contains"
    target   = "Data at the Point of Care"
  }

  options_list {
    tick_every = 3600
    retry {
      count    = 2
      interval = 300
    }
    monitor_options {
      renotify_interval = 120
    }
  }
}

resource "datadog_synthetics_test" "prod_static_site_uptime" {
  name      = "An uptime test on the prod DPC static site."
  type      = "api"
  subtype   = "http"
  status    = "live"
  message   = "TODO"
  locations = ["aws:${module.standards.primary_region.region}"]
  tags      = [for k, v in module.standards.default_tags : "${k}:${v}"]

  request_definition {
    method = "GET"
    url    = "https://dpc.cms.gov"
  }

  request_headers = {
    Content-Type = "application/json"
  }

  assertion {
    type     = "statusCode"
    operator = "is"
    target   = "200"
  }

  assertion {
    type     = "receivedMessage"
    operator = "contains"
    target   = "Data at the Point of Care"
  }

  options_list {
    tick_every = 3600
    retry {
      count    = 2
      interval = 300
    }
    monitor_options {
      renotify_interval = 120
    }
  }
}

resource "datadog_synthetics_test" "waf_allowlist" {
  name      = "A check for WAF denying access from an IP not in the allowlist."
  type      = "api"
  subtype   = "http"
  status    = "live"
  message   = "TODO"
  locations = ["aws:${module.standards.primary_region.region}"]
  tags      = [for k, v in module.standards.default_tags : "${k}:${v}"]

  request_definition {
    host = "${module.standards.env}.dpc.cms.gov"
    port = "443"
  }

  assertion {
    type     = "statusCode"
    operator = "is"
    target   = "403"
  }

  options_list {
    tick_every = 3600
  }
}

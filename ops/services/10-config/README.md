# DPC AWS Parameter Store Configuration Values

This root module is responsible for configuring the sops-enabled strategy for storing configuration values in AWS SSM Parameter Store.
The environment-specific configuration values are located in the `values` directory.  **You will need to have copied over AWS short term access keys for all of the following.**  See cloudtamer to get keys.

## Usage

### Initial Setup

First, initialize and apply the configuration with the `sopsw` script targeted.

```bash
cd ops/services/10-config
export TF_VAR_env=dev
tofu init
tofu apply -target 'module.sops.local_file.sopsw[0]' -var=create_local_sops_wrapper=true
```

NB: This setup assumes that `../root.tofu.tf` has been successfully soft-linked to this directory.

### Editing Encrypted Configuration

The `sopsw` script should be automatically generated in the `bin/` directory in the initial setup. You can then edit the encrypted configuration files for each environment:

```bash
# Edit dev environment, for example
./bin/sopsw -e values/dev.sopsw.yaml
```
NB: you cannot edit the configuration files directly. By default, sops will use vim, but you can change the editor by setting the $EDITOR environment variable. YMMV.

### Deploying Configuration Changes

After editing configuration files, deploy the changes to AWS Parameter Store:

```bash
# Review changes before applying
tofu plan -var env=dev

# Apply changes
tofu apply -var env=dev
```

## Configuration Structure

Configuration files are written in yaml, with the parameter key as the key.

### Examples

`/dpc/${env}/<blah>/<blah>: <value>`

`/dpc/${env}/<blah>/<blah>: '<value with yaml-meaningful character>'`

`/dpc/${env}/<blah>/<blah>: "<value with 'single quote'>"`

```
/dpc/${env}/<blah>/<blah>: |
    <multi-line value line 1>
    <multi-line value line 2>
```
`# <comment>`

### Non-sensitive values
By default, the parameters managed here are stored as SecureStrings. If the parameter **must** be readable in the UI without clicking the 'show value' button, add `/nonsensitive/` to the path.

`/dpc/${env}/nonsensitive/<blah>/<blah>: <value that must be visible in UI>`

## Dependencies

### Required Tools
- **awscli** - For AWS authentication and KMS operations
- **sops** - For encryption/decryption (`brew install sops`)
- **yq** - For YAML processing (`brew install yq`)
- **envsubst** - For environment variable substitution (`brew install gettext`)

### External Tools
- **tofu** - For deploying configuration to AWS Parameter Store (`brew install opentofu`)

<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Providers

No providers.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

No requirements.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_env"></a> [env](#input\_env) | The application environment (dev, test, sandbox, prod) | `string` | n/a | yes |
| <a name="input_create_local_sops_wrapper"></a> [create\_local\_sops\_wrapper](#input\_create\_local\_sops\_wrapper) | When `true`, creates sops wrapper file at `bin/sopsw`. | `bool` | `false` | no |
| <a name="input_region"></a> [region](#input\_region) | n/a | `string` | `"us-east-1"` | no |
| <a name="input_secondary_region"></a> [secondary\_region](#input\_secondary\_region) | n/a | `string` | `"us-west-2"` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_platform"></a> [platform](#module\_platform) | github.com/CMSgov/cdap//terraform/modules/platform | ff2ef539fb06f2c98f0e3ce0c8f922bdacb96d66 |
| <a name="module_sops"></a> [sops](#module\_sops) | github.com/CMSgov/cdap//terraform/modules/sops | 8874310 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

No resources.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->

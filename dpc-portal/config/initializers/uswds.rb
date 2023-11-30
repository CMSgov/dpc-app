# Ensure the USWDS dist folder is compiled as part of assets. 
# 
# This serves a couple different purposes:
# - Ensures that images and fonts are exposed properly on the client-side,
#   since USWDS CSS references specific assets for their design components.
# - Ensures that the js files are exposed properly for use in the application layout.
#
# See also: app/assets/stylesheets/uswds-theme.scss
Rails.application.config.assets.paths <<
  Rails.root.join('node_modules/@uswds/uswds/dist')
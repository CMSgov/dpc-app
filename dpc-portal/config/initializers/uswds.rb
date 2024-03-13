# Ensure the USWDS dist folder is included in asset paths. This allows us 
# to reference assets like so:
#   'img/usa-icons/my-icon.svg'
#
# instead of:
#   '@uswds/uswds/dist/img/usa-icons/my-icon.svg'
# 
# See also: assets/config/manifest.js
#           app/assets/stylesheets/uswds-theme.scss
Rails.application.config.assets.paths << Rails.root.join('@uswds/uswds/dist')

# Ensure the USWDS dist folder is compiled as part of assets, since their CSS 
# relies on specific images and other provided assets for the design system.
#
# See also: app/assets/stylesheets/uswds-theme.scss
Rails.application.config.assets.paths <<
  Rails.root.join('node_modules/@uswds/uswds/dist')
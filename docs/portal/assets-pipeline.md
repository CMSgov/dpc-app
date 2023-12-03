## The Assets Pipeline

The assets pipeline compiles Javascript, CSS, images, fonts, and other static assets for use by the Rails application. In development, this compilation is allowed to happen at runtime; however, assets are required to be precompiled in deployed environments (RAILS_ENV=production) using the `config.assets.compile = false` option.

The assets pipeline mirrors the setup established by [cssbundling-rails](https://github.com/rails/cssbundling-rails), which was adopted in Rails 7, and leverages sass compilation along with [Sprockets](https://github.com/rails/sprockets-rails).

All CSS must be imported by the [application.sass.scss](/dpc-portal/app/assets/stylesheets/application.sass.scss) file. This is compiled by the `build:css` npm script into `app/assets/builds/application.css`.

The `assets:precompile` step in the Dockerfile takes care of everything else, compiling all linked assets in the Sprockets [manifest.js](/dpc-portal/app/assets/config/manifest.js) file into the `public/assets` folder with digest strings embedded into each filename. This causes automatic browser cache-busting to ensure that users always receive the newest version of an asset after a deployment.

Read more about Sprockets [here](https://github.com/rails/sprockets/blob/main/guides/how_sprockets_works.md).

# DPC AdminV2 Portal README

This is the admin portal for the Data at the Point of Care (DPC) 2.0 [implementer portal](https://github.com/CMSgov/dpc-app/tree/master/dpc-impl).

All static public pages are hosted on a separate static site. This many result in broken links when run locally.

## Installation and Set Up

#### Static Pages

All static public pages are hosted in a separate [jekyll site](https://github.com/CMSgov/dpc-static-site). To run the full suite, clone and follow the local instructions for the jekyll site to run at the same time as this web app.

You will need to add a `STATIC_SITE_URL` variable to your `.env.development.local` file:

```
STATIC_SITE_URL=http://localhost:4001
```
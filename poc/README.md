# Proof of Concept Code for [DPC-3631](https://jira.cms.gov/browse/DPC-3631)

Each of the following PoC's implements a basic hello world page along with the header from dpc-web.

* hello_phlex: Does the job using the Phlex framework.
* hello_viewcomponent: Uses the ViewComponent framework.
* hello_rails: Uses vanilla Rails partials.

All three were developed using Ruby 3.0.6 and Rails 7.0.8.  If that's not what you have installed locally, you can use RbEnv to set everything up.

To run any of the PoC's, cd into their directory and run `rails server`.  Then go to `http://localhost:3000` to see the results.
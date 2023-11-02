# frozen_string_literal: true

class NavbarComponent < ApplicationComponent
include Phlex::Rails::Helpers::LinkTo
include Phlex::Rails::Helpers::ImageTag

# Phlex doesn't support the <use> tag, so we have to create our own
register_element :use

  def template
    a(class: "ds-c-skip-nav", href: "#main") {"Skip to main content"}

    nav(class: "navbar", id: "navbar") {
        @top_nav_img = helpers.image_tag("top-nav-heart.svg", alt: "Heart logo")
        link_to("#{@top_nav_img} Data at the Point of Care".html_safe, root_path, class: "site-logo")

        button(id: "topnav__show-button", class: "ds-c-button ds-c-button--transparent ds-c-button--big topnav__show-button", type: "button", aria: {controls: "topnav-wrap", label: "Show menu"}) {
            svg(xmlns: "http://www.w3.org/2000/svg", aria: {hidden: "true"}) {
                use("xlink:href" => "/assets/solid.svg#bars")
            }
        }

        div(class: "topnav-wrap", id: "topnav-wrap") {
            button(id: "topnav__close-button", class: "ds-c-button ds-c-button--transparent ds-c-button--big topnav__close-button", type: "button", aria: {controls: "topnav-wrap", label: "Close menu"}) {
                svg(xmlns: "http://www.w3.org/2000/svg", aria: {hidden: "true"}) {
                    use("xlink:href" => "/assets/solid.svg#times")
                }
            }

            ul(class: "topnav") {
                li(class: "topnav__item") {
                    link_to("Home", home_path, class: "topnav__link")
                }
                li(class: "topnav__item") {
                    link_to("Documentation", docs_path, class: "topnav__link")
                }
                li(class: "topnav__item") {
                    link_to("Frequently Asked Questions", faq_path, class: "topnav__link")
                }
                li(class: "topnav__item topnav__item--buttons ds-u-padding-right--1") {
                   link_to("Log in", "/session/new", class: "ds-c-button", data: { test: "login-link" })
                }
                li(class: "topnav__item") {
                   link_to("Request access", "/users/new", class: "ds-c-button ds-c-button--primary")
                }
            }
        }
    }
  end
end

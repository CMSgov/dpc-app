# frozen_string_literal: true

class Hello::IndexView < ApplicationView
  def template
    section(class: "ds-u-padding-y--7 ds-u-fill--primary-darkest") {
        h2(style: "color:blue") { "Hello World" }
        p(style: "color:red") { "Testing, testing, 1, 2... uh... 3?" }
    }
  end
end

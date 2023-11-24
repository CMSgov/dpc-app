class Alert::Preview < ViewComponent::Preview
    def standard
        render Alert::Component.new(text: "test")
    end
end
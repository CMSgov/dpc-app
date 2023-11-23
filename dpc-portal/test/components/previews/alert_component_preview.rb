class AlertComponentPreview < Lookbook::Preview
    def standard
        render AlertComponent.new(text: "test")
    end
end
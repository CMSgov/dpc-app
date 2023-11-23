class ExampleComponentPreview < Lookbook::Preview
    def standard
        render ExampleComponent.new(title: "I am a title")
    end
end
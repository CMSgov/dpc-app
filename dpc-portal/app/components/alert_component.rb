class AlertComponent < ViewComponent::Base
    erb_template <<-ERB
    <div class="usa-alert usa-alert--info">
    <div class="usa-alert__body">
      <h4 class="usa-alert__heading">Informative status</h4>
      <p class="usa-alert__text">
        Lorem ipsum dolor sit amet
      </p>
    </div>
  </div>
      ERB
  
    def initialize(text:)
        @text = text
    end
  end
  
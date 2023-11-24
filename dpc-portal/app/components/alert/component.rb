class Alert::Component < ViewComponent::Base
    attr_accessor :status, :include_icon, :heading

    def initialize(status: 'info', heading: '', include_icon: true)
        super
        @status = status.presence || 'info'
        @include_icon = include_icon
        @heading = heading
    end
  end
  
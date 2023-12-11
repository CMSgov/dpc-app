# frozen_string_literal: true

class Core::Card::OrganizationCardComponentPreview < ViewComponent::Preview
  def default
    render(Core::Card::OrganizationCardComponent.new)
  end
end

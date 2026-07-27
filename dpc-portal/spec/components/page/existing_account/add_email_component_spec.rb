# frozen_string_literal: true

RSpec.describe Page::ExistingAccount::AddEmailComponent, type: :component do
  include ComponentSupport

  describe 'html' do
    subject(:html) do
      render_inline(component)
      normalize_space(rendered_content)
    end

    let(:component) { described_class.new(email, csp) }

    before do
      render_inline(component)
    end
  end
end

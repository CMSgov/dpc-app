# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Alert::Component, type: :component do
  subject(:component) { described_class.new }

  def render_component
    render_inline(component) do
      'Check six'
    end
  end

  it 'is an alert' do
    render_component
    expect(page).to have_selector('div.usa-alert')
  end

  it 'has a body' do
    render_component
    expect(page).to have_selector('.usa-alert div.usa-alert__body')
  end

  it 'includes the content' do
    render_component
    expect(page.find('p.usa-alert__text')).to have_content('Check six')
  end

  describe 'heading option' do
    context 'when no heading is given' do
      subject(:component) { described_class.new heading: '' }

      it 'is a slim alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--slim')
      end

      it 'does not include a heading' do
        render_component
        expect(page).not_to have_selector('.usa-alert__heading')
      end
    end

    context 'when a heading is given' do
      subject(:component) { described_class.new heading: 'Look' }

      it 'includes the heading' do
        render_component
        expect(page)
          .to have_selector('.usa-alert .usa-alert__body h2.usa-alert__heading')
      end

      it 'includes the heading text' do
        render_component
        expect(page.find('.usa-alert__heading')).to have_content('Look')
      end
    end
  end

  describe 'status option' do
    context 'when no status is given' do
      subject(:component) { described_class.new }

      it 'is an info alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--info')
      end
    end

    context 'when the status is blank' do
      subject(:component) { described_class.new status: '' }

      it 'is an info alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--info')
      end
    end

    context 'when the status is :notice' do
      subject(:component) { described_class.new status: :notice }

      it 'is an info alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--info')
      end
    end

    context 'when the status is \'notice\'' do
      subject(:component) { described_class.new status: 'notice' }

      it 'is an info alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--info')
      end
    end

    context 'when the status is :alert' do
      subject(:component) { described_class.new status: :alert }

      it 'is an error alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--error')
      end
    end

    context 'when the status is \'alert\'' do
      subject(:component) { described_class.new status: 'alert' }

      it 'is an error alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--error')
      end
    end

    context 'when the status is success' do
      subject(:component) { described_class.new status: 'success' }

      it 'is an success alert' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--success')
      end
    end

    context 'when the status is invalid' do
      subject(:component) { described_class.new status: 'invalid' }

      it 'renders nothing' do
        render_component
        expect(page).to_not have_selector('.usa-alert')
      end
    end
  end

  describe 'icon option' do
    context 'when the icon option is not set' do
      subject(:component) { described_class.new }

      it 'includes an icon' do
        render_component
        expect(page).not_to have_selector('.usa-alert--no-icon')
      end
    end

    context 'when the icon option is true' do
      subject(:component) { described_class.new include_icon: true }

      it 'includes an icon' do
        render_component
        expect(page).not_to have_selector('.usa-alert--no-icon')
      end
    end

    context 'when the icon option is false' do
      subject(:component) { described_class.new include_icon: false }

      it 'does not include an icon' do
        render_component
        expect(page).to have_selector('.usa-alert.usa-alert--no-icon')
      end
    end
  end

  describe 'message_key option' do
    context 'invalid instantiation' do
      subject(:component) { described_class.new heading: 'Look', message_key: 'email_not_found' }

      it 'raises an error' do
        expect { render_component }.to raise_error(ArgumentError)
      end
    end
    context 'valid instantiation' do
      subject(:component) { described_class.new message_key: 'email_not_found' }

      before { allow(component).to receive(:t).and_call_original }

      it 'renders the component and translates the key into heading and message' do
        render_component
        expect(page).to have_selector('.usa-alert')

        # the message_key is looked up under errors.<key> for both the heading and the body
        expect(component).to have_received(:t).with('errors.email_not_found.status')
        expect(component).to have_received(:t).with('errors.email_not_found.text')

        expect(page.find('h2.usa-alert__heading'))
          .to have_content(I18n.t('errors.email_not_found.status'))
        expect(page.find('.usa-alert__text'))
          .to have_content('Try again with a different email:')
      end

      it 'renders the translated message as html rather than escaping it' do
        render_component
        expect(page).to have_selector('.usa-alert__body ol li', count: 2)
        expect(page.find('.usa-alert__body ol li', match: :first))
          .to have_content('Sign out of your identity provider')
      end
    end
  end
end

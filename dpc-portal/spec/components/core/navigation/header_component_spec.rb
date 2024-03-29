# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Navigation::HeaderComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new(links) }

    before do
      render_inline(component)
    end

    context 'without links' do
      let(:links) { [] }
      it { is_expected.to match_html_fragment(expected_html('')) }
    end

    context 'when has one link' do
      let(:links) { [['Link 1', '/href1', false]] }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1">Link 1</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_links)) }
    end

    context 'when has two links' do
      let(:links) { [['Link 1', '/href1', false], ['Link 2', '/href2', false]] }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href2">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_links)) }
    end

    context 'when has two links, first current' do
      let(:links) { [['Link 1', '/href1', true], ['Link 2', '/href2', false]] }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link usa-current" href="/href1">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href2">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_links)) }
    end

    context 'when has two links, second current' do
      let(:links) { [['Link 1', '/href1', false], ['Link 2', '/href2', true]] }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link usa-current" href="/href2">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_links)) }
    end

    context 'when has two links, both current' do
      let(:links) { [['Link 1', '/href1', true], ['Link 2', '/href2', true]] }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link usa-current" href="/href1">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link usa-current" href="/href2">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_links)) }
    end
  end

  def expected_html(expected_links)
    <<~HTML
      <div class="usa-overlay"></div>
      <header class="usa-header usa-header--extended">
        <nav aria-label="Primary navigation" class="usa-nav">
          <div class="usa-nav__inner">
            <ul class="usa-nav__primary usa-accordion">
            #{expected_links}
            </ul>
          </div>
        </nav>
      </header>
    HTML
  end
end

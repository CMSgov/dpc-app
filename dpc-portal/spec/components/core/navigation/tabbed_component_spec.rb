# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Navigation::TabbedComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    let(:component) { described_class.new('header_id', links) }
    let(:expected_selectors) { '' }
    let(:expected_start_index) { -1 }
    before do
      render_inline(component)
    end

    context 'without links' do
      let(:links) { [] }
      it { is_expected.to match_html_fragment(expected_html('', -1, '')) }
    end

    context 'when has one link' do
      let(:links) { [['Link 1', '/href1', '.area1', false]] }
      let(:expected_selectors) { '".area1"' }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1" onclick="make_current(0); return false">Link 1</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_links)) }
    end

    context 'when has two links' do
      let(:links) { [['Link 1', '/href1', '.area1', false], ['Link 2', '/href2', '.area2', false]] }
      let(:expected_selectors) { '".area1", ".area2"' }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1" onclick="make_current(0); return false">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href2" onclick="make_current(1); return false">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_links)) }
    end

    context 'when has two links, first current' do
      let(:links) { [['Link 1', '/href1', '.area1', true], ['Link 2', '/href2', '.area2', false]] }
      let(:expected_selectors) { '".area1", ".area2"' }
      let(:expected_start_index) { 0 }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1" onclick="make_current(0); return false">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href2" onclick="make_current(1); return false">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_links)) }
    end

    context 'when has two links, second current' do
      let(:links) { [['Link 1', '/href1', '.area1', false], ['Link 2', '/href2', '.area2', true]] }
      let(:expected_selectors) { '".area1", ".area2"' }
      let(:expected_start_index) { 1 }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1" onclick="make_current(0); return false">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href2" onclick="make_current(1); return false">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_links)) }
    end

    context 'when has two links, both current' do
      let(:links) { [['Link 1', '/href1', '.area1', true], ['Link 2', '/href2', '.area2', true]] }
      let(:expected_selectors) { '".area1", ".area2"' }
      let(:expected_start_index) { 1 }
      let(:expected_links) do
        <<~HTML
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href1" onclick="make_current(0); return false">Link 1</a>
          </li>
          <li class="usa-nav__primary-item">
            <a class="usa-nav-link" href="/href2" onclick="make_current(1); return false">Link 2</a>
          </li>
        HTML
      end

      it { is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_links)) }
    end
  end

  def expected_html(expected_selectors, expected_start_index, expected_links)
    <<~HTML
      <script>
        //<![CDATA[
          function make_current(link_index) {
            const selectors = [#{expected_selectors}];
            var current_index = 0;
            for (anchor of document.querySelector('.usa-nav__primary').getElementsByTagName("a")) {
              if (current_index == link_index){
                anchor.className = 'usa-nav-link usa-current';
                if (document.querySelector(selectors[current_index])) {
                  document.querySelector(selectors[current_index]).style.display = 'block';
                }
              } else {
                anchor.className = 'usa-nav-link';
                if (document.querySelector(selectors[current_index])) {
                  document.querySelector(selectors[current_index]).style.display = 'none';
                }
              }
              current_index++;
            }
          }
          document.addEventListener("DOMContentLoaded", function() {
            if (document.querySelector("#header_id")) {
              document.querySelector("#header_id").style.display = 'block';
            }
            make_current(#{expected_start_index});
          });
        //]]>
      </script>
      <div class="usa-overlay"></div>
      <header id="header_id" class="usa-header usa-header--extended" style="display: none">
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

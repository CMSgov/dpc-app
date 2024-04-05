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
      let(:links) { [['Link 1', '.area1', false]] }
      let(:expected_selectors) { '".area1"' }
      let(:expected_buttons) do
        <<~HTML
          <li>
            <button type="button" style="box-shadow: none; border-radius: 0;" class="usa-button usa-button--outline" onclick="make_current(0); return false">
              Link 1
            </button>
          </li>
        HTML
      end

      it {
        is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_buttons))
      }
    end

    context 'when has two links' do
      let(:expected_buttons) do
        <<~HTML
          <li>
            <button type="button" style="box-shadow: none; border-radius: 0;" class="usa-button usa-button--outline" onclick="make_current(0); return false">
              Link 1
            </button>
          </li>
          <li>
            <button type="button" style="box-shadow: none; border-radius: 0;" class="usa-button usa-button--outline" onclick="make_current(1); return false">
              Link 2
            </button>
          </li>
        HTML
      end

      context 'none current' do
        let(:links) { [['Link 1', '.area1', false], ['Link 2', '.area2', false]] }
        let(:expected_selectors) { '".area1", ".area2"' }

        it {
          is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_buttons))
        }
      end

      context 'first current' do
        let(:links) { [['Link 1', '.area1', true], ['Link 2', '.area2', false]] }
        let(:expected_selectors) { '".area1", ".area2"' }
        let(:expected_start_index) { 0 }

        it {
          is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_buttons))
        }
      end

      context 'second current' do
        let(:links) { [['Link 1', '.area1', false], ['Link 2', '.area2', true]] }
        let(:expected_selectors) { '".area1", ".area2"' }
        let(:expected_start_index) { 1 }

        it {
          is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_buttons))
        }
      end

      context 'both current' do
        let(:links) { [['Link 1', '.area1', true], ['Link 2', '.area2', true]] }
        let(:expected_selectors) { '".area1", ".area2"' }
        let(:expected_start_index) { 1 }

        it {
          is_expected.to match_html_fragment(expected_html(expected_selectors, expected_start_index, expected_buttons))
        }
      end
    end
  end

  def expected_html(expected_selectors, expected_start_index, expected_buttons)
    <<~HTML
      <script>
        //<![CDATA[
        function make_current(link_index) {
          const selectors = [#{expected_selectors}];
          var current_index = 0;
          for (anchor of document.querySelector('#header_id').getElementsByTagName("button")) {
            if (current_index == link_index){
              anchor.className = 'usa-button usa-button--outline usa-button--active';
              anchor.style.borderBottom = '5px solid #162E51';
            if (document.querySelector(selectors[current_index])) {
                document.querySelector(selectors[current_index]).style.display = 'block';
              }
            } else {
              anchor.className = 'usa-button usa-button--outline';
              anchor.style.borderBottom = '5px solid transparent';
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
      <header id="header_id" class="usa-header usa-header--extended margin-bottom-2" style="display: none">
        <nav>
          <ul class="usa-button-group">
            #{expected_buttons}
          </ul>
        </nav>
      </header>
    HTML
  end
end

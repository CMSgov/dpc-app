# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Core::Card::OrganizationCardComponent, type: :component do
  describe 'html' do
    subject(:html) do
      render_inline(component)
      rendered_content
    end

    before do
      render_inline(component)
    end
    let(:shared_header) do
      <<~HTML
        <li class="usa-card tablet-lg:grid-col-1 widescreen:grid-col-1" style="list-style:none; visibility:visible;" data-npi="npi" data-name="name">
            <div class="usa-card__container">
            <div class="usa-card__header">
                <h2 class="usa-card__heading">
                name
                </h2>
            </div>
            <div class="usa-card__body">
                <div class="clearfix">
                <div id="npi_div" class="float-left">
                <p class="usa-card__text">
                    <span style="font-weight:bold">NPI</span>
                    <span>npi</span>
                </p>
                </div>
                <div id="status_div" class="float-right">
                <p class="usa-card__text">
                    <form class="button_to" method="get" action="/portal/organizations/5"><button class="usa-button--outline usa-button" type="submit">View Details</button></form>
                </p>
                </div>
            </div>
      HTML
    end
    let(:shared_footer) { '</div></div></li>' }

    context 'as AO' do
      context 'valid organization' do
        let(:component) do
          org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', terms_of_service_accepted_at: 2.days.ago)
          link = AoOrgLink.new(provider_organization: org)
          described_class.new(link:)
        end
        let(:expected_html) do
          <<~HTML
            #{shared_header}
               <div class="clearfix">
                 <div class="float-left">
                   <svg class="text-accent-cool usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                    <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#verified></use>
                   </svg>
                 </div>
                 <div class="float-left margin-left-1 margin-top-neg-2px">#{I18n.t('verification.manage_org')}</div>
               </div>
            #{shared_footer}
          HTML
        end

        it { is_expected.to match_html_fragment(expected_html) }
      end
      context 'rejected organization' do
        let(:component) do
          org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', verification_status: 'rejected',
                                         verification_reason: 'no_approved_enrollment')
          link = AoOrgLink.new(provider_organization: org)
          described_class.new(link:)
        end
        let(:expected_html) do
          <<~HTML
            #{shared_header}
                        <div class="clearfix">
                          <div class="float-left">  <svg class="text-gray-50 usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                              <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#lock></use>
                            </svg>
                          </div>
                          <div class="float-left margin-left-1 margin-top-neg-2px">#{I18n.t('verification.no_approved_enrollment_status')}</div>
                        </div>
            #{shared_footer}
          HTML
        end

        it { is_expected.to match_html_fragment(expected_html) }
      end
      context 'link not valid' do
        let(:component) do
          org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', verification_status: 'approved')
          link = AoOrgLink.new(provider_organization: org, verification_status: false,
                               verification_reason: 'user_not_authorized_official')
          described_class.new(link:)
        end
        let(:expected_html) do
          <<~HTML
            #{shared_header}
                        <div class="clearfix">
                          <div class="float-left">  <svg class="text-gray-50 usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                              <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#lock></use>
                            </svg>
                          </div>
                          <div class="float-left margin-left-1 margin-top-neg-2px">#{I18n.t('verification.user_not_authorized_official_status')}</div>
                        </div>
            #{shared_footer}
          HTML
        end

        it { is_expected.to match_html_fragment(expected_html) }
      end
      context 'organization not signed tos' do
        let(:component) do
          org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5')
          link = AoOrgLink.new(provider_organization: org)
          described_class.new(link:)
        end
        let(:expected_html) do
          <<~HTML
            #{shared_header}
                        <div class="clearfix">
                          <div class="float-left">  <svg class="text-gold usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                              <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#warning></use>
                            </svg>
                          </div>
                          <div class="float-left margin-left-1 margin-top-neg-2px">#{I18n.t('verification.tos_not_signed')}</div>
                        </div>
            #{shared_footer}
          HTML
        end

        it { is_expected.to match_html_fragment(expected_html) }
      end
    end
    context 'as CD' do
      context 'valid organization' do
        let(:component) do
          org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', terms_of_service_accepted_at: 2.days.ago)
          link = CdOrgLink.new(provider_organization: org)
          described_class.new(link:)
        end
        let(:expected_html) do
          <<~HTML
            #{shared_header}
                            <div class="clearfix">
                              <div class="float-left">
                                <svg class="text-accent-cool usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                                 <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#verified></use>
                                </svg>
                              </div>
                              <div class="float-left margin-left-1 margin-top-neg-2px">#{I18n.t('cd_access.manage_org')}</div>
                            </div>
                           </div>
                           </div>
                        </li>
          HTML
        end

        it { is_expected.to match_html_fragment(expected_html) }
      end
      context 'rejected organization' do
        let(:component) do
          org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5', verification_status: 'rejected',
                                         verification_reason: 'no_approved_enrollment')
          link = CdOrgLink.new(provider_organization: org)
          described_class.new(link:)
        end
        let(:expected_html) do
          <<~HTML
            #{shared_header}
                        <div class="clearfix">
                          <div class="float-left">  <svg class="text-gray-50 usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                              <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#lock></use>
                            </svg>
                          </div>
                          <div class="float-left margin-left-1 margin-top-neg-2px">#{I18n.t('cd_access.no_approved_enrollment_status')}</div>
                        </div>
            #{shared_footer}
          HTML
        end

        it { is_expected.to match_html_fragment(expected_html) }
      end
      context 'organization not signed tos for CD' do
        let(:component) do
          org = ProviderOrganization.new(name: 'name', npi: 'npi', id: '5')
          link = CdOrgLink.new(provider_organization: org)
          described_class.new(link:)
        end
        let(:expected_html) do
          <<~HTML
            #{shared_header}
                        <div class="clearfix">
                          <div class="float-left">  <svg class="text-gold usa-icon" style="transform: scale(1)" aria-hidden="true" role="img">
                              <use xlink:href=/portal/assets/@uswds/uswds/dist/img/sprite-9865eea7b251e43137fb770626d6cd51c474a3a436678a6e66cafce50968076f.svg#warning></use>
                            </svg>
                          </div>
                          <div class="float-left margin-left-1 margin-top-neg-2px">#{I18n.t('cd_access.tos_not_signed')}</div>
                        </div>
            #{shared_footer}
          HTML
        end

        it { is_expected.to match_html_fragment(expected_html) }
      end
    end
  end
end

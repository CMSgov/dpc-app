# frozen_string_literal: true

require 'rails_helper'
require './app/services/user_info_service'

RSpec.describe Invitation, type: :model do
  let(:organization) { build(:provider_organization) }

  describe :cd do
    let(:valid_new_cd_invite) { build(:invitation, :cd) }
    describe :create do
      it 'passes validations' do
        expect(valid_new_cd_invite.valid?).to eq true
      end

      it 'fails if no provider organization' do
        valid_new_cd_invite.provider_organization = nil
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'fails if no invited by' do
        valid_new_cd_invite.invited_by = nil
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_by]).to eq ["can't be blank"]
      end

      it 'fails on blank invitation type' do
        valid_new_cd_invite.invitation_type = nil
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          valid_new_cd_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end

      it 'fails on blank given name' do
        valid_new_cd_invite.invited_given_name = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_given_name]).to eq ["can't be blank"]
      end

      it 'fails on blank family name' do
        valid_new_cd_invite.invited_family_name = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_family_name]).to eq ["can't be blank"]
      end

      it 'fails on fewer than ten digits in phone' do
        valid_new_cd_invite.phone_raw = '877-288-313'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_phone]).to eq ['is invalid']
      end

      it 'fails on more than ten digits in phone' do
        valid_new_cd_invite.phone_raw = '877-288-31333'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_phone]).to eq ['is invalid']
      end

      it 'fails on blank phone' do
        valid_new_cd_invite.phone_raw = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 2
        expect(valid_new_cd_invite.errors[:phone_raw]).to eq ["can't be blank"]
        expect(valid_new_cd_invite.errors[:invited_phone]).to eq ['is invalid']
      end

      it 'fails on bad email' do
        valid_new_cd_invite.invited_email_confirmation = valid_new_cd_invite.invited_email = 'rob-at-example.com'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_email]).to eq ['is invalid']
      end

      it 'fails on blank email' do
        valid_new_cd_invite.invited_email_confirmation = valid_new_cd_invite.invited_email = ''
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 3
        expect(valid_new_cd_invite.errors[:invited_email]).to eq ["can't be blank", 'is invalid']
        expect(valid_new_cd_invite.errors[:invited_email_confirmation]).to eq ["can't be blank"]
      end

      it 'fails on non-matching email confirmation' do
        valid_new_cd_invite.invited_email_confirmation = 'robert@example.com'
        expect(valid_new_cd_invite.valid?).to eq false
        expect(valid_new_cd_invite.errors.size).to eq 1
        expect(valid_new_cd_invite.errors[:invited_email_confirmation]).to eq ["doesn't match Invited email"]
      end

      it 'fails on bad status' do
        expect do
          valid_new_cd_invite.status = :fake_status
        end.to raise_error(ArgumentError)
      end
    end

    describe :update do
      let(:saved_cd_invite) do
        valid_new_cd_invite.save!
        valid_new_cd_invite
      end
      it 'should allow blank invitation values' do
        saved_cd_invite.invited_given_name = ''
        saved_cd_invite.invited_family_name = ''
        saved_cd_invite.invited_phone = ''
        saved_cd_invite.invited_email = ''
        saved_cd_invite.invited_email_confirmation = ''
        expect(saved_cd_invite.valid?).to eq true
      end

      it 'should allow blank phone_raw' do
        saved_cd_invite.phone_raw = ''
        expect(saved_cd_invite.valid?).to eq true
      end

      it 'fails if no provider organization' do
        saved_cd_invite.provider_organization = nil
        expect(saved_cd_invite.valid?).to eq false
        expect(saved_cd_invite.errors.size).to eq 1
        expect(saved_cd_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'fails on blank invitation type' do
        saved_cd_invite.invitation_type = nil
        expect(saved_cd_invite.valid?).to eq false
        expect(saved_cd_invite.errors.size).to eq 1
        expect(saved_cd_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          saved_cd_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end
    end

    describe :expired do
      it 'should not be expired if less than 2 days old' do
        invitation = create(:invitation, :cd, created_at: 47.hours.ago)
        expect(invitation.expired?).to eq false
      end
      it 'should be expired if more than 2 days old' do
        invitation = create(:invitation, :cd, created_at: 49.hours.ago)
        expect(invitation.expired?).to eq true
      end
      it 'should be expired if 2 days old' do
        invitation = create(:invitation, :cd, created_at: 2.days.ago)
        expect(invitation.expired?).to eq true
      end
    end

    describe :accept! do
      it 'should unset attributes and change status' do
        invitation = create(:invitation, :cd)
        invitation.accept!
        invitation.reload
        expect(invitation.invited_given_name).to be_nil
        expect(invitation.invited_family_name).to be_nil
        expect(invitation.invited_phone).to be_nil
        expect(invitation.invited_email).to be_nil
        expect(invitation).to be_accepted
      end
    end

    describe :match_user do
      let(:cd_invite) { build(:invitation, :cd) }
      let(:user_info) do
        { 'email' => 'bob@testy.com',
          'all_emails' => [
            'bob@testy.com',
            'bob@example.com'
          ],
          'given_name' => 'Bob',
          'family_name' => 'Hodges',
          'phone' => '+111-111-1111' }
      end
      it 'should match user if last name and email correct' do
        expect(cd_invite.cd_match?(user_info)).to eq true
        expect(cd_invite.email_match?(user_info)).to eq true
      end
      it 'should match user if names and email different case' do
        cd_invite.invited_family_name.downcase!
        cd_invite.invited_email = cd_invite.invited_email.upcase_first
        expect(cd_invite.cd_match?(user_info)).to eq true
        expect(cd_invite.email_match?(user_info)).to eq true
      end
      it 'should match user if given name different' do
        cd_invite.invited_given_name = 'fake'
        expect(cd_invite.cd_match?(user_info)).to eq true
      end
      it 'should match user if phone different' do
        cd_invite.invited_phone = '11234567890'
        expect(cd_invite.cd_match?(user_info)).to eq true
      end
      it 'should match if invited email eq email' do
        cd_invite.invited_email = user_info['email']
        expect(cd_invite.email_match?(user_info)).to eq true
      end
      it 'should not match user if family name not correct' do
        cd_invite.invited_family_name = "not #{cd_invite.invited_family_name}"
        expect(cd_invite.cd_match?(user_info)).to eq false
      end
      it 'should not match user if email not correct' do
        cd_invite.invited_email = "not #{cd_invite.invited_email}"
        expect(cd_invite.email_match?(user_info)).to eq false
      end
      it 'should raise error if user_info missing given name' do
        missing_info = user_info.merge({ 'given_name' => '' })
        expect do
          cd_invite.cd_match?(missing_info)
        end.to raise_error(UserInfoServiceError, 'missing_info')
      end
      it 'should raise error if user_info missing family name' do
        missing_info = user_info.merge({ 'family_name' => '' })
        expect do
          cd_invite.cd_match?(missing_info)
        end.to raise_error(UserInfoServiceError, 'missing_info')
      end
      it 'should raise error if user_info missing phone' do
        missing_info = user_info.merge({ 'phone' => '' })
        expect do
          cd_invite.cd_match?(missing_info)
        end.to raise_error(UserInfoServiceError, 'missing_info')
      end
      it 'should raise error if no user_info email' do
        missing_info = user_info.merge({ 'email' => '' })
        expect do
          cd_invite.email_match?(missing_info)
        end.to raise_error(UserInfoServiceError, 'missing_info')
      end
    end

    describe :cancel do
      it 'should not cancel an accepted invitation' do
        cd_invite = create(:invitation, :cd, status: :accepted)
        cd_invite.status = :cancelled
        expect(cd_invite.valid?).to eq false
        expect(cd_invite.errors.first.message).to eq 'You may not cancel an accepted invitation.'
      end
    end
  end

  describe :ao do
    let(:valid_new_ao_invite) { build(:invitation, :ao) }
    describe :create do
      it 'passes validations' do
        expect(valid_new_ao_invite.valid?).to eq(true), valid_new_ao_invite.errors.inspect
      end

      it 'fails if no provider organization' do
        valid_new_ao_invite.provider_organization = nil
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'does not fail if no invited by' do
        valid_new_ao_invite.invited_by = nil
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'fails on blank invitation type' do
        valid_new_ao_invite.invitation_type = nil
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          valid_new_ao_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end

      it 'does not fail on blank given name' do
        valid_new_ao_invite.invited_given_name = ''
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'does not fail on blank family name' do
        valid_new_ao_invite.invited_family_name = ''
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'does not fail on blank phone' do
        valid_new_ao_invite.phone_raw = ''
        expect(valid_new_ao_invite.valid?).to eq true
      end

      it 'fails on bad email' do
        valid_new_ao_invite.invited_email_confirmation = valid_new_ao_invite.invited_email = 'rob-at-example.com'
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:invited_email]).to eq ['is invalid']
      end

      it 'fails on blank email' do
        valid_new_ao_invite.invited_email_confirmation = valid_new_ao_invite.invited_email = ''
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 3
        expect(valid_new_ao_invite.errors[:invited_email]).to eq ["can't be blank", 'is invalid']
        expect(valid_new_ao_invite.errors[:invited_email_confirmation]).to eq ["can't be blank"]
      end

      it 'fails on non-matching email confirmation' do
        valid_new_ao_invite.invited_email_confirmation = 'robert@example.com'
        expect(valid_new_ao_invite.valid?).to eq false
        expect(valid_new_ao_invite.errors.size).to eq 1
        expect(valid_new_ao_invite.errors[:invited_email_confirmation]).to eq ["doesn't match Invited email"]
      end
    end

    describe :update do
      let(:saved_ao_invite) do
        valid_new_ao_invite.save!
        valid_new_ao_invite
      end
      it 'should allow blank invitation values' do
        saved_ao_invite.invited_given_name = ''
        saved_ao_invite.invited_family_name = ''
        saved_ao_invite.invited_phone = ''
        saved_ao_invite.invited_email = ''
        saved_ao_invite.invited_email_confirmation = ''
        expect(saved_ao_invite.valid?).to eq true
      end

      it 'should allow blank phone_raw' do
        saved_ao_invite.phone_raw = ''
        expect(saved_ao_invite.valid?).to eq true
      end

      it 'fails if no provider organization' do
        saved_ao_invite.provider_organization = nil
        expect(saved_ao_invite.valid?).to eq false
        expect(saved_ao_invite.errors.size).to eq 1
        expect(saved_ao_invite.errors[:provider_organization]).to eq ['must exist']
      end

      it 'fails on blank invitation type' do
        saved_ao_invite.invitation_type = nil
        expect(saved_ao_invite.valid?).to eq false
        expect(saved_ao_invite.errors.size).to eq 1
        expect(saved_ao_invite.errors[:invitation_type]).to eq ["can't be blank"]
      end

      it 'fails on invalid invitation type' do
        expect do
          saved_ao_invite.invitation_type = :birthday_party
        end.to raise_error(ArgumentError)
      end
    end

    describe :expired do
      it 'should not be expired if less than 2 days old' do
        invitation = create(:invitation, :ao, created_at: 47.hours.ago)
        expect(invitation.expired?).to eq false
      end
      it 'should be expired if more than 2 days old' do
        invitation = create(:invitation, :ao, created_at: 49.hours.ago)
        expect(invitation.expired?).to eq true
      end
      it 'should be expired if 2 days old' do
        invitation = create(:invitation, :ao, created_at: 2.days.ago)
        expect(invitation.expired?).to eq true
      end
    end

    describe :accept! do
      it 'should unset attributes and change status' do
        invitation = create(:invitation, :ao)
        invitation.accept!
        invitation.reload
        expect(invitation.invited_given_name).to be_nil
        expect(invitation.invited_family_name).to be_nil
        expect(invitation.invited_phone).to be_nil
        expect(invitation.invited_email).to be_nil
        expect(invitation).to be_accepted
      end
    end

    describe :match_user do
      let(:ao_invite) { build(:invitation, :ao, invited_email: 'bob@example.com') }
      it 'should match user if email match' do
        user_info = {
          'email' => 'bob@example.com',
          'all_emails' => [
            'bob@testy.com',
            'bob@example.com'
          ]
        }

        expect(ao_invite.email_match?(user_info)).to eq true
      end
      it 'should not match user if no email match' do
        user_info = { 'email' => 'tim@example.com' }
        expect(ao_invite.email_match?(user_info)).to eq false
      end
      it 'should raise error if user_info missing all_emails' do
        user_info = { 'email' => '' }
        expect do
          ao_invite.email_match?(user_info)
        end.to raise_error(UserInfoServiceError, 'missing_info')
      end
    end

    describe :ao_match do
      let(:ao_invite) { create(:invitation, :ao) }
      it 'should pass with good ssn' do
        user_info = { 'social_security_number' => '900111111' }
        expect(ao_invite.ao_match?(user_info)).to be_truthy
      end
      it 'should pass with good ssn with dashes' do
        user_info = { 'social_security_number' => '900-11-1111' }
        expect(ao_invite.ao_match?(user_info)).to be_truthy
      end
      it 'should raise with bad ssn' do
        user_info = { 'social_security_number' => '900666666' }
        expect do
          ao_invite.ao_match?(user_info)
        end.to raise_error(VerificationError, 'ao_med_sanctions')
      end
      it 'should raise error if user_info missing ssn' do
        user_info = { 'social_security_number' => nil }
        expect do
          ao_invite.ao_match?(user_info)
        end.to raise_error(UserInfoServiceError, 'missing_info')
      end
    end
  end

  describe :unacceptable_reason do
    it 'should be falsey' do
      invitation = create(:invitation, :ao)
      expect(invitation.unacceptable_reason).to be_falsey
    end
    it 'should be ao_accepted if expired and ao and accepted' do
      invitation = create(:invitation, :ao, created_at: 49.hours.ago, status: :accepted)
      expect(invitation.unacceptable_reason).to eq 'ao_accepted'
    end
    it 'should be invalid if expired and ao and cancelled' do
      invitation = create(:invitation, :ao, created_at: 49.hours.ago, status: :cancelled)
      expect(invitation.unacceptable_reason).to eq 'invalid'
    end
    it 'should be ao_expired if expired and ao' do
      invitation = create(:invitation, :ao, created_at: 49.hours.ago)
      expect(invitation.unacceptable_reason).to eq 'ao_expired'
    end
    it 'should be cd_expired if expired and cd' do
      invitation = create(:invitation, :cd, created_at: 49.hours.ago)
      expect(invitation.unacceptable_reason).to eq 'cd_expired'
    end
    it 'should be invalid if cancelled' do
      invitation = create(:invitation, :cd, status: :cancelled)
      expect(invitation.unacceptable_reason).to eq 'invalid'
    end
    it 'should be ao_accepted if accepted and ao' do
      invitation = create(:invitation, :ao)
      invitation.accept!
      expect(invitation.unacceptable_reason).to eq 'ao_accepted'
    end
    it 'should be cd_accepted if accepted and cd' do
      invitation = create(:invitation, :cd)
      invitation.accept!
      expect(invitation.unacceptable_reason).to eq 'cd_accepted'
    end
    it 'should be ao_renewed if renewed and authorized_official' do
      invitation = create(:invitation, :ao, created_at: 49.hours.ago, status: :renewed)
      expect(invitation.unacceptable_reason).to eq 'ao_renewed'
    end
  end

  describe :renew do
    context :ao do
      let!(:invitation) { create(:invitation, :ao, created_at: 49.hours.ago) }
      it 'should create another invitation for the user if expired' do
        new_invitation = nil
        expect do
          new_invitation = invitation.renew
        end.to change { Invitation.count }.by 1
        expect(new_invitation.invited_email).to eq invitation.invited_email
        expect(new_invitation.provider_organization).to eq invitation.provider_organization
        expect(new_invitation.unacceptable_reason).to be_falsey
        expect(invitation.unacceptable_reason).to be 'ao_renewed'
        expect(invitation.reload).to be_renewed
      end
      it 'should log renewal' do
        invitation.update!(created_at: 2.days.ago)
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(
          ['Authorized Official renewed expired invitation',
           { actionContext: LoggingConstants::ActionContext::Registration,
             actionType: LoggingConstants::ActionType::AoRenewedExpiredInvitation }]
        )
        invitation.renew
      end
      it 'should not create another invitation for the user if accepted' do
        invitation.accept!
        expect do
          invitation.renew
        end.to change { Invitation.count }.by 0
      end
      it 'should not create another invitation for the user if cancelled' do
        invitation.update!(status: :cancelled)
        expect do
          invitation.renew
        end.to change { Invitation.count }.by 0
      end
      it 'should not create another invitation for the user if valid' do
        invitation.update(created_at: 1.day.ago)
        expect do
          invitation.renew
        end.to change { Invitation.count }.by 0
      end
      it 'sends an invitation email on success' do
        mailer = double(InvitationMailer)
        expect(InvitationMailer).to receive(:with)
          .with(invitation: instance_of(Invitation),
                given_name: invitation.invited_given_name,
                family_name: invitation.invited_family_name)
          .and_return(mailer)
        expect(mailer).to receive(:invite_ao).and_return(mailer)
        expect(mailer).to receive(:deliver_now)
        invitation.renew
      end
    end

    context :cd do
      let!(:invitation) { create(:invitation, :cd, created_at: 49.hours.ago) }
      it 'should not create another invitation for the user' do
        old_status = invitation.status
        expect do
          invitation.renew
        end.to change { Invitation.count }.by 0
        expect(invitation.reload.status).to eq old_status
      end
    end
  end

  describe :expires_in do
    after { Timecop.return }
    let!(:invitation) { create(:invitation, :cd, created_at: 24.hours.ago) }
    it 'should expire in 23 hours 10 minutes' do
      Timecop.travel(2999.seconds.from_now)
      hours, minutes = invitation.expires_in
      expect(hours).to eq 23
      expect(minutes).to eq 10
    end
  end
end

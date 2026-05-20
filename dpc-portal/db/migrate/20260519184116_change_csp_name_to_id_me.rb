class ChangeCspNameToIdMe < ActiveRecord::Migration[8.0]
  def change
    csp = Csp.find_by(name: :id_dot_me)
    csp.update(name: :id_me)
  end
end

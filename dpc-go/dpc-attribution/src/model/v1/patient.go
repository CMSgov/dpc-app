package v1

// Patient is a struct that models the v1 Patients table
type Patient struct {
	ID  string `db:"id" json:"id" faker:"uuid_hyphenated"`
	MBI string `db:"beneficiary_id" json:"mbi" faker:"uuid_digit"`
}

package v1

// Job is a struct that models the v1 Rosters table
type Job struct {
	ID string `db:"job_id" json:"id" faker:"uuid_hyphenated"`
}

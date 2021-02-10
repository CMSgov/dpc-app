package orm

import (
	"time"
)

type Organization struct {
	tableName struct{}  `pg:"organization"`
	ID        string    `pg:",pk,type:uuid,default:gen_random_uuid()"`
	Version   int       `pg:"default:0"`
	CreatedAt time.Time `pg:"default:now()"`
	UpdatedAt time.Time `pg:"default:now()"`
	Info      map[string]interface{}
}

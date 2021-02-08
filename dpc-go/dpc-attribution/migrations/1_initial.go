package main

import (
	"fmt"
	models "github.com/CMSgov/dpc/attribution/pkg/orm"
	"github.com/go-pg/migrations/v8"
	"github.com/go-pg/pg/v10/orm"
)

func init() {
	migrations.MustRegisterTx(func(db migrations.DB) error {
		fmt.Println("creating table my_table...")
		err := db.Model((*models.Organization)(nil)).CreateTable(&orm.CreateTableOptions{
			IfNotExists: true,
		})
		return err
	}, func(db migrations.DB) error {
		fmt.Println("dropping table my_table...")
		err := db.Model((*models.Organization)(nil)).DropTable(nil)
		return err
	})
}

package service

import (
	"context"

	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	v1 "github.com/CMSgov/dpc/attribution/service/v1"
)

// Job is an interface that defines what a Job does
type Job interface {
	Export(ctx context.Context, orgID string, groupID string) (string, error)
}

// NewJobService function that creates and returns a JobService
func NewJobService(ctx context.Context) Job {
	log := logger.WithContext(ctx)
	attrDbV1 := repository.GetAttributionV1DbConnection()
	queueDbV1 := repository.GetQueueDbConnection()

	defer func() {
		if err := attrDbV1.Close(); err != nil {
			log.Fatal("Failed to close attribution v1 db connection", zap.Error(err))
		}
		if err := queueDbV1.Close(); err != nil {
			log.Fatal("Failed to close queue v1 db connection", zap.Error(err))
		}
	}()
	pr := repository.NewPatientRepo(attrDbV1)
	jr := repository.NewJobRepo(queueDbV1)
	return v1.NewJobServiceV1(pr, jr)
}

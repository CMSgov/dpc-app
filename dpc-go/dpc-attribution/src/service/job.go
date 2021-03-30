package service

import (
	"context"
)

// Job is an interface that defines what a Job does
type Job interface {
	Export(ctx context.Context, orgID string, groupID string) (string, error)
}

package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/model"
	"github.com/stretchr/testify/mock"
)

type MockJobClient struct {
	mock.Mock
}

func (mc *MockJobClient) Status(ctx context.Context, jobID string) ([]byte, error) {
	args := mc.Called(ctx, jobID)
	return args.Get(0).([]byte), args.Error(1)
}

func (mc *MockJobClient) Export(ctx context.Context, request model.ExportRequest) ([]byte, error) {
	args := mc.Called(ctx, request)
	return args.Get(0).([]byte), args.Error(1)
}

package main

import (
	"context"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/service"
	"github.com/CMSgov/dpc/api/service/admin"
	"github.com/CMSgov/dpc/api/service/public"
	"go.uber.org/zap"
	"log"
	"sync"
)

func main() {
	conf.NewConfig()
	ctx := context.Background()
	defer func() {
		err := logger.SyncLogger()
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}()

	ps := public.NewPublicServer()
	if ps == nil {
		log.Fatal("Could not create public server")
	}

	as := admin.NewAdminServer()
	if as == nil {
		log.Fatal("Could not create admin server")
	}

	startServers(ctx, ps, as)
}

func startServers(ctx context.Context, ps *service.Server, as *service.Server) {
	wg := new(sync.WaitGroup)
	wg.Add(2)

	go func() {
		err := ps.Serve(ctx)
		if err != nil {
			log.Fatal(err)
		}
		wg.Done()
	}()

	go func() {
		err := as.Serve(ctx)
		if err != nil {
			log.Fatal(err)
		}
		wg.Done()
	}()

	wg.Wait()
}

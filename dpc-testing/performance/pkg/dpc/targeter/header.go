package targeter

type Headers struct {
	ContentType, Accept string
	Custom              map[string][]string
}

func genHeaders(config Config) func() map[string][]string {
	// Build anonymous struct to store custom header generators
	dh := struct {
		ContentType, Accept string
		Custom              map[string]func() string
	}{
		ContentType: config.Headers.ContentType,
		Accept:      config.Headers.Accept,
		Custom:      make(map[string]func() string),
	}

	// Instantiate generator for each custom set of headers
	for header, values := range config.Headers.Custom {
		dh.Custom[header] = GenStrs(values)
	}

	// Each call to generator creates a fresh set of headers
	return func() map[string][]string {
		headers := make(map[string][]string)
		if dh.ContentType != "" {
			headers["Content-Type"] = []string{dh.ContentType}
		}
		if dh.Accept != "" {
			headers["Accept"] = []string{dh.Accept}
		}
		for header, fn := range dh.Custom {
			headers[header] = []string{fn()}
		}
		return headers
	}
}

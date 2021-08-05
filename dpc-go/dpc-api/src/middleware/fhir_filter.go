package middleware

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/model"
	"github.com/pkg/errors"
	"strings"
)

var filters = map[string]func([]byte) ([]byte, error){
	"organization": filterOrganization,
	"group":        filterGroup,
}

// Filter is a function that filters out all FHIR fields that aren't explicitly whitelisted
func Filter(ctx context.Context, body []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	rt, err := getResourceType(body)
	if err != nil {
		return nil, err
	}
	log.Debug(fmt.Sprintf("Filtering out data for resource type %s", rt))
	fn := filters[rt]
	if fn == nil {
		return nil, errors.New("Resource type not found to filter")
	}
	return filters[rt](body)
}

func getResourceType(body []byte) (string, error) {
	var result model.ResourceType
	if err := json.Unmarshal(body, &result); err != nil {
		return "", err
	}
	return strings.ToLower(result.ResourceType), nil
}

func filterOrganization(body []byte) ([]byte, error) {
	var organization model.Organization
	if err := json.Unmarshal(body, &organization); err != nil {
		return nil, err
	}
	return json.Marshal(organization)
}

func filterGroup(body []byte) ([]byte, error) {
	var group model.Group
	if err := json.Unmarshal(body, &group); err != nil {
		return nil, err
	}
	for i := range group.Member {
		prac := group.Member[i].FindPractitionerExtension()
		if prac != nil {
			group.Member[i].Extension = []model.Extension{*prac}
		} else {
			group.Member[i].Extension = make([]model.Extension, 0)
		}
	}
	return json.Marshal(group)
}

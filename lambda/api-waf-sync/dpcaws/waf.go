package dpcaws

import (
	"bytes"
	"fmt"
	"os"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/wafv2"
)

func UpdateIPSetInWAF(ipSetName string, ipAddresses []string) {
    params := {"scope": "CLOUDFRONT"}
    ipSetList, err := wafv2.ListIpSets(params)
    if err != nil {
		return fmt.Errorf("failed to fetch ip address sets, %v", err), params
    }

    for _, ipSet := range ipSetList {
        if ipSet["Name"] == ipSetName {
            params["Id"] = ipSet["Id"]
            break;
        }
    }
    params["Name"] = ipSetName
    ipSet, err := wafv2.GetIpSet(params)
    if err != nil {
        return fmt.Errorf("failed to get expected ip address set, %v", err), params
    }

    params["LockToken"] = ipSet["LockToken"]
    params["Addresses"] = ipAddresses
    _, err := wafv2.UpdateIPSet(params)
    if err != nil {
    	return fmt.Errorf("failed to update ip address set, %v", err), params
    }

    return "", params
}

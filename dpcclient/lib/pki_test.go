package lib

import (
	"crypto/rsa"
	"os"
	"reflect"
	"strings"
	"testing"
)

func TestGenRSAKeyPair(t *testing.T) {
	private, public, err := GenRSAKeyPair()
	if err != nil {
		t.Errorf("GenRSAKeyPair() should not fail; error = %v", err)
	}
	if private == nil || public == nil {
		t.Errorf("No keys generated? private (%v), public (%v)", private, public)
	}
	if !(private.Size()*8 == 4096) {
		t.Errorf("GenRSAKeyPair() key is too small? size in bytes is %d", private.Size())
	}
	public2 := &private.PublicKey
	if !reflect.DeepEqual(public2, public) {
		t.Errorf("KeysFrom() got1 = %v, want %v", public2, public)
	}
}

func TestKeyFromPEM(t *testing.T) {
	bytes, _ := ReadSmallFile("testdata/test-key-private.pem")
	eccBytes, _ := ReadSmallFile("testdata/ecc-key.pem")
	type args struct {
		privateKey []byte
	}

	tests := []struct {
		name    string
		args    args
		wantErr bool
		errHas  string
	}{
		{"rsa bytes", args{bytes}, false, ""},
		{"fake bytes", args{[]byte("I am not any kind of key")}, true, "unable to decode private key"},
		{"ecc key bytes", args{eccBytes}, true, "unable to parse RSA private key"},
		{"nil arg", args{[]byte{}}, true, "empty or nil bytes"},
		{"empty slice", args{make([]byte, 0)}, true, "empty or nil bytes"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, _, err := KeyFromPEM(tt.args.privateKey)
			if err != nil {
				t.Logf("KeyFromPEM() error = %v", err)
				if tt.wantErr {
					if !strings.Contains(err.Error(), tt.errHas) {
						t.Errorf("KeyFromPEM() error should contain %s", tt.errHas)
					}
				}
			}
		})
	}
}

func TestSaveDPCKeyPair(t *testing.T) {
	private, public, err := GenRSAKeyPair()
	if err != nil {
		t.Error("Error generating a key pair for test; ", err)
	}
	pemBytes, _ := ReadSmallFile("testdata/small-key.pem")
	privateSmall, publicSmall, err2 := KeyFromPEM(pemBytes)
	if err2 != nil {
		t.Error("Error reading keys from testdata/small-key.pem", err2)
	}
	type args struct {
		prefix  string
		private *rsa.PrivateKey
		public  *rsa.PublicKey
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"valid key", args{"dpcSandboxKey", private, public}, false},
		{"nil key", args{"badKey", nil, nil}, true},
		{"rsa key but too small", args{"small-key", privateSmall, publicSmall}, true},
		{"valid key with invalid prefix", args{"nosuchpath/noPathToHere", private, public}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := SaveDPCKeyPair(tt.args.prefix, tt.args.private, tt.args.public); (err != nil) != tt.wantErr {
				t.Errorf("SaveDPCKeyPair() error = %v, wantErr %v", err, tt.wantErr)
			} else {
				t.Logf("SaveDPCKeyPair() error = %v", err)
			}
		})
	}
	_ = os.Remove("dpcSandboxKey-private.pem")
	_ = os.Remove("dpcSandboxKey-public.pem")
}

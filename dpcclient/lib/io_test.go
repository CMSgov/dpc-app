package lib

import (
	"bytes"
	"io/ioutil"
	"os"
	"reflect"
	"testing"
)

func Test_readSmallFile(t *testing.T) {
	type args struct {
		path string
	}
	tests := []struct {
		name    string
		args    args
		want    []byte
		wantErr bool
	}{
		{"file exists", args{"testdata/read-this"}, []byte("hello dpc"), false},
		{"good path, bad file name", args{"no-such-file"}, nil, true},
		{"bad path", args{"no-such-dir/no-such-file"}, nil, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := ReadSmallFile(tt.args.path)
			if (err != nil) != tt.wantErr {
				t.Errorf("readSmallFile() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("readSmallFile() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_writeSmallFile(t *testing.T) {
	type args struct {
		content []byte
		path    string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"good args", args{[]byte("I am the content"), "test-write-small-file"}, false},
		{"overwrite existing", args{[]byte("I overwrote the content"), "test-write-small-file"}, false},
		{"bad path", args{[]byte("I am the content"), "nosuchdir/test-write-small-file"}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := WriteSmallFile(tt.args.path, tt.args.content); (err != nil) != tt.wantErr {
				t.Errorf("writeSmallFile() error = %v, wantErr %v", err, tt.wantErr)
			}
			if !tt.wantErr {
				content, err := ioutil.ReadFile(tt.args.path)
				if err != nil {
					t.Errorf("can't read written file; %s", err.Error())
				}
				if !bytes.Equal(content, tt.args.content) {
					t.Errorf("wanted content (%s) did not match content read from written file (%s)", tt.args.content, content)
				}
			}
		})
	}
	// failure to clean up is not a test failure
	_ = os.Remove("test-write-small-file")
}

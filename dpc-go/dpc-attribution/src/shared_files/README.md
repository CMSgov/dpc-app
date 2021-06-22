# Shared Files

What's in here and why?

* decrypted
  * git-ignored, decrypted files sourced from the `encrypted/` directory
* encrypted
  * sensitive configuration values that should be encrypted in the repository
    * `bfd-dev-test-cert.pem`: a certificate identifying and authorizing this application to retrieve claims data; encrypted
    * `bfd-dev-test-key.pem`: the private key for the above certificate; encrypted
    * `bfd-dev-test-ca-file.crt`: the trusted certificate from bfd

## Sensitive Configuration Files

The files committed in the `shared_files/encrypted` directory hold secret information, and are encrypted with [Ansible Vault](https://docs.ansible.com/ansible/2.4/vault.html).

### Setup

#### Password

- See a team member for the Ansible Vault password
- Create a file named `.vault_password` in the root directory of the repository
- Place the Ansible Vault password in this file

### Managing encrypted files

- Temporarily decrypt files by running `make bfd-certs` from the repository root directory or by using the following command for each file:

```
./ops/scripts/secrets --decrypt <filename>
```

- If not using the `make bfd-certs` command, after files are decrypted, copy the files in this directory to the sibling directory `shared_files/decrypted`
- Encrypt changed files with:

```
./ops/scripts/secrets --encrypt <filename>
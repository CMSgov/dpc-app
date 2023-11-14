## Managing Encrypted Files

###### [`^`](#table-of-contents)

The files committed in the `ops/config/encrypted` directory hold secret information, and are encrypted with [Ansible Vault](https://docs.ansible.com/ansible/2.4/vault.html).

Before building the app or running any tests, the decrypted secrets must be available as environment variables.

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in the root directory. Contact another team member to gain access to the vault password.

Run the following to decrypt the encrypted files:

```sh
make secure-envs
```

If decrypted successfully, you will see the decrypted data in new files under `/ops/config/decrypted` with the same names as the corresponding encrypted files.

### Re-encrypting files

To re-encrypt files after updating them, you can run the following command:

```
./ops/scripts/secrets --encrypt <filename>
```

Note that this will always generate a unique hash, even if you didn't change the file.

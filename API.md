This page describes the process of interacting with the Proteome Commons Tranche repository programmatically.

  1. Download the stable build of the core JAR.
  1. Before interacting with the repository, execute the following command: `org.proteomecommons.tranche.ProteomeCommonsTrancheConfig.load();`
  1. Use one of the following code templates to interact with the repository.

## Downloading ##

The following code is sufficient to download a public data set (not passphrase-protected):

```
GetFileTool gft = new GetFileTool();
gft.setHash(BigHash.createHashFromString("hash"));
gft.setSaveFile(new File("absolute path"));
gft.getDirectory();
```

The `getDirectory()` method can be replaced with the `getFile()` method. The methods work as they are named; which one should be used requires previous knowledge of the data set.

The following line of code is required before the `getFile()` or `getDirectory()` method to download an encrypted data set:

```
gft.setPassphrase("passphrase");
```

## Uploading ##

The following code is sufficient to upload a public data set (not passphrase-protected):

```
AddFileTool aft = new AddFileTool();
aft.setTitle("title");
aft.setDescription("description");
aft.setLicense(License.CC0);
UserZipFile user = UserZipFileUtil.getUserZipFile("proteome commons user name", "proteome commons password");
aft.setUserCertificate(user.getCertificate());
aft.setUserPrivateKey(user.getPrivateKey());
aft.setFile(new File("absolute path of file/directory to upload"));
aft.execute();
```

Replace `License.CC0` with `new License("title", "brief description", "complete description", false)` if you want to use a custom license.

The following line of code is required before the `execute()` method to upload an encrypted data set:

```
aft.setPassphrase("passphrase");
```
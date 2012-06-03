/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.proteomecommons.tranche.scripts;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import junit.framework.TestCase;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.tranche.FileEncoding;
import org.tranche.commons.RandomUtil;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.DataDirectoryConfiguration;
import org.tranche.flatfile.ServerConfiguration;
import org.tranche.hash.Base64;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.meta.MetaData;
import org.tranche.meta.MetaDataAnnotation;
import org.tranche.meta.MetaDataUtil;
import org.tranche.security.SecurityUtil;
import org.tranche.security.Signature;
import org.tranche.time.TimeUtil;
import org.tranche.users.MakeUserZipFileTool;
import org.tranche.users.User;
import org.tranche.users.UserZipFile;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 *
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class DevUtil {

    // make up a dev user
    private static UserZipFile devUser = null;
    private static UserZipFile fftsUser = null;
    private static UserZipFile routingTrancheServerUser = null;
    private static final String trancheLoopbackAddr = "tranche://127.0.0.1";
    private static final int defaultTestPort = 1500; // Mac-friendly port
    public static final HashSet<User> DEV_USER_SET = new HashSet<User>();


    static {
        try {
            DEV_USER_SET.add(DevUtil.getDevUser());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param md5Check
     * @param md5
     */
    public static final void assertBytesMatch(final byte[] md5Check, final byte[] md5) {
        TestCase.assertEquals("Hashes should be the same length", md5.length, md5Check.length);
        for (int i = 0; i < md5.length; i++) {
            TestCase.assertEquals("Bytes should match.", md5[i], md5Check[i]);
        }
    }

    /**
     *
     * @param md5Check
     * @param md5
     */
    public static final void assertBytesMatch(final byte[] md5Check, final ByteBuffer md5) {
        TestCase.assertEquals("Hashes should be the same length", md5.limit() - md5.position(), md5Check.length);
        for (int i = 0; i < md5Check.length; i++) {
            TestCase.assertEquals("Bytes should match.", md5.get(), md5Check[i]);
        }
    }

    /**
     *
     * @param md5Check
     * @param md5
     */
    public static final void assertBytesMatch(final ByteBuffer md5Check, final ByteBuffer md5) {
        TestCase.assertEquals("Hashes should be the same length", md5.limit() - md5.position(), md5Check.limit() - md5Check.position());
        for (int i = 0; md5Check.position() < md5Check.limit(); i++) {
            TestCase.assertEquals("Bytes should match.", md5.get(), md5Check.get());
        }
    }

    /**
     * <p>Returns a user that has full permissions and a made up certificate.</p>
     */
    public static synchronized UserZipFile getDevUser() throws Exception {
        // if the dev user is null, make it up
        if (devUser == null) {
            devUser = makeNewUser(RandomUtil.getString(10), User.ALL_PRIVILEGES);
        }
        return devUser;
    }

    /**
     * <p>Returns a user that can write. Intended as authorized user for ffts (so can replicate to other servers).</p>
     * @return
     * @throws java.lang.Exception
     */
    public static synchronized UserZipFile getFFTSUser() throws Exception {
        // if the dev user is null, make it up
        if (fftsUser == null) {
            fftsUser = makeNewUser(RandomUtil.getString(10), User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA);
        }
        return fftsUser;
    }

    /**
     * <p>Returns a user that can write/delete. Intended as authorized user for routing server (can can set and delete chunks from managed data servers).</p>
     * <p>There is one short-coming for this approach: whereas on a production network, the routing server's cert would only apply to managed data servers, here it will work for all servers. The tests will just have to be designed to deal with this weakness.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static synchronized UserZipFile getRoutingTrancheServerUser() throws Exception {
        // if the dev user is null, make it up
        if (routingTrancheServerUser == null) {
            routingTrancheServerUser = makeNewUser(RandomUtil.getString(10), User.CAN_GET_CONFIGURATION | User.CAN_SET_DATA | User.CAN_SET_META_DATA | User.CAN_DELETE_DATA | User.CAN_DELETE_META_DATA);
        }
        return routingTrancheServerUser;
    }

    public static UserZipFile makeNewUser(String name, int flags) throws Exception {
        // make up a random user
        MakeUserZipFileTool make = new MakeUserZipFileTool();
        // create an appropriate temp file
        File temp = TempFileUtil.createTemporaryFile();
        try {
            make.setName(name);
            make.setPassphrase("");
            make.setValidDays(2);
            make.setSaveFile(temp);
            // create the user
            UserZipFile user = make.makeCertificate();
            user.setFlags(flags);
            return user;
        } finally {
            // clean up the temp
            IOUtil.safeDelete(temp);
        }
    }

    public static UserZipFile makeNewUserWithRandomFlags() throws Exception {
        UserZipFile user = makeNewUser(RandomUtil.getString(10), User.ALL_PRIVILEGES);
        int userFlags = User.VERSION_ONE;
        if (RandomUtil.getBoolean()) {
            userFlags = userFlags | User.CAN_DELETE_DATA;
        }
        if (RandomUtil.getBoolean()) {
            userFlags = userFlags | User.CAN_DELETE_META_DATA;
        }
        if (RandomUtil.getBoolean()) {
            userFlags = userFlags | User.CAN_GET_CONFIGURATION;
        }
        if (RandomUtil.getBoolean()) {
            userFlags = userFlags | User.CAN_SET_CONFIGURATION;
        }
        if (RandomUtil.getBoolean()) {
            userFlags = userFlags | User.CAN_SET_DATA;
        }
        if (RandomUtil.getBoolean()) {
            userFlags = userFlags | User.CAN_SET_META_DATA;
        }
        user.setFlags(userFlags);
        return user;
    }

    public static DataDirectoryConfiguration makeNewDataDirectoryConfiguration() {
        File dir = null;
        try {
            dir = TempFileUtil.createTemporaryDirectory();
            DataDirectoryConfiguration ddc = new DataDirectoryConfiguration(dir.getAbsolutePath(), RandomUtil.getLong());
            return ddc;
        } finally {
            IOUtil.recursiveDeleteWithWarning(dir);
        }
    }

    public static ServerConfiguration makeNewServerConfiguration(int randomHostNameLength) throws Exception {
        ServerConfiguration sc = null;
        switch (RandomUtil.getInt(2)) {
            case 0:
                return new ServerConfiguration(RandomUtil.getString(10), RandomUtil.getInt(1499), RandomUtil.getString(randomHostNameLength));
            case 1:
                return new ServerConfiguration(RandomUtil.getString(10), RandomUtil.getInt(1499), RandomUtil.getString(randomHostNameLength));
            case 2:
                return new ServerConfiguration(RandomUtil.getString(10), RandomUtil.getInt(1499), RandomUtil.getString(randomHostNameLength));
            default:
                throw new Exception("Problem creating random server configuration.");
        }
    }

    public static HashSpan makeRandomHashSpan() throws Exception {
        return new HashSpan(getRandomBigHash(RandomUtil.getInt(DataBlockUtil.getMaxChunkSize())), getRandomBigHash(RandomUtil.getInt(DataBlockUtil.getMaxChunkSize())));
    }

    public static Set<HashSpan> createRandomHashSpanSet(int maxSize) throws Exception {
        Set<HashSpan> hashSpans = new HashSet<HashSpan>();
        for (int i = 0; i < RandomUtil.getInt(maxSize); i++) {
            hashSpans.add(DevUtil.makeRandomHashSpan());
        }
        return hashSpans;
    }

    public static X509Certificate getDevAuthority() throws Exception {
        return getDevUser().getCertificate();
    }

    public static PrivateKey getDevPrivateKey() throws Exception {
        return getDevUser().getPrivateKey();
    }

    /**
     * <p>Returns a user that can write. Intended as authorized user for ffts (so can replicate to other servers).</p>
     * @return
     * @throws java.lang.Exception
     */
    public static X509Certificate getFFTSAuthority() throws Exception {
        return getFFTSUser().getCertificate();
    }

    /**
     * <p>Returns a user that can write. Intended as authorized user for ffts (so can replicate to other servers).</p>
     * @return
     * @throws java.lang.Exception
     */
    public static PrivateKey getFFTSPrivateKey() throws Exception {
        return getFFTSUser().getPrivateKey();
    }

    /**
     * <p>Returns a user that can write/delete. Intended as authorized user for routing server (can can set and delete chunks from managed data servers).</p>
     * <p>There is one short-coming for this approach: whereas on a production network, the routing server's cert would only apply to managed data servers, here it will work for all servers. The tests will just have to be designed to deal with this weakness.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static X509Certificate getRoutingTrancheServerAuthority() throws Exception {
        return getRoutingTrancheServerUser().getCertificate();
    }

    /**
     * <p>Returns a user that can write/delete. Intended as authorized user for routing server (can can set and delete chunks from managed data servers).</p>
     * <p>There is one short-coming for this approach: whereas on a production network, the routing server's cert would only apply to managed data servers, here it will work for all servers. The tests will just have to be designed to deal with this weakness.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static PrivateKey getRoutingTrancheServerPrivateKey() throws Exception {
        return getRoutingTrancheServerUser().getPrivateKey();
    }

    /**
     * Returns the default port.
     */
    public static int getDefaultTestPort() {
        return defaultTestPort;
    }

    /**
     * Creates a random BigHash. Uses 1MB chunk of random data to generate.
     * @return
     */
    public static BigHash getRandomBigHash() {
        return getRandomBigHash(1024 * 1024);
    }

    /**
     * <p>Returns a bogus big hash representing data of a particular size.</p>
     */
    public static BigHash getRandomBigHash(int dataSize) {
        byte[] data = new byte[dataSize];
        RandomUtil.getBytes(data);
        return new BigHash(data);
    }

    /**
     * <p>Creates a random BigHash that does NOT start with supplied prefix.</p>
     * <p>Useful when generating hashes that should not fall in a particular data block, for example.</p>
     * @param prefix String that should not be the prefix for the generated BigHash.
     * @return BigHash that does not start with supplied prefix.
     * @throws java.lang.Exception Failed to create a BigHash that does not start with prefix with 10000 attempts.
     */
    public static BigHash getRandomBigHashNotStartWith(String prefix) throws Exception {
        final int maxCutoff = 10000;
        int attempt = 0;
        while (attempt < maxCutoff) {
            BigHash h = getRandomBigHash();
            if (!h.toString().startsWith(prefix)) {
                return h;
            }
            attempt++;
        }

        throw new Exception("Failed to create a BigHash that does not start with " + prefix + ", tried " + attempt + " times.");
    }

    /**
     * <p>Create a random BigHash with a prefix. This method takes the argument and adds enough characters to make it a valid BigHash.</p>
     * <p>Useful if creating a test that requires BigHashes that share a similar prefix, such as a DataBlock test.</p>
     * @param prefix A string between 0 and n characters, where n must be less than base 64 string length.
     * @return A BigHash containing the prefix
     */
    public static BigHash getRandomBigHashStartsWith(String prefix) {

        // Prefix must be a multiple of four. Otherwise, bytes don't align to same
        // ASCI characters.
        final int modulus = prefix.length() % 4;
        if (modulus != 0) {
            int toAdd = 4 - modulus;
            prefix = prefix + RandomUtil.getString(toAdd);
        }

        byte[] prefixBytes = Base64.decode(prefix);

        if (prefixBytes.length > BigHash.HASH_LENGTH) {
            throw new RuntimeException("Found prefix with " + prefixBytes.length + " bytes, must be less than " + BigHash.HASH_LENGTH);
        }

        byte[] randomBytes = new byte[BigHash.HASH_LENGTH - prefixBytes.length];
        RandomUtil.getBytes(randomBytes);

        byte[] bigHashBytes = new byte[BigHash.HASH_LENGTH];

        System.arraycopy(prefixBytes, 0, bigHashBytes, 0, prefixBytes.length);
        System.arraycopy(randomBytes, 0, bigHashBytes, prefixBytes.length, randomBytes.length);

        return BigHash.createFromBytes(bigHashBytes);
    }
    static final String signatureAlgorithm = safeGetSignatureAlg();

    private static String safeGetSignatureAlg() {
        try {
            return SecurityUtil.getSignatureAlgorithm(DevUtil.getDevPrivateKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>This signature is good for nothing.</p>
     */
    public static Signature getBogusSignature() throws Exception {
        byte[] bytes = new byte[32];
        RandomUtil.getBytes(bytes);
        byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), DevUtil.getDevPrivateKey(), signatureAlgorithm);

        return new Signature(sig, signatureAlgorithm, DevUtil.getDevAuthority());
    }

    /**
     * Returns whether the specified port is available
     */
    public static boolean isPortAvailable(int port) {
        try {
            ServerSocket srv = new ServerSocket(port);
            srv.close();
            srv = null;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the tranche loopback address (e.g., tranche://127.0.0.1:...) associated with default port.
     */
    public static String getDefaultAddr() {
        return trancheLoopbackAddr + ":" + defaultTestPort;
    }

    /**
     * Escapes bad characters for temp directories. Useful if URLs appear. Please add character replacements as needed.
     */
    public static String escapeBadCharacters(String fileOrDirectoryName) {
        return fileOrDirectoryName.replace(":", "");
    }

    /**
     * <p>Create a new user. Use if you need unique users for tests.</p>
     * @param username Simply a string
     * @param password Simply a string
     * @param filename The filename for user's zip file
     * @param isAdmin True if should have admin privileges on test servers
     */
    public static UserZipFile createUser(String username, String password, String filename, boolean isAdmin, boolean isExpired) throws Exception {

        MakeUserZipFileTool maker = new MakeUserZipFileTool();
        maker.setName(username);
        maker.setPassphrase(password);
        maker.setSaveFile(new File(filename));
        if (isExpired) {
            maker.setValidDays(-1);
        } else {
            maker.setValidDays(1);
        }
        UserZipFile zip = (UserZipFile) maker.makeCertificate();

        // Set user permissions as admin (server needs user registered or it will
        // throw a SecurityException on attempted file upload)
        if (isAdmin) {
            zip.setFlags(SecurityUtil.getAdmin().getFlags());
        }

        return zip;
    }

    /**
     * <p>Create a new user signed by another user. Use if you need unique users for tests and need to test conditions where some users are signed.</p>
     * @param username Simply a string
     * @param password Simply a string
     * @param filename The filename for user's zip file
     * @param signerCertificate The public certificate of user signing new user
     * @param signerPrivateKey The private key of user signing new user
     * @param isAdmin True if should have admin privileges on test servers
     */
    public static UserZipFile createSignedUser(String username, String password, String filename, X509Certificate signerCertificate, PrivateKey signerPrivateKey, boolean isAdmin, boolean isExpired) throws Exception {

        MakeUserZipFileTool maker = new MakeUserZipFileTool();
        maker.setName(username);
        maker.setPassphrase(password);
        maker.setSaveFile(new File(filename));
        maker.setSignerCertificate(signerCertificate);
        maker.setSignerPrivateKey(signerPrivateKey);
        if (isExpired) {
            maker.setValidDays(-1);
        } else {
            maker.setValidDays(1);
        }
        UserZipFile zip = (UserZipFile) maker.makeCertificate();

        // Set user permissions as admin (server needs user registered or it will
        // throw a SecurityException on attempted file upload)
        if (isAdmin) {
            zip.setFlags((new SecurityUtil()).getAdmin().getFlags());
        }

        return zip;
    }

    /**
     * Returns a print stream that does nothing. Useful to prevent output from becoming unruly.
     */
    public static PrintStream getNullPrintStream() {
        return new NullPrintStream();
    }

    /**
     * <p>Creates a test project with many small files (b/w 16KB and 64KB).</p>
     * @param numFilesInProject Number of desired files in project
     * @return A directory containing many small files.
     */
    public static File createTestProjectWithSmallFiles(final int numFilesInProject) throws Exception {
        return createTestProject(numFilesInProject, 1024 * 16, 1024 * 64);
    }

    /**
     * <p>Creates a test project with large files (e.g., between 16KB and 4MB.) Not intended for recursive testing!</p>
     * @param numFilesInProject Number of desired files in project
     * @return A directory containing many small files.
     */
    public static File createTestProjectWithLargeFiles(final int numFilesInProject) throws Exception {
        return createTestProject(numFilesInProject, 1024 * 16, 4 * 1024 * 1024);
    }

    /**
     *
     */
    public static File createTestProjectWithEvenSizedFiles(int numFiles, int sizeOfFiles) throws Exception {
        return createTestProject(numFiles, sizeOfFiles, sizeOfFiles);
    }

    /**
     * <p>Creates a test project with specified number of files of random size between user specified min and max.</p>
     * @param numFilesInProjects Number of files for project.
     * @param minFileSize The minimum size of a file.
     * @param maxFileSize The maximum size of a file.
     * @return File directory that contains test project.
     */
    public static File createTestProject(final int numFilesInProject, final int minFileSize, final int maxFileSize) throws Exception {
        File directory = TempFileUtil.createTemporaryDirectory();
        File nextFile;
        OutputStream out = null;
        for (int i = 0; i < numFilesInProject; i++) {
            nextFile = new File(directory, i + ".tmp");
            try {
                out = new FileOutputStream(nextFile);
                // Want files that are at least 16K
                int size = RandomUtil.getInt(maxFileSize - minFileSize + 1) + minFileSize;

                byte[] bytes = new byte[size];

                RandomUtil.getBytes(bytes);
                out.write(bytes);
            } finally {
                IOUtil.safeClose(out);
            }
        }
        return directory;
    }

    /**
     * 
     * @param min
     * @param max
     * @return
     * @throws java.lang.Exception
     */
    public static File createTestFile(int min, int max) throws Exception {
        File f = TempFileUtil.createTemporaryFile();
        createTestFile(f, min, max);
        return f;
    }

    /**
     * <p>Create a file with random data of a specific size.</p>
     * @param f Temporary file.
     * @param size Desired number of bytes.
     * @throws java.lang.Exception
     */
    public static void createTestFile(File f, int size) throws Exception {
        createTestFile(f, size, size);
    }

    /**
     * <p>Create a file with random data of a specific range of possible sizes.</p>
     * @param f Temporary file.
     * @param min Desired minimum number of bytes.
     * @param max Desired maximum number of bytes.
     * @throws java.lang.Exception
     */
    public static void createTestFile(File f, int min, int max) throws Exception {
        // Make a single file to upload between 64KB and 5MB
        final int size = RandomUtil.getInt(max - min) + min;
        final int batchSize = 64 * 1024;

        if (f == null || !f.exists()) {
            f = TempFileUtil.createTemporaryFile();
        }

        // Write out a little at a time
        OutputStream os = null;

        try {
            os = new BufferedOutputStream(new FileOutputStream(f));
            for (int i = 0; i < size;) {
                int bytesToWrite = batchSize;
                if (bytesToWrite > size - i) {
                    bytesToWrite = size - i;
                }
                byte[] bytes = new byte[bytesToWrite];
                RandomUtil.getBytes(bytes);
                os.write(bytes);
                i += bytesToWrite;
            }
        } finally {
            IOUtil.safeClose(os);
        }
    }

    /**
     * <p>Creates a valid MetaData object with some random properties.</p>
     * @return
     */
    public static MetaData createRandomMetaData() throws Exception {
        return createRandomMetaData(1);
    }

    public static MetaData createRandomMetaData(int uploaders) throws Exception {
        return createRandomMetaData(uploaders, false);
    }

    public static MetaData createRandomMetaData(int uploaders, boolean isProjectFile) throws Exception {
        return createRandomMetaData(uploaders, isProjectFile, false);
    }

    public static MetaData createRandomMetaData(int uploaders, boolean isProjectFile, boolean isEncrypt) throws Exception {
        // make some random data less than one MB
        byte[] data = new byte[RandomUtil.getInt(DataBlockUtil.getMaxChunkSize())];
        RandomUtil.getBytes(data);
        ByteArrayInputStream bais = null;

        // create the meta data
        MetaData md = new MetaData();

        md.setIsProjectFile(isProjectFile);

        for (int i = 0; i < uploaders; i++) {
            // uploader
            Signature signature = null;
            ArrayList<FileEncoding> encodings = new ArrayList<FileEncoding>();
            Map<String, String> properties = new HashMap<String, String>();
            ArrayList<MetaDataAnnotation> annotations = new ArrayList<MetaDataAnnotation>();

            // make the signature
            try {
                bais = new ByteArrayInputStream(data);
                byte[] sigBytes = SecurityUtil.sign(bais, getDevPrivateKey());
                String algorithm = SecurityUtil.getSignatureAlgorithm(getDevPrivateKey());
                signature = new Signature(sigBytes, algorithm, DevUtil.getDevAuthority());
            } finally {
                IOUtil.safeClose(bais);
            }

            // make encoding
            encodings.add(new FileEncoding(FileEncoding.NONE, DevUtil.getRandomBigHash()));
            if (isEncrypt) {
                encodings.add(new FileEncoding(FileEncoding.AES, DevUtil.getRandomBigHash()));
            }

            // set the basic properties
            properties.put(MetaData.PROP_NAME, RandomUtil.getString(10));
            properties.put(MetaData.PROP_TIMESTAMP_UPLOADED, String.valueOf(TimeUtil.getTrancheTimestamp()));

            // add the uploader
            md.addUploader(signature, encodings, properties, annotations);

            // add parts
            ArrayList<BigHash> parts = new ArrayList<BigHash>();
            for (int j = 0; j < RandomUtil.getInt(50); j++) {
                parts.add(DevUtil.getRandomBigHash());
            }
            md.setParts(parts);
        }
        return md;
    }

    /**
     * <p>Create a test meta data chunk that is stuck to the server with the given host name.</p>
     * @param host
     * @return
     * @throws java.lang.Exception
     */
    public static MetaData createRandomStickyMetaData(String host) throws Exception {
        MetaData md = createRandomMetaData();
        md.addStickyServer(host);
        return md;
    }

    /**
     * <p>Create a test meta data chunk that is stuck to the server with the given host name.</p>
     * @param host
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] createRandomStickyMetaDataChunk(String host) throws Exception {
        MetaData md = createRandomStickyMetaData(host);
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            MetaDataUtil.write(md, baos);
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     * <p>Creates a valid MetaData object with some random properties and a size greater than 1MB.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static MetaData createRandomBigMetaData() throws Exception {
        MetaData md = createRandomMetaData();

        ArrayList<BigHash> parts = new ArrayList<BigHash>();
        // add the same hash over and over with slight changes - takes too long to make a new one every time
        BigHash hash = getRandomBigHash(RandomUtil.getInt(DataBlockUtil.getMaxChunkSize() / 2));
        // add hashes
        for (int i = 0; i < 15000; i++) {
            parts.add(hash);
            hash.getNext();
        }
        md.setParts(parts);

        return md;
    }

    /**
     * <p>Creates a valid MetaData object with some random properties and a size greater than 1MB.</p>
     * @return byte[]
     */
    public static byte[] createRandomBigMetaDataChunk() throws Exception {
        MetaData md = createRandomBigMetaData();
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            MetaDataUtil.write(md, baos);
            return baos.toByteArray();
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     * <p>Creates a valid MetaData object with some random properties. Then converts to a byte array.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] createRandomMetaDataChunk() throws Exception {
        return createRandomMetaDataChunk(1);
    }

    /**
     * 
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] createRandomMetaDataChunk(int uploaders) throws Exception {
        return createRandomMetaDataChunk(uploaders, false);
    }

    public static byte[] createRandomMetaDataChunk(int uploaders, boolean isProjectFile) throws Exception {
        return createRandomMetaDataChunk(uploaders, isProjectFile, false);
    }
    
    public static byte[] createRandomMetaDataChunk(int uploaders, boolean isProjectFile, boolean isEncrypt) throws Exception {

        MetaData md = createRandomMetaData(uploaders, isProjectFile, isEncrypt);
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            MetaDataUtil.write(md, baos);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } finally {
            IOUtil.safeClose(baos);
        }
    }

    /**
     * <p>Create a chunk of random data exactly 1MB in size.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] createRandomDataChunk1MB() throws Exception {
        return createRandomDataChunk(1024 * 1024);
    }

    /**
     * <p>Create a chunk with random data between size of 1KB and 1MB.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] createRandomDataChunkVariableSize() throws Exception {
        int size = RandomUtil.getInt(1024 * 1024 - 1024) + 1024;
        if (size < 1024 || size > 1024 * 1024) {
            throw new Exception("Size should not be less than 1KB or more than 1MB, created " + size);
        }
        return createRandomDataChunk(size);
    }

    /**
     * <p>Create a random chunk of a certain size.</p>
     * @param size
     * @return
     * @throws java.lang.Exception
     */
    public static byte[] createRandomDataChunk(int size) throws Exception {
        byte[] data = new byte[size];
        RandomUtil.getBytes(data);
        return data;
    }

    /**
     * <p>Create a data chunk that starts w/ a hash prefix of a specific size.</p>
     * <p>Warning: this can take a long time! To keep times down:</p>
     * <ul>
     *   <li>Keeps sizes low. Uses sizes under 100KB more likely to run under 10 seconds!</li>
     *   <li>Use small prefixes. Should only use prefixes of sizes less than or equal to 2. (Every additional prefix makes method take 16x longer!)</li>
     * </ul>
     * @param hashPrefix A string of length 1 and 2.
     * @return Random data array. It's BigHash has a base 64 String that starts with supplied string
     * @throws java.lang.Exception If prefix is greater than length 2. This is intended to keep method relatively fast.
     */
    public static byte[] createRandomDataChunkStartsWith(String hashPrefix) throws Exception {
        return createRandomDataChunk(hashPrefix, true);
    }

    /**
     * <p>Create a data chunk that does not start w/ a hash prefix of a specific size.</p>
     * <p>Warning: this can take a little time! (Much less than createRandomDataChunkStartsWith, though.) To keep times down:</p>
     * <ul>
     *   <li>Keeps sizes low. Uses sizes under 100KB more likely to run under 10 seconds!</li>
     *   <li>Use small prefixes. Should only use prefixes of sizes less than or equal to 2. (Every additional prefix makes method take 16x longer!)</li>
     * </ul>
     * @param hashPrefix A string of length 1 and 2.
     * @return Random data array. It's BigHash has a base 64 String that starts with supplied string
     * @throws java.lang.Exception If prefix is greater than length 2. This is intended to keep method relatively fast.
     */
    public static byte[] createRandomDataChunkDoesNotStartWith(String hashPrefix) throws Exception {
        return createRandomDataChunk(hashPrefix, false);
    }

    /**
     * <p>Create a data chunk that starts w/ a hash prefix of a specific size.</p>
     * <p>Warning: this can take a long time! To keep times down:</p>
     * <ul>
     *   <li>Keeps sizes low. Uses sizes under 100KB more likely to run under 10 seconds!</li>
     *   <li>Use small prefixes. Should only use prefixes of sizes less than or equal to 2. (Every additional prefix makes method take 16x longer!)</li>
     * </ul>
     * @param hashPrefix A string of length 1 and 2.
     * @param shouldStartWithPrefix True if want chunk's hash to start, false if it shouldn't.
     * @return Random data array. It's BigHash has a base 64 String that starts with supplied string
     * @throws java.lang.Exception If prefix is greater than length 2. This is intended to keep method relatively fast.
     */
    private static byte[] createRandomDataChunk(String hashPrefix, boolean shouldStartWithPrefix) throws Exception {

        /**
         * The following sized prefixes will take an average number of tries to get right chunk:
         * - 1 char:  16
         * - 2 chars: 256
         * - 3 chars: 4096
         * - 4 chars: 65,536
         */
        final int prefixCutoff = 2;
        if (hashPrefix.length() > prefixCutoff) {
            throw new Exception("The maximum size of allowed prefix to createRandomDataChunk is " + prefixCutoff + ", but argument is of size " + hashPrefix.length() + ". Please make a shorter prefix. The algorithm is very slow at larges sizes.");
        }

        // Between 1024 bytes (inclusive) and 25KB (exclusive)
        final int size = RandomUtil.getInt(25 * 1024 - 1024) + 1024;

        byte[] data = createRandomDataChunk(size);

        // Don't let run on forever.
        int cutoff = 100000;
        int count = 0;

        /**
         * Used to tweak algorithm speeds. I've tested, and the option selected below seems to be fastest.
         */
        boolean isUseFreshByteBatch = true;

        // Do we have what we want yet?
        boolean matches = false;
        String h = new BigHash(data).toString();
        if (h.startsWith(hashPrefix) && shouldStartWithPrefix) {
            matches = true;
        } else if (!h.startsWith(hashPrefix) && !shouldStartWithPrefix) {
            matches = true;
        }

        // Iterate until we match or exceed maximum number of attempts
        while (!matches && count < cutoff) {

            if (isUseFreshByteBatch) {
                data = createRandomDataChunk(size);
            } else {
                // Change a byte randomly inside the chunk. Will affect hash.
                int byteIndex = RandomUtil.getInt(size);
                byte prevByte = data[byteIndex];
                byte nextByte = RandomUtil.getByte();

                // Don't let generating new byte run forever, either.
                int byteCount = 0;
                while (prevByte == nextByte && byteCount < cutoff) {
                    nextByte = RandomUtil.getByte();
                }

                data[byteIndex] = nextByte;
            }

            // Do we have what we want yet?
            h = new BigHash(data).toString();
            if (h.startsWith(hashPrefix) && shouldStartWithPrefix) {
                matches = true;
            } else if (!h.startsWith(hashPrefix) && !shouldStartWithPrefix) {
                matches = true;
            }

            count++;
        }

        if (count >= cutoff && !new BigHash(data).toString().startsWith(hashPrefix)) {
            throw new Exception("Tried " + count + " times to create a data chunk whose hash would start with " + hashPrefix + ", failed.");
        }

        return data;
    }

    /**
     * Creates a hash span with two random BigHash objects.
     * @return
     */
    public static HashSpan createRandomHashSpan() {
        BigHash hash1 = DevUtil.getRandomBigHash();
        BigHash hash2 = DevUtil.getRandomBigHash();

        HashSpan span = null;

        if (hash1.compareTo(hash2) < 0) {
            span = new HashSpan(hash1, hash2);
        } else {
            span = new HashSpan(hash2, hash1);
        }

        return span;
    }
    public static final int NUM_ARCHIVES = 3;
    public static final int NUM_FILES_IN_ARCHIVE = 5;

    private static File getZipFile() throws Exception {
        // the files to ZIP
        File[] zipFiles = new File[2];
        File zipFile;
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            for (int i = 0; i < zipFiles.length; i++) {
                zipFiles[i] = TempFileUtil.createTemporaryFile(".txt");
                Utils.makeTempFile(Utils.makeRandomData((int) (100 * Math.random())), zipFiles[i]);
            }
            // make a ZIP to test
            zipFile = TempFileUtil.createTempFileWithName("zipTest.zip");
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
            for (File file : zipFiles) {
                ZipEntry ze = new ZipEntry(file.getName());
                zos.putNextEntry(ze);
                zos.write(IOUtil.getBytes(file));
                zos.flush();
            }
        } finally {
            IOUtil.safeClose(zos);
            IOUtil.safeClose(fos);
            for (File tmp : zipFiles) {
                IOUtil.safeDelete(tmp);
            }
        }

        return zipFile;
    }

    private static File getTGZFile() throws Exception {
        // the files to TAR+GZIP
        File[] tarGzipFiles = new File[2];
        File tarGzipFile = null;
        FileOutputStream fos = null;
        GZIPOutputStream gos = null;
        TarOutputStream zos = null;

        try {
            for (int i = 0; i < tarGzipFiles.length; i++) {
                tarGzipFiles[i] = TempFileUtil.createTemporaryFile(".txt");
                Utils.makeTempFile(Utils.makeRandomData((int) (100 * Math.random())), tarGzipFiles[i]);
            }
            // make a TAR+GZIP to test
            tarGzipFile = TempFileUtil.createTempFileWithName("tgzTest.tgz");
            fos = new FileOutputStream(tarGzipFile);
            gos = new GZIPOutputStream(fos);
            zos = new TarOutputStream(gos);
            for (File file : tarGzipFiles) {
                TarEntry ze = new TarEntry(file.getName());
                byte[] bytes = IOUtil.getBytes(file);
                ze.setSize(bytes.length);
                zos.putNextEntry(ze);
                zos.write(bytes);
                zos.closeEntry();
            }
        } finally {
            IOUtil.safeClose(zos);
            IOUtil.safeClose(gos);
            IOUtil.safeClose(fos);
            for (File tmp : tarGzipFiles) {
                IOUtil.safeDelete(tmp);
            }
        }

        return tarGzipFile;
    }
}

class NullPrintStream extends PrintStream {

    public NullPrintStream() {
        // Output stream doesn't matter
        super(System.out);
    }

    public void print(boolean b) {
    }

    public void print(char c) {
    }

    public void print(char[] s) {
    }

    public void print(double d) {
    }

    public void print(float f) {
    }

    public void print(int i) {
    }

    public void print(long l) {
    }

    public void print(Object obj) {
    }

    public void print(String s) {
    }

    public void println() {
    }

    public void println(boolean x) {
    }

    public void println(char x) {
    }

    public void println(char[] x) {
    }

    public void println(double x) {
    }

    public void println(float x) {
    }

    public void println(int x) {
    }

    public void println(long x) {
    }

    public void println(Object x) {
    }

    public void println(String x) {
    }
}


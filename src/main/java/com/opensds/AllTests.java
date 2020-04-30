
package main.java.com.opensds;

import com.google.gson.Gson;
import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import main.java.com.opensds.jsonmodels.inputs.addbackend.AddBackendInputHolder;
import main.java.com.opensds.jsonmodels.inputs.addbackend.Backends;
import main.java.com.opensds.jsonmodels.inputs.addbackend.BackendsInputHolder;
import main.java.com.opensds.jsonmodels.inputs.createbucket.CreateBucketFileInput;
import main.java.com.opensds.jsonmodels.typesresponse.Type;
import main.java.com.opensds.jsonmodels.typesresponse.TypesHolder;
import main.java.com.opensds.utils.Constant;
import main.java.com.opensds.utils.TextUtils;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

// how to get POJO from any response JSON, use this site
// http://pojo.sodhanalibrary.com/

@ExtendWith(TestResultHTMLPrinter.class)
class AllTests {
    //
//    public static AuthTokenHolder getAuthTokenHolder() {
//        return mAuthTokenHolder;
//    }
//
    public TypesHolder getTypesHolder() {
        return mTypesHolder;
    }

    public static HttpHandler getHttpHandler() {
        return mHttpHandler;
    }

    private static main.java.com.opensds.jsonmodels.authtokensresponses.AuthTokenHolder mAuthTokenHolder = null;
    private static TypesHolder mTypesHolder = null;
    private static HttpHandler mHttpHandler = new HttpHandler();

    @org.junit.jupiter.api.BeforeAll
    static void setUp() {
//        tTokenHolder tokenHolder = getHttpHandler().loginAndGetToken();
//        ProjectsHolder projectsHolder = getHttpHandler().getProjects(tokenHolder.getResponseHeaderSubjectToken(),
//                tokenHolder.getToken().getUser().getId());
//        mAuthTokenHolder = getHttpHandler().getAuthToken(tokenHolder.getResponseHeaderSubjectToken());
//        mTypesHolder = getHttpHandler().getTypes(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                getAuthTokenHolder().getToken().getProject().getId());
        mTypesHolder = getHttpHandler().getTypes(null, "adminTenantId");

    }
//
//    @org.junit.jupiter.api.AfterEach
//    void tearDown() {
//    }

    @Test
    @Order(1)
    @DisplayName("Test creating bucket and backend on OPENSDS")
    public void testCreateBucketAndBackend() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);
                int code = getHttpHandler().addBackend(null,
                        "adminTenantId",
                        inputHolder);
                assertEquals(code, 200);

                // backend added, now create buckets
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_BUCKET_PATH);
                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
                        getAuthTokenHolder().getToken().getProject().getId());*/
                // create the bucket specified in each file
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));

                    // now create buckets
                    // TODO Bugs: Re-create same bucket return 500 response code.
                    int cbCode = getHttpHandler().createBucket(null,
                            bfi, bName, null, "adminTenantId");//signatureKey);
                    System.out.println(cbCode);
                    assertEquals(cbCode, 200);
                    Response listBucketResponse = getHttpHandler().getBuckets(null, "adminTenantId");
                    boolean bucketFound = false;
                    try {
                        bucketFound = getHttpHandler()
                                .doesListBucketResponseContainBucketByName(listBucketResponse.body().string(), bName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assertTrue(bucketFound);
                }
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test re-creating backend with same name on OPENSDS")
    public void testReCreateBackend() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            Gson gson = new Gson();
            // Re-create backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);
                int code = getHttpHandler().addBackend(null,
                        "adminTenantId",
                        inputHolder);
                assertEquals("Re-create backend with same name:Response code not matched:",code, 409);
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test re-creating bucket with same name on OPENSDS")
    public void testReCreateBucket() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            Gson gson = new Gson();
            // Re-create backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_BUCKET_PATH);
                // create the bucket specified in each file
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));

                    // now create buckets
                    int cbCode = getHttpHandler().createBucket(null,
                            bfi, bName, null, "adminTenantId");//signatureKey);
                    assertEquals("Re-create bucket with same name failed:Response code not matched: "
                            , cbCode, 409);
                    Response listBucketResponse = getHttpHandler().getBuckets(null, "adminTenantId");
                    boolean bucketFound = false;
                    try {
                        bucketFound = getHttpHandler()
                                .doesListBucketResponseContainBucketByName(listBucketResponse.body().string(), bName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assertTrue(bucketFound);
                }
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test create bucket with empty name")
    public void testCreateBucketWithEmptyName() {
        System.out.println("Verifying response code: Input (Backend name) with empty value and bucket name is empty");
        File bucketFile = new File(Constant.EMPTY_FIELD_PATH, "bucket_emptyvalue.json");
        String bucketContent = Utils.readFileContentsAsString(bucketFile);
        assertNotNull(bucketContent);
        Gson gson = new Gson();
        CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);
        int cbCode = getHttpHandler().createBucket(null,
                bfi, "", null, "adminTenantId");//signatureKey);
        assertEquals("Bucket name and backend name is empty:Response code not matched: "
                , cbCode, 405);
        Response listBucketResponse = getHttpHandler().getBuckets(null, "adminTenantId");
        boolean bucketFound = false;
        try {
            bucketFound = getHttpHandler()
                    .doesListBucketResponseContainBucketByName(listBucketResponse.body().string(), "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertFalse(bucketFound);

        System.out.println("Verifying response code: In input (Backend name) with not valid value but bucket name is valid");
        File file = new File(Constant.EMPTY_FIELD_PATH, "bucket_b1324.json");
        String content = Utils.readFileContentsAsString(file);
        assertNotNull(content);
        String bName = file.getName().substring(bucketFile.getName().indexOf("_") + 1,
                bucketFile.getName().indexOf("."));

        CreateBucketFileInput input = gson.fromJson(content, CreateBucketFileInput.class);
        int code = getHttpHandler().createBucket(null,
                input, bName, null, "adminTenantId");//signatureKey);
        assertEquals("Backend does not exist:Response code not matched: "
                , code, 404);
    }

    @Test
    @Order(5)
    @DisplayName("Test request body with empty value,try to create backend")
    public void testRequestBodyWithEmptyFieldBackend() {
        Gson gson = new Gson();
        File file = new File(Constant.EMPTY_FIELD_PATH+"ibm-cos_b1321.json");
        String content = Utils.readFileContentsAsString(file);
        assertNotNull(content);

        AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);
        int code = getHttpHandler().addBackend(null,
                "adminTenantId",
                inputHolder);
        assertEquals("Request body with empty value:Response code not matched:",code, 400);
    }

    @Test
    @Order(6)
    @DisplayName("Test uploading object in a bucket")
    public void testUploadObject() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_BUCKET_PATH);
                // Get bucket name.
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);
                    String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));
                    // Get object for upload.
                    File fileRawData = new File(Constant.RAW_DATA_PATH);
                    File[] files = fileRawData.listFiles();
                    String mFileName = null;
                    File mFilePath = null;
                    for (File fileName : files) {
                        mFileName = fileName.getName();
                        mFilePath = fileName;

                        int cbCode = getHttpHandler().uploadObject(null,
                                bucketName, mFileName, mFilePath);
                        assertEquals("Uploaded object failed", cbCode, 200);

                        //Verifying object is uploaded in bucket.
                        Response listObjectResponse = getHttpHandler().getBucketObjects(bucketName);
                        assertEquals("Get list of object failed", listObjectResponse.code(), 200);
                        try {
                            JSONObject jsonObject = XML.toJSONObject(listObjectResponse.body().string());
                            JSONObject jsonObjectListBucket = jsonObject.getJSONObject("ListBucketResult");
                            if (jsonObjectListBucket.has("Contents")) {
                                if (jsonObjectListBucket.get("Contents") instanceof JSONArray){
                                    JSONArray objects = jsonObjectListBucket.getJSONArray("Contents");
                                    for (int i = 0; i < objects.length(); i++) {
                                        assertEquals("Object is not uploaded", objects.getJSONObject(i).get("Key")
                                                , mFileName);
                                    }
                                } else {
                                    assertEquals("Object is not uploaded", jsonObjectListBucket
                                            .getJSONObject("Contents").get("Key"), mFileName);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test uploading object failed scenario")
    public void testUploadObjectFailed(){
        System.out.println("Verifying upload object in not existing bucket");
        File fileRawData = new File(Constant.RAW_DATA_PATH);
        File[] files = fileRawData.listFiles();
        String mFileName = null;
        File mFilePath = null;
        for (File fileName : files) {
            mFileName = fileName.getName();
            mFilePath = fileName;
        }

        int cbCode = getHttpHandler().uploadObject(null,
                "bucketName", mFileName, mFilePath);
        assertEquals("Upload object with non existing bucket: Response code not matched", cbCode, 404);

        System.out.println("Verifying upload object in existing bucket with file name is empty");
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_BUCKET_PATH);
        // Get bucket name.
        for (File bucketFile : listOfIBucketInputs) {
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            int code = getHttpHandler().uploadObject(null,
                    bucketName, "", mFilePath);
            assertEquals("Upload object with existing bucket with file name empty: Response code not matched"
                    , code, 400);
        }
    }

    @Test
    @Order(8)
    @DisplayName("Test downloading object in a folder")
    public void testDownloadObject() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);
                // backend added, now create buckets
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_BUCKET_PATH);
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);
                    String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));
                    // Get object for upload.
                    File fileRawData = new File(Constant.RAW_DATA_PATH);
                    File[] files = fileRawData.listFiles();
                    String mFileName = null;
                    for (File fileName : files) {
                        mFileName = fileName.getName();
                    }
                    File filePath = new File(Constant.DOWNLOAD_FILES_PATH);
                    File downloadedFile = new File(Constant.DOWNLOAD_FILES_PATH,"download_image.jpg");
                    if (filePath.exists()) {
                        if (downloadedFile.exists()) {
                            boolean isDownloadedFileDeleted = downloadedFile.delete();
                            assertTrue(isDownloadedFileDeleted, "Image deleting is failed");
                        } else {
                            assertFalse(downloadedFile.exists());
                        }
                    } else {
                        filePath.mkdirs();
                    }
                    int cbCode = getHttpHandler().downloadObject(null,
                            bucketName, mFileName);
                    assertEquals("Downloading failed", cbCode, 200);
                    assertTrue(downloadedFile.isFile(), "Downloaded Image is not available");
                }
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test verifying download non exist file")
    public void testDownloadNonExistFile() {
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_BUCKET_PATH);
        for (File bucketFile : listOfIBucketInputs) {
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            int cbCode = getHttpHandler().downloadObject(null,
                    bucketName, "23455@###");
            assertEquals("Downloading non exist file: ", cbCode, 404);
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test verifying download file from non exist bucket")
    public void testDownloadFileFromNonExistBucket() {
        File fileRawData = new File(Constant.RAW_DATA_PATH);
        File[] files = fileRawData.listFiles();
        String mFileName = null;
        for (File fileName : files) {
            mFileName = fileName.getName();
        }

        int code = getHttpHandler().downloadObject(null,
                "hfhfhd", mFileName);
        assertEquals("Downloading file from non exist bucket: ", code, 404);
    }

    @Test
    @Order(11)
    @DisplayName("Test verifying download file from non exist bucket and file name")
    public void testDownloadNonExistBucketAndFile() {
        System.out.println("Verifying download file from non exist bucket and file name");
        int responseCode = getHttpHandler().downloadObject(null,
                "ghjhb", "yuyiyh");
        assertEquals("Downloading file from non exist bucket and file name: ", responseCode, 400);
    }

    @Test
    @Order(12)
    @DisplayName("Test verifying backends list and single backend")
    public void testAddBackendGetBackends() throws IOException {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);
                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);

                // Get backend list
                Response response = getHttpHandler().getBackends(null,
                        "adminTenantId");
                assertEquals(response.code(), 200);
                String responseBody = response.body().string();
                assertNotNull(responseBody);
                BackendsInputHolder backendsInputHolder = gson.fromJson(responseBody,
                        BackendsInputHolder.class);
                // Filter backend
                // TODO Bugs: Need to confirm backend name is same.
                List<Backends> backendFilter = backendsInputHolder.getBackends().stream()
                        .filter(p -> !TextUtils.isEmpty(p.getName()))
                        .collect(Collectors.toList());

                List<Backends> backend = backendFilter.stream()
                        .filter(p -> p.getName().equals(inputHolder.getName()))
                        .collect(Collectors.toList());

                assertNotNull(backend);

                // Get backend
                for (int i = 0; i < backend.size(); i++) {
                    Response responseBackend = getHttpHandler().getBackend(null,
                            "adminTenantId", backend.get(i).getId());
                    assertEquals(responseBackend.code(), 200);
                    String responseBackendBody = responseBackend.body().string();
                    assertNotNull(responseBackendBody);
                    Backends backends = gson.fromJson(responseBackendBody, Backends.class);
                    assertNotNull(backends);
                    assertEquals(backends.getName(), inputHolder.getName());
                }
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("Test verifying get backend failed")
    public void testBackendFailed() {
        Response responseBackend = getHttpHandler().getBackend(null,
                "adminTenantId", "reuiu5475");
        assertEquals("Get backend failed:Response code not matched: ", responseBackend.code(), 400);
    }

    @Test
    @Order(14)
    @DisplayName("Test verifying delete non empty backend")
    public void testDeleteNonEmptyBackend() throws IOException {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);
                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);

                // Get backend list
                Response response = getHttpHandler().getBackends(null,
                        "adminTenantId");
                assertEquals(response.code(), 200);
                String responseBody = response.body().string();
                assertNotNull(responseBody);
                BackendsInputHolder backendsInputHolder = gson.fromJson(responseBody,
                        BackendsInputHolder.class);
                // Filter backend
                List<Backends> backendFilter = backendsInputHolder.getBackends().stream()
                        .filter(p -> !TextUtils.isEmpty(p.getName()))
                        .collect(Collectors.toList());

                List<Backends> backend = backendFilter.stream()
                        .filter(p -> p.getName().equals(inputHolder.getName()))
                        .collect(Collectors.toList());
                assertNotNull(backend);

                // Get backend
                for (int i = 0; i < backend.size(); i++) {
                    Response responseBackend = getHttpHandler().getBackend(null,
                            "adminTenantId", backend.get(i).getId());
                    assertEquals(responseBackend.code(), 200);
                    String responseBackendBody = responseBackend.body().string();
                    assertNotNull(responseBackendBody);
                    Backends backends = gson.fromJson(responseBackendBody, Backends.class);
                    assertNotNull(backends);
                    assertEquals(backends.getName(), inputHolder.getName());

                    int responseCode = getHttpHandler().getDeleteBackend(null,
                            "adminTenantId", backend.get(i).getId());
                    assertEquals("Deleting Non empty backend:Response code not matched: ",responseCode, 409);
                }
            }
        }
    }

    //@Test
    @Order(12)
    @DisplayName("Test deleting bucket and object")
    public void testDeleteBucketAndObject() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_BUCKET_PATH);
                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
                        getAuthTokenHolder().getToken().getProject().getId());*/
                // create the bucket specified in each file
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));

                    // Verifying Bucket not empty
                    int responseCode = getHttpHandler().deleteBucketNotEmpty(null,
                            "adminTenantId", bName);//signatureKey);
                    System.out.println("Verifying Bucket not empty: "+responseCode);
                    assertEquals(responseCode, 409);

                    // now delete the object
                    int code = getHttpHandler().deleteObject(null,
                            "adminTenantId", bName, "Screenshot_1.jpg");
                    System.out.println("Verifying object is deleted: "+code);
                    assertEquals(code, 204);

                    // now delete the bucket
                    int dbCode = getHttpHandler().deleteBucket(null,
                            "adminTenantId", bName);//signatureKey);
                    System.out.println("Verifying bucket is deleted: "+dbCode);
                    assertEquals(dbCode, 200);
                }
            }
        }
    }

    //@Test
    @Order(8)
    @DisplayName("Test deleting backend")
    public void testDeleteBackend() throws IOException {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);
                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);

                // Get backend list
                Response response = getHttpHandler().getBackends(null,
                        "adminTenantId");
                assertEquals(response.code(), 200);
                String responseBody = response.body().string();
                assertNotNull(responseBody);
                BackendsInputHolder backendsInputHolder = gson.fromJson(responseBody,
                        BackendsInputHolder.class);
                // Filter backend
                // TODO Bugs: Need to confirm backend name is same.
                List<Backends> backend = backendsInputHolder.getBackends().stream()
                        .filter(p -> p.getName().equals(inputHolder.getName()))
                        .collect(Collectors.toList());
                assertNotNull(backend);

                // Get backend
                for (int i = 0; i < backend.size(); i++) {
                    int responseCode = getHttpHandler().getDeleteBackend(null,
                            "adminTenantId", backend.get(i).getId());
                    assertEquals(responseCode, 500);
                }
            }
        }
    }
}



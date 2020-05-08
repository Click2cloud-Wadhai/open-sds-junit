package main.java.com.opensds.tests;

import com.google.gson.Gson;
import main.java.com.opensds.HttpHandler;
import main.java.com.opensds.TestResultHTMLPrinter;
import main.java.com.opensds.Utils;
import main.java.com.opensds.jsonmodels.inputs.addbackend.AddBackendInputHolder;
import main.java.com.opensds.jsonmodels.inputs.addbackend.Backends;
import main.java.com.opensds.jsonmodels.inputs.addbackend.BackendsInputHolder;
import main.java.com.opensds.jsonmodels.inputs.createbucket.CreateBucketFileInput;
import main.java.com.opensds.jsonmodels.inputs.createlifecycle.AddLifecycleInputHolder;
import main.java.com.opensds.jsonmodels.inputs.createlifecycle.DisplayLifecycleInputHolder;
import main.java.com.opensds.jsonmodels.typesresponse.Type;
import main.java.com.opensds.jsonmodels.typesresponse.TypesHolder;
import main.java.com.opensds.utils.Constant;
import main.java.com.opensds.utils.TextUtils;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestResultHTMLPrinter.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LifecycleTests {
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
    @Order(0)
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
    @Order(1)
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
    @Order(2)
    @DisplayName("Test creating Lifecycle")
    public void testCreateLifecycle() {
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
                    System.out.println(bName);

                    List<File> listOfLifeCycleInputs =
                            Utils.listFilesMatchingBeginsWithPatternInPath("lifecycle",
                                    Constant.CREATE_LIFECYCLE_PATH);
                    String createLifeCycleContent = Utils.readFileContentsAsString(listOfLifeCycleInputs.get(0));
                    assertNotNull(createLifeCycleContent);
                    AddLifecycleInputHolder lifecycleInputHolder = gson.fromJson(createLifeCycleContent, AddLifecycleInputHolder.class);
                    int codeResponse = getHttpHandler().createLifecycle(null,
                            "adminTenantId",lifecycleInputHolder,
                            bName);
                    assertEquals(codeResponse, 200);
                }

            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test creating Lifecycle rule with same name")
    public void testCreateLifecycleSameRule() {
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
                    System.out.println(bName);

                    List<File> listOfLifeCycleInputs =
                            Utils.listFilesMatchingBeginsWithPatternInPath("lifecycle",
                                    Constant.CREATE_LIFECYCLE_PATH);
                    String createLifeCycleContent = Utils.readFileContentsAsString(listOfLifeCycleInputs.get(0));
                    assertNotNull(createLifeCycleContent);
                    AddLifecycleInputHolder lifecycleInputHolder = gson.fromJson(createLifeCycleContent, AddLifecycleInputHolder.class);
                    int codeResponse = getHttpHandler().createLifecycleSameRule(null,
                            "adminTenantId",lifecycleInputHolder,
                            bName);
                    assertEquals("rule already exists.", codeResponse, 409);
                }

            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test creating Lifecycle rule with extended days")
    public void testCreateLifecycleExtendedDays() {
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
                    System.out.println(bName);

                    List<File> listOfLifeCycleInputs =
                            Utils.listFilesMatchingBeginsWithPatternInPath("lifecycle",
                                    Constant.CREATE_LIFECYCLE_PATH);
                    String createLifeCycleContent = Utils.readFileContentsAsString(listOfLifeCycleInputs.get(0));
                    assertNotNull(createLifeCycleContent);
                    AddLifecycleInputHolder lifecycleInputHolder = gson.fromJson(createLifeCycleContent, AddLifecycleInputHolder.class);
                    int codeResponse = getHttpHandler().createLifecycleExtendedDays(null,
                            "adminTenantId",lifecycleInputHolder,
                            bName);
                    assertEquals("minimum days for an object in the current storage class is less before transition action", codeResponse, 403);
                }

            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Listing Lifecycle")
    public void testDisplayLifecycle() {
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
                    System.out.println(bName);

                    List<File> listOfLifeCycleInputs =
                            Utils.listFilesMatchingBeginsWithPatternInPath("lifecycle",
                                    Constant.DISPLAY_LIFECYCLE_PATH);
                    String displayLifeCycleContent = Utils.readFileContentsAsString(listOfLifeCycleInputs.get(0));
                    assertNotNull(displayLifeCycleContent);
                    DisplayLifecycleInputHolder lifecycleInputHolder = gson.fromJson(displayLifeCycleContent, DisplayLifecycleInputHolder.class);
                    int codeResponse = getHttpHandler().displayLifecycle(null,
                            "adminTenantId",lifecycleInputHolder,
                            bName);
                    assertEquals(codeResponse, 200);
                }

            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test deleting lifecycle")
    public void testDeleteLifecycle() throws IOException {
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

                    // now delete the object
                    int code = getHttpHandler().deleteLifecycle(null,
                            "adminTenantId", bName, "abcc");
                    System.out.println("Verifying lifecycle is deleted: "+code);
                    assertEquals(code, 200);

                }
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test deleting bucket and object")
    public void testDeleteBucketAndObject() throws IOException {
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

                    // now delete the object
                    int code = getHttpHandler().deleteObject(null,
                            "adminTenantId", bName, "Screenshot_1.jpg");
                    System.out.println("Verifying object is deleted: "+code);
                    assertEquals(code, 204);
                    Response listObjectResponse = getHttpHandler().getBucketObjects(bName);
                    assertEquals("Get list of object failed", listObjectResponse.code(), 200);
                    JSONObject jsonObject = XML.toJSONObject(listObjectResponse.body().string());
                    JSONObject jsonObjectListBucket = jsonObject.getJSONObject("ListBucketResult");
                    if (jsonObjectListBucket.has("Contents")) {
                        if (jsonObjectListBucket.get("Contents") instanceof JSONArray){
                            JSONArray objects = jsonObjectListBucket.getJSONArray("Contents");
                            for (int i = 0; i < objects.length(); i++) {
                                assertNotEquals(objects.getJSONObject(i).get("Key").equals(bName),"Object is equal");
                            }
                        } else {
                            assertNotEquals(jsonObjectListBucket.getJSONObject("Contents").get("Key").equals(bName),
                                    "Object is equal");
                        }
                    } else {
                        assertFalse(jsonObjectListBucket.has("Contents"));
                    }

                    // now delete the bucket
                    int dbCode = getHttpHandler().deleteBucket(null,
                            "adminTenantId", bName);//signatureKey);
                    System.out.println("Verifying bucket is deleted: "+dbCode);
                    assertEquals(dbCode, 200);
                }
            }
        }
    }

    @Test
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
                List<Backends> backendFilter = backendsInputHolder.getBackends().stream()
                        .filter(p -> !TextUtils.isEmpty(p.getName()))
                        .collect(Collectors.toList());

                List<Backends> backend = backendFilter.stream()
                        .filter(p -> p.getName().equals(inputHolder.getName()))
                        .collect(Collectors.toList());
                assertNotNull(backend);

                // Get backend
                for (int i = 0; i < backend.size(); i++) {
                    int responseCode = getHttpHandler().getDeleteBackend(null,
                            "adminTenantId", backend.get(i).getId());
                    assertEquals(responseCode, 200);
                }
            }
        }
    }

}

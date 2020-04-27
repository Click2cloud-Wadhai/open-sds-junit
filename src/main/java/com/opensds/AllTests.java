
package main.java.com.opensds;

import com.google.gson.Gson;
import main.java.com.opensds.jsonmodels.inputs.addbackend.AddBackendInputHolder;
import main.java.com.opensds.jsonmodels.inputs.createbucket.CreateBucketFileInput;
import main.java.com.opensds.jsonmodels.typesresponse.Type;
import main.java.com.opensds.jsonmodels.typesresponse.TypesHolder;
import main.java.com.opensds.utils.Constant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    @DisplayName("Creating bucket on OPENSDS using IBM backend")
    public void testCreateBucket() {
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

                    //delete bucket
//                    int dbCode = getHttpHandler().deleteBucket(null,"adminTenantId"
//                           , bName);//signatureKey);
//                   System.out.println(dbCode);
//                   assertEquals(dbCode, 200);
                }
            }
        }

    }

    @Test
    @DisplayName("Uploading object in a bucket on OPENSDS")
    public void testUploadObject() {
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
                // create the bucket specified in each file
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
                    }
                    int cbCode = getHttpHandler().uploadObject(null,
                            bucketName, mFileName, mFilePath);
                    assertEquals("Upload object failed", cbCode, 200);
                }
            }
        }
    }

//        for (Type t : getTypesHolder().getTypes()) {
//            List<File> listOfIInputsForType =
//                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
//                            "C:/Users/puja.domke/IdeaProjects/osdsjunit/inputs/addbackend");
//            Gson gson = new Gson();
//            // add the backend specified in each file
//            for (File file : listOfIInputsForType) {
//                String content = Utils.readFileContentsAsString(file);
//                assertNotNull(content);
//
//                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);
//                int code = getHttpHandler().addBackend(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                        getAuthTokenHolder().getToken().getProject().getId(),
//                        inputHolder);
//                assertEquals(code, 200);
//
//                Response lbr = getHttpHandler().getBackends(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                        getAuthTokenHolder().getToken().getProject().getId());
//                assertEquals(lbr.code(), 200);
//                try {
//                    ListBackendResponse listBackendResponse = gson.fromJson(lbr.body().string(), ListBackendResponse.class);
//                    boolean found = false;
//                    String backendId = null;
//                    // check the newly created backend, is listed in the output of listBackends
//                    for (Backend b : listBackendResponse.getBackends()) {
//                        if (b.getName().equals(inputHolder.getName())) {
//                            backendId = b.getId();
//                            found = true;
//                            break;
//
//                        }}
//                   assertTrue(found);
    // now, delete the backend, to restore system to original state
//                   Response delbr = getHttpHandler().deleteBackend(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                           getAuthTokenHolder().getToken().getProject().getId(),
//                            backendId);
//                    assertEquals(delbr.code(), 200);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }}}}

//    @Test
//    @DisplayName("Create a bucket, then test delete bucket")
//    public void testDeleteBucketForAllTypes() {
//        // load input files for each type and create the backend
//        for (Type t : getTypesHolder().getTypes()) {
//            List<File> listOfIInputsForType =
//                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
//                            "C:/Users/puja.domke/IdeaProjects/osdsjunit/inputs/createbucket");
//            Gson gson = new Gson();
//            // add the backend specified in each file
//            for (File file : listOfIInputsForType) {
//                String content = Utils.readFileContentsAsString(file);
//                assertNotNull(content);
//
//                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);
//                int code = getHttpHandler().addBackend(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                        getAuthTokenHolder().getToken().getProject().getId(),
//                        inputHolder);
//                assertEquals(code, 200);
//
//                // backend added, now create buckets
//                List<File> listOfIBucketInputs =
//                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
//                                "C:/Users/puja.domke/IdeaProjects/osdsjunit/inputs/deletebucket");
//                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                        getAuthTokenHolder().getToken().getProject().getId());*/
//                // create the bucket specified in each file
//                for (File bucketFile : listOfIBucketInputs) {
//                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
//                    assertNotNull(bucketContent);
//
//                    CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);
//
//                    // filename format is "bucket_<bucketname>.json", get the bucket name here
//                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
//                            bucketFile.getName().indexOf("."));
//
//                    // now create buckets
//                    int cbCode = getHttpHandler().createBucket(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                            bfi, bName, null, getAuthTokenHolder().getToken().getProject().getId());//signatureKey);
//                    System.out.println(cbCode);
//                    assertEquals(cbCode, 200);
//
//                    // now delete the bucket
//                    int dbCode = getHttpHandler().deleteBucket(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                            getAuthTokenHolder().getToken().getProject().getId(), bName);//signatureKey);
//                    System.out.println(cbCode);
//                    assertEquals(cbCode, 200);
//                }
//            }
//        }
//
//    }}

}



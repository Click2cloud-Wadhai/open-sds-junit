
package main.java.com.opensds;

import com.google.gson.Gson;
import main.java.com.opensds.jsonmodels.inputs.addbackend.AddBackendInputHolder;
import main.java.com.opensds.jsonmodels.inputs.createbucket.CreateBucketFileInput;
import main.java.com.opensds.jsonmodels.typesresponse.Type;
import main.java.com.opensds.jsonmodels.typesresponse.TypesHolder;
import main.java.com.opensds.utils.Constant;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.*;
import java.util.List;

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
                    }
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

    @Test
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
    @DisplayName("Test deleting bucket and object")
    public void testDeleteBucketForAllTypes() {
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
}


